from pathlib import Path
import hashlib
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []


def read_utf8(path: Path) -> str:
    """Use an explicit encoding so the audit behaves the same on Windows."""
    return path.read_text(encoding="utf-8")


# R6 is cumulative: R2-R5 must remain intact.
r5 = subprocess.run([sys.executable, str(root / "tools/audit-rogue-r5.py")], cwd=root)
if r5.returncode != 0:
    print("R6 STATIC AUDIT FAILED: cumulative R5 audit failed")
    sys.exit(r5.returncode)

spell_path = root / "src/main/java/com/w0of26/martialspells/spells/MutilateSpell.java"
manager_path = root / "src/main/java/com/w0of26/martialspells/combat/MutilateAttackManager.java"
attack_accessor_path = root / "src/main/java/com/w0of26/martialspells/mixin/LivingEntityAttackStrengthAccessor.java"
invulnerability_accessor_path = root / "src/main/java/com/w0of26/martialspells/mixin/EntityInvulnerabilityAccessor.java"
spell_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java"
sound_registry_path = root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java"
mixins_path = root / "src/main/resources/martial_spells.mixins.json"
sync_script_path = root / "tools/sync-rogue-r6-assets.ps1"

for path in (
    spell_path,
    manager_path,
    attack_accessor_path,
    invulnerability_accessor_path,
    spell_registry_path,
    sound_registry_path,
    mixins_path,
    sync_script_path,
):
    if not path.is_file():
        errors.append(f"missing required R6 source: {path.relative_to(root)}")

spell = read_utf8(spell_path) if spell_path.is_file() else ""
manager = read_utf8(manager_path) if manager_path.is_file() else ""
attack_accessor = read_utf8(attack_accessor_path) if attack_accessor_path.is_file() else ""
invulnerability_accessor = read_utf8(invulnerability_accessor_path) if invulnerability_accessor_path.is_file() else ""
spell_registry = read_utf8(spell_registry_path) if spell_registry_path.is_file() else ""
sound_registry = read_utf8(sound_registry_path) if sound_registry_path.is_file() else ""
mixins = json.loads(read_utf8(mixins_path)) if mixins_path.is_file() else {}
sync_script = read_utf8(sync_script_path) if sync_script_path.is_file() else ""

required_spell_markers = (
    "class MutilateSpell extends AbstractSpell implements MartialTechnique",
    "MartialTechniqueClass.ROGUE",
    "public static final int MAX_LEVEL = 1;",
    "public static final float RANGE = 3.0F;",
    "public static final float ARC_DEGREES = 160.0F;",
    "public static final float HITBOX_WIDTH_FACTOR = 0.5F;",
    "public static final float HITBOX_HEIGHT_FACTOR = 0.2F;",
    "public static final float ATTACK_DELAY_FRACTION = 0.5F;",
    "public static final int BASE_COOLDOWN_SECONDS = 12;",
    ".setMinRarity(SpellRarity.EPIC)",
    ".setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)",
    ".setCooldownSeconds(BASE_COOLDOWN_SECONDS)",
    "baseManaCost = 0;",
    "manaCostPerLevel = 0;",
    "return CastType.INSTANT;",
    "MutilateAttackManager.begin(player)",
    '"mutilate_dual_slash_cross"',
    "return new AnimationHolder(MUTILATE_ANIMATION, true, false);",
)
for marker in required_spell_markers:
    if marker not in spell:
        errors.append(f"MutilateSpell missing required behavior/presentation: {marker}")

for forbidden in (
    "net.spell_engine",
    "net.spell_power",
    "spell_power:physical_melee_dual",
):
    if forbidden in spell or forbidden in manager:
        errors.append(f"R6 contains forbidden runtime dependency/reference: {forbidden}")

