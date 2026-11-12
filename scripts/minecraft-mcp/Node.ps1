# Keep CLI tools and their process.execPath children on the project runtime.
$ErrorActionPreference = 'Stop'
$nodePath = Join-Path $PSScriptRoot '../../.build/node/node-v22.23.4-win-x64/node.exe'
if (!(Test-Path -LiteralPath $nodePath)) {
    throw 'Run scripts/minecraft-mcp/Setup-Node.ps1 first.'
}
& $nodePath @args
exit $LASTEXITCODE
