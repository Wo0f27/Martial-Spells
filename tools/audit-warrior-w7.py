from pathlib import Path
import hashlib
import json
import re
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


def git_blob_sha(path: str, normalize_text: bool = False) -> str:
    target = ROOT / path
    require(target.is_file(), f"missing asset: {path}")
    if not target.is_file():
        return ""

    if normalize_text:
        data = target.read_text(encoding="utf-8").replace("\r\n", "\n").encode("utf-8")
    else:
        data = target.read_bytes()

    return hashlib.sha1(
        f"blob {len(data)}\0".encode("ascii") + data
    ).hexdigest()


# Frozen W2-W6 sentinels.
charge = read("src/main/java/com/w0of26/martialspells/spells/ChargeSpell.java")
require("EFFECT_DURATION_TICKS = 200" in charge, "W7 drifted frozen Charge duration")

shout = read("src/main/java/com/w0of26/martialspells/spells/DemoralizingShoutSpell.java")
for token in ("RANGE = 12.0F", "EFFECT_DURATION_TICKS = 160", "AMPLIFIER_CAP = 5"):
    require(token in shout, f"W7 drifted frozen Demoralizing Shout token: {token}")

throw_net = read("src/main/java/com/w0of26/martialspells/spells/ThrowNetSpell.java")
for token in ("CAST_TIME_TICKS = 9", "OUTPUT_SCALING = 0.50F", "NETTED_DURATION_TICKS = 60"):
    require(token in throw_net, f"W7 drifted frozen Throw Net token: {token}")

shattering = read("src/main/java/com/w0of26/martialspells/spells/ShatteringThrowSpell.java")
for token in ("CAST_TIME_TICKS = 10", "PROJECTILE_VELOCITY = 0.8F", "BOUNCES = 1"):
    require(token in shattering, f"W7 drifted frozen Shattering Throw token: {token}")

mortal = read("src/main/java/com/w0of26/martialspells/spells/MortalStrikeSpell.java")
for token in (
    "CAST_TIME_TICKS = 10",
    "DAMAGE_BONUS = 0.50F",
    "BLEED_DURATION_TICKS = 120",
    "BASE_COOLDOWN_SECONDS = 15",
):
    require(token in mortal, f"W7 drifted frozen Mortal Strike token: {token}")

bleed = read("src/main/java/com/w0of26/martialspells/effects/BleedEffect.java")
require("TICK_INTERVAL = 25" in bleed, "W7 drifted frozen .034 Bleed cadence")
require("MOVEMENT_SPEED_CAP = 2.0D" in bleed, "W7 drifted frozen .034 Bleed movement cap")

# W7 spell contract.
spell = read("src/main/java/com/w0of26/martialspells/spells/LastStandSpell.java")
for token, message in (
    ('"last_stand"', "Last Stand resource id missing"),
    ("MartialTechniqueClass.WARRIOR", "Last Stand must be WARRIOR"),
    ("SpellRarity.EPIC", "source tier 4 must map to EPIC"),
    ("CAST_TIME_TICKS = 50", "Last Stand cast must be 50 ticks"),
    ("CHANNEL_TICKS = 5", "Last Stand must have five source pulses"),
    ("CHANNEL_INTERVAL_TICKS = 10", "source channel interval must be 10 ticks"),
    ("CHANNEL_OFFSET_TICKS = 5", "source half-interval offset must be 5 ticks"),
    ("{5, 15, 25, 35, 45}", "source channel schedule must be 5/15/25/35/45"),
    ("EFFECT_DURATION_TICKS = 200", "Last Stand effect must last 200 ticks"),
    ("AMPLIFIER_CAP = 4", "Last Stand zero-based cap must be amplifier 4"),
    ("MAX_HEALTH_PER_STACK = 0.20D", "Last Stand max-health stack must be +20%"),
    ("KNOCKBACK_RESISTANCE_PER_STACK = 0.20D", "Last Stand KB-resistance stack must be +20%"),
    ("HEAL_CURRENT_MAX_HEALTH_FRACTION = 0.10F", "source channel heal must resolve to 10% current max health per pulse"),
    ("BASE_COOLDOWN_SECONDS = 60", "Last Stand base cooldown must be 60 sec"),
    ("CastType.LONG", "W7 must use LONG transport, not mismatched Iron CONTINUOUS cadence"),
    ("return CAST_TIME_TICKS;", "Last Stand effective cast time must remain fixed at 50 ticks"),
    ("LastStandVisuals.spawnCasting", "per-tick source casting VFX missing"),
    ("applyChannelImpact(player)", "source channel impact delivery missing"),
    ("elapsed >= CAST_TIME_TICKS", "tick-50 automatic channel completion missing"),
    ("MagicManager.getEffectiveSpellCooldown", "proportional cooldown must preserve Iron effective cooldown"),
    ("fullEffectiveCooldown * progress", "proportional cooldown ratio missing"),
    ("SyncLastStandReleaseAnimationPacket", "early-release animation synchronization missing"),
    ("one_handed_ground_charge", "source cast animation id missing"),
    ("one_handed_shout_release", "source release animation id missing"),
):
    require(token in spell, message)

