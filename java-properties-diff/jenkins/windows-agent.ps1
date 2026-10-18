#Requires -Version 7.3
# Connects this Windows PC to the local Jenkins as the inbound agent "windows-agent" (label
# "windows"), so the Jenkinsfile's Windows cell can build the Windows zip. Leave it running;
# Ctrl+C disconnects. Setup and background: ../.claude/windows-ci.md
#
# Usage: pwsh jenkins/windows-agent.ps1 [-PrintGitConfig]
#   -PrintGitConfig  only print the git settings the agent's builds get, then exit
#
# The functions are also dot-sourced by test-windows-agent.ps1, so both use the same logic.
[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSReviewUnusedParameter', '',
    Justification = 'The functions below read these parameters through script scope')]
param(
    [switch] $PrintGitConfig,
    [string] $JenkinsUrl = 'http://localhost:8080',
    [string] $NodeName = 'windows-agent',
    # Short on purpose: multibranch workspaces plus Maven/jpackage output nest deep, and Windows
    # paths are limited to 260 characters
    [string] $WorkDir = 'C:\ja'
)
$ErrorActionPreference = 'Stop'

# The agent process runs Jenkins code, which needs Java 21+
$MinimumAgentJava = 21
# How long to wait for a starting Jenkins: attempts, 5 seconds apart
$StartupWaitAttempts = 36

<#
.SYNOPSIS
The Java feature version (17, 21, 26, ...) of a java executable.
#>
function Get-JavaMajorVersion([string] $JavaCommand) {
    # -XshowSettings prints to stderr; java.specification.version is the plain feature number
    $settings = & $JavaCommand -XshowSettings:properties -version 2>&1 | Out-String
    if ($settings -match 'java\.specification\.version = (\d+)') {
        return [int] $Matches[1]
    }
    throw "Cannot tell the Java version of $JavaCommand"
}

<#
.SYNOPSIS
Fails unless JavaHome can both run the agent (Java 21+) and build the zip (has jpackage).
#>
function Assert-Jdk([string] $JavaHome, [string] $JavaCommand = (Join-Path $JavaHome 'bin\java.exe')) {
    if (-not $JavaHome -or -not (Test-Path -LiteralPath $JavaCommand)) {
        throw "No Java at '$JavaHome'. Set JAVA_HOME to a JDK $MinimumAgentJava+ (Scoop: scoop install temurin-jdk)."
    }
    $major = Get-JavaMajorVersion $JavaCommand
    if ($major -lt $MinimumAgentJava) {
        throw "Java $major at '$JavaHome' is too old: the Jenkins agent needs Java $MinimumAgentJava or newer."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\jpackage.exe'))) {
        throw "No jpackage.exe in '$JavaHome\bin': the Windows zip is built with jpackage, so JAVA_HOME must be a full JDK."
    }
}

<#
.SYNOPSIS
Git settings for the agent's builds, as key/value pairs.
.DESCRIPTION
Jenkins knows the repository as file:///repo, the path of the read-only mount inside the
containers. On this PC the same repository is the git root of this checkout, so builds rewrite
that URL to it. core.longpaths lets git create paths over 260 characters.
#>
function Get-RepoGitConfig([string] $RepoRoot) {
    # rev-parse gives forward slashes (C:/Users/...), as a file:// URL needs
    [ordered]@{
        "url.file:///$RepoRoot.insteadOf" = 'file:///repo'
        'core.longpaths' = 'true'
    }
}

<#
.SYNOPSIS
Applies git settings to this process and its children only, through GIT_CONFIG_COUNT and
GIT_CONFIG_KEY_n / GIT_CONFIG_VALUE_n. The user's global git config is not touched.
#>
function Set-GitConfigEnvironment {
    [Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSUseShouldProcessForStateChangingFunctions', '',
        Justification = 'Only sets environment variables of this process; -WhatIf would add nothing')]
    param([System.Collections.IDictionary] $Settings)

    $n = 0
    foreach ($entry in $Settings.GetEnumerator()) {
        Set-Item "env:GIT_CONFIG_KEY_$n" $entry.Key
        Set-Item "env:GIT_CONFIG_VALUE_$n" $entry.Value
        $n++
    }
    $env:GIT_CONFIG_COUNT = "$n"
}

<#
.SYNOPSIS
The git root of this checkout, with forward slashes.
#>
function Get-RepoRoot {
    $root = git -C $PSScriptRoot rev-parse --show-toplevel
    if ($LASTEXITCODE -ne 0) { throw "$PSScriptRoot is not inside a git checkout" }
    $root.Trim()
}

