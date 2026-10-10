# BusGuard AI Launch Script

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host " Building & Launching BusGuard AI System (Java 21)..." -ForegroundColor Cyan
Write-Host "========================================================="

# Stop existing running instances if any
$existingProcs = Get-WmiObject Win32_Process | Where-Object { $_.CommandLine -like "*com.busguard.BusGuardApp*" }
if ($existingProcs) {
    Write-Host "Stopping existing BusGuard server process(es)..." -ForegroundColor Yellow
    foreach ($proc in $existingProcs) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 1
}

& .\build.ps1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed. Cannot launch server." -ForegroundColor Red
    exit 1
}

Write-Host "Starting BusGuard AI Server..." -ForegroundColor Green
& java -cp bin com.busguard.BusGuardApp
