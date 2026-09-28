from pathlib import Path
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

p4 = subprocess.run(
    [sys.executable, str(root / "tools" / "audit-paladin-p4.py")],
    cwd=root,
    text=True,
    capture_output=True,
)
output = (p4.stdout + p4.stderr).strip()
if output:
    print(output)
if p4.returncode != 0:
    errors.append("P0-P4 integrated Paladin/Priest audit failed")

catalog_path = root / "src/main/java/com/w0of26/martialspells/integration/PaladinPriestSpellCatalog.java"
if not catalog_path.is_file():
    errors.append("P5 Paladin/Priest spell catalog missing")
    catalog = ""
else:
    catalog = catalog_path.read_text(encoding="utf-8")

for token in (
    '"irons_spellbooks",',
    '"blessing_of_life"',
    '"holy_shock"',
    "PALADIN_LIBRAM",
    "PRIEST_HOLY_BOOK",
    "HOLY_MOTE_HELPER",
    "PLAYER_FACING_SOURCE_EQUIVALENTS",
    'martial("flash_heal")',
    'martial("blessed_strikes")',
    'martial("divine_protection")',
    'martial("judgement")',
    'martial("battle_banner")',
    'martial("immolation")',
    'martial("holy_beam")',
    'martial("circle_of_healing")',
    'martial("barrier")',
    'martial("lightwell")',
    'martial("levitate")',
    'martial("penance")',
    'martial("lightwell_orb")',
):
    if token not in catalog:
        errors.append(f"P5 spell catalog missing {token}")

registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java").read_text(encoding="utf-8")
custom_player_spells = (
    "holy_shock",
    "flash_heal",
    "circle_of_healing",
    "blessed_strikes",
    "divine_protection",
    "judgement",
    "immolation",
    "holy_beam",
    "levitate",
    "penance",
    "barrier",
    "battle_banner",
    "lightwell",
)
for spell_id in custom_player_spells:
    if f'SPELLS.register("{spell_id}"' not in registry:
        errors.append(f"P5 registry missing player spell: {spell_id}")
if 'SPELLS.register("lightwell_orb"' not in registry:
    errors.append("P5 registry missing internal Holy Mote helper")
if 'SPELLS.register("heal"' in registry:
    errors.append("martial_spells:heal must remain intentionally unregistered")

spell_icons = root / "src/main/resources/assets/martial_spells/textures/gui/spell_icons"
for spell_id in custom_player_spells:
    path = spell_icons / f"{spell_id}.png"
    if not path.is_file():
        errors.append(f"P5 player spell icon missing after sync: {spell_id}.png")

effect_icons = root / "src/main/resources/assets/martial_spells/textures/mob_effect"
visible_effects = (
    "blessed_strikes",
    "divine_protection",
    "levitate",
    "priest_absorption",
    "battle_banner",
)
for effect_id in visible_effects:
    path = effect_icons / f"{effect_id}.png"
    if not path.is_file():
        errors.append(f"P5 visible Paladin/Priest effect icon missing after sync: {effect_id}.png")

lang = json.loads((root / "src/main/resources/assets/martial_spells/lang/en_us.json").read_text(encoding="utf-8"))
for spell_id in custom_player_spells:
    if f"spell.martial_spells.{spell_id}" not in lang:
        errors.append(f"P5 lang missing player spell name: {spell_id}")
for effect_id in visible_effects:
    if f"effect.martial_spells.{effect_id}" not in lang:
        errors.append(f"P5 lang missing visible effect name: {effect_id}")

sync = (root / "tools/sync-paladin-p5-assets.ps1").read_text(encoding="utf-8")
if "sync-paladin-p4-assets.ps1" not in sync:
    errors.append("P5 asset sync must delegate the complete P4 resource chain")

build = (root / "build.gradle").read_text(encoding="utf-8")
props = (root / "gradle.properties").read_text(encoding="utf-8")
for token in (
    "Paladins-And-Priests-Forge-1.20.1",
    "paladinsDevEnabled",
    "Paladins dev runtime skipped",
):
    if token not in build:
        errors.append(f"P5 Paladins dev-runtime fixture missing: {token}")
for token in (
    "paladins_version=3.1.1-forge-private.cp11",
    "paladins_dev_root=../Paladins-And-Priests-Forge-1.20.1",
    "paladins_dev_enabled=true",
):
    if token not in props:
        errors.append(f"P5 Paladins dev property missing: {token}")

for obsolete in (
    root / "src/main/resources/assets/martial_spells/textures/misc/paladin_item_glow.png",
    root / "src/main/resources/assets/martial_spells/sounds/blessed_strike_start.ogg",
    root / "src/main/resources/assets/martial_spells/sounds/blessed_strike_casting.ogg",
    root / "src/main/resources/assets/martial_spells/sounds/blessed_strike_release.ogg",
):
    if obsolete.exists():
        errors.append(f"obsolete Blessed Strikes presentation asset remains: {obsolete.name}")

print("")
print("CP11 P5 bindings/final-integration summary")
print(" - source-equivalent player spell entries: 14")
print(" - Martial Spells custom player spells: 13")
print(" - Paladin Libram group: 6")
print(" - Priest Holy Book group: 6")
print(" - Holy Wand binding: irons_spellbooks:blessing_of_life")
print(" - Holy Staff binding: martial_spells:holy_shock")
print(" - Holy Mote: internal helper only")
print(" - Paladins equipment: development fixture only / no hard metadata dependency")
print(" - visible effect icons audited: 5")

if errors:
    print("CP11 P5 AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("CP11 P5 STATIC AUDIT PASSED")
print("P5 still requires local builds, client/server integration tests, packaged-JAR audits, and developer confirmation.")
