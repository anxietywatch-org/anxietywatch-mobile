[CmdletBinding()]
param(
    [string]$Serial,
    [switch]$VerifyDataPreservation,
    [switch]$CleanupTestPackage
)

$ErrorActionPreference = "Stop"

$TargetPackage = "com.anxietywatch.mobile"
$TestPackage = "com.anxietywatch.mobile.test"
$Runner = "androidx.test.runner.AndroidJUnitRunner"
$TestApkRelativePath = "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
$MarkerRelativePath = "files/stateful_test_marker"

$scriptsRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptsRoot
$gradlePath = Join-Path $repoRoot "gradlew.bat"
$testApk = Join-Path $repoRoot $TestApkRelativePath

function Invoke-CheckedProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [Parameter(Mandatory = $false)]
        [string[]]$Arguments = @(),
        [Parameter(Mandatory = $true)]
        [string]$Description,
        [switch]$ReturnOutput
    )

    Write-Host $Description
    $output = @(& $FilePath @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        throw "$Description failed with exit code $exitCode."
    }
    if ($ReturnOutput) {
        return $output
    }
}

function Invoke-AdbChecked {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    return Invoke-CheckedProcess -FilePath $adbPath -Arguments $Arguments -Description $Description -ReturnOutput
}

function Get-DeviceSerials {
    $lines = @(& $adbPath devices 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to query adb devices."
    }
    $deviceSerials = @()
    foreach ($line in $lines) {
        if ($line -match '^\s*([^\s]+)\s+device\s*$') {
            $deviceSerials += $Matches[1]
        }
    }
    return $deviceSerials
}

