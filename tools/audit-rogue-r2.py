from pathlib import Path
import hashlib
import json
import sys

root = Path(__file__).resolve().parents[1]
errors = []

spell_path = root / "src/main/java/com/w0of26/martialspells/spells/ShockPowderSpell.java"
registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java"
sound_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java"
particle_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialParticleRegistry.java"
client_events_path = root / "src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java"
smoke_particle_path = root / "src/main/java/com/w0of26/martialspells/client/particle/ShockPowderSmokeParticle.java"
arc_particle_path = root / "src/main/java/com/w0of26/martialspells/client/particle/ShockPowderArcParticle.java"
mod_path = root / "src/main/java/com/w0of26/martialspells/MartialSpells.java"

required_sources = (
    spell_path,
    registry_path,
    sound_registry_path,
    particle_registry_path,
    client_events_path,
    smoke_particle_path,
    arc_particle_path,
    mod_path,
)
for path in required_sources:
    if not path.is_file():
        errors.append(f"missing required R2 source: {path.relative_to(root)}")

spell = spell_path.read_text() if spell_path.is_file() else ""
registry = registry_path.read_text() if registry_path.is_file() else ""
sound_registry = sound_registry_path.read_text() if sound_registry_path.is_file() else ""
particle_registry = particle_registry_path.read_text() if particle_registry_path.is_file() else ""
client_events = client_events_path.read_text() if client_events_path.is_file() else ""
smoke_particle = smoke_particle_path.read_text() if smoke_particle_path.is_file() else ""
arc_particle = arc_particle_path.read_text() if arc_particle_path.is_file() else ""
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
    "MartialParticleRegistry.SHOCK_POWDER_SMOKE.get()",
    "MartialParticleRegistry.SHOCK_POWDER_ARC.get()",
)
for marker in required_spell_markers:
    if marker not in spell:
        errors.append(f"ShockPowderSpell missing required behavior: {marker}")

# Source batch identity: three smoke groups 50/60/50 and two arc groups 6/8.
for count_marker in ("\n                50,", "\n                60,", "\n                6,", "\n                8,"):
    if count_marker not in spell:
        errors.append(f"Shock Powder VFX missing source-shaped batch count: {count_marker.strip()}")
if spell.count("MartialParticleRegistry.SHOCK_POWDER_SMOKE.get()") != 3:
    errors.append("Shock Powder must emit exactly three custom smoke batches")
if spell.count("MartialParticleRegistry.SHOCK_POWDER_ARC.get()") != 2:
    errors.append("Shock Powder must emit exactly two custom arc batches")

for forbidden in (
    "FixedCooldownSpell",
    "net.spell_engine",
    "spell_power",
    "MartialPowerHelper",
    "getMartialPower",
    "ParticleTypes.SMOKE",
    "ParticleTypes.CLOUD",
    "ParticleTypes.ELECTRIC_SPARK",
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

for marker in (
    'PARTICLES.register("shock_powder_smoke", () -> new SimpleParticleType(false))',
    'PARTICLES.register("shock_powder_arc", () -> new SimpleParticleType(false))',
):
    if marker not in particle_registry:
        errors.append(f"MartialParticleRegistry missing: {marker}")

for marker in (
    "MartialParticleRegistry.SHOCK_POWDER_SMOKE.get(), ShockPowderSmokeParticle.Provider::new",
    "MartialParticleRegistry.SHOCK_POWDER_ARC.get(), ShockPowderArcParticle.Provider::new",
):
    if marker not in client_events:
        errors.append(f"client particle provider missing: {marker}")

for name, source in (("smoke", smoke_particle), ("arc", arc_particle)):
    for marker in (
        "extends TextureSheetParticle",
        "implements ParticleProvider<SimpleParticleType>",
        "ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT",
    ):
        if marker not in source:
            errors.append(f"Shock Powder {name} particle missing: {marker}")

if "MartialSoundRegistry.register(modEventBus);" not in mod_java:
    errors.append("MartialSpells does not register MartialSoundRegistry")
if "MartialParticleRegistry.register(modEventBus);" not in mod_java:
    errors.append("MartialSpells does not register MartialParticleRegistry")

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

# Sound definitions are deliberately only the two source-owned R2 sounds.
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

# Frozen binary identity for Rogues-owned icon/audio. These assets are synced
# locally and must remain byte-identical to the frozen upstream source.
def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()

frozen_assets = {
    "src/main/resources/assets/martial_spells/sounds/shock_powder_release.ogg":
        "92f7f6172ac4860190311a9249d86c90da2fdd22",
    "src/main/resources/assets/martial_spells/sounds/shock_powder_impact.ogg":
        "3c6c868898d0fcd1525f224d86e9ad4bf1cd4399",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/shock_powder.png":
        "af728d73e743688872022d9a43c8ba00ae238c30",
}
for relative, expected in frozen_assets.items():
    path = root / relative
    if not path.is_file():
        errors.append(f"missing frozen asset {relative}; run tools/sync-rogue-r2-assets.ps1")
        continue
    actual = git_blob_sha(path)
    if actual != expected:
        errors.append(f"wrong frozen asset for {relative}: expected {expected}, got {actual}")

# Martial-owned particle resources must be real PNGs and their particle JSONs
# must point only at Martial Spells textures, never Spell Engine assets.
particle_jsons = {
    "src/main/resources/assets/martial_spells/particles/shock_powder_smoke.json": [
        "martial_spells:shock_powder_smoke_0",
        "martial_spells:shock_powder_smoke_1",
        "martial_spells:shock_powder_smoke_2",
        "martial_spells:shock_powder_smoke_3",
    ],
    "src/main/resources/assets/martial_spells/particles/shock_powder_arc.json": [
        "martial_spells:shock_powder_arc_0",
        "martial_spells:shock_powder_arc_1",
    ],
}
for relative, expected_textures in particle_jsons.items():
    path = root / relative
    if not path.is_file():
        errors.append(f"missing custom particle definition: {relative}")
        continue
    data = json.loads(path.read_text())
    if data.get("textures") != expected_textures:
        errors.append(f"custom particle texture list drifted for {relative}: {data}")
    if "spell_engine:" in path.read_text():
        errors.append(f"Spell Engine particle namespace leaked into {relative}")

original_particle_pngs = [
    f"src/main/resources/assets/martial_spells/textures/particle/shock_powder_smoke_{i}.png"
    for i in range(4)
] + [
    f"src/main/resources/assets/martial_spells/textures/particle/shock_powder_arc_{i}.png"
    for i in range(2)
]
for relative in original_particle_pngs:
    path = root / relative
    if not path.is_file():
        errors.append(f"missing Martial-owned particle texture: {relative}")
        continue
    if path.read_bytes()[:8] != b"\x89PNG\r\n\x1a\n":
        errors.append(f"particle texture is not a binary PNG: {relative}")

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
print("Fidelity: frozen icon/sounds + Martial-owned custom smoke/arc particle textures verified")
print("R2 STATIC AUDIT PASSED")
