from pathlib import Path
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

p1 = subprocess.run(
    [sys.executable, str(root / "tools" / "audit-paladin-p1.py")],
    cwd=root,
    text=True,
    capture_output=True,
)
output = (p1.stdout + p1.stderr).strip()
if output:
    print(output)
if p1.returncode != 0:
    errors.append("P1 Holy healing audit failed")

spell_dir = root / "src/main/java/com/w0of26/martialspells/spells"
event_dir = root / "src/main/java/com/w0of26/martialspells/events"
combat_dir = root / "src/main/java/com/w0of26/martialspells/combat"
effect_dir = root / "src/main/java/com/w0of26/martialspells/effects"

registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java").read_text(encoding="utf-8")
effect_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java").read_text(encoding="utf-8")
sound_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java").read_text(encoding="utf-8")
lang = json.loads((root / "src/main/resources/assets/martial_spells/lang/en_us.json").read_text(encoding="utf-8"))
sounds = json.loads((root / "src/main/resources/assets/martial_spells/sounds.json").read_text(encoding="utf-8"))

spell_contract = {
    "PaladinBlessedStrikesSpell.java": (
        '"blessed_strikes"',
        "SpellRarity.RARE",
        "SchoolRegistry.HOLY_RESOURCE",
        ".setMaxLevel(1)",
        "CAST_TIME_TICKS = 50",
        "BASE_COOLDOWN_SECONDS = 12",
        "EFFECT_DURATION_TICKS = 15 * 20",
        "MAX_AMPLIFIER = 5",
        "DAMAGE_COEFFICIENT = 0.50F",
        "KNOCKBACK_STRENGTH = 0.50D",
        "CastType.CONTINUOUS",
        "getCastDurationRemaining() >= CAST_TIME_TICKS",
    ),
    "PaladinDivineProtectionSpell.java": (
        '"divine_protection"',
        "SpellRarity.EPIC",
        "SchoolRegistry.HOLY_RESOURCE",
        ".setMaxLevel(1)",
        "EFFECT_DURATION_TICKS = 8 * 20",
        "BASE_COOLDOWN_SECONDS = 30",
        "AMPLIFIER_POWER_MULTIPLIER = 0.50F",
        "AMPLIFIER_CAP = 2",
        "getEntityPowerMultiplier(caster)",
    ),
    "PaladinJudgementSpell.java": (
        '"judgement"',
        "SpellRarity.EPIC",
        "SchoolRegistry.HOLY_RESOURCE",
        ".setMaxLevel(1)",
        "RANGE = 16",
        "CAST_TIME_TICKS = 10",
        "BASE_COOLDOWN_SECONDS = 15",
        "METEOR_LAUNCH_HEIGHT = 12.0D",
        "METEOR_VELOCITY = 1.2D",
        "METEOR_TRAVEL_TICKS = 10",
        "IMPACT_RADIUS = 6.0D",
        "DAMAGE_COEFFICIENT = 0.90F",
        "STUN_DURATION_TICKS = 3 * 20",
        "CONTROL_HEALTH_BASE = 50.0D",
        "CONTROL_POWER_MULTIPLIER = 2.0D",
        "UNDEAD_POWER_MULTIPLIER = 1.50F",
        "PaladinHybridPower.meleeDominant",
        "StunService.apply",
    ),
    "PaladinImmolationSpell.java": (
        '"immolation"',
        "SpellRarity.LEGENDARY",
        "SchoolRegistry.HOLY_RESOURCE",
        ".setMaxLevel(1)",
        "RANGE = 5.0D",
        "VERTICAL_RANGE_MULTIPLIER = 0.50D",
        "BASE_COOLDOWN_SECONDS = 12",
        "DAMAGE_COEFFICIENT = 1.20F",
        "HEAL_COEFFICIENT = 0.50F",
        "FIRE_DURATION_SECONDS = 4",
        "UNDEAD_POWER_MULTIPLIER = 1.50F",
        "SOURCE_DEFAULT_CRITICAL_MULTIPLIER = 1.50F",
        "PaladinHybridPower.holyDominant",
        "Utils.shouldHealEntity",
        "SpellHealEvent",
        "DamageSources.applyDamage",
    ),
}

for filename, tokens in spell_contract.items():
    path = spell_dir / filename
    if not path.is_file():
        errors.append(f"missing P2 spell class: {filename}")
        continue
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{filename}: missing contract token {token!r}")

hybrid = (combat_dir / "PaladinHybridPower.java").read_text(encoding="utf-8")
for token in (
    "* 0.75F",
    "* 0.25F",
    "PhysicalMeleePower.get",
    "spell.getSpellPower(1, caster)",
):
    if token not in hybrid:
        errors.append(f"PaladinHybridPower missing {token}")

blessed_events = (event_dir / "BlessedStrikesEvents.java").read_text(encoding="utf-8")
for token in (
    "DamageTypes.PLAYER_ATTACK",
    "DamageSources.applyDamage",
    "PENDING_CONSUME_TAG",
    "TickEvent.Phase.END",
    "currentAmplifier - 1",
):
    if token not in blessed_events:
        errors.append(f"BlessedStrikesEvents missing {token}")

