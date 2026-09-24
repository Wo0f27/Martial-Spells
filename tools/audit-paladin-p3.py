from pathlib import Path
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

p2 = subprocess.run(
    [sys.executable, str(root / "tools" / "audit-paladin-p2.py")],
    cwd=root,
    text=True,
    capture_output=True,
)
output = (p2.stdout + p2.stderr).strip()
if output:
    print(output)
if p2.returncode != 0:
    errors.append("P2 Retribution/protection audit failed")

spell_dir = root / "src/main/java/com/w0of26/martialspells/spells"
effect_dir = root / "src/main/java/com/w0of26/martialspells/effects"
entity_dir = root / "src/main/java/com/w0of26/martialspells/entity"

channel = (spell_dir / "PaladinChannelSupport.java").read_text(encoding="utf-8")
for token in (
    "nextImpactIndex + 0.5F",
    "effectiveCastTicks / (float) channelTicks",
    "getEffectiveSpellCooldown",
    "fullEffectiveCooldown * progress",
    "getManaCost(spellLevel) * progress",
    "SpellOnCastEvent",
    "SpellCooldownAddedEvent.Pre",
    "SpellCooldownAddedEvent.Post",
    "ServerConfigs.CREATIVE_MANA_COST",
    "ServerConfigs.CREATIVE_COOLDOWN",
    "SyncManaPacket",
    "settleFullCast",
):
    if token not in channel:
        errors.append(f"PaladinChannelSupport missing {token}")

holy_beam = (spell_dir / "PaladinHolyBeamSpell.java").read_text(encoding="utf-8")
for token in (
    '"holy_beam"',
    "SpellRarity.RARE",
    "SchoolRegistry.HOLY_RESOURCE",
    "RANGE = 32",
    "CAST_TIME_TICKS = 5 * 20",
    "CHANNEL_TICKS = 25",
    "CHANNEL_VALUE_MULTIPLIER = 0.20F",
    "BASE_COOLDOWN_SECONDS = 10",
    "HEAL_COEFFICIENT = 0.40F",
    "DAMAGE_COEFFICIENT = 0.80F",
    "KNOCKBACK_STRENGTH = 0.50D",
    "BASE_MANA_COST = 40",
    "HEAL_COEFFICIENT",
    "* CHANNEL_VALUE_MULTIPLIER",
    "DAMAGE_COEFFICIENT",
    "damageBypassingIframes",
    "targetsOnBeam(",
    "Utils.checkEntityIntersecting",
    "target.getPickRadius()",
    "level.getEntities(",
    "ClipContext.Block.COLLIDER",
    "Comparator.comparingDouble",
    "Utils.shouldHealEntity",
    "HOLY_BEAM_START_CASTING",
    "HOLY_BEAM_CASTING",
    "HOLY_BEAM_RELEASE",
    "HOLY_BEAM_HEAL",
    "HOLY_BEAM_DAMAGE",
):
    if token not in holy_beam:
        errors.append(f"PaladinHolyBeamSpell missing {token}")

if "RaycastBuilder" in holy_beam:
    errors.append("Holy Light must not collapse Spell Engine BEAM into a first-hit RaycastBuilder result")
if "EntityHitResult" in holy_beam:
    errors.append("Holy Light BEAM must not use a single EntityHitResult target")

levitate = (spell_dir / "PaladinLevitateSpell.java").read_text(encoding="utf-8")
for token in (
    '"levitate"',
    "SpellRarity.RARE",
    "SchoolRegistry.HOLY_RESOURCE",
    "CAST_TIME_TICKS = 30",
    "CHANNEL_TICKS = 4",
    "UPWARD_VELOCITY = 0.15D",
    "LEVITATE_DURATION_TICKS = 5 * 20",
    "BASE_COOLDOWN_SECONDS = 24",
    "BASE_MANA_COST = 25",
    "new Vec3(",
    "0.0D,",
    "UPWARD_VELOCITY",
    "player.hasImpulse = true",
    "player.hurtMarked = true",
    "MartialEffectRegistry.LEVITATE.get()",
    "AttributeModifier.Operation.MULTIPLY_TOTAL",
    "HOLY_WARD_IMPACT",
):
    if token not in levitate:
        errors.append(f"PaladinLevitateSpell missing {token}")

levitate_effect = (effect_dir / "LevitateEffect.java").read_text(encoding="utf-8")
for token in (
    "SLOW_FALLING_TICKS = 3 * 20",
    "REFRESH_BELOW_TICKS = 20",
    "MobEffects.SLOW_FALLING",
    "remaining",
    "+ SLOW_FALLING_TICKS",
):
    if token not in levitate_effect:
        errors.append(f"LevitateEffect missing {token}")