required_manager_markers = (
    "class MutilateAttackManager",
    "20.0D / attackSpeed",
    "MutilateSpell.ATTACK_DELAY_FRACTION",
    "MutilateSpell.RANGE",
    "MutilateSpell.ARC_DEGREES",
    "MutilateSpell.HITBOX_WIDTH_FACTOR",
    "MutilateSpell.HITBOX_HEIGHT_FACTOR",
    "player.getOffhandItem()",
    "stack.getAttributeModifiers(slot).get(Attributes.ATTACK_DAMAGE)",
    "AttributeModifier.Operation.ADDITION",
    "case MULTIPLY_BASE -> multiplyBase += modifier.getAmount();",
    "case MULTIPLY_TOTAL -> multiplyTotal += modifier.getAmount();",
    "dualWielded / singleHanded - 1.0D",
    "AttributeModifier.Operation.MULTIPLY_TOTAL",
    "attackStrength.martialSpells$setAttackStrengthTicker(100)",
    "invulnerability.martialSpells$setInvulnerableTime(0)",
    "player.attack(target)",
    "martialSpells$setAttackStrengthTicker(\n                    originalAttackStrengthTicker",
    "martialSpells$setInvulnerableTime(\n                            originalInvulnerableTime",
    "MartialSoundRegistry.MUTILATE_IMPACT.get()",
    "SoundEvents.PLAYER_ATTACK_SWEEP",
    "player.isAlliedTo(target)",
    "ClipContext.Block.COLLIDER",
)
for marker in required_manager_markers:
    if marker not in manager:
        errors.append(f"MutilateAttackManager missing required source translation: {marker}")

# Source FX semantics: the swing cue belongs to attack start, not the delayed
# target-contact block. This also guarantees feedback when Mutilate whiffs.
begin_index = manager.find("public static void begin(ServerPlayer player)")
impact_index = manager.find("private static void performAttack(ServerPlayer player)")
sweep_index = manager.find("SoundEvents.PLAYER_ATTACK_SWEEP")
if min(begin_index, impact_index, sweep_index) < 0 or not (begin_index < sweep_index < impact_index):
    errors.append("Mutilate swing cue must play during begin(), before delayed performAttack()")

required_attack_accessor = (
    '@Mixin(LivingEntity.class)',
    '@Accessor("attackStrengthTicker")',
    "martialSpells$getAttackStrengthTicker",
    "martialSpells$setAttackStrengthTicker",
)
for marker in required_attack_accessor:
    if marker not in attack_accessor:
        errors.append(f"attack-strength accessor missing: {marker}")

required_invulnerability_accessor = (
    '@Mixin(Entity.class)',
    '@Accessor("invulnerableTime")',
    "martialSpells$getInvulnerableTime",
    "martialSpells$setInvulnerableTime",
)
for marker in required_invulnerability_accessor:
    if marker not in invulnerability_accessor:
        errors.append(f"invulnerability accessor missing: {marker}")

if 'SPELLS.register("mutilate", MutilateSpell::new)' not in spell_registry:
    errors.append("Mutilate is not registered in MartialSpellRegistry")
if 'register("mutilate_impact")' not in sound_registry:
    errors.append("MartialSoundRegistry missing mutilate_impact")

for mixin_name in (
    "LivingEntityAttackStrengthAccessor",
    "EntityInvulnerabilityAccessor",
):
    if mixin_name not in mixins.get("mixins", []):
        errors.append(f"mixins config missing {mixin_name}")

for relative in (
    "src/main/resources/data/martial_spells/tags/spells/martial_techniques.json",
    "src/main/resources/data/martial_spells/tags/spells/rogue_techniques.json",
):
    path = root / relative
    if not path.is_file():
        errors.append(f"missing spell tag: {relative}")
        continue
    data = json.loads(read_utf8(path))
    values = data.get("values", [])
    if values.count("martial_spells:mutilate") != 1:
        errors.append(f"Mutilate must appear exactly once in {relative}")

sounds_path = root / "src/main/resources/assets/martial_spells/sounds.json"
if not sounds_path.is_file():
    errors.append("missing assets/martial_spells/sounds.json")
