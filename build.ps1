# BusGuard AI Automated PowerShell Build & Test Script

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host " Compiling BusGuard AI System (Java 21)..." -ForegroundColor Cyan
Write-Host "========================================================="

# Clean bin output directory
if (Test-Path bin) {
    Remove-Item -Recurse -Force bin\*
} else {
    New-Item -ItemType Directory -Path bin | Out-Null
}

# Find all Java source files
$sources = Get-ChildItem -Path src/main/java -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$tests = Get-ChildItem -Path src/test/java -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

Write-Host "Compiling $($sources.Count) main Java source files..." -ForegroundColor Yellow
& javac -d bin -encoding UTF-8 $sources
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed for main sources!" -ForegroundColor Red
    exit 1
}

Write-Host "Compiling $($tests.Count) test Java files..." -ForegroundColor Yellow
& javac -d bin -cp bin -encoding UTF-8 $tests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed for test files!" -ForegroundColor Red
    exit 1
}

Write-Host "Compilation SUCCESSFUL!" -ForegroundColor Green
Write-Host "Running Test Suite..." -ForegroundColor Cyan

& java -cp bin com.busguard.TestRunner
if ($LASTEXITCODE -ne 0) {
    Write-Host "Test Suite Failed!" -ForegroundColor Red
    exit 1
}

Write-Host "All Tests PASSED Successfully!" -ForegroundColor Green
