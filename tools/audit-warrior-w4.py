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


# W2/W3 frozen sentinels.
charge = read("src/main/java/com/w0of26/martialspells/spells/ChargeSpell.java")
require("EFFECT_DURATION_TICKS = 200" in charge, "W4 drifted accepted Charge duration")
require("CHARGE_SPEED_SIGN" not in charge, "W4 restored rejected Charge overhead sign")

shout = read("src/main/java/com/w0of26/martialspells/spells/DemoralizingShoutSpell.java")
for token in (
    "RANGE = 12.0F",
    "EFFECT_DURATION_TICKS = 160",
    "AMPLIFIER_CAP = 5",
    "DAMAGE_COEFFICIENT = 0.05D",
    "CONTROL_HEALTH_BASE = 50.0D",
):
    require(token in shout, f"W4 drifted frozen Demoralizing Shout token: {token}")

spell = read("src/main/java/com/w0of26/martialspells/spells/ThrowNetSpell.java")
for token, message in (
    ('"throw_net"', "Throw Net resource id missing"),
    ("MartialTechniqueClass.WARRIOR", "Throw Net must be WARRIOR"),
    ("ReleaseChargedTechnique", "Throw Net must opt into charged-release bridge"),
    ("SpellRarity.UNCOMMON", "source tier 2 must map to UNCOMMON"),
    ("CAST_TIME_TICKS = 9", "cast time must be 9 ticks"),
    ("MIN_RELEASE_RATIO = 0.20F", "minimum release ratio must be 0.2"),
    ("OUTPUT_SCALING = 0.50F", "output scaling must be 0.5"),
    ("BASE_RANGE = 10.0F", "base range must be 10"),
    ("CHARGE_RANGE_BONUS = 12.0F", "charge range bonus must be 12"),
    ("PROJECTILE_VELOCITY = 1.0F", "projectile velocity must be 1"),
    ("HOMING_DEGREES_PER_TICK = 1.0F", "homing must be 1 degree/tick"),
    ("SPIN_DEGREES_PER_TICK = 12.0F", "model spin must be 12 degrees/tick"),
    ("DAMAGE_COEFFICIENT = 0.10D", "damage coefficient must be 0.1"),
    ("KNOCKBACK_COEFFICIENT = 0.10F", "knockback coefficient must be 0.1"),
    ("NETTED_DURATION_TICKS = 60", "Netted must last 60 ticks"),
    ("CONTROL_HEALTH_BASE = 100.0D", "control health base must be 100"),
    ("CONTROL_POWER_MULTIPLIER = 2.0D", "control power multiplier must be 2"),
    ("BASE_COOLDOWN_SECONDS = 12", "base cooldown must be 12"),
    ("baseManaCost = 0", "Throw Net must remain zero mana"),
    ("CastType.LONG", "Throw Net must use held charged casting"),
    ("Scroll.attemptRemoveScrollAfterCast", "partial scroll release must preserve scroll consumption"),
    ("0.5D", "source launch-point forward offset translation missing"),
):
    require(token in spell, message)

bridge = read("src/main/java/com/w0of26/martialspells/mixin/IronsReleaseUsingHelperMixin.java")
require("Utils.class" in bridge, "charged-release bridge must target Iron's Utils")
require("releaseUsingHelper" in bridge, "charged-release bridge injection missing")
require("ReleaseChargedTechnique" in bridge, "bridge must be opt-in only")

projectile = read("src/main/java/com/w0of26/martialspells/entity/ThrowNetProjectile.java")
for token, message in (
    ("SOURCE_AGE_CAP_TICKS = 1200", "source projectile age cap missing"),
    ("TRAVEL_SOUND_INTERVAL = 8", "travel sound interval must be 8"),
    ("distanceTraveled >= maxRange", "charge-scaled projectile range termination missing"),
    ("applyHoming()", "homing runtime missing"),
    ("Math.toRadians(", "homing must use angular turn cap"),
    ("MartialDamageTypes.throwNet(owner, this)", "Throw Net damage source missing"),
    ("VANILLA_KNOCKBACK_BASE * knockbackCoefficient", "source knockback translation missing"),
    ("target.getMaxHealth() <= controlHealthLimit", "Netted health gate missing"),
    ("MartialEffectRegistry.NET_TRAP.get()", "Netted application missing"),
    ("MartialSoundRegistry.NET_TRAVEL.get()", "travel sound missing"),
    ("MartialSoundRegistry.NET_IMPACT.get()", "impact sound missing"),
):
    require(token in projectile, message)

effect = read("src/main/java/com/w0of26/martialspells/effects/NettedEffect.java")
require("MobEffectCategory.HARMFUL" in effect, "Netted must be harmful")
require("Attributes.MOVEMENT_SPEED" in effect and "-2.0D" in effect,
        "Netted source movement modifier missing")
require("Attributes.KNOCKBACK_RESISTANCE" in effect and "100.0D" in effect,
        "Netted source knockback-resistance modifier missing")