else:
    sounds = json.loads(read_utf8(sounds_path))
    expected = {"sounds": ["martial_spells:mutilate_impact"]}
    if sounds.get("mutilate_impact") != expected:
        errors.append(f"wrong Mutilate sound definition: {sounds.get('mutilate_impact')}")

lang_path = root / "src/main/resources/assets/martial_spells/lang/en_us.json"
if not lang_path.is_file():
    errors.append("missing en_us.json")
else:
    lang = json.loads(read_utf8(lang_path))
    required_lang = {
        "spell.martial_spells.mutilate": "Mutilate",
        "ui.martial_spells.mutilate_range": "Melee Range: %s blocks",
        "ui.martial_spells.mutilate_arc": "Forward Arc: %s degrees",
        "ui.martial_spells.mutilate_dual_damage": "Damage: main-hand melee + offhand weapon/unarmed contribution",
        "ui.martial_spells.mutilate_delay": "Contact: %s%% of current melee attack cycle",
    }
    for key, expected in required_lang.items():
        if lang.get(key) != expected:
            errors.append(f"missing/wrong R6 localization {key}")
    for key in (
        "spell.martial_spells.mutilate.description",
        "spell.martial_spells.mutilate.guide",
    ):
        if key not in lang:
            errors.append(f"missing R6 localization {key}")

# Frozen binary/text asset identity.
def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()

frozen_assets = {
    "src/main/resources/assets/martial_spells/sounds/mutilate_impact.ogg":
        "e365f284a43fd0ae447b09652f4144f8e0c2c09b",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/mutilate.png":
        "fb98fa0cacdbb47dd5b3bf2c162583d419a34c62",
    "src/main/resources/assets/martial_spells/player_animation/mutilate_dual_slash_cross.json":
        "02c168e0fd4d1ea6b39f2843dd05365aedf112d0",
}
for relative, expected in frozen_assets.items():
    path = root / relative
    if not path.is_file():
        errors.append(f"missing frozen R6 asset {relative}; run tools/sync-rogue-r6-assets.ps1")
        continue
    actual = git_blob_sha(path)
    if actual != expected:
        errors.append(f"wrong frozen R6 asset for {relative}: expected {expected}, got {actual}")

for marker in (
    'SpellEngineCommit = "76cd9e128468ebe005463c729ec73eff7de5fb68"',
    'player_animations/weapon_dual_slash_cross.json',
    'player_animation/mutilate_dual_slash_cross.json',
    '02c168e0fd4d1ea6b39f2843dd05365aedf112d0',
):
    if marker not in sync_script:
        errors.append(f"R6 asset sync missing frozen animation marker: {marker}")

roadmap_path = root / "ROGUE_SPELL_PORT.md"
if not roadmap_path.is_file():
    errors.append("missing ROGUE_SPELL_PORT.md")
else:
    roadmap = read_utf8(roadmap_path)
    # Check semantic pieces independently rather than one Markdown-sensitive
    # substring. This also avoids locale/encoding false negatives on Windows.
    for marker in (
        "R6",
        "Mutilate",
        "IMPLEMENTED / VALIDATING",
        "3-block/160-degree",
        "physical_melee_dual",
        "player.attack(...)"
    ):
        if marker not in roadmap:
            errors.append(f"R6 roadmap missing marker: {marker}")

if errors:
    print("R6 STATIC AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("R6 static: Mutilate registered as a single-level Epic Rogue Martial technique")
print("Delivery: delayed at 50% of the current melee attack cycle; 3-block / 160-degree source-shaped target geometry")
print("Damage: frozen physical_melee_dual main+offhand calculation translated through temporary MULTIPLY_TOTAL Attack Damage")
print("Impact: fully charged vanilla player.attack per direct target with temporary target i-frame bypass/restoration")
print("Presentation: exact frozen dual-cross-slash PlayerAnimator JSON + immediate swing cue; exact Rogues impact sound/icon")
print("R6 STATIC AUDIT PASSED")
