[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$workspaceRoot = (Resolve-Path (Join-Path $backendRoot '..')).Path
$environmentScript = Join-Path $workspaceRoot '.local-data\dev-env.ps1'

if (-not (Test-Path -LiteralPath $environmentScript)) {
    Write-Host 'O ambiente local ainda não foi configurado.'
    exit 0
}

. $environmentScript
& (Join-Path $env:MYSQL_HOME 'bin\mysqladmin.exe') --host=127.0.0.1 --port=3306 --user=root shutdown