root_mixin = read("src/main/java/com/w0of26/martialspells/mixin/LivingEntityNettedMixin.java")
require('method = "travel"' in root_mixin, "Netted locomotion root missing")
require('method = "jumpFromGround"' in root_mixin, "Netted jump root missing")

net_events = read("src/main/java/com/w0of26/martialspells/events/NettedEvents.java")
require("LivingKnockBackEvent" in net_events and "event.setCanceled(true)" in net_events,
        "Netted knockback immunity missing")

entity_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialEntityRegistry.java")
for token in ('.sized(0.25F, 0.25F)', '.fireImmune()', '.clientTrackingRange(128)', '.updateInterval(2)'):
    require(token in entity_registry, f"Throw Net entity source default missing: {token}")

spell_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java")
require('SPELLS.register("throw_net", ThrowNetSpell::new)' in spell_registry,
        "spell registry missing Throw Net")

effect_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java")
require('"net_trap"' in effect_registry and "NettedEffect::new" in effect_registry,
        "effect registry missing net_trap/Netted")

mixins = json.loads(read("src/main/resources/martial_spells.mixins.json"))
require("LivingEntityNettedMixin" in mixins.get("mixins", []),
        "Netted root mixin missing from config")
require("IronsReleaseUsingHelperMixin" in mixins.get("mixins", []),
        "charged-release mixin missing from config")
require("LivingEntityTrappedMixin" not in mixins.get("mixins", []),
        "W4 must not silently activate the old Bear Trap mixin")

warrior = json.loads(read("src/main/resources/data/martial_spells/tags/spells/warrior_techniques.json"))
require(warrior.get("values") == [
    "martial_spells:charge",
    "martial_spells:demoralizing_shout",
    "martial_spells:throw_net",
], "W4 Warrior tag must contain exactly Charge + Demoralizing Shout + Throw Net")

martial = json.loads(read("src/main/resources/data/martial_spells/tags/spells/martial_techniques.json"))
require("martial_spells:throw_net" in martial.get("values", []),
        "Throw Net missing from martial_techniques")

sounds = json.loads(read("src/main/resources/assets/martial_spells/sounds.json"))
for name in ("net_casting", "throw", "net_travel", "net_impact"):
    require(
        sounds.get(name, {}).get("sounds") == [f"martial_spells:{name}"],
        f"sounds.json mapping wrong for {name}"
    )

model = read("src/main/resources/assets/martial_spells/models/spell_projectile/throw_net.json")
require("martial_spells:spell_projectile/throw_net" in model,
        "Throw Net model texture namespace not translated")
require("rogues:spell_projectile/throw_net" not in model,
        "Throw Net model retains source namespace")

expected_assets = {
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/throw_net.png":
        "6bffb24937a7aeddb959f05994bd846f21f8164e",
    "src/main/resources/assets/martial_spells/textures/mob_effect/net_trap.png":
        "6bffb24937a7aeddb959f05994bd846f21f8164e",
    "src/main/resources/assets/martial_spells/textures/spell_projectile/throw_net.png":
        "57dcfd0337d544ae2f51459b6d7dbb5b3a82e815",
    "src/main/resources/assets/martial_spells/sounds/net_casting.ogg":
        "5495c850ccfd2ab16becefb4831eccfc3e3834d0",
    "src/main/resources/assets/martial_spells/sounds/throw.ogg":
        "ce5a789e74441a01f63624a608f8ba3ca1987a2d",
    "src/main/resources/assets/martial_spells/sounds/net_travel.ogg":
        "304fb9c0eae54244c1d05b1252241f3ba84720d1",
    "src/main/resources/assets/martial_spells/sounds/net_impact.ogg":
        "d0a9568906788c50def55cdaeb9a73bd51b6466d",
}
for path, expected in expected_assets.items():
    require(git_blob_sha(path) == expected, f"frozen asset mismatch: {path}")

for filename in (
    "ShatteringThrowSpell.java",
    "LastStandSpell.java",
    "MortalStrikeSpell.java",
):
    require(
        not (ROOT / "src/main/java/com/w0of26/martialspells/spells" / filename).exists(),
        f"W4 must not contain later Warrior gameplay class {filename}"
    )

build_gradle = read("build.gradle")
mods_toml = read("src/main/resources/META-INF/mods.toml")
for dependency_token in ("spell_engine", "spell-engine", "spell_power", "spell-power"):
    require(dependency_token not in build_gradle.lower(),
            f"W4 introduced forbidden dependency token in build.gradle: {dependency_token}")
    require(dependency_token not in mods_toml.lower(),
            f"W4 introduced forbidden dependency token in mods.toml: {dependency_token}")

if errors:
    print("Warrior W4 audit FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Warrior W4 audit PASS")
print("Throw Net: 9-tick charge, 20% minimum release, 10->22 range, 0.5 damped output scaling.")
print("Projectile: velocity 1, homing 1 deg/tick, spin 12 deg/tick, 8-tick travel sound.")
print("Netted: 3-sec ROOT + knockback immunity under 100 + 2x Attack Damage max-health gate.")
print("W2 Charge and W3 Demoralizing Shout remain frozen; W5+ remain locked.")
