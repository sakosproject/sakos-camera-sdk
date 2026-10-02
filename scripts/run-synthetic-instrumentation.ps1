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
    & $adb -s $Serial install -r (Join-Path $repoRoot $RelativePath)
    if ($LASTEXITCODE -ne 0) { throw "Synthetic test APK install failed: $RelativePath" }
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
    # Reset only this synthetic sample's state, making denied-permission coverage repeatable.
    $uninstallOutput = & $adb -s $Serial uninstall org.sakos.camera.sample 2>&1
    if ($LASTEXITCODE -ne 0 -and ($uninstallOutput -join "`n") -notmatch '(?i)not installed') {
        throw 'Synthetic sample reset failed.'
    }
    Install-Apk 'sample-app\build\outputs\apk\debug\sample-app-debug.apk'
    Install-Apk 'sample-app\build\outputs\apk\androidTest\debug\sample-app-debug-androidTest.apk'
    Run-Instrumentation 'org.sakos.camera.sample.test'
}
Write-Output 'Synthetic emulator instrumentation: OK (no accuracy or physical-device claim)'
