from pathlib import Path
import hashlib
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

# R3 is cumulative: preserve the user-confirmed R2 implementation first.
r2 = subprocess.run([sys.executable, str(root / "tools/audit-rogue-r2.py")], cwd=root)
if r2.returncode != 0:
    print("R3 STATIC AUDIT FAILED: cumulative R2 audit failed")
    sys.exit(r2.returncode)

spell_path = root / "src/main/java/com/w0of26/martialspells/spells/ShadowstepSpell.java"
effect_path = root / "src/main/java/com/w0of26/martialspells/effects/ShadowstepEffect.java"
effect_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java"
spell_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java"
sound_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java"
mixin_path = root / "src/main/java/com/w0of26/martialspells/mixin/TargetGoalShadowstepMixin.java"
mixin_config_path = root / "src/main/resources/martial_spells.mixins.json"

for path in (
    spell_path,
    effect_path,
    effect_registry_path,
    spell_registry_path,
    sound_registry_path,
    mixin_path,
    mixin_config_path,
):
    if not path.is_file():
        errors.append(f"missing required R3 source: {path.relative_to(root)}")

spell = spell_path.read_text() if spell_path.is_file() else ""
effect = effect_path.read_text() if effect_path.is_file() else ""
effect_registry = effect_registry_path.read_text() if effect_registry_path.is_file() else ""
spell_registry = spell_registry_path.read_text() if spell_registry_path.is_file() else ""
sound_registry = sound_registry_path.read_text() if sound_registry_path.is_file() else ""
mixin = mixin_path.read_text() if mixin_path.is_file() else ""
mixin_config = mixin_config_path.read_text() if mixin_config_path.is_file() else ""

required_spell_markers = (
    "class ShadowstepSpell extends AbstractSpell implements MartialTechnique",
    "MartialTechniqueClass.ROGUE",
    "public static final int MAX_LEVEL = 1;",
    "public static final float RANGE = 15.0F;",
    "public static final float BEHIND_TARGET_DISTANCE = 1.0F;",
    "public static final float GROUND_SEARCH_DEPTH = 1.5F;",
    "public static final int SHADOWSTEP_DURATION_TICKS = 30;",
    "public static final double STEALTH_FOLLOW_DISTANCE = 5.0D;",
    "public static final int BASE_COOLDOWN_SECONDS = 12;",
    "public static final int DEPART_PARTICLES = 20;",
    "public static final int ARRIVE_PARTICLES = 10;",
    ".setMinRarity(SpellRarity.RARE)",
    ".setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)",
    ".setCooldownSeconds(BASE_COOLDOWN_SECONDS)",
    "baseManaCost = 0;",
    "manaCostPerLevel = 0;",
    "return CastType.INSTANT;",
    "return Utils.preCastTargetHelper(",
    "TargetEntityCastData",
    "!caster.isAlliedTo(target)",
    "target.getLookAngle().scale(-BEHIND_TARGET_DISTANCE)",
    "ClipContext.Block.COLLIDER",
    "ClipContext.Fluid.NONE",
    "Vec3 destination = resolveDestination(serverLevel, caster, target);",
    "serverPlayer.teleportTo(",
    "MartialSoundRegistry.SHADOW_STEP_DEPART.get()",
    "ParticleTypes.CLOUD",
    "ParticleTypes.POOF",
    "MartialEffectRegistry.SHADOW_STEP.get()",
    "SpellAnimations.ANIMATION_INSTANT_CAST",
)
for marker in required_spell_markers:
    if marker not in spell:
        errors.append(f"ShadowstepSpell missing required behavior: {marker}")

for forbidden in (
    "net.spell_engine",
    "spell_power",
    "shadow_step_arrive",
    "SHADOW_STEP_ARRIVE",
    "resolveSafeDestination",
    "level.noCollision",
    "getWorldBorder",
    "getMinBuildHeight",
    "shadow_step_blocked",
):
    if forbidden in spell:
        errors.append(f"ShadowstepSpell contains forbidden R3 dependency/behavior: {forbidden}")

if 'SPELLS.register("shadow_step", ShadowstepSpell::new)' not in spell_registry:
    errors.append("Shadowstep is not registered in MartialSpellRegistry")
if 'MOB_EFFECTS.register(\n                    "shadow_step",\n                    ShadowstepEffect::new' not in effect_registry:
    errors.append("Shadowstep marker effect is not registered in MartialEffectRegistry")

