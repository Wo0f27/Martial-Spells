from pathlib import Path
import hashlib
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def read(path: str) -> str:
    target = ROOT / path
    require(target.is_file(), f"missing file: {path}")
    return target.read_text(encoding="utf-8") if target.is_file() else ""


def git_blob_sha(path: str) -> str:
    target = ROOT / path
    require(target.is_file(), f"missing asset: {path}")
    if not target.is_file():
        return ""
    data = target.read_bytes()
    return hashlib.sha1(
        f"blob {len(data)}\0".encode("ascii") + data
    ).hexdigest()


# Frozen W2-W4 sentinels.
charge = read("src/main/java/com/w0of26/martialspells/spells/ChargeSpell.java")
require("EFFECT_DURATION_TICKS = 200" in charge, "W5 drifted frozen Charge duration")
require("CHARGE_SPEED_SIGN" not in charge, "W5 restored rejected Charge overhead sign")

shout = read("src/main/java/com/w0of26/martialspells/spells/DemoralizingShoutSpell.java")
for token in (
    "RANGE = 12.0F",
    "EFFECT_DURATION_TICKS = 160",
    "AMPLIFIER_CAP = 5",
    "DAMAGE_COEFFICIENT = 0.05D",
    "CONTROL_HEALTH_BASE = 50.0D",
):
    require(token in shout, f"W5 drifted frozen Demoralizing Shout token: {token}")

throw_net = read("src/main/java/com/w0of26/martialspells/spells/ThrowNetSpell.java")
for token in (
    "CAST_TIME_TICKS = 9",
    "MIN_RELEASE_RATIO = 0.20F",
    "OUTPUT_SCALING = 0.50F",
    "BASE_RANGE = 10.0F",
    "CHARGE_RANGE_BONUS = 12.0F",
    "NETTED_DURATION_TICKS = 60",
    "BASE_COOLDOWN_SECONDS = 12",
):
    require(token in throw_net, f"W5 drifted frozen Throw Net token: {token}")

network = read("src/main/java/com/w0of26/martialspells/network/MartialNetwork.java")
require('PROTOCOL_VERSION = "13"' in network, "W5 drifted frozen W4 network protocol")
require("SyncNettedVisualPacket.class" in network, "W5 removed frozen Netted visual sync")

# W5 spell contract.
spell = read("src/main/java/com/w0of26/martialspells/spells/ShatteringThrowSpell.java")
for token, message in (
    ('"shattering_throw"', "Shattering Throw resource id missing"),
    ("MartialTechniqueClass.WARRIOR", "Shattering Throw must be WARRIOR"),
    ("ReleaseChargedTechnique", "Shattering Throw must reuse charged-release bridge"),
    ("SpellRarity.UNCOMMON", "source tier 2 must map to UNCOMMON"),
    ("CAST_TIME_TICKS = 10", "cast time must be 10 ticks"),
    ("MIN_RELEASE_RATIO = 0.20F", "minimum release ratio must be 0.2"),
    ("OUTPUT_SCALING = 1.0F", "source default output scaling must be 1.0"),
    ("BASE_RANGE = 12.0F", "base range must be 12"),
    ("CHARGE_RANGE_BONUS = 12.0F", "charge range bonus must be 12"),
    ("PROJECTILE_VELOCITY = 0.8F", "projectile velocity must be 0.8"),
    ("HOMING_DEGREES_PER_TICK = 2.0F", "homing must be 2 degrees/tick"),
    ("SPIN_DEGREES_PER_TICK = -36.0F", "spin must be -36 degrees/tick"),
    ("BOUNCES = 1", "Shattering Throw must have one block bounce"),
    ("DAMAGE_COEFFICIENT = 1.0D", "damage coefficient must be 1.0"),
    ("KNOCKBACK_COEFFICIENT = 1.0F", "source damage knockback default must be 1.0"),
    ("SHATTER_DURATION_TICKS = 160", "Shattered Armor must last 160 ticks"),
    ("CONTROL_HEALTH_BASE = 100.0D", "Shatter health base must be 100"),
    ("CONTROL_POWER_MULTIPLIER = 2.0D", "Shatter power multiplier must be 2"),
    ("BASE_COOLDOWN_SECONDS = 8", "base cooldown must be 8"),
    ("baseManaCost = 0", "Shattering Throw must remain zero mana"),
    ("CastType.LONG", "Shattering Throw must use held charged casting"),
    ("findAimTarget(caster, range)", "AIM target must be resolved at release"),
    ("getMainHandItem()", "held main-hand item capture missing"),
    ("BuiltInRegistries.ITEM.getKey", "held item must be captured by registry id"),
):
    require(token in spell, message)

