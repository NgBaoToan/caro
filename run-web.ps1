param([int]$Port = 8088)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & mvn --batch-mode --no-transfer-progress package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
    $env:PORT = "$Port"
    & java '-Djava.awt.headless=true' -cp target/caroai.jar caroai.web.WebServer
} finally { Pop-Location }