penance = (spell_dir / "PaladinPenanceSpell.java").read_text(encoding="utf-8")
for token in (
    '"penance"',
    "SpellRarity.EPIC",
    "SchoolRegistry.HOLY_RESOURCE",
    "RANGE = 20",
    "CAST_TIME_TICKS = 30",
    "CHANNEL_TICKS = 3",
    "CHANNEL_VALUE_MULTIPLIER = 0.50F",
    "DAMAGE_COEFFICIENT = 0.55F",
    "KNOCKBACK_STRENGTH = 0.20F",
    "PROJECTILE_VELOCITY = 0.80F",
    "HOMING_DEGREES_PER_TICK = 16.0F",
    "ABSORPTION_RADIUS = 8.0D",
    "ABSORPTION_DURATION_TICKS = 6 * 20",
    "SHIELD_STACKS_PER_BOLT = 1",
    "SHIELD_POWER_COEFFICIENT = 0.10F",
    "ABSORPTION_HEALTH_PER_STACK = 2.0F",
    "caster.getEyeHeight()",
    "caster.getBbHeight()",
    "look.scale(0.5D)",
    "Vec3 direction = look",
    "BASE_COOLDOWN_SECONDS = 12",
    "BASE_MANA_COST = 45",
    "DAMAGE_COEFFICIENT",
    "* CHANNEL_VALUE_MULTIPLIER",
    "Math.floor(",
    "SHIELD_POWER_COEFFICIENT",
    "CHANNEL_TICKS",
    "* power",
    "MartialEntityRegistry",
    ".PENANCE_PROJECTILE",
    "PENANCE_RELEASE",
):
    if token not in penance:
        errors.append(f"PaladinPenanceSpell missing {token}")

projectile = (entity_dir / "PenanceProjectile.java").read_text(encoding="utf-8")
for token in (
    "applyHoming()",
    "HOMING_DEGREES_PER_TICK",
    "PROJECTILE_VELOCITY",
    "distanceTraveled",
    ">= PaladinPenanceSpell.RANGE",
    "previousInvulnerableTime =",
    "target.invulnerableTime = 0",
    "DamageSources.applyDamage",
    "target.invulnerableTime =",
    "MartialSpellRegistry.PENANCE",
    "Utils.shouldHealEntity",
    "ABSORPTION_RADIUS",
    "PRIEST_ABSORPTION",
    "currentStacks + shieldStacksPerBolt",
    "shieldAmplifierCap + 1",
    "ABSORPTION_HEALTH_PER_STACK",
    "PENANCE_IMPACT",
    "NetworkHooks.getEntitySpawningPacket",
):
    if token not in projectile:
        errors.append(f"PenanceProjectile missing {token}")

holy_support = (spell_dir / "PaladinHolySpellSupport.java").read_text(encoding="utf-8")
for token in (
    "damageBypassingIframes(",
    "previousInvulnerableTime",
    "target.invulnerableTime = 0",
):
    if token not in holy_support:
        errors.append(f"PaladinHolySpellSupport missing P3 iframe-bypass token: {token}")

absorption_effect = (effect_dir / "PriestAbsorptionEffect.java").read_text(encoding="utf-8")
if "MobEffectCategory.BENEFICIAL" not in absorption_effect:
    errors.append("PriestAbsorptionEffect must be beneficial")

spell_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java").read_text(encoding="utf-8")
for spell_id, class_name in (
    ("holy_beam", "PaladinHolyBeamSpell::new"),
    ("levitate", "PaladinLevitateSpell::new"),
    ("penance", "PaladinPenanceSpell::new"),
):
    if f'SPELLS.register("{spell_id}", {class_name})' not in spell_registry:
        errors.append(f"spell registry missing {spell_id}")

effect_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java").read_text(encoding="utf-8")
for effect_id, class_name in (
    ("levitate", "LevitateEffect::new"),
    ("priest_absorption", "PriestAbsorptionEffect::new"),
):
    if f'"{effect_id}"' not in effect_registry or class_name not in effect_registry:
        errors.append(f"effect registry missing {effect_id}")

entity_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialEntityRegistry.java").read_text(encoding="utf-8")
for token in (
    'ENTITY_TYPES.register("penance_projectile"',
    ".sized(0.25F, 0.25F)",
    ".fireImmune()",
    ".clientTrackingRange(128)",
    ".updateInterval(2)",
):
    if token not in entity_registry:
        errors.append(f"Penance entity registration missing {token}")

client_events = (root / "src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java").read_text(encoding="utf-8")
if "PenanceProjectileRenderer::new" not in client_events:
    errors.append("Penance projectile renderer not registered")

