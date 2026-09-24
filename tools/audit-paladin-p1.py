from pathlib import Path
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

p0 = subprocess.run(
    [sys.executable, str(root / "tools" / "audit-paladin-p0.py")],
    cwd=root,
    text=True,
    capture_output=True,
)
output = (p0.stdout + p0.stderr).strip()
if output:
    print(output)
if p0.returncode != 0:
    errors.append("P0 archaeology contract audit failed")

spell_dir = root / "src/main/java/com/w0of26/martialspells/spells"
registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java").read_text(encoding="utf-8")
sound_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java").read_text(encoding="utf-8")
lang = json.loads((root / "src/main/resources/assets/martial_spells/lang/en_us.json").read_text(encoding="utf-8"))
sounds = json.loads((root / "src/main/resources/assets/martial_spells/sounds.json").read_text(encoding="utf-8"))

contract = {
    "PaladinHolyShockSpell.java": {
        "id": "holy_shock", "rarity": "UNCOMMON", "cast": "30",
        "cooldown": "3", "coefficient": "0.80F", "mana": "20",
    },
    "PaladinFlashHealSpell.java": {
        "id": "flash_heal", "rarity": "RARE", "cast": "10",
        "cooldown": "6", "coefficient": "1.20F", "mana": "30",
    },
    "PaladinCircleOfHealingSpell.java": {
        "id": "circle_of_healing", "rarity": "EPIC", "cast": "10",
        "cooldown": "10", "coefficient": "0.40F", "mana": "40",
    },
}

for filename, cfg in contract.items():
    path = spell_dir / filename
    if not path.is_file():
        errors.append(f"missing P1 spell class: {filename}")
        continue
    text = path.read_text(encoding="utf-8")
    required = (
        f'"{cfg["id"]}"',
        f"SpellRarity.{cfg['rarity']}",
        "SchoolRegistry.HOLY_RESOURCE",
        ".setMaxLevel(1)",
        f"BASE_COOLDOWN_SECONDS = {cfg['cooldown']}",
        f"CAST_TIME_TICKS = {cfg['cast']}",
        cfg["coefficient"],
        f"baseManaCost = {cfg['mana']}",
        "PaladinHolySpellSupport.SOURCE_POWER_REFERENCE",
    )
    for token in required:
        if token not in text:
            errors.append(f"{filename}: missing contract token {token!r}")

support = (spell_dir / "PaladinHolySpellSupport.java").read_text(encoding="utf-8")
for token in (
    "SOURCE_POWER_REFERENCE = 5",
    "Utils.shouldHealEntity",
    "Utils.preCastTargetHelper",
    "TargetEntityCastData",
    "SpellHealEvent",
    "DamageSources.applyDamage",
    "spell.getDamageSource(caster)",
):
    if token not in support:
        errors.append(f"PaladinHolySpellSupport missing {token}")

if (spell_dir / "PaladinHealSpell.java").exists():
    errors.append("redundant custom PaladinHealSpell.java must remain removed")

if 'SPELLS.register("heal"' in registry:
    errors.append("redundant martial_spells:heal registration must remain removed")

flash = (spell_dir / "PaladinFlashHealSpell.java").read_text(encoding="utf-8")
shock = (spell_dir / "PaladinHolyShockSpell.java").read_text(encoding="utf-8")
circle = (spell_dir / "PaladinCircleOfHealingSpell.java").read_text(encoding="utf-8")

if "targetFriendlyOrSelf" not in flash or "Utils.shouldHealEntity" not in flash:
    errors.append("Flash Heal: friendly-target/self-fallback contract missing")

for token in (
    "targetAnyOrSelf",
    "HEAL_COEFFICIENT = 0.40F",
    "DAMAGE_COEFFICIENT = 0.80F",
    "KNOCKBACK_STRENGTH = 0.50D",
    "Utils.shouldHealEntity",
    "HOLY_SHOCK_HEAL",
    "HOLY_SHOCK_DAMAGE",
):
    if token not in shock:
        errors.append(f"Holy Shock contract missing {token}")

for token in (
    "RANGE = 8.0D",
    "VERTICAL_RANGE_MULTIPLIER = 0.60D",
    "Utils.shouldHealEntity",
    "target != caster",
    "PaladinHolySpellSupport.heal",
):
    if token not in circle:
        errors.append(f"Circle of Healing contract missing {token}")

for spell_id, class_name in (
    ("holy_shock", "PaladinHolyShockSpell::new"),
    ("flash_heal", "PaladinFlashHealSpell::new"),
    ("circle_of_healing", "PaladinCircleOfHealingSpell::new"),
):
    if f'SPELLS.register("{spell_id}", {class_name})' not in registry:
        errors.append(f"spell registry missing {spell_id}")

for key in (
    "spell.martial_spells.holy_shock",
    "spell.martial_spells.flash_heal",
    "spell.martial_spells.circle_of_healing",
):
    if key not in lang:
        errors.append(f"lang missing {key}")

for sound in ("holy_shock_heal", "holy_shock_damage"):
    if f'register("{sound}")' not in sound_registry:
        errors.append(f"sound registry missing {sound}")
    if sounds.get(sound, {}).get("sounds") != [f"martial_spells:{sound}"]:
        errors.append(f"sounds.json mismatch for {sound}")

sync_text = (root / "tools/sync-paladin-p1-assets.ps1").read_text(encoding="utf-8")
if "2807417a1dd9a65204c002ded487da0e6ae467a1" not in sync_text:
    errors.append("P1 asset sync is not pinned to frozen Paladins commit")

for spell_id in ("holy_shock", "flash_heal", "circle_of_healing"):
    icon = root / f"src/main/resources/assets/martial_spells/textures/gui/spell_icons/{spell_id}.png"
    if not icon.is_file():
        errors.append(f"source icon missing; run P1 asset sync: {spell_id}.png")

redundant_heal_icon = root / "src/main/resources/assets/martial_spells/textures/gui/spell_icons/heal.png"
if redundant_heal_icon.exists():
    errors.append("redundant martial_spells Heal icon must be removed; rerun P1 asset sync")

for sound in ("holy_shock_heal.ogg", "holy_shock_damage.ogg"):
    path = root / "src/main/resources/assets/martial_spells/sounds" / sound
    if not path.is_file():
        errors.append(f"source sound missing; run P1 asset sync: {sound}")

build = (root / "build.gradle").read_text(encoding="utf-8").lower()
for forbidden in ("spell_engine", "spell-power", "spell_power"):
    if forbidden in build:
        errors.append(f"P1 must not add source runtime dependency: {forbidden}")

print("")
print("CP11 P1 Holy healing summary")
print(" - custom spells: 3")
print(" - school: irons_spellbooks:holy")
print(" - source power reference: 5")
print(" - exact source coefficients: 0.4+0.8 / 1.2 / 0.4")
print(" - source icons: 3")
print(" - source-only impact sounds: 2")
print(" - upstream Heal: intentionally not ported; native Iron's healing retained")

if errors:
    print("CP11 P1 AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("CP11 P1 AUDIT PASSED")
