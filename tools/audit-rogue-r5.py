from pathlib import Path
import hashlib
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

# R5 is cumulative: locked R2-R4 behavior must remain intact.
r4 = subprocess.run([sys.executable, str(root / "tools/audit-rogue-r4.py")], cwd=root)
if r4.returncode != 0:
    print("R5 STATIC AUDIT FAILED: cumulative R4 audit failed")
    sys.exit(r4.returncode)

spell_path = root / "src/main/java/com/w0of26/martialspells/spells/VanishSpell.java"
effect_path = root / "src/main/java/com/w0of26/martialspells/effects/StealthEffect.java"
events_path = root / "src/main/java/com/w0of26/martialspells/events/VanishEvents.java"
invisibility_mixin_path = root / "src/main/java/com/w0of26/martialspells/mixin/LivingEntityStealthMixin.java"
target_mixin_path = root / "src/main/java/com/w0of26/martialspells/mixin/TargetGoalShadowstepMixin.java"
effect_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java"
spell_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java"
sound_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java"
mixins_json_path = root / "src/main/resources/martial_spells.mixins.json"

for path in (
    spell_path,
    effect_path,
    events_path,
    invisibility_mixin_path,
    target_mixin_path,
    effect_registry_path,
    spell_registry_path,
    sound_registry_path,
    mixins_json_path,
):
    if not path.is_file():
        errors.append(f"missing required R5 source: {path.relative_to(root)}")

spell = spell_path.read_text() if spell_path.is_file() else ""
effect = effect_path.read_text() if effect_path.is_file() else ""
events = events_path.read_text() if events_path.is_file() else ""
invisibility_mixin = invisibility_mixin_path.read_text() if invisibility_mixin_path.is_file() else ""
target_mixin = target_mixin_path.read_text() if target_mixin_path.is_file() else ""
effect_registry = effect_registry_path.read_text() if effect_registry_path.is_file() else ""
spell_registry = spell_registry_path.read_text() if spell_registry_path.is_file() else ""
sound_registry = sound_registry_path.read_text() if sound_registry_path.is_file() else ""
mixins_json = mixins_json_path.read_text() if mixins_json_path.is_file() else ""

required_spell_markers = (
    "class VanishSpell extends AbstractSpell implements MartialTechnique",
    "MartialTechniqueClass.ROGUE",
    "public static final int MAX_LEVEL = 1;",
    "public static final int EFFECT_DURATION_TICKS = 160;",
    "public static final int BASE_COOLDOWN_SECONDS = 30;",
    "public static final double MOVEMENT_SPEED_MULTIPLIER = -0.50D;",
    "public static final double STEALTH_FOLLOW_DISTANCE = 1.0D;",
    ".setMinRarity(SpellRarity.EPIC)",
    ".setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)",
    ".setCooldownSeconds(BASE_COOLDOWN_SECONDS)",
    "baseManaCost = 0;",
    "manaCostPerLevel = 0;",
    "return CastType.INSTANT;",
    "MartialEffectRegistry.STEALTH.get()",
    "EFFECT_DURATION_TICKS,\n                0,\n                false,\n                false,\n                true",
    "caster.setInvisible(true);",
    "MartialSoundRegistry.VANISH_COMBINED.get()",
    "ParticleTypes.SMOKE",
    "ParticleTypes.POOF",
    "ParticleTypes.CAMPFIRE_COSY_SMOKE",
)
for marker in required_spell_markers:
    if marker not in spell:
        errors.append(f"VanishSpell missing required behavior: {marker}")

for forbidden in (
    "FixedCooldownSpell",
    "net.spell_engine",
    "spell_power",
    "vanish_release",
):
    if forbidden in spell:
        errors.append(f"VanishSpell contains forbidden R5 dependency/behavior: {forbidden}")

required_effect_markers = (
    "class StealthEffect extends MobEffect",
    "MobEffectCategory.BENEFICIAL",
    "0xAAAAAA",
    "Attributes.MOVEMENT_SPEED",
    "-0.5D",
    "AttributeModifier.Operation.MULTIPLY_BASE",
)
for marker in required_effect_markers:
    if marker not in effect:
        errors.append(f"StealthEffect missing source-faithful modifier: {marker}")

required_event_markers = (
    "LivingAttackEvent",
    "event.getSource().is(DamageTypes.PLAYER_ATTACK)",
    "event.getSource().getEntity() instanceof Player attacker",
    "breakStealth(event.getEntity());",
    "LivingEntityUseItemEvent.Start",
    "PlayerInteractEvent.RightClickItem",
    "SpellOnCastEvent",
    "!VanishSpell.SPELL_ID.toString().equals(event.getSpellId())",
    "MobEffectEvent.Remove",
    "MobEffectEvent.Expired",
    "MartialSoundRegistry.STEALTH_LEAVE.get()",
    "LEAVE_PARTICLES = 20",
)
for marker in required_event_markers:
    if marker not in events:
        errors.append(f"VanishEvents missing required source break/removal behavior: {marker}")

for forbidden in (
    "DamageTypes.ARROW",
    "DamageTypes.TRIDENT",
    "vanish_release",
    "net.spell_engine",
):
    if forbidden in events:
        errors.append(f"VanishEvents contains forbidden behavior/dependency: {forbidden}")

