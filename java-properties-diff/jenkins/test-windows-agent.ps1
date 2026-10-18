#Requires -Version 7.3
# Tests for windows-agent.ps1. It dot-sources the script, so these checks use the real functions.
# The last checks ask Jenkins about the node, so run the agent first (in another terminal).
#
# Usage: pwsh jenkins/test-windows-agent.ps1
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'windows-agent.ps1')

$script:failures = 0
function Pass([string] $Description) { Write-Output "ok   - $Description" }
function Fail([string] $Description) { Write-Output "FAIL - $Description"; $script:failures++ }

# Assert-Throw <script block> <text> <description>: the block throws a message containing text
function Assert-Throw([scriptblock] $Block, [string] $Text, [string] $Description) {
    try {
        & $Block
        Fail "${Description}: no error"
    } catch {
        if ("$_".Contains($Text)) { Pass $Description } else { Fail "${Description}: wrong error '$_'" }
    }
}

$work = Join-Path ([IO.Path]::GetTempPath()) "propdiff-agent-test-$([guid]::NewGuid())"
$saved = @{}
Get-ChildItem env:GIT_CONFIG_* | ForEach-Object { $saved[$_.Name] = $_.Value }
try {
    # --- git mapping: the controller's file:///repo must resolve to this checkout ---
    $settings = Get-RepoGitConfig (Get-RepoRoot)
    if ($settings['core.longpaths'] -eq 'true') { Pass 'git settings enable core.longpaths' }
    else { Fail 'git settings lack core.longpaths=true' }

    Set-GitConfigEnvironment $settings
    $heads = git ls-remote --heads -- file:///repo 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0 -and $heads.Contains('refs/heads/')) {
        Pass 'file:///repo lists the branches of this checkout'
    } else {
        Fail "file:///repo does not resolve to this checkout: $heads"
    }

    # --- Java checks ---
    $oldJdk = Join-Path $work 'old-jdk'
    New-Item -ItemType Directory -Force (Join-Path $oldJdk 'bin') | Out-Null
    $fakeJava = Join-Path $oldJdk 'bin\java.cmd'
    Set-Content -LiteralPath $fakeJava "@echo     java.specification.version = 17 1>&2"
    New-Item -ItemType File (Join-Path $oldJdk 'bin\jpackage.exe') | Out-Null
    Assert-Throw -Block { Assert-Jdk $oldJdk $fakeJava } -Text 'needs Java 21' -Description 'Java 17 is rejected for the agent'

    $noJpackage = Join-Path $work 'jre'
    New-Item -ItemType Directory -Force (Join-Path $noJpackage 'bin') | Out-Null
    $fakeJre = Join-Path $noJpackage 'bin\java.cmd'
    Set-Content -LiteralPath $fakeJre "@echo     java.specification.version = 26 1>&2"
    Assert-Throw -Block { Assert-Jdk $noJpackage $fakeJre } -Text 'No jpackage.exe' -Description 'a Java without jpackage is rejected'

    Assert-Throw -Block { Assert-Jdk (Join-Path $work 'missing') } -Text 'No Java at' -Description 'a missing JAVA_HOME is rejected'

    try {
        Assert-Jdk $env:JAVA_HOME
        Pass "JAVA_HOME ($env:JAVA_HOME) can run the agent and build the zip"
    } catch {
        Fail "JAVA_HOME is not usable: $_"
    }

    # --- the node in Jenkins ---
    $token = Join-Path $PSScriptRoot '.secrets\api-token'
    # -g: the [name] in the URL is Jenkins' tree syntax, not a curl range
    $json = curl.exe -fsSg --config $token `
        "$JenkinsUrl/computer/$NodeName/api/json?tree=offline,numExecutors,assignedLabels[name]"
    if ($LASTEXITCODE -ne 0) {
        Fail "Jenkins has no node $NodeName (is Jenkins up with the new casc.yaml?)"
    } else {
        $node = $json | ConvertFrom-Json
        if ($node.assignedLabels.name -contains 'windows') { Pass "$NodeName has the label windows" }
        else { Fail "$NodeName lacks the label windows" }
        if ($node.numExecutors -eq 1) { Pass "$NodeName has 1 executor" }
        else { Fail "$NodeName has $($node.numExecutors) executors, expected 1" }
        if (-not $node.offline) { Pass "$NodeName is online" }
        else { Fail "$NodeName is offline: start pwsh jenkins/windows-agent.ps1 first" }
    }
} finally {
    Remove-Item -Recurse -Force -LiteralPath $work -ErrorAction SilentlyContinue
    Get-ChildItem env:GIT_CONFIG_* | ForEach-Object { Remove-Item "env:$($_.Name)" }
    $saved.GetEnumerator() | ForEach-Object { Set-Item "env:$($_.Key)" $_.Value }
}

if ($script:failures -eq 0) {
    Write-Output 'windows agent test passed'
} else {
    Write-Output "windows agent test FAILED: $script:failures check(s)"
}
exit $script:failures
