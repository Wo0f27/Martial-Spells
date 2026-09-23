from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
errors = []

contract = root / "PALADIN_PRIEST_SPELL_PORT.md"
if not contract.is_file():
    errors.append("PALADIN_PRIEST_SPELL_PORT.md missing")
    text = ""
else:
    text = contract.read_text(encoding="utf-8")

for required in (
    "2807417a1dd9a65204c002ded487da0e6ae467a1",
    "v1.20.1-3.16.3",
    "14 player-facing",
    "lightwell_orb",
    "SchoolRegistry.HOLY_RESOURCE",
    "max level 1",
    "Holy Wand -> Heal",
    "Holy Staff -> Holy Shock",
):
    if required.lower() not in text.lower():
        errors.append(f"CP11 P0 contract missing: {required}")

expected = {
    "heal", "holy_shock", "flash_heal", "blessed_strikes",
    "holy_beam", "levitate", "divine_protection", "judgement",
    "circle_of_healing", "penance", "barrier", "battle_banner",
    "immolation", "lightwell", "lightwell_orb",
}
backticked = set(re.findall(r"`([a-z][a-z0-9_]*)`", text))
missing = sorted(expected - backticked)
if missing:
    errors.append(f"source spell catalogue missing IDs: {missing}")

props = (root / "gradle.properties").read_text(encoding="utf-8")
for required in (
    "minecraft_version=1.20.1",
    "forge_version=47.4.10",
    "mapping_version=2023.09.03-1.20.1",
    "irons_spells_version=1.20.1-3.16.3",
):
    if required not in props:
        errors.append(f"target dependency contract mismatch: {required}")

build = (root / "build.gradle").read_text(encoding="utf-8")
for forbidden in (
    "spell_engine",
    "spell-power",
    "spell_power",
):
    if forbidden in build.lower():
        errors.append(f"removed source runtime must not become a Martial Spells dependency: {forbidden}")

print("CP11 P0 archaeology summary")
print(" - frozen Paladins spell JSONs: 15")
print(" - player-facing spells: 14")
print(" - internal helper spells: 1")
print(" - target Iron's release: 1.20.1-3.16.3")
print(" - target school: irons_spellbooks:holy")

if errors:
    print("CP11 P0 AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("CP11 P0 AUDIT PASSED")
