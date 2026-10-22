param(
    [switch]$Clean
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$jdk21 = "C:\Users\George\.jdks\ms-21.0.12.1"
if (Test-Path $jdk21) {
    $env:JAVA_HOME = $jdk21
} else {
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
}

if (-not (Test-Path (Join-Path $Root "keystore.properties"))) {
    throw "keystore.properties is missing. Run tools\make-keystore.ps1 first."
}
if (-not (Test-Path (Join-Path $Root "upload-keystore.jks"))) {
    throw "upload-keystore.jks is missing. Restore it from your backup."
}

$tasks = @()
if ($Clean) { $tasks += "clean" }
$tasks += @("bundleRelease", "assembleRelease", "--no-daemon")

Write-Host "Building signed release..." -ForegroundColor Cyan
& (Join-Path $Root "gradlew.bat") @tasks
if ($LASTEXITCODE -ne 0) { throw "Release build failed" }

$bundle = Join-Path $Root "app\build\outputs\bundle\release\app-release.aab"
$apk = Join-Path $Root "app\build\outputs\apk\release\app-release.apk"
$mapping = Join-Path $Root "app\build\outputs\mapping\release\mapping.txt"

Write-Host ""
Write-Host "AAB (upload this to Play Console):" -ForegroundColor Green
Write-Host ("  {0}  ({1:N1} KB)" -f $bundle, ((Get-Item $bundle).Length / 1KB))
Write-Host "Release APK (sideload test):" -ForegroundColor Green
Write-Host ("  {0}  ({1:N1} KB)" -f $apk, ((Get-Item $apk).Length / 1KB))
if (Test-Path $mapping) {
    Write-Host "R8 mapping (keep per release):" -ForegroundColor Yellow
    Write-Host "  $mapping"
}
Write-Host ""
Write-Host "Reminder: bump versionCode/versionName in app/build.gradle before every upload." -ForegroundColor Yellow
