#Requires -Version 7.3
# Stores the GitHub token that Jenkins uses to create draft releases (scripts/github-release.sh).
# Create the token first: GitHub > Settings > Developer settings > Fine-grained tokens, only the
# repository shreyanshVIT23/java-properties-diff, permission "Contents: Read and write".
# Details: java-properties-diff/.claude/notes/github-token.md
# Run from anywhere: .\jenkinsgithubsetup.ps1   (again to replace an expired token)
$ErrorActionPreference = 'Stop'
$jenkins = Join-Path $PSScriptRoot 'java-properties-diff\jenkins'
$envFile = Join-Path $jenkins '.env'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "$envFile does not exist. Run .\jenkinssetup.ps1 first."
}

$token = Read-Host 'GitHub token' -AsSecureString | ConvertFrom-SecureString -AsPlainText
if (-not $token.Trim()) { throw 'No token entered; nothing changed.' }

# Keep every other line of .env, replace an existing GITHUB_TOKEN line
$lines = @(Get-Content -LiteralPath $envFile | Where-Object { $_ -and $_ -notmatch '^GITHUB_TOKEN=' })
$lines += "GITHUB_TOKEN=$($token.Trim())"
# LF line endings and no BOM, like the rest of .env
($lines -join "`n") + "`n" | Set-Content -NoNewline -LiteralPath $envFile
Remove-Variable token, lines

# chmod 600 equivalent: no inherited access, only the current user
icacls $envFile /inheritance:r /grant:r "${env:USERNAME}:F" | Out-Null
Write-Output "Saved GITHUB_TOKEN in $envFile."
# Full path, so the command works from any folder
Write-Output "Apply it: docker compose -f `"$(Join-Path $jenkins 'docker-compose.yml')`" up -d --build"
