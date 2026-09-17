[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

& .\gradlew.bat --no-daemon `
    :safety-core:publishReleasePublicationToSakosLocalRepository `
    :safety-opennsfw2:publishReleasePublicationToSakosLocalRepository `
    :capture-camerax:publishReleasePublicationToSakosLocalRepository `
    :capture-video:publishReleasePublicationToSakosLocalRepository
if ($LASTEXITCODE -ne 0) {
    throw "Local Maven publication failed with exit code $LASTEXITCODE."
}

& .\gradlew.bat --no-daemon -p integration-tests\consumer :app:assembleRelease
if ($LASTEXITCODE -ne 0) {
    throw "Separate minified consumer build failed with exit code $LASTEXITCODE."
}

Write-Output 'Local Maven consumer verification: OK'
