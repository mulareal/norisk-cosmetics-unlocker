# Copies the NoRisk Client jars this mod compiles against into ./libs.
# They are NoRisk's property and are not part of this repository.
# Needs the NoRisk launcher installed and a Fabric profile started at least once.
#
#   .\setup-libs.ps1                          # newest profile for MC 26.3
#   .\setup-libs.ps1 -Minecraft 26.3 -Profile "Fabric 26.3"

param(
    [string]$Minecraft = "26.3",
    [string]$Profile = ""
)

$ErrorActionPreference = 'Stop'
$root = Join-Path $env:APPDATA "norisk\NoRiskClientV3"
$modCache = Join-Path $root "meta\mod_cache"
$profiles = Join-Path $root "data\profiles"
$libs = Join-Path $PSScriptRoot "libs"

if (-not (Test-Path $modCache)) { throw "NoRisk launcher not found ($root)." }

if ($Profile) {
    $processed = Join-Path $profiles "$Profile\.fabric\processedMods"
} else {
    $processed = Get-ChildItem $profiles -Directory |
        ForEach-Object { Join-Path $_.FullName ".fabric\processedMods" } |
        Where-Object { Test-Path $_ } |
        Sort-Object { (Get-Item $_).LastWriteTime } -Descending |
        Select-Object -First 1
}
if (-not $processed -or -not (Test-Path $processed)) { throw "No profile with .fabric\processedMods found. Start NoRisk once." }

New-Item -ItemType Directory -Force $libs | Out-Null
Get-ChildItem $libs -Filter *.jar | Remove-Item -Force

# Highest version per artifact (name up to the version part)
function Copy-Newest($files) {
    $files | Group-Object { $_.Name -replace '-\d.*$', '' } | ForEach-Object {
        $newest = $_.Group | Sort-Object { if ($_.Name -match '-(\d+(\.\d+){1,3})') { [version]$Matches[1] } else { [version]"0.0" } }, LastWriteTime -Descending | Select-Object -First 1
        Copy-Item $newest.FullName $libs
        Write-Host "  $($newest.Name)"
    }
}

Write-Host "NoRisk modules (mod_cache):"
Copy-Newest (Get-ChildItem $modCache -Filter "nrc-*+fabric.$Minecraft.jar")

Write-Host "Libraries ($processed):"
Copy-Newest (Get-ChildItem $processed -Filter *.jar | Where-Object {
    $_.Name -match '^(gg_norisk_backend|mixinextras|org_jetbrains_kotlin|org_mongodb_bson)'
})

Write-Host "Done: $libs"
