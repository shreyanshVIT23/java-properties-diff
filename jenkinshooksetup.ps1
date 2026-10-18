#Requires -Version 7.3
# Installs the git hook that asks Jenkins to scan on every commit or tag (PowerShell version of
# jenkinshooksetup.sh). Create a token first: Jenkins > your user > Security > API Token.
# Run from anywhere: .\jenkinshooksetup.ps1
$ErrorActionPreference = 'Stop'
$project = Join-Path $PSScriptRoot 'java-properties-diff'
$tokenFile = Join-Path $project 'jenkins\.secrets\api-token'
$hook = Join-Path $project 'scripts\git-hooks\reference-transaction'
$installed = Join-Path $PSScriptRoot '.git\hooks\reference-transaction'

# The hook reads the token as a curl config file, so it never appears in the process list
$t = Read-Host 'Jenkins API token' -AsSecureString | ConvertFrom-SecureString -AsPlainText
"user = `"admin:$t`"`n" | Set-Content -NoNewline $tokenFile
Remove-Variable t
icacls $tokenFile /inheritance:r /grant:r "${env:USERNAME}:F" | Out-Null

# chmod +x is not needed: Git for Windows runs the hook with its sh because of the #!/bin/sh line
Remove-Item -Force $installed -ErrorAction SilentlyContinue
try {
    # A symbolic link needs Developer Mode or an administrator terminal
    New-Item -ItemType SymbolicLink -Path $installed -Target $hook | Out-Null
    Write-Host "Linked $installed -> $hook"
} catch {
    Copy-Item $hook $installed
    Write-Warning "No permission for a symbolic link, so the hook was copied. Run this script again after changing $hook."
}
