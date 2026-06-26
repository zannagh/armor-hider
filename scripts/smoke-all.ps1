<#
.SYNOPSIS
    Smoke-launch every loader/version combination on Windows.

.DESCRIPTION
    Iterates each fabric-* and neoforge-* variant defined in stonecutter.properties.toml,
    runs the gradle :loader:variant:runClient task with -Psmoke (and optionally
    -Pcompat=<set>). Each launch opens a Minecraft window briefly and exits 0 if no
    crash within the boot window.

    Writes per-combo pass/fail to .me/smoke-results-<timestamp>.txt and returns
    non-zero if any combo fails.

.PARAMETER Only
    fabric | neoforge | both (default: both)

.PARAMETER NoCompat
    Run with an empty run/mods (only this mod loaded).

.PARAMETER Include
    Comma-separated compat keys to fetch (e.g. "gender,geckolib"). Mutually exclusive
    with -NoCompat.

.PARAMETER DelayMs
    Boot window in milliseconds before the JVM exits cleanly. Default 15000.

.EXAMPLE
    scripts\smoke-all.ps1
    scripts\smoke-all.ps1 -Only fabric -NoCompat
    scripts\smoke-all.ps1 -Include "gender,geckolib"
#>

[CmdletBinding()]
param(
    [ValidateSet("fabric", "neoforge", "both")]
    [string]$Only = "both",
    [switch]$NoCompat,
    [string]$Include = "",
    [int]$DelayMs = 15000
)

$ErrorActionPreference = "Continue"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $Root

$FabricVersions = @(
    "fabric-1.20.1", "fabric-1.21.1", "fabric-1.21.4", "fabric-1.21.8",
    "fabric-1.21.10", "fabric-1.21.11", "fabric-26.1.2", "fabric-26.2"
)
$NeoForgeVersions = @(
    "neoforge-1.21.1", "neoforge-1.21.4", "neoforge-1.21.8",
    "neoforge-1.21.10", "neoforge-1.21.11", "neoforge-26.1.2", "neoforge-26.2"
)

$Versions = switch ($Only) {
    "fabric"   { $FabricVersions }
    "neoforge" { $NeoForgeVersions }
    "both"     { $FabricVersions + $NeoForgeVersions }
}

$Compat = "all"
if ($NoCompat)             { $Compat = "none" }
elseif ($Include -ne "")   { $Compat = $Include }

New-Item -ItemType Directory -Path ".me" -Force | Out-Null
$Ts = Get-Date -Format "yyyyMMdd-HHmmss"
$Results = ".me/smoke-results-$Ts.txt"
"armor-hider smoke run @ $Ts" | Out-File $Results
"compat=$Compat, delay=${DelayMs}ms" | Out-File $Results -Append
"" | Out-File $Results -Append

$pass = 0
$fail = 0
$failed = @()
foreach ($v in $Versions) {
    $loader = $v -replace '-.*$', ''
    $label = ":${loader}:${v}:runClient"
    Write-Host "::: smoke $v"
    & ./gradlew.bat $label `
        -Psmoke `
        -Pcompat=$Compat `
        -Psmoke.delay.ms=$DelayMs `
        --console=plain --no-daemon
    if ($LASTEXITCODE -eq 0) {
        "  PASS  $v" | Tee-Object $Results -Append
        $pass++
    } else {
        "  FAIL  $v" | Tee-Object $Results -Append
        $fail++
        $failed += $v
    }
}

"" | Tee-Object $Results -Append
"Done — pass=$pass fail=$fail" | Tee-Object $Results -Append
if ($fail -gt 0) {
    "Failed combos:" | Tee-Object $Results -Append
    foreach ($v in $failed) { "  - $v" | Tee-Object $Results -Append }
    exit 1
}
