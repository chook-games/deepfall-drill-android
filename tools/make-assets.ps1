param(
    [string]$Root = (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path))
)

$ErrorActionPreference = "Stop"
Set-Location $Root
Add-Type -AssemblyName System.Drawing

$res = Join-Path $Root "app\src\main\res"
$store = Join-Path $Root "store"
New-Item -ItemType Directory -Force $store | Out-Null
New-Item -ItemType Directory -Force (Join-Path $store "screenshots") | Out-Null

$bgCol = [System.Drawing.Color]::FromArgb(255, 11, 14, 19)
$steel = [System.Drawing.Color]::FromArgb(255, 125, 134, 148)
$steelDark = [System.Drawing.Color]::FromArgb(255, 59, 65, 75)
$steelLight = [System.Drawing.Color]::FromArgb(255, 201, 209, 220)
$amber = [System.Drawing.Color]::FromArgb(255, 224, 160, 58)
$amberBright = [System.Drawing.Color]::FromArgb(255, 255, 176, 58)
$ink = [System.Drawing.Color]::FromArgb(255, 32, 36, 43)
$glass = [System.Drawing.Color]::FromArgb(255, 154, 215, 255)

function New-RoundRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

function Draw-DrillGlyph($g, [float]$size) {
    $s = $size / 108.0
    $bg = New-Object System.Drawing.SolidBrush $bgCol
    $st = New-Object System.Drawing.SolidBrush $steel
    $sd = New-Object System.Drawing.SolidBrush $steelDark
    $sl = New-Object System.Drawing.SolidBrush $steelLight
    $am = New-Object System.Drawing.SolidBrush $amber
    $ik = New-Object System.Drawing.SolidBrush $ink
    $gl = New-Object System.Drawing.SolidBrush $glass

    $g.FillRectangle($sd, 34 * $s, 38 * $s, 4 * $s, 18 * $s)
    $g.FillRectangle($sd, 70 * $s, 38 * $s, 4 * $s, 18 * $s)

    $bodyPath = New-RoundRectPath (37 * $s) (26 * $s) (34 * $s) (35 * $s) (5 * $s)
    $g.FillPath($st, $bodyPath)

    $g.FillRectangle($am, 37 * $s, 44 * $s, 34 * $s, 6 * $s)
    foreach ($x in @(43, 51, 59, 67)) {
        $g.FillRectangle($ik, $x * $s, 44 * $s, 3 * $s, 6 * $s)
    }

    $g.FillEllipse($gl, (54 - 3.6) * $s, (32 - 3.6) * $s, 7.2 * $s, 7.2 * $s)

    $bit = New-Object System.Drawing.Drawing2D.GraphicsPath
    $bit.AddPolygon(@(
        [System.Drawing.PointF]::new(38 * $s, 60 * $s),
        [System.Drawing.PointF]::new(70 * $s, 60 * $s),
        [System.Drawing.PointF]::new(54 * $s, 86 * $s)
    ))
    $g.FillPath($sl, $bit)
    $g.FillRectangle($ik, 38 * $s, 60 * $s, 32 * $s, 2 * $s)

    $bodyPath.Dispose()
    $bit.Dispose()
    $bg.Dispose(); $st.Dispose(); $sd.Dispose(); $sl.Dispose(); $am.Dispose(); $ik.Dispose(); $gl.Dispose()
}

function Save-Icon([int]$size, [string]$path, [bool]$round) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    $bg = New-Object System.Drawing.SolidBrush $bgCol
    if ($round) {
        $g.FillEllipse($bg, 0, 0, $size, $size)
        $clip = New-Object System.Drawing.Drawing2D.GraphicsPath
        $clip.AddEllipse(0, 0, $size, $size)
        $g.SetClip($clip)
        $clip.Dispose()
    } else {
        $g.FillRectangle($bg, 0, 0, $size, $size)
    }
    Draw-DrillGlyph $g $size
    $bg.Dispose()
    $g.Dispose()
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$buckets = [ordered]@{
    "mipmap-mdpi"    = 48
    "mipmap-hdpi"    = 72
    "mipmap-xhdpi"   = 96
    "mipmap-xxhdpi"  = 144
    "mipmap-xxxhdpi" = 192
}
foreach ($bucket in $buckets.Keys) {
    $dir = Join-Path $res $bucket
    New-Item -ItemType Directory -Force $dir | Out-Null
    $size = $buckets[$bucket]
    Save-Icon $size (Join-Path $dir "ic_launcher.png") $false
    Save-Icon $size (Join-Path $dir "ic_launcher_round.png") $true
}

Save-Icon 512 (Join-Path $store "icon-512.png") $false

# ---------------------------------------------------------------- feature graphic
$fgW = 1024
$fgH = 500
$fg = New-Object System.Drawing.Bitmap $fgW, $fgH
$g = [System.Drawing.Graphics]::FromImage($fg)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    [System.Drawing.Rectangle]::new(0, 0, $fgW, $fgH),
    [System.Drawing.Color]::FromArgb(255, 24, 18, 13),
    [System.Drawing.Color]::FromArgb(255, 5, 6, 9),
    [System.Drawing.Drawing2D.LinearGradientMode]::Vertical)
$g.FillRectangle($grad, 0, 0, $fgW, $fgH)
$grad.Dispose()

$glow = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(28, 255, 160, 60))
$g.FillEllipse($glow, 60, 90, 360, 360)
$glow.Dispose()

$iconBmp = New-Object System.Drawing.Bitmap 300, 300
$gi = [System.Drawing.Graphics]::FromImage($iconBmp)
$gi.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$gi.Clear($bgCol)
Draw-DrillGlyph $gi 300
$gi.Dispose()
$g.DrawImage($iconBmp, 70, 100, 300, 300)
$iconBmp.Dispose()

$fontBlack = New-Object System.Drawing.Font "Segoe UI Black", 84, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$fontAmber = New-Object System.Drawing.Font "Segoe UI Black", 62, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$fontTag = New-Object System.Drawing.Font "Segoe UI Semibold", 22, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$fontSmall = New-Object System.Drawing.Font "Segoe UI", 17, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)

$whiteBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 232, 238, 247))
$amberBrush = New-Object System.Drawing.SolidBrush $amberBright
$greyBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 150, 160, 175))

$g.DrawString("DEEPFALL", $fontBlack, $whiteBrush, 400, 120)
$g.DrawString("DRILL", $fontAmber, $amberBrush, 406, 214)
$g.DrawString("GRAVITY PULLS. YOU STEER.", $fontTag, $greyBrush, 410, 306)
$g.DrawString("Arcade mining roguelite  |  solid ground  |  endless descent", $fontSmall, $greyBrush, 410, 344)

$fontBlack.Dispose(); $fontAmber.Dispose(); $fontTag.Dispose(); $fontSmall.Dispose()
$whiteBrush.Dispose(); $amberBrush.Dispose(); $greyBrush.Dispose()
$g.Dispose()
$fg.Save((Join-Path $store "feature-graphic-1024x500.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$fg.Dispose()

Write-Host "Icons written to res/mipmap-* and store/icon-512.png"
Write-Host "Feature graphic: store/feature-graphic-1024x500.png"