try {
    if (-not (Test-Path -LiteralPath $gradlePath)) {
        throw "Repository root was not resolved correctly: gradlew.bat is missing."
    }
    if (-not (Test-Path -LiteralPath $testApk)) {
        # The APK is checked again after the build; this only catches an invalid root early.
        Write-Host "Test APK is not built yet; it will be produced by the build steps."
    }

    $adbCommand = Get-Command adb -ErrorAction Stop
    $adbPath = $adbCommand.Source
    if ([string]::IsNullOrWhiteSpace($adbPath)) {
        $adbPath = $adbCommand.Path
    }

    $markerCreated = $false

    Write-Host "STATEFUL Android instrumentation"
    Write-Host "Target app data will be preserved."
    Write-Host "This script does NOT use connectedDebugAndroidTest, pm clear, or target uninstall."

    $availableDevices = @(Get-DeviceSerials)
    if ($Serial) {
        if ($availableDevices -notcontains $Serial) {
            throw "Requested serial '$Serial' is not connected in state 'device'."
        }
        $device = $Serial
    } elseif ($availableDevices.Count -eq 1) {
        $device = $availableDevices[0]
    } elseif ($availableDevices.Count -eq 0) {
        throw "No adb device is connected in state 'device'. Provide -Serial after connecting one."
    } else {
        throw "Multiple adb devices are connected. Provide -Serial explicitly."
    }

    Push-Location $repoRoot
    try {
        Invoke-CheckedProcess -FilePath $gradlePath -Arguments @(":app:assembleDebug", "--console=plain") -Description "[1/6] Building target"
        Invoke-CheckedProcess -FilePath $gradlePath -Arguments @(":app:assembleDebugAndroidTest", "--console=plain") -Description "[2/6] Building androidTest"
        Invoke-CheckedProcess -FilePath $gradlePath -Arguments @(":app:installDebug", "--console=plain") -Description "[3/6] Installing target"

        $targetPath = Invoke-AdbChecked -Arguments @("-s", $device, "shell", "pm", "path", $TargetPackage) -Description "Checking target package"
        if (-not ($targetPath -match 'package:')) {
            throw "Target package '$TargetPackage' is not installed after installDebug."
        }

        if ($VerifyDataPreservation) {
            Invoke-AdbChecked -Arguments @("-s", $device, "shell", "run-as", $TargetPackage, "mkdir", "-p", "files") -Description "Creating sentinel directory"
            $markerCommand = "run-as $TargetPackage sh -c 'printf stateful > $MarkerRelativePath'"
            Invoke-AdbChecked -Arguments @("-s", $device, "shell", $markerCommand) -Description "Creating data-preservation sentinel"
            $markerCreated = $true
            $markerCheck = Invoke-AdbChecked -Arguments @("-s", $device, "shell", "run-as", $TargetPackage, "ls", $MarkerRelativePath) -Description "Checking sentinel before tests"
            if (-not ($markerCheck -match [regex]::Escape($MarkerRelativePath))) {
                throw "The data-preservation sentinel could not be verified before tests."
            }
        }

        if (-not (Test-Path -LiteralPath $testApk)) {
            throw "Test APK was not produced: $testApk"
        }
        Invoke-AdbChecked -Arguments @("-s", $device, "install", "-r", $testApk) -Description "[4/6] Installing test APK only"

        $testPath = Invoke-AdbChecked -Arguments @("-s", $device, "shell", "pm", "path", $TestPackage) -Description "Checking test package"
        if (-not ($testPath -match 'package:')) {
            throw "Test package '$TestPackage' is not installed."
        }

        $instrumentationArguments = @(
            "-s", $device,
            "shell", "am", "instrument", "-w", "-r",
            "-e", "debug", "false",
            "$TestPackage/$Runner"
        )
        $instrumentationOutput = Invoke-AdbChecked -Arguments $instrumentationArguments -Description "[5/6] Running instrumentation"
        if ($instrumentationOutput -match 'FAILURES!!!|INSTRUMENTATION_CODE:\s*0\b') {
            throw "Instrumentation reported test failures."
        }
        $summaryMatch = [regex]::Match(($instrumentationOutput -join "`n"), 'OK \((\d+) tests?\)')
        if (-not $summaryMatch.Success) {
            throw "Instrumentation did not report a successful test summary."
        }
        $testCount = $summaryMatch.Groups[1].Value
        Write-Host "Instrumentation result: $testCount tests passed."

        $targetAfter = Invoke-AdbChecked -Arguments @("-s", $device, "shell", "pm", "path", $TargetPackage) -Description "[6/6] Verifying target preservation"
        if (-not ($targetAfter -match 'package:')) {
            throw "Target package disappeared after instrumentation."
        }
        if ($VerifyDataPreservation) {
            $markerAfter = Invoke-AdbChecked -Arguments @("-s", $device, "shell", "run-as", $TargetPackage, "ls", $MarkerRelativePath) -Description "Verifying data-preservation sentinel"
            if (-not ($markerAfter -match [regex]::Escape($MarkerRelativePath))) {
                throw "The data-preservation sentinel disappeared after instrumentation."
            }
        }

        if ($CleanupTestPackage) {
            Invoke-AdbChecked -Arguments @("-s", $device, "uninstall", $TestPackage) -Description "Cleaning up test package only"
            $targetAfterCleanup = Invoke-AdbChecked -Arguments @("-s", $device, "shell", "pm", "path", $TargetPackage) -Description "Verifying target after test-package cleanup"
            if (-not ($targetAfterCleanup -match 'package:')) {
                throw "Target package disappeared after test-package cleanup."
            }
            if ($VerifyDataPreservation) {
                $markerAfterCleanup = Invoke-AdbChecked -Arguments @("-s", $device, "shell", "run-as", $TargetPackage, "ls", $MarkerRelativePath) -Description "Verifying sentinel after test-package cleanup"
                if (-not ($markerAfterCleanup -match [regex]::Escape($MarkerRelativePath))) {
                    throw "The data-preservation sentinel disappeared after test-package cleanup."
                }
            }
        }

        if ($VerifyDataPreservation) {
            Invoke-AdbChecked -Arguments @("-s", $device, "shell", "run-as", $TargetPackage, "rm", "-f", $MarkerRelativePath) -Description "Removing sentinel only"
        }
        Write-Host "Stateful instrumentation completed successfully."
        exit 0
    } finally {
        Pop-Location
    }
} catch {
    if ($VerifyDataPreservation -and $markerCreated -and $device) {
        # Best-effort cleanup; preserve the original failure and never touch app data.
        & $adbPath "-s" $device "shell" "run-as" $TargetPackage "rm" "-f" $MarkerRelativePath 2>&1 | Out-Null
    }
    Write-Error $_.Exception.Message
    exit 1
}
