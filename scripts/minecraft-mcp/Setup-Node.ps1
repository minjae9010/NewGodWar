$ErrorActionPreference = 'Stop'
$runtimeRoot = Join-Path $PSScriptRoot '../../.build/node'
$archiveName = 'node-v22.23.4-win-x64.zip'
$expectedHash = 'cbc74b5e0982c9dbb41525fd8fb5dcd32fbee237863f7829072f4639a1ce5f20'
$nodePath = Join-Path $runtimeRoot 'node-v22.23.4-win-x64/node.exe'
New-Item -ItemType Directory -Force $runtimeRoot | Out-Null
$archivePath = Join-Path $runtimeRoot $archiveName
if (!(Test-Path -LiteralPath $archivePath)) {
    Invoke-WebRequest "https://nodejs.org/dist/v22.23.4/$archiveName" -OutFile $archivePath
}
if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
    throw 'Node archive SHA-256 mismatch. Remove the invalid archive and retry.'
}
Expand-Archive -LiteralPath $archivePath -DestinationPath $runtimeRoot -Force
$version = & $nodePath --version
if ($LASTEXITCODE -ne 0 -or $version -ne 'v22.23.4') { throw 'Unexpected Node runtime version.' }
Write-Output "Project Node ready: $version ($nodePath)"