divine_events = (event_dir / "DivineProtectionEvents.java").read_text(encoding="utf-8")
for token in (
    "LivingAttackEvent",
    "event.setCanceled(true)",
    "amplifier - 1",
    "DIVINE_PROTECTION_IMPACT",
):
    if token not in divine_events:
        errors.append(f"DivineProtectionEvents missing {token}")

judgement_manager = (combat_dir / "JudgementImpactManager.java").read_text(encoding="utf-8")
for token in (
    "METEOR_TRAVEL_TICKS",
    "METEOR_LAUNCH_HEIGHT",
    "TickEvent.PlayerTickEvent",
    "spell.resolveImpact",
):
    if token not in judgement_manager:
        errors.append(f"JudgementImpactManager missing {token}")

judgement_visual_path = root / "src/main/java/com/w0of26/martialspells/entity/JudgementVisualEntity.java"
if judgement_visual_path.is_file():
    judgement_visual = judgement_visual_path.read_text(encoding="utf-8")
    for token in (
        "PaladinJudgementSpell.METEOR_VELOCITY",
        "PaladinJudgementSpell.METEOR_TRAVEL_TICKS",
    ):
        if token not in judgement_visual:
            errors.append(f"JudgementVisualEntity missing {token}")
else:
    # Pre-VFX P2 layout kept velocity in the impact manager.
    if "METEOR_VELOCITY" not in judgement_manager:
        errors.append("Judgement meteor velocity is not represented in manager or visual entity")

for effect in ("BlessedStrikesEffect.java", "DivineProtectionEffect.java"):
    if not (effect_dir / effect).is_file():
        errors.append(f"missing P2 effect class: {effect}")

for spell_id, class_name in (
    ("blessed_strikes", "PaladinBlessedStrikesSpell::new"),
    ("divine_protection", "PaladinDivineProtectionSpell::new"),
    ("judgement", "PaladinJudgementSpell::new"),
    ("immolation", "PaladinImmolationSpell::new"),
):
    if f'SPELLS.register("{spell_id}", {class_name})' not in registry:
        errors.append(f"spell registry missing {spell_id}")

for effect_id, class_name in (
    ("blessed_strikes", "BlessedStrikesEffect::new"),
    ("divine_protection", "DivineProtectionEffect::new"),
):
    if f'"{effect_id}"' not in effect_registry or class_name not in effect_registry:
        errors.append(f"effect registry missing {effect_id}")

for key in (
    "spell.martial_spells.blessed_strikes",
    "spell.martial_spells.divine_protection",
    "spell.martial_spells.judgement",
    "spell.martial_spells.immolation",
    "effect.martial_spells.blessed_strikes",
    "effect.martial_spells.divine_protection",
):
    if key not in lang:
        errors.append(f"lang missing {key}")

p2_sounds = (
    "blessed_strike_start",
    "blessed_strike_casting",
    "blessed_strike_release",
    "divine_protection_release",
    "divine_protection_impact",
    "judgement_impact",
    "immolation_release",
)
for sound in p2_sounds:
    if f'register("{sound}")' not in sound_registry:
        errors.append(f"sound registry missing {sound}")
    if sounds.get(sound, {}).get("sounds") != [f"martial_spells:{sound}"]:
        errors.append(f"sounds.json mismatch for {sound}")

sync_text = (root / "tools/sync-paladin-p2-assets.ps1").read_text(encoding="utf-8")
if "2807417a1dd9a65204c002ded487da0e6ae467a1" not in sync_text:
    errors.append("P2 asset sync is not pinned to frozen Paladins commit")
if "sync-paladin-p1-assets.ps1" not in sync_text:
    errors.append("P2 asset sync must re-assert finalized P1 assets first")

for spell_id in (
    "blessed_strikes",
    "divine_protection",
    "judgement",
    "immolation",
):
    icon = root / f"src/main/resources/assets/martial_spells/textures/gui/spell_icons/{spell_id}.png"
    if not icon.is_file():
        errors.append(f"source icon missing; run P2 asset sync: {spell_id}.png")

for sound in p2_sounds:
    path = root / "src/main/resources/assets/martial_spells/sounds" / f"{sound}.ogg"
    if not path.is_file():
        errors.append(f"source sound missing; run P2 asset sync: {sound}.ogg")

build = (root / "build.gradle").read_text(encoding="utf-8").lower()
for forbidden in ("spell_engine", "spell-power", "spell_power"):
    if forbidden in build:
        errors.append(f"P2 must not add source runtime dependency: {forbidden}")

print("")
print("CP11 P2 Retribution/protection summary")
print(" - spells: 4")
print(" - status effects: 2")
print(" - Holy-dominant blend: 75% Holy / 25% melee")
print(" - Judgement blend: 75% melee / 25% Holy")
print(" - Blessed Strikes: 5 seal pulses; next-tick one-seal consumption")
print(" - Divine Protection: 1-3 protected hits for 8 seconds")
print(" - Judgement: 10-tick meteor; 6-block squared falloff")
print(" - Immolation: ally heal / enemy damage + 4-second burn")
print(" - source icons: 4")
print(" - source sounds: 7")

if errors:
    print("CP11 P2 AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("CP11 P2 AUDIT PASSED")
