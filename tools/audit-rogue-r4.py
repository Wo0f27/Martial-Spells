from pathlib import Path
import hashlib
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

# R4 is cumulative: R2 and R3 must remain intact.
r3 = subprocess.run([sys.executable, str(root / "tools/audit-rogue-r3.py")], cwd=root)
if r3.returncode != 0:
    print("R4 STATIC AUDIT FAILED: cumulative R3 audit failed")
    sys.exit(r3.returncode)

spell_path = root / "src/main/java/com/w0of26/martialspells/spells/SliceAndDiceSpell.java"
effect_path = root / "src/main/java/com/w0of26/martialspells/effects/SliceAndDiceEffect.java"
events_path = root / "src/main/java/com/w0of26/martialspells/events/SliceAndDiceEvents.java"
effect_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java"
spell_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java"
sound_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java"

for path in (
    spell_path,
    effect_path,
    events_path,
    effect_registry_path,
    spell_registry_path,
    sound_registry_path,
):
    if not path.is_file():
        errors.append(f"missing required R4 source: {path.relative_to(root)}")

spell = spell_path.read_text() if spell_path.is_file() else ""
effect = effect_path.read_text() if effect_path.is_file() else ""
events = events_path.read_text() if events_path.is_file() else ""
effect_registry = effect_registry_path.read_text() if effect_registry_path.is_file() else ""
spell_registry = spell_registry_path.read_text() if spell_registry_path.is_file() else ""
sound_registry = sound_registry_path.read_text() if sound_registry_path.is_file() else ""

required_spell_markers = (
    "class SliceAndDiceSpell extends AbstractSpell implements MartialTechnique",
    "MartialTechniqueClass.ROGUE",
    "public static final int MAX_LEVEL = 1;",
    "public static final int EFFECT_DURATION_TICKS = 200;",
    "public static final int MAX_AMPLIFIER = 9;",
    "public static final int BASE_COOLDOWN_SECONDS = 15;",
    "public static final double ATTACK_DAMAGE_PER_STACK = 0.10D;",
    ".setMinRarity(SpellRarity.UNCOMMON)",
    ".setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)",
    ".setCooldownSeconds(BASE_COOLDOWN_SECONDS)",
    "baseManaCost = 0;",
    "manaCostPerLevel = 0;",
    "return CastType.INSTANT;",
    "MartialEffectRegistry.SLICE_AND_DICE.get()",
    "EFFECT_DURATION_TICKS,\n                0,\n                false,\n                false,\n                true",
    "MartialSoundRegistry.SLICE_AND_DICE.get()",
    "ParticleTypes.CRIT",
    "RELEASE_PARTICLES",
    "RELEASE_PARTICLE_RADIUS",
)
for marker in required_spell_markers:
    if marker not in spell:
        errors.append(f"SliceAndDiceSpell missing required behavior: {marker}")

for forbidden in (
    "FixedCooldownSpell",
    "net.spell_engine",
    "spell_power",
    "MartialPowerHelper",
):
    if forbidden in spell:
        errors.append(f"SliceAndDiceSpell contains forbidden R4 dependency/behavior: {forbidden}")

required_effect_markers = (
    "class SliceAndDiceEffect extends MobEffect",
    "MobEffectCategory.BENEFICIAL",
    "0x993333",
    "Attributes.ATTACK_DAMAGE",
    "0.1D",
    "AttributeModifier.Operation.MULTIPLY_BASE",
)
for marker in required_effect_markers:
    if marker not in effect:
        errors.append(f"SliceAndDiceEffect missing source-faithful modifier: {marker}")

required_event_markers = (
    "@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID)",
    "LivingDamageEvent",
    "EventPriority.LOWEST",
    "receiveCanceled = false",
    "event.getAmount() <= 0.0F",
    "event.getSource().is(DamageTypes.PLAYER_ATTACK)",
    "event.getSource().getEntity() instanceof Player attacker",
    "attacker.getEffect(MartialEffectRegistry.SLICE_AND_DICE.get())",
    "current.getAmplifier() >= SliceAndDiceSpell.MAX_AMPLIFIER",
    "int remainingDuration = current.getDuration();",
    "int nextAmplifier = current.getAmplifier() + 1;",
    "remainingDuration,\n                nextAmplifier,\n                false,\n                true,\n                true",
)
for marker in required_event_markers:
    if marker not in events:
        errors.append(f"SliceAndDiceEvents missing required trigger behavior: {marker}")

