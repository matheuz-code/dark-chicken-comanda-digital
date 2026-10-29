[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$workspaceRoot = (Resolve-Path (Join-Path $backendRoot '..')).Path
$toolsRoot = Join-Path $workspaceRoot '.local-tools'
$dataRoot = Join-Path $workspaceRoot '.local-data'
$downloadsRoot = Join-Path $toolsRoot 'downloads'

$mavenVersion = '3.9.16'
$mysqlVersion = '8.4.11'
$mysqlArchiveMd5 = '2e833921898a9a030ea6bfe81bd811bc'

$jdkRoot = Join-Path $toolsRoot 'jdk-17'
$mavenRoot = Join-Path $toolsRoot "apache-maven-$mavenVersion"
$mysqlRoot = Join-Path $toolsRoot "mysql-$mysqlVersion-winx64"
$mysqlDataRoot = Join-Path $dataRoot 'mysql'
$mysqlDataDirectory = Join-Path $mysqlDataRoot 'data'
$mysqlConfig = Join-Path $mysqlDataRoot 'my.ini'
$environmentScript = Join-Path $dataRoot 'dev-env.ps1'

New-Item -ItemType Directory -Force -Path $toolsRoot, $dataRoot, $downloadsRoot, $mysqlDataRoot | Out-Null

function Get-OfficialArchive {
    param(
        [Parameter(Mandatory)] [string] $Uri,
        [Parameter(Mandatory)] [string] $Destination
    )

    if (Test-Path -LiteralPath $Destination) {
        return
    }

    Write-Host "Baixando $Uri"
    Invoke-WebRequest -Uri $Uri -OutFile $Destination -UseBasicParsing
}

if (-not (Test-Path -LiteralPath (Join-Path $jdkRoot 'bin\java.exe'))) {
    $jdkArchive = Join-Path $downloadsRoot 'temurin-jdk-17.zip'
    Get-OfficialArchive `
        -Uri 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' `
        -Destination $jdkArchive

    $jdkExtractRoot = Join-Path $toolsRoot 'jdk-17-extracted'
    if (Test-Path -LiteralPath $jdkExtractRoot) {
        Remove-Item -LiteralPath $jdkExtractRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Path $jdkExtractRoot | Out-Null
    Expand-Archive -LiteralPath $jdkArchive -DestinationPath $jdkExtractRoot -Force
    $jdkExtractedDirectory = Get-ChildItem -LiteralPath $jdkExtractRoot -Directory | Select-Object -First 1
    Move-Item -LiteralPath $jdkExtractedDirectory.FullName -Destination $jdkRoot
    Remove-Item -LiteralPath $jdkExtractRoot -Recurse -Force
}

if (-not (Test-Path -LiteralPath (Join-Path $mavenRoot 'bin\mvn.cmd'))) {
    $mavenArchive = Join-Path $downloadsRoot "apache-maven-$mavenVersion-bin.zip"
    $mavenChecksumFile = "$mavenArchive.sha512"
    Get-OfficialArchive `
        -Uri "https://dlcdn.apache.org/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip" `
        -Destination $mavenArchive
    Get-OfficialArchive `
        -Uri "https://dlcdn.apache.org/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip.sha512" `
        -Destination $mavenChecksumFile

    $expectedMavenHash = (Get-Content -Raw -LiteralPath $mavenChecksumFile).Trim().Split(' ')[0].ToUpperInvariant()
    $actualMavenHash = (Get-FileHash -LiteralPath $mavenArchive -Algorithm SHA512).Hash
    if ($actualMavenHash -ne $expectedMavenHash) {
        throw 'O checksum SHA-512 do Maven não corresponde ao publicado pela Apache.'
    }
    Expand-Archive -LiteralPath $mavenArchive -DestinationPath $toolsRoot -Force
}

if (-not (Test-Path -LiteralPath (Join-Path $mysqlRoot 'bin\mysqld.exe'))) {
    $mysqlArchive = Join-Path $downloadsRoot "mysql-$mysqlVersion-winx64.zip"
    Get-OfficialArchive `
        -Uri "https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-$mysqlVersion-winx64.zip" `
        -Destination $mysqlArchive

    $actualMysqlHash = (Get-FileHash -LiteralPath $mysqlArchive -Algorithm MD5).Hash.ToLowerInvariant()
    if ($actualMysqlHash -ne $mysqlArchiveMd5) {
        throw 'O checksum MD5 do MySQL não corresponde ao publicado pela Oracle.'
    }
    Expand-Archive -LiteralPath $mysqlArchive -DestinationPath $toolsRoot -Force
}

$mysqlRootForConfig = $mysqlRoot.Replace('\', '/')
$mysqlDataForConfig = $mysqlDataDirectory.Replace('\', '/')
$mysqlConfigContent = @"
[mysqld]
basedir=$mysqlRootForConfig
datadir=$mysqlDataForConfig
port=3306
bind-address=127.0.0.1
character-set-server=utf8mb4
collation-server=utf8mb4_0900_ai_ci
mysqlx=0
default-time-zone=-03:00
log-error=$($mysqlDataRoot.Replace('\', '/'))/mysql-error.log
pid-file=$($mysqlDataRoot.Replace('\', '/'))/mysql.pid
"@
Set-Content -LiteralPath $mysqlConfig -Value $mysqlConfigContent -Encoding ASCII

if (-not (Test-Path -LiteralPath (Join-Path $mysqlDataDirectory 'mysql'))) {
    New-Item -ItemType Directory -Force -Path $mysqlDataDirectory | Out-Null
    Write-Host 'Inicializando os dados do MySQL...'
    & (Join-Path $mysqlRoot 'bin\mysqld.exe') "--defaults-file=$mysqlConfig" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao inicializar o MySQL. Código: $LASTEXITCODE"
    }
}

$mysqlAdmin = Join-Path $mysqlRoot 'bin\mysqladmin.exe'
$mysqlClient = Join-Path $mysqlRoot 'bin\mysql.exe'
$mysqlServer = Join-Path $mysqlRoot 'bin\mysqld.exe'

function Test-MySqlReady {
    try {
        & $mysqlAdmin --host=127.0.0.1 --port=3306 --connect-timeout=1 --user=root ping *> $null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

if (-not (Test-MySqlReady)) {
    Write-Host 'Iniciando o MySQL local...'
    Start-Process `
        -FilePath $mysqlServer `
        -ArgumentList "--defaults-file=`"$mysqlConfig`"", '--console' `
        -RedirectStandardOutput (Join-Path $mysqlDataRoot 'mysql-stdout.log') `
        -RedirectStandardError (Join-Path $mysqlDataRoot 'mysql-stderr.log') `
        -WindowStyle Hidden | Out-Null

    $mysqlReady = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Milliseconds 500
        if (Test-MySqlReady) {
            $mysqlReady = $true
            break
        }
    }
    if (-not $mysqlReady) {
        throw "O MySQL não iniciou. Consulte $mysqlDataRoot\mysql-error.log"
    }
}

if (Test-Path -LiteralPath $environmentScript) {
    . $environmentScript
    $databasePassword = $env:DB_PASSWORD
    $jwtSecret = $env:JWT_SECRET
} else {
    $randomBytes = New-Object byte[] 24
    $randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $randomGenerator.GetBytes($randomBytes)
    $randomGenerator.Dispose()
    $databasePassword = ([BitConverter]::ToString($randomBytes)).Replace('-', '')
    $jwtSecret = $null
}

if ([string]::IsNullOrWhiteSpace($jwtSecret)) {
    $jwtRandomBytes = New-Object byte[] 48
    $jwtRandomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $jwtRandomGenerator.GetBytes($jwtRandomBytes)
    $jwtRandomGenerator.Dispose()
    $jwtSecret = [Convert]::ToBase64String($jwtRandomBytes)
}

$databaseSql = @"
CREATE DATABASE IF NOT EXISTS comanda_digital CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'comanda_app'@'localhost' IDENTIFIED BY '$databasePassword';
ALTER USER 'comanda_app'@'localhost' IDENTIFIED BY '$databasePassword';
CREATE USER IF NOT EXISTS 'comanda_app'@'127.0.0.1' IDENTIFIED BY '$databasePassword';
ALTER USER 'comanda_app'@'127.0.0.1' IDENTIFIED BY '$databasePassword';
GRANT ALL PRIVILEGES ON comanda_digital.* TO 'comanda_app'@'localhost';
GRANT ALL PRIVILEGES ON comanda_digital.* TO 'comanda_app'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
$databaseSql | & $mysqlClient --host=127.0.0.1 --port=3306 --user=root
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao criar o banco e o usuário local. Código: $LASTEXITCODE"
}

$environmentContent = @"
`$env:JAVA_HOME = '$jdkRoot'
`$env:MAVEN_HOME = '$mavenRoot'
`$env:MYSQL_HOME = '$mysqlRoot'
`$env:PATH = "`$env:JAVA_HOME\bin;`$env:MAVEN_HOME\bin;`$env:MYSQL_HOME\bin;`$env:PATH"
`$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/comanda_digital?serverTimezone=America/Asuncion&useSSL=false&allowPublicKeyRetrieval=true'
`$env:DB_USERNAME = 'comanda_app'
`$env:DB_PASSWORD = '$databasePassword'
`$env:CORS_ALLOWED_ORIGINS = 'http://localhost:4200'
`$env:JWT_SECRET = '$jwtSecret'
`$env:DEBUG = 'false'
"@
Set-Content -LiteralPath $environmentScript -Value $environmentContent -Encoding UTF8

. $environmentScript
Write-Host ''
Write-Host 'Ambiente local configurado com sucesso.' -ForegroundColor Green
& java -version
& mvn -version
& $mysqlClient --host=127.0.0.1 --port=3306 --user=comanda_app "--password=$databasePassword" --execute 'SELECT VERSION() AS mysql_version, DATABASE() AS banco;' comanda_digital
