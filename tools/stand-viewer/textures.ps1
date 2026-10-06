# Copies the vanilla textures the model viewer draws the Wither Storm with out of your own Minecraft
# client into target/stand-viewer/textures, next to models.json. They are Mojang's, so they are
# never committed: run this once on any machine that has the game installed.
#
#   .\tools\stand-viewer\textures.ps1                  # newest installed release
#   .\tools\stand-viewer\textures.ps1 -Version 1.21.11
param([string]$Version)
$ErrorActionPreference = "Stop"
$versions = Join-Path $env:APPDATA ".minecraft\versions"
if (-not $Version) {
    $Version = Get-ChildItem $versions -Directory | Where-Object { $_.Name -match '^1\.\d+(\.\d+)?$' } |
        Sort-Object { [version]$_.Name } | Select-Object -Last 1 -ExpandProperty Name
}
$jar = Join-Path $versions "$Version\$Version.jar"
if (-not (Test-Path $jar)) { throw "No Minecraft $Version client at $jar" }

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$out = Join-Path $here "..\..\target\stand-viewer\textures"
New-Item -ItemType Directory -Force $out | Out-Null

$blocks = 'obsidian', 'crying_obsidian', 'black_concrete', 'blackstone', 'blackstone_top', 'polished_blackstone',
    'gray_concrete', 'purple_concrete', 'magenta_concrete', 'amethyst_block', 'light_gray_concrete', 'white_concrete',
    'command_block_front', 'command_block_back', 'command_block_side'
$wanted = @{}
foreach ($b in $blocks) { $wanted["assets/minecraft/textures/block/$b.png"] = "$b.png" }
$wanted["assets/minecraft/textures/entity/skeleton/wither_skeleton.png"] = "wither_skeleton.png"

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    foreach ($entry in $zip.Entries) {
        if ($wanted.ContainsKey($entry.FullName)) {
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path $out $wanted[$entry.FullName]), $true)
        }
    }
} finally {
    $zip.Dispose()
}
# The same textures as data URLs in a script, for the viewer opened straight from disk: a file://
# page may not draw local images on its canvas, but it runs a script tag.
$entries = Get-ChildItem $out -Filter *.png | Sort-Object Name | ForEach-Object {
    '"' + $_.BaseName + '":"data:image/png;base64,' + [Convert]::ToBase64String([IO.File]::ReadAllBytes($_.FullName)) + '"'
}
$script = "window.STAND_VIEWER_TEXTURES = {" + ($entries -join ",") + "};`n"
[IO.File]::WriteAllText((Join-Path $out "..\textures.js"), $script, (New-Object Text.UTF8Encoding $false))
Write-Host "Copied $((Get-ChildItem $out -Filter *.png).Count) textures from Minecraft $Version to $out"
