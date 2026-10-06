[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [switch]$ConsumerOnly
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
if (!(Test-Path $adb)) { throw 'ANDROID_HOME must identify the installed Android SDK.' }
if ($Serial -notmatch '^127\.0\.0\.1:\d+$|^emulator-\d+$') { throw 'An explicitly named isolated emulator is required.' }
$qemu = & $adb -s $Serial shell getprop ro.kernel.qemu
if ($LASTEXITCODE -ne 0 -or "$qemu".Trim() -ne '1') { throw 'The selected serial is not an emulator.' }

function Install-Apk([string]$RelativePath) {
    $installPath = Join-Path $repoRoot $RelativePath
    $lastOutput = @()
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $lastOutput = & $adb -s $Serial install -r $installPath 2>&1
        if ($LASTEXITCODE -eq 0) {
            $lastOutput | Write-Output
            return
        }
        # ADB can lose its local server between the two sequential suites; retry
        # only that emulator transport failure and retain the final diagnostic.
        if (($lastOutput -join "`n") -notmatch '(?i)cannot connect to daemon|could not read ok from ADB Server') { break }
        & $adb start-server 2>&1 | Out-Null
        Start-Sleep -Milliseconds 500
    }
    $lastOutput | Write-Output
    throw "Synthetic test APK install failed: $RelativePath"
}
function Run-Instrumentation([string]$Package) {
    $output = & $adb -s $Serial shell am instrument -w "$Package/androidx.test.runner.AndroidJUnitRunner" 2>&1
    $exitCode = $LASTEXITCODE
    $output | Write-Output
    # am instrument may return zero even when the tests failed.
    if ($exitCode -ne 0 -or ($output -join "`n") -notmatch 'OK \(\d+ tests?\)' -or
        ($output -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
        throw "Synthetic instrumentation failed: $Package"
    }
}
if ($ConsumerOnly) {
    Install-Apk 'integration-tests\consumer\app\build\outputs\apk\localRuntime\app-localRuntime.apk'
    Install-Apk 'integration-tests\consumer\app\build\outputs\apk\androidTest\localRuntime\app-localRuntime-androidTest.apk'
    Run-Instrumentation 'org.sakos.camera.consumer.test'
} else {
    Install-Apk 'capture-video\build\outputs\apk\androidTest\debug\capture-video-debug-androidTest.apk'
    Run-Instrumentation 'org.sakos.camera.capture.video.test'
    Install-Apk 'safety-opennsfw2\build\outputs\apk\androidTest\debug\safety-opennsfw2-debug-androidTest.apk'
    Run-Instrumentation 'org.sakos.camera.safety.opennsfw2.test'
    # Reset only this synthetic sample when present; a fresh emulator has nothing to uninstall.
    $installedPackages = & $adb -s $Serial shell pm list packages org.sakos.camera.sample 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not inspect the isolated emulator package state.'
    }
    if (($installedPackages -join "`n") -match '(?m)^package:org\.sakos\.camera\.sample$') {
        $uninstallOutput = & $adb -s $Serial uninstall org.sakos.camera.sample 2>&1
        if ($LASTEXITCODE -ne 0) {
            $uninstallOutput | Write-Output
            throw 'Synthetic sample reset failed.'
        }
    }
    Install-Apk 'sample-app\build\outputs\apk\debug\sample-app-debug.apk'
    Install-Apk 'sample-app\build\outputs\apk\androidTest\debug\sample-app-debug-androidTest.apk'
    Run-Instrumentation 'org.sakos.camera.sample.test'
}
Write-Output 'Synthetic emulator instrumentation: OK (no accuracy or physical-device claim)'