<#
.SYNOPSIS
Reads the agent's secret from its jenkins-agent.jnlp file, authenticated with the API token.
#>
function Get-AgentSecret([string] $TokenFile) {
    if (-not (Test-Path -LiteralPath $TokenFile)) {
        throw "No API token at $TokenFile. Create one (Jenkins > your user > Security > API Token) and run jenkinshooksetup.ps1."
    }
    $url = "$JenkinsUrl/computer/$NodeName/jenkins-agent.jnlp"
    $file = New-TemporaryFile
    try {
        # Jenkins answers 503 while it starts (e.g. right after docker compose up), so wait for it
        for ($attempt = 1; ; $attempt++) {
            # The token stays in the curl config file instead of on the command line
            $status = curl.exe -s --config $TokenFile -o $file -w '%{http_code}' $url
            if ($status -ne '503' -or $attempt -ge $StartupWaitAttempts) { break }
            if ($attempt -eq 1) { Write-Output 'Jenkins is starting; waiting for it...' }
            Start-Sleep -Seconds 5
        }
        switch ($status) {
            '200' { }
            '000' { throw "Jenkins is not reachable at $JenkinsUrl. Start it: docker compose -f jenkins\docker-compose.yml up -d" }
            '401' { throw "Jenkins rejected the API token in $TokenFile. Create a new one and run jenkinshooksetup.ps1." }
            '404' { throw "Jenkins has no node $NodeName. Rebuild it so casc.yaml is applied: docker compose -f jenkins\docker-compose.yml up -d --build" }
            '503' { throw "Jenkins is still starting after $($StartupWaitAttempts * 5) seconds. Check: docker compose -f jenkins\docker-compose.yml logs controller" }
            default { throw "Cannot read ${url}: HTTP $status" }
        }
        # The first application argument is the secret
        @(([xml] (Get-Content -Raw -LiteralPath $file)).jnlp.'application-desc'.argument)[0]
    } finally {
        Remove-Item -LiteralPath $file -ErrorAction SilentlyContinue
    }
}

function Start-WindowsAgent {
    [Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSUseShouldProcessForStateChangingFunctions', '',
        Justification = 'Interactive entry point that starts the agent; -PrintGitConfig is its dry run')]
    param()

    if (-not $IsWindows) {
        Write-Output 'This is not a Windows machine, so there is no Windows agent to start.'
        Write-Output 'Jenkins skips the Windows build on branches; tag builds need this script running on Windows.'
        exit 1
    }

    $settings = Get-RepoGitConfig (Get-RepoRoot)
    if ($PrintGitConfig) {
        $settings.GetEnumerator() | ForEach-Object { Write-Output "$($_.Key)=$($_.Value)" }
        return
    }

    Assert-Jdk $env:JAVA_HOME
    $java = Join-Path $env:JAVA_HOME 'bin\java.exe'

    $secrets = Join-Path $PSScriptRoot '.secrets'
    $secretFile = Join-Path $secrets "$NodeName-secret"
    Get-AgentSecret (Join-Path $secrets 'api-token') | Set-Content -NoNewline -LiteralPath $secretFile
    # chmod 600 equivalent: no inherited access, only the current user
    icacls $secretFile /inheritance:r /grant:r "${env:USERNAME}:F" | Out-Null

    # Fresh every start, so agent.jar always matches the controller's version
    New-Item -ItemType Directory -Force $WorkDir | Out-Null
    $agentJar = Join-Path $WorkDir 'agent.jar'
    Invoke-WebRequest "$JenkinsUrl/jnlpJars/agent.jar" -OutFile $agentJar

    # Builds inherit this environment: the git URL mapping and JAVA_HOME (also the build JDK)
    Set-GitConfigEnvironment $settings

    Write-Output "Connecting $NodeName to $JenkinsUrl (Ctrl+C to stop)"
    # -secret @file keeps the secret out of the process list; -webSocket uses port 8080 only
    # --enable-native-access: the agent loads JNA, which Java 24+ warns about otherwise (JEP 472)
    & $java --enable-native-access=ALL-UNNAMED -jar $agentJar -url "$JenkinsUrl/" -name $NodeName -secret "@$secretFile" `
        -webSocket -workDir $WorkDir
    exit $LASTEXITCODE
}

# Run only when executed, not when dot-sourced by the test
if ($MyInvocation.InvocationName -ne '.') {
    Start-WindowsAgent
}