require("CastType.CONTINUOUS" not in spell,
        "W7 must not use Iron's mismatched native CONTINUOUS scheduler")
require(spell.index("player.addEffect(") < spell.index("player.heal("),
        "source impact ordering must apply stack before healing")
require("player.getMaxHealth()" in spell,
        "Last Stand healing must read current post-stack max health")
require("AttributeModifier.Operation.MULTIPLY_TOTAL" in spell and "-1.0D" in spell,
        "channel movement_speed=0 translation missing")
require("cooldowns.syncToPlayer(player)" in spell,
        "proportional cooldown replacement must sync to client")

# Effect executable behavior, explicitly excluding the stale source comment.
effect = read("src/main/java/com/w0of26/martialspells/effects/LastStandEffect.java")
for token, message in (
    ("MobEffectCategory.BENEFICIAL", "Last Stand must be beneficial"),
    ("0xCC0000", "Last Stand source status color drifted"),
    ("Attributes.MAX_HEALTH", "Last Stand max-health modifier missing"),
    ("Attributes.KNOCKBACK_RESISTANCE", "Last Stand knockback-resistance modifier missing"),
    ("0.20D", "Last Stand +20% source modifier missing"),
    ("AttributeModifier.Operation.MULTIPLY_BASE", "Last Stand modifiers must use MULTIPLY_BASE"),
):
    require(token in effect, message)
require("LivingHurtEvent" not in effect, "W7 invented stale-comment damage reduction")
require("damage_taken" not in effect.lower(), "W7 invented stale-comment damage_taken modifier")

# Presentation and persistent aura.
visuals = read("src/main/java/com/w0of26/martialspells/visual/LastStandVisuals.java")
for token, message in (
    ("CAST_SPARK_COUNT = 8", "source cast spark count must be 8"),
    ("CAST_SMOKE_COUNT = 6", "source cast smoke count must be 6"),
    ("outward.scale(6.0D)", "source spark pre_travel=6 missing"),
    ("outward.scale(-1.0D)", "source inward spark inversion missing"),
    ("0.20D, 0.30D", "source spark speed range missing"),
    ("0.05D", "source smoke minimum speed missing"),
    ("entity.getBbHeight() * 0.10D", "source smoke FEET vertical origin missing"),
    ("entity.getId()", "aura attachment entity id missing"),
):
    require(token in visuals, message)

visual_events = read("src/main/java/com/w0of26/martialspells/events/LastStandVisualEvents.java")
require("AURA_FREQUENCY_TICKS = 20" in visual_events,
        "persistent Last Stand aura must pulse every 20 ticks")
require("MartialEffectRegistry.LAST_STAND.get()" in visual_events,
        "persistent aura must be gated by Last Stand effect")
require("LastStandSpell.clearState(player)" in visual_events,
        "Last Stand channel cleanup hooks missing")

spark = read("src/main/java/com/w0of26/martialspells/client/particle/LastStandSparkParticle.java")
for token in ("friction = 0.768F", "quadSize = 0.11F", "0.65F", "alpha = 0.75F", "0xF000F0"):
    require(token in spark, f"Last Stand magic_spark translation missing: {token}")

smoke = read("src/main/java/com/w0of26/martialspells/client/particle/LastStandSmokeParticle.java")
for token in ("friction = 0.80F", "gravity = -0.01F", "quadSize = 0.15F", "alpha = 0.40F", "9.0F / 0.46F"):
    require(token in smoke, f"Last Stand smoke_medium translation missing: {token}")

aura = read("src/main/java/com/w0of26/martialspells/client/particle/LastStandAuraParticle.java")
for token in ("quadSize = 1.5F", "lifetime = 22", "alpha = 0.50F", "rotationX(", "followEntity.getX()", "groundY"):
    require(token in aura, f"Last Stand area_effect_700 translation missing: {token}")

# Registries/network.
effect_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java")
require('"last_stand"' in effect_registry and "LastStandEffect::new" in effect_registry,
        "effect registry missing Last Stand")

spell_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java")
require('SPELLS.register("last_stand", LastStandSpell::new)' in spell_registry,
        "spell registry missing Last Stand")

sound_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java")
for name in ("last_stand_start", "last_stand_casting", "last_stand_release"):
    require(f'register("{name}")' in sound_registry, f"sound registry missing {name}")

particle_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialParticleRegistry.java")
for name in ("last_stand_spark", "last_stand_smoke", "last_stand_aura"):
    require(f'PARTICLES.register("{name}"' in particle_registry, f"particle registry missing {name}")

client_events = read("src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java")
for provider in ("LastStandSparkParticle.Provider::new", "LastStandSmokeParticle.Provider::new", "LastStandAuraParticle.Provider::new"):
    require(provider in client_events, f"client particle provider missing: {provider}")

network = read("src/main/java/com/w0of26/martialspells/network/MartialNetwork.java")
require('PROTOCOL_VERSION = "14"' in network, "W7 packet addition requires network protocol 14")
require("SyncNettedVisualPacket.class" in network, "W7 removed frozen W4 Netted sync")
require("SyncLastStandReleaseAnimationPacket.class" in network,
        "Last Stand early-release animation packet not registered")

