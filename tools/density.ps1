param(
    [int]$Seed = 335341
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$JavaHome = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_HOME = $JavaHome
$Javac = Join-Path $JavaHome "bin\javac.exe"
$Java = Join-Path $JavaHome "bin\java.exe"
$Out = Join-Path $Root "build\testclasses"

if (Test-Path $Out) { Remove-Item -Recurse -Force $Out }
New-Item -ItemType Directory -Force $Out | Out-Null

$Sources = @(
    "app\src\main\java\com\deepfalldrill\game\Config.java",
    "app\src\main\java\com\deepfalldrill\game\Rng.java",
    "app\src\main\java\com\deepfalldrill\game\Materials.java",
    "app\src\main\java\com\deepfalldrill\game\Terrain.java",
    "app\src\main\java\com\deepfalldrill\game\Generator.java",
    "tools\DensityTest.java"
)
& $Javac --release 17 -d $Out $Sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }
& $Java -cp $Out DensityTest $Seed
