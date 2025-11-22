# Build all OpenGnosis service Docker images locally
param(
    [string]$Version = "latest",
    [string]$Registry = "localhost:5000"
)

$ErrorActionPreference = "Stop"

$Services = @(
    "gnosis-iam",
    "gnosis-api-gateway",
    "gnosis-structure",
    "gnosis-scheduler",
    "gnosis-journal-command",
    "gnosis-analytics-query",
    "gnosis-notifier"
)

Write-Host "Building OpenGnosis Docker images..." -ForegroundColor Green
Write-Host "Version: $Version"
Write-Host "Registry: $Registry"
Write-Host ""

foreach ($Service in $Services) {
    Write-Host "Building $Service..." -ForegroundColor Cyan
    
    docker build `
        --build-arg SERVICE_NAME=$Service `
        -t "$Registry/${Service}:$Version" `
        -t "$Registry/${Service}:latest" `
        -f service.Dockerfile `
        ..
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ $Service built successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ Failed to build $Service" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

Write-Host "All images built successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "To push images to registry, run:" -ForegroundColor Yellow
foreach ($Service in $Services) {
    Write-Host "  docker push $Registry/${Service}:$Version"
}
