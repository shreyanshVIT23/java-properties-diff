#Requires -Version 7.3
# One-time Jenkins setup on Windows (PowerShell version of jenkinssetup.sh): the agent SSH key
# pair and jenkins/.env. Run from anywhere: .\jenkinssetup.ps1
$ErrorActionPreference = 'Stop'
$jenkins = Join-Path $PSScriptRoot 'java-properties-diff\jenkins'
$secrets = Join-Path $jenkins '.secrets'
$key = Join-Path $secrets 'agent_key'
$envFile = Join-Path $jenkins '.env'

# A new key would no longer match the running agent; delete the old files to really redo setup
if ((Test-Path $key) -or (Test-Path $envFile)) {
    throw "Already set up: $key or $envFile exists. Delete them first to start over."
}

New-Item -ItemType Directory -Force $secrets | Out-Null
# PowerShell 7.3+ passes '' to native programs as a real empty argument (no passphrase)
ssh-keygen -t ed25519 -N '' -C propdiff-jenkins-agent -f $key
if ($LASTEXITCODE -ne 0) { throw 'ssh-keygen failed' }

$pw = Read-Host 'Jenkins admin password' -AsSecureString | ConvertFrom-SecureString -AsPlainText
$pub = (Get-Content -Raw "$key.pub").Trim()
# LF line endings and no BOM, like printf in the .sh version
"JENKINS_ADMIN_PASSWORD=$pw`nAGENT_SSH_PUBKEY=$pub`n" | Set-Content -NoNewline $envFile
Remove-Variable pw

# chmod 600 equivalent: no inherited access, only the current user
icacls $envFile /inheritance:r /grant:r "${env:USERNAME}:F" | Out-Null
Write-Host "Created $key, $key.pub and $envFile"