for marker in (
    "class ShadowstepEffect extends MobEffect",
    "MobEffectCategory.BENEFICIAL",
    "0x000000",
):
    if marker not in effect:
        errors.append(f"ShadowstepEffect missing: {marker}")

required_mixin_markers = (
    "@Mixin(TargetGoal.class)",
    "protected Mob mob;",
    '@Inject(method = "getFollowDistance", at = @At("HEAD"), cancellable = true)',
    "mob.getTarget()",
    "target.hasEffect(MartialEffectRegistry.SHADOW_STEP.get())",
    "cir.setReturnValue(5.0D)",
)
for marker in required_mixin_markers:
    if marker not in mixin:
        errors.append(f"TargetGoalShadowstepMixin missing: {marker}")
if '"TargetGoalShadowstepMixin"' not in mixin_config:
    errors.append("martial_spells.mixins.json does not register TargetGoalShadowstepMixin")

if 'register("shadow_step_depart")' not in sound_registry:
    errors.append("MartialSoundRegistry missing shadow_step_depart")
if "shadow_step_arrive" in sound_registry:
    errors.append("unused frozen shadow_step_arrive sound must not be registered in R3")

# Spell tags: Shadowstep is both a generic Martial technique and a Rogue technique.
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
    if values.count("martial_spells:shadow_step") != 1:
        errors.append(f"Shadowstep must appear exactly once in {relative}")

sounds_path = root / "src/main/resources/assets/martial_spells/sounds.json"
if not sounds_path.is_file():
    errors.append("missing assets/martial_spells/sounds.json")
else:
    sounds = json.loads(sounds_path.read_text())
    expected_depart = {"sounds": ["martial_spells:shadow_step_depart"]}
    if sounds.get("shadow_step_depart") != expected_depart:
        errors.append(f"wrong Shadowstep departure sound definition: {sounds.get('shadow_step_depart')}")
    if "shadow_step_arrive" in sounds:
        errors.append("unused shadow_step_arrive definition must not be present in R3")

# Frozen Rogues-owned assets are synchronized locally and must be byte-exact.
def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()

frozen_assets = {
    "src/main/resources/assets/martial_spells/sounds/shadow_step_depart.ogg":
        "372197145aaaed43ef2e55902451f4355286b85e",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/shadow_step.png":
        "4b5137f52cec53ada756488776e0371b50a95df6",
    "src/main/resources/assets/martial_spells/textures/mob_effect/shadow_step.png":
        "4b5137f52cec53ada756488776e0371b50a95df6",
}
for relative, expected in frozen_assets.items():
    path = root / relative
    if not path.is_file():
        errors.append(f"missing frozen R3 asset {relative}; run tools/sync-rogue-r3-assets.ps1")
        continue
    actual = git_blob_sha(path)
    if actual != expected:
        errors.append(f"wrong frozen R3 asset for {relative}: expected {expected}, got {actual}")

lang_path = root / "src/main/resources/assets/martial_spells/lang/en_us.json"
if not lang_path.is_file():
    errors.append("missing en_us.json")
else:
    lang = json.loads(lang_path.read_text())
    required_lang = {
        "effect.martial_spells.shadow_step": "Shadowstep",
        "spell.martial_spells.shadow_step": "Shadowstep",
        "ui.martial_spells.shadow_step_range": "Target Range: %s blocks",
        "ui.martial_spells.shadow_step_distance": "Behind Target: %s blocks",
        "ui.martial_spells.shadow_step_untraceable": "Reduced Tracking: %s seconds at %s blocks",
    }
    for key, expected in required_lang.items():
        if lang.get(key) != expected:
            errors.append(f"missing/wrong R3 localization {key}")
    for key in (
        "spell.martial_spells.shadow_step.description",
        "spell.martial_spells.shadow_step.guide",
    ):
        if key not in lang:
            errors.append(f"missing R3 localization {key}")

if errors:
    print("R3 STATIC AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("R3 static: Shadowstep registered as a single-level Rogue Martial technique")
print("Behavior: required 15-block harmful aim / source-shaped 1.0-block behind-target teleport / 30-tick 5-block anti-tracking")
print("Destination gate: disabled for R3 validation; no collision/world-border/build-height rejection")
print("Fidelity: frozen icon/effect icon + departure sound / 20 cloud depart + 10 poof arrive")
print("R3 STATIC AUDIT PASSED")
