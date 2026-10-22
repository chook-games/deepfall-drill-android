param(
    [string]$Out = "shot.png",
    [int]$Width = 480,
    [string]$Device = "emulator-5554"
)

$ErrorActionPreference = "Stop"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$tmp = Join-Path $env:TEMP "deepfall-raw.png"

& $adb -s $Device shell screencap -p /sdcard/df_shot.png
& $adb -s $Device pull /sdcard/df_shot.png $tmp | Out-Null
if (-not (Test-Path $tmp)) { throw "screencap failed" }

Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile($tmp)
try {
    $h = [int]($img.Height * $Width / $img.Width)
    $bmp = New-Object System.Drawing.Bitmap $Width, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, $Width, $h)
    $g.Dispose()
    $bmp.Save((Join-Path (Get-Location) $Out), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Saved $Out ($Width x $h)"
} finally {
    $img.Dispose()
    Remove-Item -Force $tmp -ErrorAction SilentlyContinue
}