require("STICKY_TARGETS" not in spell, "Shattering Throw AIM must not become sticky during charge")

# Projectile/delivery.
projectile = read("src/main/java/com/w0of26/martialspells/entity/ShatteringThrowProjectile.java")
for token, message in (
    ("SOURCE_AGE_CAP_TICKS = 1200", "source projectile age cap missing"),
    ("TRAVEL_SOUND_INTERVAL = 8", "source travel sound cadence missing"),
    ("EntityDataSerializers.STRING", "held item model id must sync to clients"),
    ("ProjectileUtil.getHitResultOnMoveVector", "projectile ray collision missing"),
    ("ForgeEventFactory.onProjectileImpact", "Forge projectile impact hook missing"),
    ("applyHoming()", "projectile homing runtime missing"),
    ("Math.toRadians(", "homing angular cap missing"),
    ("2.0D * previousDirection.dot(normal)", "source vector-reflection bounce missing"),
    ("bouncesRemaining--", "source bounce consumption missing"),
    ("previousInvulnerableTime = target.invulnerableTime", "source iframe bypass save missing"),
    ("target.invulnerableTime = 0", "source iframe bypass missing"),
    ("target.invulnerableTime = previousInvulnerableTime", "source iframe restore missing"),
    ("MartialDamageTypes.shatteringThrow(owner, this)", "Shattering Throw damage source missing"),
    ("VANILLA_KNOCKBACK_BASE", "source 0.4 knockback base missing"),
    ("target.getMaxHealth() <= controlHealthLimit", "Shatter health gate missing"),
    ("MartialEffectRegistry.SHATTER.get()", "Shattered Armor application missing"),
    ("ShatterBloodVfx.spawnImpact", "Shatter impact blood VFX missing"),
    ("MartialSoundRegistry.THROW_IMPACT.get()", "throw impact sound missing"),
):
    require(token in projectile, message)

# Held-item renderer.
renderer = read("src/main/java/com/w0of26/martialspells/client/render/ShatteringThrowRenderer.java")
for token, message in (
    ("getItemModelId()", "renderer must consume synced held-item id"),
    ("getDefaultInstance()", "renderer must use upstream default item stack model"),
    ("distanceToSqr(entity) < 12.25D", "source near-camera guard missing"),
    ("+ 180.0F", "ALONG_MOTION yaw correction missing"),
    ("Math.asin(", "ALONG_MOTION pitch missing"),
    ("Axis.YP.rotationDegrees(90.0F)", "ALONG_MOTION +90 degree conversion missing"),
    ("SPIN_DEGREES_PER_TICK", "source held-item spin missing"),
    ("ItemDisplayContext.FIXED", "source held-item FIXED display transform missing"),
    ("translate(-0.5D, -0.5D, -0.5D)", "source raw model centering missing"),
):
    require(token in renderer, message)

# Shattered Armor / particles.
effect = read("src/main/java/com/w0of26/martialspells/effects/ShatterEffect.java")
for token, message in (
    ("MobEffectCategory.HARMFUL", "Shatter must be harmful"),
    ("Attributes.ARMOR", "Shatter armor attribute missing"),
    ("-0.30D", "Shatter armor reduction must be -30%"),
    ("AttributeModifier.Operation.MULTIPLY_BASE", "Shatter must use MULTIPLY_BASE"),
    ("isDurationEffectTick", "persistent Shatter VFX tick missing"),
    ("ShatterBloodVfx.spawn(", "persistent Shatter blood VFX missing"),
):
    require(token in effect, message)

blood_vfx = read("src/main/java/com/w0of26/martialspells/visual/ShatterBloodVfx.java")
require("spawn(level, target, 10, 0.05D, 0.30D)" in blood_vfx,
        "impact blood batch must be 10 particles at 0.05-0.30 speed")
require("target.getBbHeight() * 0.5D" in blood_vfx,
        "blood particle source must originate at entity center")

blood_particle = read("src/main/java/com/w0of26/martialspells/client/particle/ShatterBloodParticle.java")
for token, message in (
    ("gravity = 0.8F", "dripping blood gravity must be 0.8"),
    ("quadSize = 0.11F", "dripping blood base scale must be 0.11"),
    ("0.33F", "dripping blood scale variance must be 33%"),
    ("rCol = 0.349F", "dripping blood #590000 red channel missing"),
    ("xd *= 0.95D", "DRIFT X damping missing"),
    ("yd *= 0.90D", "DRIFT Y damping missing"),
    ("zd *= 0.95D", "DRIFT Z damping missing"),
    ("lifetime = 20", "dripping blood lifetime must be 20 ticks"),
):
    require(token in blood_particle, message)