for forbidden in (
    "event.setAmount",
    "EFFECT_DURATION_TICKS",
    "DamageTypes.ARROW",
):
    if forbidden in events:
        errors.append(f"SliceAndDiceEvents contains forbidden behavior: {forbidden}")

if 'SPELLS.register("slice_and_dice", SliceAndDiceSpell::new)' not in spell_registry:
    errors.append("Slice & Dice is not registered in MartialSpellRegistry")
if 'MOB_EFFECTS.register(\n                    "slice_and_dice",\n                    SliceAndDiceEffect::new' not in effect_registry:
    errors.append("Slice & Dice effect is not registered in MartialEffectRegistry")
if 'register("slice_and_dice")' not in sound_registry:
    errors.append("MartialSoundRegistry missing slice_and_dice")

for relative in (
    "src/main/resources/data/martial_spells/tags/spells/martial_techniques.json",
    "src/main/resources/data/martial_spells/tags/spells/rogue_techniques.json",
):
    path = root / relative
    if not path.is_file():
        errors.append(f"missing spell tag: {relative}")
        continue
    data = json.loads(path.read_text())
    values = data.get("values", [])
    if values.count("martial_spells:slice_and_dice") != 1:
        errors.append(f"Slice & Dice must appear exactly once in {relative}")

sounds_path = root / "src/main/resources/assets/martial_spells/sounds.json"
if not sounds_path.is_file():
    errors.append("missing assets/martial_spells/sounds.json")
else:
    sounds = json.loads(sounds_path.read_text())
    expected = {"sounds": ["martial_spells:slice_and_dice"]}
    if sounds.get("slice_and_dice") != expected:
        errors.append(f"wrong Slice & Dice sound definition: {sounds.get('slice_and_dice')}")

# Frozen Rogues-owned binary identity.
def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()

frozen_assets = {
    "src/main/resources/assets/martial_spells/sounds/slice_and_dice.ogg":
        "34fd984446705092bf88e818a9868b9106db5e6d",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/slice_and_dice.png":
        "c5bdd79703ca6ab2c78a8091778ded3c297ecac2",
    "src/main/resources/assets/martial_spells/textures/mob_effect/slice_and_dice.png":
        "c5bdd79703ca6ab2c78a8091778ded3c297ecac2",
}
for relative, expected in frozen_assets.items():
    path = root / relative
    if not path.is_file():
        errors.append(f"missing frozen R4 asset {relative}; run tools/sync-rogue-r4-assets.ps1")
        continue
    actual = git_blob_sha(path)
    if actual != expected:
        errors.append(f"wrong frozen R4 asset for {relative}: expected {expected}, got {actual}")

lang_path = root / "src/main/resources/assets/martial_spells/lang/en_us.json"
if not lang_path.is_file():
    errors.append("missing en_us.json")
else:
    lang = json.loads(lang_path.read_text())
    required_lang = {
        "effect.martial_spells.slice_and_dice": "Slice & Dice",
        "spell.martial_spells.slice_and_dice": "Slice & Dice",
        "ui.martial_spells.slice_and_dice_duration": "Duration: %s seconds",
        "ui.martial_spells.slice_and_dice_initial_bonus": "Initial Attack Damage: +%s%% base",
        "ui.martial_spells.slice_and_dice_max_bonus": "Maximum Attack Damage: +%s%% base",
    }
    for key, expected in required_lang.items():
        if lang.get(key) != expected:
            errors.append(f"missing/wrong R4 localization {key}")
    for key in (
        "effect.martial_spells.slice_and_dice.description",
        "spell.martial_spells.slice_and_dice.description",
        "spell.martial_spells.slice_and_dice.guide",
    ):
        if key not in lang:
            errors.append(f"missing R4 localization {key}")

if errors:
    print("R4 STATIC AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("R4 static: Slice & Dice registered as a single-level Rogue Martial technique")
print("Behavior: 10s amp-0 start / +10% base Attack Damage per amplifier / melee-only stacking to amp 9")
print("Timer: successful melee impacts preserve remaining duration and do not refresh the 10s window")
print("Fidelity: frozen icon/effect icon + release sound; Spell Engine magic_spark translated to a 20-point vanilla crit ring")
print("R4 STATIC AUDIT PASSED")