penance_renderer = (root / "src/main/java/com/w0of26/martialspells/client/render/PenanceProjectileRenderer.java").read_text(encoding="utf-8")
if "extends EntityRenderer<PenanceProjectile>" not in penance_renderer:
    errors.append("P3 Penance renderer must use the no-geometry EntityRenderer safety surface")
if "ThrownItemRenderer" in penance_renderer:
    errors.append("P3 Penance renderer must not substitute a placeholder item cube")

lang = json.loads((root / "src/main/resources/assets/martial_spells/lang/en_us.json").read_text(encoding="utf-8"))
for key in (
    "spell.martial_spells.holy_beam",
    "spell.martial_spells.levitate",
    "spell.martial_spells.penance",
    "effect.martial_spells.levitate",
    "effect.martial_spells.priest_absorption",
):
    if key not in lang:
        errors.append(f"lang missing {key}")

sound_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java").read_text(encoding="utf-8")
sounds = json.loads((root / "src/main/resources/assets/martial_spells/sounds.json").read_text(encoding="utf-8"))

single_sounds = (
    "holy_beam_start_casting",
    "holy_beam_casting",
    "holy_beam_release",
    "holy_beam_heal",
    "holy_beam_damage",
    "holy_ward_impact",
    "penance_impact",
)
for sound in single_sounds:
    if f'register("{sound}")' not in sound_registry:
        errors.append(f"sound registry missing {sound}")
    if sounds.get(sound, {}).get("sounds") != [f"martial_spells:{sound}"]:
        errors.append(f"sounds.json mismatch for {sound}")

if 'register("penance_release")' not in sound_registry:
    errors.append("sound registry missing penance_release")
if sounds.get("penance_release", {}).get("sounds") != [
    "martial_spells:penance_release_1",
    "martial_spells:penance_release_2",
    "martial_spells:penance_release_3",
]:
    errors.append("sounds.json mismatch for penance_release variants")

sync_text = (root / "tools/sync-paladin-p3-assets.ps1").read_text(encoding="utf-8")
if "2807417a1dd9a65204c002ded487da0e6ae467a1" not in sync_text:
    errors.append("P3 asset sync is not pinned to frozen Paladins commit")
if "sync-paladin-p2-assets.ps1" not in sync_text:
    errors.append("P3 asset sync must re-assert finalized P2 assets first")

for spell_id in ("holy_beam", "levitate", "penance"):
    path = root / f"src/main/resources/assets/martial_spells/textures/gui/spell_icons/{spell_id}.png"
    if not path.is_file():
        errors.append(f"source spell icon missing; run P3 asset sync: {spell_id}.png")

for effect_id in ("levitate", "priest_absorption"):
    path = root / f"src/main/resources/assets/martial_spells/textures/mob_effect/{effect_id}.png"
    if not path.is_file():
        errors.append(f"source effect icon missing; run P3 asset sync: {effect_id}.png")

for sound in (
    "holy_beam_start_casting.ogg",
    "holy_beam_casting.ogg",
    "holy_beam_release.ogg",
    "holy_beam_heal.ogg",
    "holy_beam_damage.ogg",
    "holy_ward_impact.ogg",
    "penance_impact.ogg",
    "penance_release_1.ogg",
    "penance_release_2.ogg",
    "penance_release_3.ogg",
):
    path = root / "src/main/resources/assets/martial_spells/sounds" / sound
    if not path.is_file():
        errors.append(f"source sound missing; run P3 asset sync: {sound}")

for locked_spell in ("barrier", "battle_banner", "lightwell", "lightwell_orb"):
    if f'SPELLS.register("{locked_spell}"' in spell_registry:
        errors.append(f"P3 must not register locked P4 spell: {locked_spell}")

build = (root / "build.gradle").read_text(encoding="utf-8").lower()
for forbidden in ("spell_engine", "spell-engine", "spell-power", "spell_power"):
    if forbidden in build:
        errors.append(f"P3 must not add source runtime dependency: {forbidden}")

print("")
print("CP11 P3 Priest channel/control summary")
print(" - spells: Holy Light, Levitate, Penance")
print(" - source channel scheduling: equal-interval midpoints")
print(" - Holy Light: 5 sec / 25 pulses / 0.2x per-pulse channel multiplier")
print(" - Levitate: 1.5 sec / 4 lift releases / 5 sec float + source Slow Falling buffer logic")
print(" - Penance: 1.5 sec / 3 look-launched homing bolts / 20-block travel cap / 0.5x per-bolt damage multiplier")
print(" - Penance: 8-block no-falloff ally absorption pulse, 6 sec; executable independent-floor cap")
print(" - early release: proportional mana + effective cooldown")
print(" - target-side mana: 40 / 25 / 45")
print(" - exact Penance orb model remains P5 presentation work")

if errors:
    print("CP11 P3 AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("CP11 P3 AUDIT PASSED")
