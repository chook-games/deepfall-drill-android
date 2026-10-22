param(
    [switch]$Install,
    [switch]$Run,
    [switch]$Release
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

$Sdk = "$env:LOCALAPPDATA\Android\Sdk"
$BuildTools = Join-Path $Sdk "build-tools\36.0.0"
$AndroidJar = Join-Path $Sdk "platforms\android-36\android.jar"
$JavaHome = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_HOME = $JavaHome
$Java = Join-Path $JavaHome "bin\java.exe"
$Javac = Join-Path $JavaHome "bin\javac.exe"
$Jar = Join-Path $JavaHome "bin\jar.exe"
$Aapt2 = Join-Path $BuildTools "aapt2.exe"
$D8 = Join-Path $BuildTools "d8.bat"
$ZipAlign = Join-Path $BuildTools "zipalign.exe"
$ApkSigner = Join-Path $BuildTools "apksigner.bat"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"

foreach ($tool in @($Aapt2, $D8, $ZipAlign, $ApkSigner, $AndroidJar, $Javac)) {
    if (-not (Test-Path $tool)) { throw "Missing build tool: $tool" }
}

$Build = Join-Path $Root "build"
$Classes = Join-Path $Build "classes"
$Gen = Join-Path $Build "gen"
$Dex = Join-Path $Build "dex"
$Dist = Join-Path $Root "dist"
foreach ($d in @($Build, $Dist)) { New-Item -ItemType Directory -Force -Path $d | Out-Null }
if (Test-Path $Classes) { Remove-Item -Recurse -Force $Classes }
if (Test-Path $Gen) { Remove-Item -Recurse -Force $Gen }
if (Test-Path $Dex) { Remove-Item -Recurse -Force $Dex }
New-Item -ItemType Directory -Force -Path $Classes, $Gen, $Dex | Out-Null

$ResDir = Join-Path $Root "app\src\main\res"
$ManifestSrc = Join-Path $Root "app\src\main\AndroidManifest.xml"
$Manifest = Join-Path $Build "AndroidManifest.xml"
$ManifestXml = [System.IO.File]::ReadAllText($ManifestSrc)
$ManifestXml = $ManifestXml.Replace(
    '<manifest xmlns:android="http://schemas.android.com/apk/res/android"',
    '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.deepfalldrill.game"')
[System.IO.File]::WriteAllText($Manifest, $ManifestXml, (New-Object System.Text.UTF8Encoding($false)))

# ---------------------------------------------------------------- app icon
$Icon = Join-Path $ResDir "mipmap-xxxhdpi\ic_launcher.png"
if (-not (Test-Path $Icon)) {
    Write-Host "Generating icon..."
    Add-Type -AssemblyName System.Drawing
    $bmp = New-Object System.Drawing.Bitmap 192, 192
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::FromArgb(255, 7, 8, 12))
    $steel = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 125, 134, 148))
    $amber = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 176, 58))
    $dial = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 154, 215, 255))
    $g.FillRectangle($steel, 68, 34, 56, 70)
    $g.FillRectangle($amber, 68, 78, 56, 14)
    $g.FillEllipse($dial, 84, 48, 24, 24)
    $tri = [System.Drawing.Point[]]@(
        (New-Object System.Drawing.Point 62, 108),
        (New-Object System.Drawing.Point 130, 108),
        (New-Object System.Drawing.Point 96, 168)
    )
    $g.FillPolygon($amber, $tri)
    $g.Dispose()
    $bmp.Save($Icon, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# ------------------------------------------------------------ resources
Write-Host "Compiling resources..."
$ResZip = Join-Path $Build "res.zip"
if (Test-Path $ResZip) { Remove-Item -Force $ResZip }
& $Aapt2 compile --dir $ResDir -o $ResZip
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

$BaseApk = Join-Path $Build "base.apk"
if (Test-Path $BaseApk) { Remove-Item -Force $BaseApk }
Write-Host "Linking manifest..."
& $Aapt2 link -o $BaseApk -I $AndroidJar --manifest $Manifest `
    $ResZip --java $Gen --min-sdk-version 24 --target-sdk-version 36 `
    --version-code 1 --version-name "1.0.0"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

# ------------------------------------------------------------ compile java
Write-Host "Compiling Java..."
$Sources = @()
$Sources += Get-ChildItem -Path "$Root\app\src\main\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$Sources += Get-ChildItem -Path $Gen -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$SourceList = Join-Path $Build "sources.txt"
$Sources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' } | Set-Content -Encoding ASCII $SourceList

& $Javac --release 17 -encoding UTF-8 -cp $AndroidJar -d $Classes "@$SourceList"
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

# ------------------------------------------------------------ dex + package
Write-Host "Dexing..."
$ClassesJar = Join-Path $Build "classes.jar"
if (Test-Path $ClassesJar) { Remove-Item -Force $ClassesJar }
& $Jar cf $ClassesJar -C $Classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

& $D8 --lib $AndroidJar --min-api 24 --output $Dex $ClassesJar
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

$DexFile = Join-Path $Dex "classes.dex"
if (-not (Test-Path $DexFile)) { throw "classes.dex was not produced" }

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($BaseApk, [System.IO.Compression.ZipArchiveMode]::Update)
$entry = $zip.GetEntry("classes.dex")
if ($entry) { $entry.Delete() }
$entry = $zip.CreateEntry("classes.dex", [System.IO.Compression.CompressionLevel]::NoCompression)
$out = $entry.Open()
$in = [System.IO.File]::OpenRead($DexFile)
$in.CopyTo($out)
$in.Close()
$out.Close()
$zip.Dispose()

Write-Host "Aligning and signing..."
$Aligned = Join-Path $Build "aligned.apk"
if (Test-Path $Aligned) { Remove-Item -Force $Aligned }
& $ZipAlign -f 4 $BaseApk $Aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

$Keystore = "$env:USERPROFILE\.android\debug.keystore"
if (-not (Test-Path $Keystore)) {
    & (Join-Path $JavaHome "bin\keytool.exe") -genkeypair -v -keystore $Keystore `
        -storepass android -alias androiddebugkey -keypass android `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=Android Debug,O=Android,C=US"
}
$OutApk = Join-Path $Dist "deepfall-drill.apk"
if (Test-Path $OutApk) { Remove-Item -Force $OutApk }
& $ApkSigner sign --ks $Keystore --ks-pass pass:android --key-pass pass:android `
    --min-sdk-version 24 --out $OutApk $Aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host ""
Write-Host "APK: $OutApk" -ForegroundColor Green
Write-Host ("Size: {0:N1} MB" -f ((Get-Item $OutApk).Length / 1MB))

if ($Install -or $Run) {
    Write-Host "Installing..."
    & $Adb install -r $OutApk
    if ($LASTEXITCODE -ne 0) { throw "adb install failed" }
    if ($Run) {
        & $Adb shell am force-stop com.deepfalldrill.game
        & $Adb shell am start -n com.deepfalldrill.game/.MainActivity
    }
}
