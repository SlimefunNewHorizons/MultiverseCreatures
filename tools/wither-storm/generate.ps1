# Regenerates src/main/resources/witherstorm/*.txt from Cracker's Wither Storm Mod.
#
#   .\tools\wither-storm\generate.ps1 -Mod "C:\path\to\witherstormmod-1.20.1-4.2.1-all.jar"
#
# -Mod is the mod's jar or a folder it was unzipped into. -Decompiler is a Fernflower jar (IntelliJ
# ships one as plugins\java-decompiler\lib\java-decompiler.jar) and -Java a java.exe new enough to
# run it. Nothing of the mod is written into the repository except the generated form files.
param(
    [Parameter(Mandatory = $true)][string]$Mod,
    [string]$Decompiler = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\java-decompiler\lib\java-decompiler.jar",
    [string]$Java = "java"
)
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Resolve-Path (Join-Path $here "..\..")
# Short paths: the decompiled sources nest deep enough to pass Windows' 260 character limit.
$work = Join-Path $env:TEMP "wsgen"
if (Test-Path $work) { Remove-Item -Recurse -Force $work }
New-Item -ItemType Directory -Force "$work\mod", "$work\src", "$work\out" | Out-Null

if ((Get-Item $Mod).PSIsContainer) {
    $classes = $Mod
} else {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($Mod, "$work\mod")
    $classes = "$work\mod"
}

$models = Join-Path $classes "nonamecrackers2\witherstormmod\client\renderer\entity\model\witherstorm"
# Fernflower writes the sources relative to the folder it is given, so they go straight where the
# package says they belong.
$decompiled = Join-Path $work "dec\witherstormmod\client\renderer\entity\model\witherstorm"
New-Item -ItemType Directory -Force $decompiled | Out-Null
& $Java -cp $Decompiler org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler -dgs=1 -log=ERROR $models $decompiled

Copy-Item -Recurse (Join-Path $here "src\*") "$work\src"
$mass = "$work\src\nonamecrackers2\witherstormmod\client\renderer\entity\model\witherstorm\mass"
New-Item -ItemType Directory -Force $mass | Out-Null
# Every mass model but MassModel, the one that needs the game's real ModelPart.
Copy-Item (Join-Path $decompiled "mass\*.java") $mass -Exclude "MassModel.java"

function Check($step) { if ($LASTEXITCODE -ne 0) { throw "$step failed" } }

javac -nowarn -encoding UTF-8 -d "$work\out" "$work\src\gen\Extract.java"; Check "compiling Extract"
java -cp "$work\out" gen.Extract (Join-Path $work "dec") "$work\src\gen\Parts.java"; Check "extracting the builders"

$files = Get-ChildItem "$work\src" -Recurse -Filter *.java | ForEach-Object FullName
$files | Out-File -Encoding ascii "$work\files.txt"
# The mass models are single expressions thousands of calls long: javac needs a deep stack.
javac "-J-Xss512m" -nowarn -encoding UTF-8 -d "$work\out" "@$work\files.txt"; Check "compiling the models"

$textures = Join-Path $classes "assets\witherstormmod\textures\entity\wither_storm"
if (-not (Test-Path $textures)) {
    $textures = Join-Path (Split-Path $classes) "assets\witherstormmod\textures\entity\wither_storm"
}
$target = Join-Path $repo "src\main\resources\witherstorm"
java -cp "$work\out" gen.Gen (Join-Path $textures "wither_storm.png") (Join-Path $textures "wither_storm_emissive_decal.png") $target
Check "generating the forms"
Write-Host "Forms written to $target"
