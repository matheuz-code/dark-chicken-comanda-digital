# Execute este script a partir da pasta raiz do projeto, apos criar um repositorio VAZIO no GitHub.
# O script NAO cria um repositorio no GitHub e NAO faz deploy.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host 'Git nao encontrado. Instale o Git for Windows antes de continuar.' -ForegroundColor Red
    exit 1
}
if (-not (Test-Path '.gitignore')) {
    throw 'Arquivo .gitignore ausente; publicacao interrompida.'
}
$unsafe = @(Get-ChildItem -Force -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '(\\|/)(\.git|node_modules|dist|target|\.angular)(\\|/)' -and $_.Name -match '^\.env($|\.(?!example$))|\.pem$|\.p12$|\.key$' })
if ($unsafe.Count -gt 0) {
    Write-Host 'Arquivos sensiveis encontrados; remova-os antes de publicar:' -ForegroundColor Red
    $unsafe | ForEach-Object { Write-Host $_.FullName }
    exit 1
}
Write-Host 'Crie PRIMEIRO um repositorio vazio em https://github.com/new (sem README e sem .gitignore).'
$repoUrl = Read-Host 'Cole a URL HTTPS do repositorio vazio (https://github.com/usuario/repositorio.git)'
if ($repoUrl -notmatch '^https://github\.com/[A-Za-z0-9-]+/[A-Za-z0-9_.-]+(\.git)?$') {
    throw 'URL invalida. Esperado: https://github.com/usuario/repositorio.git'
}
if (-not (Test-Path '.git')) { git init }
git branch -M main
git add .
Write-Host 'Arquivos preparados para commit:'
git status --short
$confirm = Read-Host 'Conferiu os arquivos? Digite PUBLICAR para fazer commit e push'
if ($confirm -cne 'PUBLICAR') { Write-Host 'Cancelado sem publicar.'; exit 0 }
git commit -m 'Organiza projeto Dark Chicken SRS para portfolio'
if ($LASTEXITCODE -ne 0) { throw 'Commit nao concluido. Configure nome/email do Git ou confira o status.' }
$existing = git remote get-url origin 2>$null
if ($LASTEXITCODE -eq 0 -and $existing) { git remote set-url origin $repoUrl } else { git remote add origin $repoUrl }
git push -u origin main
if ($LASTEXITCODE -ne 0) { throw 'Push nao concluido. Verifique autenticacao e permissoes do GitHub.' }
Write-Host "Repositorio publicado: $repoUrl" -ForegroundColor Green
