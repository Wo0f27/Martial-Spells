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
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


# W2 must stay frozen while W3 is added.
charge = read("src/main/java/com/w0of26/martialspells/spells/ChargeSpell.java")
require("EFFECT_DURATION_TICKS = 200" in charge, "W3 drifted accepted Charge duration")
require("CHARGE_SPEED_SIGN" not in charge, "W3 restored the rejected Charge overhead sign")

effect = read("src/main/java/com/w0of26/martialspells/effects/DemoralizedEffect.java")
require("MobEffectCategory.HARMFUL" in effect, "Demoralized must be harmful")
require("Attributes.ATTACK_DAMAGE" in effect, "Demoralized must modify Attack Damage")
require("-0.20D" in effect, "Demoralized must reduce base Attack Damage by 20% per effect level")
require("AttributeModifier.Operation.MULTIPLY_BASE" in effect,
        "Demoralized must use MULTIPLY_BASE")

spell = read("src/main/java/com/w0of26/martialspells/spells/DemoralizingShoutSpell.java")
for token, message in (
    ('"demoralizing_shout"', "Demoralizing Shout resource id is missing"),
    ("MartialTechniqueClass.WARRIOR", "Demoralizing Shout must be WARRIOR"),
    ("SpellRarity.RARE", "source tier 3 must map to RARE"),
    ("RANGE = 12.0F", "range must be 12"),
    ("VERTICAL_RANGE_MULTIPLIER = 0.5F", "vertical range multiplier must be 0.5"),
    ("EFFECT_DURATION_TICKS = 160", "Demoralized duration must be 160 ticks"),
    ("AMPLIFIER_INCREMENT = 1", "ADD increment must be 1"),
    ("AMPLIFIER_CAP = 5", "source amplifier cap must be 5"),
    ("ATTACK_DAMAGE_REDUCTION_PER_LEVEL = 0.20D", "attack reduction must be 20%"),
    ("DAMAGE_COEFFICIENT = 0.05D", "direct damage coefficient must be 0.05"),
    ("CONTROL_HEALTH_BASE = 50.0D", "control health base must be 50"),
    ("CONTROL_POWER_MULTIPLIER = 2.0D", "control power multiplier must be 2"),
    ("BASE_COOLDOWN_SECONDS = 12", "base cooldown must be 12 seconds"),
    ("baseManaCost = 0", "W3 must remain zero mana"),
    ("castTime = 0", "W3 must be instant"),
    ("PhysicalMeleePower.controlHealthLimit", "W3 must use the W1 physical-melee adapter for the health gate"),
    ("PhysicalMeleePower.get(caster) * DAMAGE_COEFFICIENT", "W3 damage must use single-hand physical-melee power"),
    ("current.getAmplifier() + AMPLIFIER_INCREMENT", "W3 must use ADD amplifier semantics"),
    ("Math.min(", "W3 must cap ADD amplifier"),
    ("target.getMaxHealth() <= controlHealthLimit", "health gate must apply only to Demoralized"),
    ("target.setDeltaMovement(velocityBeforeImpact)", "W3 must preserve zero-knockback damage behavior"),
    ("MartialDamageTypes.demoralizingShout(caster)", "W3 must use the armor-respecting Martial damage source"),
    ("MartialSoundRegistry.SHOUT_RELEASE.get()", "release sound missing"),
    ("MartialSoundRegistry.DEMORALIZE_IMPACT.get()", "impact sound missing"),
):
    require(token in spell, message)

spell_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java")
require('SPELLS.register("demoralizing_shout", DemoralizingShoutSpell::new)' in spell_registry,
        "spell registry missing Demoralizing Shout")

effect_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java")
require('"demoralized"' in effect_registry and "DemoralizedEffect::new" in effect_registry,
        "effect registry missing Demoralized")

particle_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialParticleRegistry.java")
require('PARTICLES.register("demoralize_smoke"' in particle_registry,
        "particle registry missing Demoralize smoke")

client_events = read("src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java")
require("DemoralizeSmokeParticle.Provider" in client_events,
        "client particle provider missing Demoralize smoke")

sound_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java")
for token in ('register("shout_release")', 'register("demoralize_impact")'):
    require(token in sound_registry, f"sound registry missing {token}")

sounds = json.loads(read("src/main/resources/assets/martial_spells/sounds.json"))
require(sounds.get("shout_release", {}).get("sounds") == ["martial_spells:shout_release"],
        "sounds.json shout_release mapping is wrong")
require(sounds.get("demoralize_impact", {}).get("sounds") == ["martial_spells:demoralize_impact"],
        "sounds.json demoralize_impact mapping is wrong")

warrior = json.loads(read("src/main/resources/data/martial_spells/tags/spells/warrior_techniques.json"))
require(warrior.get("values") == [
    "martial_spells:charge",
    "martial_spells:demoralizing_shout",
], "W3 Warrior tag must contain only Charge + Demoralizing Shout")

martial = json.loads(read("src/main/resources/data/martial_spells/tags/spells/martial_techniques.json"))
require("martial_spells:demoralizing_shout" in martial.get("values", []),
        "Demoralizing Shout missing from martial_techniques")

damage_type = json.loads(read("src/main/resources/data/martial_spells/damage_type/demoralizing_shout.json"))
require(damage_type.get("message_id") == "martial_spells.demoralizing_shout",
        "W3 damage type message id drifted")

require(git_blob_sha("src/main/resources/assets/martial_spells/textures/gui/spell_icons/demoralizing_shout.png")
        == "5e0d0e332fe365dc0f1e677618e7ec33390e39c9",
        "Demoralizing Shout icon does not match frozen Rogues")
require(git_blob_sha("src/main/resources/assets/martial_spells/textures/mob_effect/demoralized.png")
        == "5e0d0e332fe365dc0f1e677618e7ec33390e39c9",
        "Demoralized icon does not match frozen Rogues")
require(git_blob_sha("src/main/resources/assets/martial_spells/sounds/shout_release.ogg")
        == "353c003ba15948d8ff55c6624a939b7cbf07d09b",
        "shout_release.ogg does not match frozen Rogues")
require(git_blob_sha("src/main/resources/assets/martial_spells/sounds/demoralize_impact.ogg")
        == "e78e21e8b74f6bc188de0d3f1b46bad2fb64d29d",
        "demoralize_impact.ogg does not match frozen Rogues")

for filename in (
    "ShatteringThrowSpell.java",
    "ThrowNetSpell.java",
    "LastStandSpell.java",
    "MortalStrikeSpell.java",
):
    require(not (ROOT / "src/main/java/com/w0of26/martialspells/spells" / filename).exists(),
            f"W3 must not contain later Warrior gameplay class {filename}")

build_gradle = read("build.gradle")
mods_toml = read("src/main/resources/META-INF/mods.toml")
for dependency_token in ("spell_engine", "spell-engine", "spell_power", "spell-power"):
    require(dependency_token not in build_gradle.lower(),
            f"W3 introduced forbidden dependency token in build.gradle: {dependency_token}")
    require(dependency_token not in mods_toml.lower(),
            f"W3 introduced forbidden dependency token in mods.toml: {dependency_token}")

if errors:
    print("Warrior W3 audit FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Warrior W3 audit PASS")
print("Demoralizing Shout: 12-block area, 8-sec ADD debuff, -20% base Attack Damage per effect level, 5% Attack Damage impact, 12-sec cooldown.")
print("Source amplifier_cap=5 is preserved as maximum Minecraft amplifier 5 / Demoralized VI.")
print("W2 Charge remains frozen; W4+ remain locked.")
