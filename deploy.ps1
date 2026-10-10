# BusGuard AI Automated Deployment Package Script

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host " Packaging BusGuard AI System for Production Deployment" -ForegroundColor Cyan
Write-Host "========================================================="

# Run full compilation and test verification
& .\build.ps1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed. Cannot create deployment package." -ForegroundColor Red
    exit 1
}

# Create dist directory
if (Test-Path dist) {
    Remove-Item -Recurse -Force dist\*
} else {
    New-Item -ItemType Directory -Path dist | Out-Null
}

# Copy compiled bin, data, resources, and configuration
Copy-Item -Recurse bin dist/
Copy-Item -Recurse data dist/
Copy-Item run.ps1 dist/
Copy-Item README.md dist/
Copy-Item Dockerfile dist/

Write-Host "=========================================================" -ForegroundColor Green
Write-Host " Deployment Package Ready in dist/ folder!" -ForegroundColor Green
Write-Host " 1. Local/Server Execution: cd dist; java -cp bin com.busguard.BusGuardApp" -ForegroundColor Yellow
Write-Host " 2. Docker Container Build: docker build -t busguard-ai ." -ForegroundColor Yellow
Write-Host " 3. Docker Container Run:   docker run -p 8080:8080 busguard-ai" -ForegroundColor Yellow
Write-Host "========================================================="
