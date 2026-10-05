$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskPreviousAccount = $env:CLOUDFLARE_ACCOUNT_ID
Push-Location $taskRoot
try {
    $taskStatus = git status --porcelain
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect the Git working tree.' }
    if ($taskStatus) { throw 'Commit the site changes before deploying so the upload has a clean source revision.' }
    $taskRevision = git rev-parse HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Cannot identify the site source revision.' }

    $taskAssets = @('index.html', 'camera/index.html', 'docs/camera/index.html', 'assets/site.css')
    $taskUpload = Join-Path $taskRoot 'build/site-upload'
    New-Item -ItemType Directory -Path $taskUpload -Force | Out-Null
    foreach ($taskAsset in $taskAssets) {
        $taskDestination = Join-Path $taskUpload $taskAsset
        New-Item -ItemType Directory -Path (Split-Path -Parent $taskDestination) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $taskRoot "website/$taskAsset") -Destination $taskDestination -Force
    }
    $taskUnexpected = Get-ChildItem -LiteralPath $taskUpload -Recurse -File | Where-Object {
        [IO.Path]::GetRelativePath($taskUpload, $_.FullName).Replace('\', '/') -notin $taskAssets
    }
    if ($taskUnexpected) { throw 'The site upload directory contains files outside the four intended static assets.' }

    if ([string]::IsNullOrWhiteSpace($env:SAKOS_CLOUDFLARE_ACCOUNT_ID)) {
        throw 'Set SAKOS_CLOUDFLARE_ACCOUNT_ID before deployment.'
    }
    if ($env:SAKOS_CLOUDFLARE_ACCOUNT_ID -notmatch '^[0-9a-fA-F]{32}$') {
        throw 'SAKOS_CLOUDFLARE_ACCOUNT_ID must contain 32 hexadecimal characters.'
    }
    $env:CLOUDFLARE_ACCOUNT_ID = $env:SAKOS_CLOUDFLARE_ACCOUNT_ID
    & wrangler pages deploy $taskUpload --project-name sakosproject --branch main --commit-hash $taskRevision --commit-dirty=false
    if ($LASTEXITCODE -ne 0) { throw "Pages deployment failed with exit code $LASTEXITCODE." }
}
finally {
    $env:CLOUDFLARE_ACCOUNT_ID = $taskPreviousAccount
    Pop-Location
}
