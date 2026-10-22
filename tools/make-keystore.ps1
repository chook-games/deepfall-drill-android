param(
    [string]$Root = (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path))
)

$ErrorActionPreference = "Stop"
Set-Location $Root

$keystore = Join-Path $Root "upload-keystore.jks"
$props = Join-Path $Root "keystore.properties"

if (Test-Path $keystore) {
    Write-Host "upload-keystore.jks already exists - not overwriting." -ForegroundColor Yellow
    if (-not (Test-Path $props)) {
        Write-Host "keystore.properties is missing. Restore it from your backup." -ForegroundColor Red
    }
    exit 0
}

$javaHome = "C:\Program Files\Android\Android Studio\jbr"
$keytool = Join-Path $javaHome "bin\keytool.exe"
if (-not (Test-Path $keytool)) { throw "keytool not found at $keytool" }

# Alphanumeric only so it survives shell/gradle quoting.
$chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".ToCharArray()
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 28
$rng.GetBytes($bytes)
$sb = New-Object System.Text.StringBuilder
foreach ($b in $bytes) { [void]$sb.Append($chars[$b % $chars.Length]) }
$password = $sb.ToString()

& $keytool -genkeypair `
    -keystore $keystore `
    -alias deepfall `
    -keyalg RSA `
    -keysize 4096 `
    -validity 10000 `
    -storepass $password `
    -keypass $password `
    -dname "CN=Deepfall Drill, OU=Games, O=Deepfall Games, L=Athens, C=GR"

if ($LASTEXITCODE -ne 0) { throw "keytool failed" }

@"
storeFile=upload-keystore.jks
storePassword=$password
keyAlias=deepfall
keyPassword=$password
"@ | Set-Content -Path $props -Encoding ASCII

Write-Host ""
Write-Host "Upload key created: $keystore" -ForegroundColor Green
Write-Host "Credentials:        $props" -ForegroundColor Green
Write-Host ""
Write-Host "BACK BOTH FILES UP NOW (password manager / encrypted drive)." -ForegroundColor Yellow
Write-Host "Never commit them. Losing this key means requesting an upload-key reset in Play Console." -ForegroundColor Yellow
Write-Host ""
Write-Host "Next: .\gradlew.bat bundleRelease" -ForegroundColor Cyan