# Tags.
warrior = json.loads(read("src/main/resources/data/martial_spells/tags/spells/warrior_techniques.json"))
require(warrior.get("values") == [
    "martial_spells:charge",
    "martial_spells:demoralizing_shout",
    "martial_spells:throw_net",
    "martial_spells:shattering_throw",
    "martial_spells:mortal_strike",
    "martial_spells:last_stand",
], "W7 Warrior tag must contain exactly all six frozen Warrior techniques")

martial = json.loads(read("src/main/resources/data/martial_spells/tags/spells/martial_techniques.json"))
require("martial_spells:last_stand" in martial.get("values", []),
        "Last Stand missing from martial_techniques")

# Particle JSON texture inventories.
spark_json = json.loads(read("src/main/resources/assets/martial_spells/particles/last_stand_spark.json"))
require(spark_json.get("textures") == ["minecraft:generic_0"],
        "Last Stand spark must use source minecraft:generic_0")

smoke_json = json.loads(read("src/main/resources/assets/martial_spells/particles/last_stand_smoke.json"))
require(smoke_json.get("textures") == [
    f"martial_spells:last_stand_smoke_{i}" for i in range(9)
], "Last Stand smoke particle must contain exactly nine frozen frames")

aura_json = json.loads(read("src/main/resources/assets/martial_spells/particles/last_stand_aura.json"))
require(aura_json.get("textures") == [
    f"martial_spells:last_stand_aura_{i}" for i in range(22)
], "Last Stand aura particle must contain exactly 22 frozen frames")

sounds = json.loads(read("src/main/resources/assets/martial_spells/sounds.json"))
for name in ("last_stand_start", "last_stand_casting", "last_stand_release"):
    require(sounds.get(name, {}).get("sounds") == [f"martial_spells:{name}"],
            f"sounds.json mapping wrong for {name}")

# Frozen primary assets. Text animation hashes normalize Windows CRLF.
expected_assets = {
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/last_stand.png":
        "5f6e39441c5368b379731ad086db332079223b18",
    "src/main/resources/assets/martial_spells/textures/mob_effect/last_stand.png":
        "5f6e39441c5368b379731ad086db332079223b18",
    "src/main/resources/assets/martial_spells/sounds/last_stand_start.ogg":
        "f6ce90b0a0c89e66cc60e74bedc64b04d03871cf",
    "src/main/resources/assets/martial_spells/sounds/last_stand_casting.ogg":
        "ff5dafe52ce8e3f22a867238f8d4bf1e6fd7dbac",
    "src/main/resources/assets/martial_spells/sounds/last_stand_release.ogg":
        "19f4d8deead19c22eff36454e5f3c2e8cf1176d1",
    "src/main/resources/assets/martial_spells/player_animation/one_handed_ground_charge.json":
        "7294d126bf932f843e82f3864f5a5b5961f35839",
    "src/main/resources/assets/martial_spells/player_animation/one_handed_shout_release.json":
        "292e820254a1088297ad518b1deb49f612b9739d",
    "src/main/resources/assets/martial_spells/textures/particle/last_stand_smoke_0.png":
        "3367acb25c283891bece18cb776bf027ccf007b3",
    "src/main/resources/assets/martial_spells/textures/particle/last_stand_smoke_8.png":
        "a6bf00c86d9c6b1b20f7e171ff3b596ffac92c20",
    "src/main/resources/assets/martial_spells/textures/particle/last_stand_aura_0.png":
        "bbd8968d68a21fd371382af1d4f9cb54dea900c2",
    "src/main/resources/assets/martial_spells/textures/particle/last_stand_aura_21.png":
        "b60d39212b80c2a9b3904987a1e70b20bebd4b25",
}
text_assets = {
    "src/main/resources/assets/martial_spells/player_animation/one_handed_ground_charge.json",
    "src/main/resources/assets/martial_spells/player_animation/one_handed_shout_release.json",
}
for path, expected in expected_assets.items():
    require(
        git_blob_sha(path, normalize_text=path in text_assets) == expected,
        f"frozen asset mismatch: {path}"
    )

# No dependency regression.
build_gradle = read("build.gradle")
mods_toml = read("src/main/resources/META-INF/mods.toml")
for dependency_token in ("spell_engine", "spell-engine", "spell_power", "spell-power"):
    require(dependency_token not in build_gradle.lower(),
            f"W7 introduced forbidden dependency token in build.gradle: {dependency_token}")
    require(dependency_token not in mods_toml.lower(),
            f"W7 introduced forbidden dependency token in mods.toml: {dependency_token}")

if errors:
    print("Warrior W7 audit FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Warrior W7 audit PASS")
print("Last Stand: exact source channel frames 5/15/25/35/45, release at tick 50.")
print("Stacks: 10 sec, amplifier 0->4, +20% base Max Health and KB Resistance per effective stack.")
print("Heal: stack first, then 10% of current post-stack Max Health per pulse.")
print("Cooldown: 60 sec full; proportional effective cooldown on early release.")
print("W0-W6 remain frozen; all six Warrior techniques are now implemented.")
