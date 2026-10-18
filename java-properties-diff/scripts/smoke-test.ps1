#Requires -Version 7
# Smoke test for the packaged Windows propdiff release zip, as a user would get it.
# Windows twin of scripts/smoke-test.sh.
#
# Usage (from the project directory): pwsh scripts/smoke-test.ps1 target/propdiff-<version>-windows-x64.zip
#
# Unzips into a temporary directory, then runs the bundled propdiff.exe with PATH emptied so it
# cannot fall back to an installed Java. Exits with the number of failed checks.
param(
    [Parameter(Mandatory)] [string] $Zip
)
$ErrorActionPreference = 'Stop'

$fixtures = Join-Path $PSScriptRoot '..\src\test\resources\fixtures'
$left = Join-Path $fixtures 'left.properties'
$right = Join-Path $fixtures 'right.properties'
$work = Join-Path ([IO.Path]::GetTempPath()) "propdiff-smoke-$([guid]::NewGuid())"
$script:failures = 0
$script:output = ''

function Pass([string] $Description) { Write-Output "ok   - $Description" }

function Fail([string] $Description, [string] $Detail = '') {
    Write-Output "FAIL - $Description"
    if ($Detail) { $Detail -split "`r?`n" | ForEach-Object { Write-Output "       $_" } }
    $script:failures++
}

# Runs propdiff.exe with an empty PATH. Output is decoded as UTF-8, which is what the box
# characters must be when the output goes to a file or pipe.
function Invoke-Propdiff([string[]] $Arguments) {
    $info = [Diagnostics.ProcessStartInfo]::new($script:bin)
    foreach ($argument in $Arguments) { $info.ArgumentList.Add($argument) }
    $info.Environment['PATH'] = ''
    $info.UseShellExecute = $false
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = [Text.UTF8Encoding]::new($false)
    $info.StandardErrorEncoding = [Text.UTF8Encoding]::new($false)
    $process = [Diagnostics.Process]::Start($info)
    # Read both streams at once so a full pipe buffer can't block the process
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    [pscustomobject]@{ Exit = $process.ExitCode; Out = $stdout.Result; Err = $stderr.Result }
}

# Check <expected exit code> <description> <propdiff arguments>. Exit codes 0 and 1 are
# successful runs, so they must print nothing on stderr (catches launcher noise).
function Check([int] $Expected, [string] $Description, [string[]] $Arguments) {
    $result = Invoke-Propdiff $Arguments
    if ($result.Exit -ne $Expected) {
        Fail "${Description}: exit $($result.Exit), expected $Expected" ($result.Out + $result.Err)
    } elseif ($Expected -le 1 -and $result.Err) {
        Fail "${Description}: unexpected output on stderr" $result.Err
    } else {
        Pass $Description
    }
    # Later Contains checks look at both streams, as a user would see them
    $script:output = $result.Out + $result.Err
}

# Contains <text> <description>: the output of the previous Check contains the text
function Contains([string] $Text, [string] $Description) {
    if ($script:output.Contains($Text)) { Pass $Description } else { Fail "${Description}: output lacks '$Text'" }
}

try {
    try {
        Expand-Archive -LiteralPath $Zip -DestinationPath $work
    } catch {
        Write-Output "FAIL - cannot unzip ${Zip}: $_"
        exit 1
    }
    $script:bin = Join-Path $work 'propdiff\propdiff.exe'
    if (-not (Test-Path -LiteralPath $script:bin)) {
        Write-Output "FAIL - $script:bin is missing"
        exit 1
    }

    Check -Expected 0 -Description '--version runs on the bundled runtime (no Java on PATH)' -Arguments @('--version')
    Contains 'propdiff' '--version names the program'

    Check -Expected 1 -Description 'differences exit with 1' -Arguments @('--no-pager', '--color', 'never', $left, $right)
    Contains 'changed (2)' 'unified output lists the changed keys'
    # `u{22EE} is the vertical ellipsis and `u{2502} the box line of the gutter (escaped: ASCII file)
    Contains "   3 `u{22EE}     `u{2502} - app.version=1.0" 'line numbers and UTF-8 box characters survive packaging'

    Check -Expected 1 -Description 'side-by-side works' -Arguments @('--no-pager', '--color', 'never', '-s', '--width', '80', $left, $right)

    Check -Expected 0 -Description 'identical files exit with 0' -Arguments @('--no-pager', $left, $left)
    Contains 'no differences' 'identical files say no differences'

    Check -Expected 2 -Description 'a missing file exits with 2' -Arguments @('--no-pager', $left, 'missing.properties')
    Check -Expected 2 -Description 'an unknown option exits with 2' -Arguments @('--bogus')

    # README.txt ships next to propdiff.exe and names the same version as the zip file
    $readme = Join-Path $work 'propdiff\README.txt'
    $version = if ((Split-Path -Leaf $Zip) -match '^propdiff-(.+)-windows-x64\.zip$') { $Matches[1] } else { '' }
    if (-not (Test-Path -LiteralPath $readme)) {
        Fail 'propdiff/README.txt is missing from the zip'
    } else {
        $text = Get-Content -Raw -Encoding utf8 -LiteralPath $readme
        if ($text.Contains('@VERSION@')) {
            Fail 'README.txt still contains the @VERSION@ placeholder'
        } elseif ($version -and -not $text.Contains("propdiff $version")) {
            Fail "README.txt does not mention 'propdiff $version'"
        } else {
            Pass "README.txt is included and names version $version"
        }
    }
} finally {
    Remove-Item -Recurse -Force -LiteralPath $work -ErrorAction SilentlyContinue
}

if ($script:failures -eq 0) {
    Write-Output 'smoke test passed'
} else {
    Write-Output "smoke test FAILED: $script:failures check(s)"
}
exit $script:failures
