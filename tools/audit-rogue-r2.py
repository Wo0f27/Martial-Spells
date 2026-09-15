from pathlib import Path
import hashlib
import json
import sys

root = Path(__file__).resolve().parents[1]
errors = []

spell_path = root / "src/main/java/com/w0of26/martialspells/spells/ShockPowderSpell.java"
registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java"
sound_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java"
mod_path = root / "src/main/java/com/w0of26/martialspells/MartialSpells.java"

for path in (spell_path, registry_path, sound_registry_path, mod_path):
    if not path.is_file():
        errors.append(f"missing required R2 source: {path.relative_to(root)}")

spell = spell_path.read_text() if spell_path.is_file() else ""
registry = registry_path.read_text() if registry_path.is_file() else ""
sound_registry = sound_registry_path.read_text() if sound_registry_path.is_file() else ""
mod_java = mod_path.read_text() if mod_path.is_file() else ""

required_spell_markers = (
    "class ShockPowderSpell extends AbstractSpell implements MartialTechnique",
    "MartialTechniqueClass.ROGUE",
    "public static final int MAX_LEVEL = 1;",
    "public static final float RANGE = 5.0F;",
    "public static final float VERTICAL_RANGE_MULTIPLIER = 0.5F;",
    "public static final int STUN_DURATION_TICKS = 40;",
    "public static final int BASE_COOLDOWN_SECONDS = 16;",
    "public static final float CONTROL_HEALTH_BASE = 50.0F;",
    "public static final float CONTROL_ATTACK_DAMAGE_MULTIPLIER = 2.0F;",
    ".setMinRarity(SpellRarity.UNCOMMON)",
    ".setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)",
    ".setMaxLevel(MAX_LEVEL)",
    ".setCooldownSeconds(BASE_COOLDOWN_SECONDS)",
    "baseManaCost = 0;",
    "manaCostPerLevel = 0;",
    "caster.getAttribute(Attributes.ATTACK_DAMAGE)",
    "StunService.apply(target, caster, STUN_DURATION_TICKS)",
    "caster.getBoundingBox().inflate(",
    "targetCenter.distanceToSqr(origin) > RANGE * RANGE",
    "ClipContext.Block.COLLIDER",
    "ClipContext.Fluid.NONE",
    "caster.isAlliedTo(target)",
    "target.getMaxHealth() > controlHealthLimit",
    "MartialSoundRegistry.SHOCK_POWDER_RELEASE.get()",
    "MartialSoundRegistry.SHOCK_POWDER_IMPACT.get()",
    "ParticleTypes.ELECTRIC_SPARK",
)
for marker in required_spell_markers:
    if marker not in spell:
        errors.append(f"ShockPowderSpell missing required behavior: {marker}")

for forbidden in (
    "FixedCooldownSpell",
    "net.spell_engine",
    "spell_power",
    "MartialPowerHelper",
    "getMartialPower",
):
    if forbidden in spell:
        errors.append(f"ShockPowderSpell contains forbidden R2 dependency/behavior: {forbidden}")

if 'SPELLS.register("shock_powder", ShockPowderSpell::new)' not in registry:
    errors.append("Shock Powder is not registered in MartialSpellRegistry")

for marker in (
    'register("shock_powder_release")',
    'register("shock_powder_impact")',
    "DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MartialSpells.MOD_ID)",
):
    if marker not in sound_registry:
        errors.append(f"MartialSoundRegistry missing: {marker}")

if "MartialSoundRegistry.register(modEventBus);" not in mod_java:
    errors.append("MartialSpells does not register MartialSoundRegistry")

# Spell tags: R2 must be both a generic Martial technique and specifically Rogue.
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
    if "martial_spells:shock_powder" not in values:
        errors.append(f"Shock Powder missing from {relative}")
    if values.count("martial_spells:shock_powder") != 1:
        errors.append(f"Shock Powder duplicated in {relative}")

# Sound definitions are deliberately only the two R2 source-owned sounds.
sounds_path = root / "src/main/resources/assets/martial_spells/sounds.json"
expected_sounds = {
    "shock_powder_release": {"sounds": ["martial_spells:shock_powder_release"]},
    "shock_powder_impact": {"sounds": ["martial_spells:shock_powder_impact"]},
}
if not sounds_path.is_file():
    errors.append("missing assets/martial_spells/sounds.json")
else:
    sounds = json.loads(sounds_path.read_text())
    if sounds != expected_sounds:
        errors.append(f"R2 sounds.json drifted: {sounds}")

# Frozen binary identity. The assets are intentionally synchronized locally
# rather than approximated or re-encoded in this checkpoint.
def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()

assets = {
    "src/main/resources/assets/martial_spells/sounds/shock_powder_release.ogg":
        "92f7f6172ac4860190311a9249d86c90da2fdd22",
    "src/main/resources/assets/martial_spells/sounds/shock_powder_impact.ogg":
        "3c6c868898d0fcd1525f224d86e9ad4bf1cd4399",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/shock_powder.png":
        "af728d73e743688872022d9a43c8ba00ae238c30",
}
for relative, expected in assets.items():
    path = root / relative
    if not path.is_file():
        errors.append(f"missing frozen asset {relative}; run tools/sync-rogue-r2-assets.ps1")
        continue
    actual = git_blob_sha(path)
    if actual != expected:
        errors.append(f"wrong frozen asset for {relative}: expected {expected}, got {actual}")

# Localization is a required R2 presentation surface.
lang_path = root / "src/main/resources/assets/martial_spells/lang/en_us.json"
if not lang_path.is_file():
    errors.append("missing en_us.json")
else:
    lang = json.loads(lang_path.read_text())
    required_lang = {
        "spell.martial_spells.shock_powder": "Shock Powder",
        "spell.martial_spells.shock_powder.description":
            "Scatter electrified powder around you, stunning nearby enemies that can be overwhelmed.",
        "ui.martial_spells.shock_powder_stun_duration": "Stun Duration: %s seconds",
        "ui.martial_spells.shock_powder_control_limit": "Control Limit: %s target max health",
    }
    for key, expected in required_lang.items():
        if lang.get(key) != expected:
            errors.append(f"missing/wrong R2 localization {key}")
    if "spell.martial_spells.shock_powder.guide" not in lang:
        errors.append("missing Shock Powder guide localization")

if errors:
    print("R2 STATIC AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("R2 static: Shock Powder registered as a single-level Rogue Martial technique")
print("Behavior: 5-block / 0.5Y area, 2s shared stun, 50 + 2x Attack Damage control cap, base CD 16")
print("Fidelity: frozen icon + release/impact sounds verified")
print("R2 STATIC AUDIT PASSED")
