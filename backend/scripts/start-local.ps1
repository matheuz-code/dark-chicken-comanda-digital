[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$workspaceRoot = (Resolve-Path (Join-Path $backendRoot '..')).Path
$environmentScript = Join-Path $workspaceRoot '.local-data\dev-env.ps1'
$mysqlConfig = Join-Path $workspaceRoot '.local-data\mysql\my.ini'

if (-not (Test-Path -LiteralPath $environmentScript)) {
    throw 'Ambiente ainda não configurado. Execute .\scripts\setup-local.ps1 primeiro.'
}

. $environmentScript
$env:DEBUG = 'false'

$mysqlAdmin = Join-Path $env:MYSQL_HOME 'bin\mysqladmin.exe'
$mysqlReady = $false
try {
    & $mysqlAdmin --host=127.0.0.1 --port=3306 --connect-timeout=1 --user=root ping *> $null
    $mysqlReady = $LASTEXITCODE -eq 0
} catch {
    $mysqlReady = $false
}

if (-not $mysqlReady) {
    Start-Process `
        -FilePath (Join-Path $env:MYSQL_HOME 'bin\mysqld.exe') `
        -ArgumentList "--defaults-file=`"$mysqlConfig`"", '--console' `
        -RedirectStandardOutput (Join-Path $workspaceRoot '.local-data\mysql\mysql-stdout.log') `
        -RedirectStandardError (Join-Path $workspaceRoot '.local-data\mysql\mysql-stderr.log') `
        -WindowStyle Hidden | Out-Null

    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Milliseconds 500
        try {
            & $mysqlAdmin --host=127.0.0.1 --port=3306 --connect-timeout=1 --user=root ping *> $null
            $mysqlReady = $LASTEXITCODE -eq 0
        } catch {
            $mysqlReady = $false
        }
        if ($mysqlReady) {
            break
        }
    }

    if (-not $mysqlReady) {
        throw 'O MySQL não ficou disponível na porta 3306 dentro do tempo esperado.'
    }
}

Set-Location $backendRoot
& mvn spring-boot:run