particle_json = json.loads(read("src/main/resources/assets/martial_spells/particles/shatter_blood.json"))
require(particle_json.get("textures") == ["minecraft:drip_hang"],
        "Shatter blood must use upstream minecraft:drip_hang sprite")

# Registrations.
effect_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java")
require('"shatter"' in effect_registry and "ShatterEffect::new" in effect_registry,
        "effect registry missing Shatter")

entity_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialEntityRegistry.java")
for token in (
    'ENTITY_TYPES.register("shattering_throw"',
    '.sized(0.25F, 0.25F)',
    '.fireImmune()',
    '.clientTrackingRange(128)',
    '.updateInterval(2)',
):
    require(token in entity_registry, f"Shattering Throw entity registration missing: {token}")

spell_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java")
require('SPELLS.register("shattering_throw", ShatteringThrowSpell::new)' in spell_registry,
        "spell registry missing Shattering Throw")

sound_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java")
require('register("throw_impact")' in sound_registry, "throw_impact sound registry missing")

particle_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialParticleRegistry.java")
require('PARTICLES.register("shatter_blood"' in particle_registry, "shatter_blood particle registry missing")

client_events = read("src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java")
require("ShatteringThrowRenderer::new" in client_events, "Shattering Throw renderer not registered")
require("ShatterBloodParticle.Provider::new" in client_events, "Shatter blood provider not registered")

warrior = json.loads(read("src/main/resources/data/martial_spells/tags/spells/warrior_techniques.json"))
require(warrior.get("values") == [
    "martial_spells:charge",
    "martial_spells:demoralizing_shout",
    "martial_spells:throw_net",
    "martial_spells:shattering_throw",
], "W5 Warrior tag must contain exactly W2-W5 techniques")

martial = json.loads(read("src/main/resources/data/martial_spells/tags/spells/martial_techniques.json"))
require("martial_spells:shattering_throw" in martial.get("values", []),
        "Shattering Throw missing from martial_techniques")

sounds = json.loads(read("src/main/resources/assets/martial_spells/sounds.json"))
require(sounds.get("throw", {}).get("sounds") == ["martial_spells:throw"],
        "W5 drifted frozen throw sound mapping")
require(sounds.get("throw_impact", {}).get("sounds") == ["martial_spells:throw_impact"],
        "throw_impact sound mapping missing")

expected_assets = {
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/shattering_throw.png":
        "d43c529e6ba3b6970be7b68809c38a4ccab0ca0f",
    "src/main/resources/assets/martial_spells/textures/mob_effect/shatter.png":
        "d43c529e6ba3b6970be7b68809c38a4ccab0ca0f",
    "src/main/resources/assets/martial_spells/sounds/throw.ogg":
        "ce5a789e74441a01f63624a608f8ba3ca1987a2d",
    "src/main/resources/assets/martial_spells/sounds/throw_impact.ogg":
        "4266dde3a8b9682b4dfd46342d5630534ab4a965",
}
for path, expected in expected_assets.items():
    require(git_blob_sha(path) == expected, f"frozen asset mismatch: {path}")

# W6+ remain locked.
for filename in (
    "MortalStrikeSpell.java",
    "LastStandSpell.java",
):
    require(
        not (ROOT / "src/main/java/com/w0of26/martialspells/spells" / filename).exists(),
        f"W5 must not contain later Warrior gameplay class {filename}"
    )

build_gradle = read("build.gradle")
mods_toml = read("src/main/resources/META-INF/mods.toml")
for dependency_token in ("spell_engine", "spell-engine", "spell_power", "spell-power"):
    require(dependency_token not in build_gradle.lower(),
            f"W5 introduced forbidden dependency token in build.gradle: {dependency_token}")
    require(dependency_token not in mods_toml.lower(),
            f"W5 introduced forbidden dependency token in mods.toml: {dependency_token}")

if errors:
    print("Warrior W5 audit FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Warrior W5 audit PASS")
print("Shattering Throw: 10-tick charge, 20% minimum, fully proportional output, 12->24 range.")
print("Projectile: held-item visual, velocity 0.8, homing 2 deg/tick, one block bounce, -36 deg/tick spin.")
print("Shattered Armor: 8 sec, -30% base Armor under 100 + 2x Attack Damage max-health gate.")
print("W0-W4 remain frozen; W6+ remain locked.")
