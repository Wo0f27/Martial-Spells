$ErrorActionPreference = "Stop"

# P5 introduces no new source art. Re-assert the complete accepted P1-P4
# resource set, including the final P2 status-effect icon correction.
& "$PSScriptRoot\sync-paladin-p4-assets.ps1"

Write-Host ""
Write-Host "Paladin/Priest P5 integrated asset sync complete."
Write-Host "P5 adds bindings/grouping/package validation only; no new source art."
Write-Host "Next: python .\tools\audit-paladin-p5.py"