required_invisibility_markers = (
    '@Mixin(LivingEntity.class)',
    '@Inject(method = "updateInvisibilityStatus", at = @At("TAIL"))',
    "entity.hasEffect(MartialEffectRegistry.STEALTH.get())",
    "entity.setInvisible(true);",
)
for marker in required_invisibility_markers:
    if marker not in invisibility_mixin:
        errors.append(f"LivingEntityStealthMixin missing invisibility integration: {marker}")

if '"LivingEntityStealthMixin"' not in mixins_json:
    errors.append("LivingEntityStealthMixin is not enabled in martial_spells.mixins.json")

if "target.hasEffect(MartialEffectRegistry.STEALTH.get())" not in target_mixin:
    errors.append("TargetGoal mixin is missing Stealth target suppression")
if "cir.setReturnValue(1.0D);" not in target_mixin:
    errors.append("TargetGoal Stealth follow distance must be 1.0 block")
if "cir.setReturnValue(5.0D);" not in target_mixin:
    errors.append("R3 Shadowstep 5.0-block follow distance regression")

if 'SPELLS.register("vanish", VanishSpell::new)' not in spell_registry:
    errors.append("Vanish is not registered in MartialSpellRegistry")
if 'MOB_EFFECTS.register(\n                    "stealth",\n                    StealthEffect::new' not in effect_registry:
    errors.append("Stealth is not registered in MartialEffectRegistry")
if 'register("vanish_combined")' not in sound_registry:
    errors.append("MartialSoundRegistry missing vanish_combined")
if 'register("stealth_leave")' not in sound_registry:
    errors.append("MartialSoundRegistry missing stealth_leave")

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
    if values.count("martial_spells:vanish") != 1:
        errors.append(f"Vanish must appear exactly once in {relative}")

sounds_path = root / "src/main/resources/assets/martial_spells/sounds.json"
if not sounds_path.is_file():
    errors.append("missing assets/martial_spells/sounds.json")
else:
    sounds = json.loads(sounds_path.read_text())
    if sounds.get("vanish_combined") != {"sounds": ["martial_spells:vanish_combined"]}:
        errors.append("wrong vanish_combined sound definition")
    if sounds.get("stealth_leave") != {"sounds": ["martial_spells:stealth_leave"]}:
        errors.append("wrong stealth_leave sound definition")
    if "vanish_release" in sounds:
        errors.append("unused frozen vanish_release must not be registered for R5")

# Frozen Rogues-owned binary identity.
def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()

frozen_assets = {
    "src/main/resources/assets/martial_spells/sounds/vanish_combined.ogg":
        "ba68a4b7f561ce6842ffb1730f5c6fb9bee60c39",
    "src/main/resources/assets/martial_spells/sounds/stealth_leave.ogg":
        "43d306ab3344aa9c2fa6636ba527aaeb53967384",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/vanish.png":
        "7ee700360ad64723bb32660c6cee5f01fdd73ad7",
    "src/main/resources/assets/martial_spells/textures/mob_effect/stealth.png":
        "7ee700360ad64723bb32660c6cee5f01fdd73ad7",
}
for relative, expected in frozen_assets.items():
    path = root / relative
    if not path.is_file():
        errors.append(f"missing frozen R5 asset {relative}; run tools/sync-rogue-r5-assets.ps1")
        continue
    actual = git_blob_sha(path)
    if actual != expected:
        errors.append(f"wrong frozen R5 asset for {relative}: expected {expected}, got {actual}")

lang_path = root / "src/main/resources/assets/martial_spells/lang/en_us.json"
if not lang_path.is_file():
    errors.append("missing en_us.json")
else:
    lang = json.loads(lang_path.read_text())
    required_lang = {
        "effect.martial_spells.stealth": "Stealth",
        "spell.martial_spells.vanish": "Vanish",
        "ui.martial_spells.vanish_duration": "Stealth Duration: %s seconds",
        "ui.martial_spells.vanish_movement_penalty": "Movement Speed: -%s%% base",
        "ui.martial_spells.vanish_tracking_range": "Enemy Follow Distance: %s blocks",
    }
    for key, expected in required_lang.items():
        if lang.get(key) != expected:
            errors.append(f"missing/wrong R5 localization {key}")
    for key in (
        "effect.martial_spells.stealth.description",
        "spell.martial_spells.vanish.description",
        "spell.martial_spells.vanish.guide",
    ):
        if key not in lang:
            errors.append(f"missing R5 localization {key}")

if errors:
    print("R5 STATIC AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("R5 static: Vanish registered as a single-level Epic Rogue Martial technique")
print("Behavior: 8s Stealth / -50% base movement speed / true invisibility / 1-block hostile follow distance")
print("Breaks: direct player melee attack, incoming hit, timed or instant item use, and any other Iron's spell cast")
print("Fidelity: frozen Vanish/Stealth icon, vanish_combined activation sound, stealth_leave removal sound")
print("Particles: Spell Engine smoke_medium translated to vanilla smoke; vanilla poof/campfire smoke retained")
print("R5 STATIC AUDIT PASSED")
