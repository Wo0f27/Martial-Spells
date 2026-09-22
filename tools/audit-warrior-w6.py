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


def git_blob_sha(path: str, normalize_text: bool = False) -> str:
    target = ROOT / path
    require(target.is_file(), f"missing asset: {path}")
    if not target.is_file():
        return ""

    if normalize_text:
        # Git may check text files out as CRLF on Windows even though the
        # frozen upstream blob is LF. Hash the canonical LF representation so
        # platform line-ending conversion does not produce a false failure.
        data = target.read_text(encoding="utf-8").replace("\r\n", "\n").encode("utf-8")
    else:
        data = target.read_bytes()

    return hashlib.sha1(
        f"blob {len(data)}\0".encode("ascii") + data
    ).hexdigest()


# Frozen W2-W5 sentinels.
charge = read("src/main/java/com/w0of26/martialspells/spells/ChargeSpell.java")
require("EFFECT_DURATION_TICKS = 200" in charge, "W6 drifted frozen Charge duration")
require("CHARGE_SPEED_SIGN" not in charge, "W6 restored rejected Charge overhead sign")

shout = read("src/main/java/com/w0of26/martialspells/spells/DemoralizingShoutSpell.java")
for token in (
    "RANGE = 12.0F",
    "EFFECT_DURATION_TICKS = 160",
    "AMPLIFIER_CAP = 5",
    "DAMAGE_COEFFICIENT = 0.05D",
):
    require(token in shout, f"W6 drifted frozen Demoralizing Shout token: {token}")

throw_net = read("src/main/java/com/w0of26/martialspells/spells/ThrowNetSpell.java")
for token in (
    "CAST_TIME_TICKS = 9",
    "MIN_RELEASE_RATIO = 0.20F",
    "OUTPUT_SCALING = 0.50F",
    "NETTED_DURATION_TICKS = 60",
):
    require(token in throw_net, f"W6 drifted frozen Throw Net token: {token}")

shattering = read("src/main/java/com/w0of26/martialspells/spells/ShatteringThrowSpell.java")
for token in (
    "CAST_TIME_TICKS = 10",
    "OUTPUT_SCALING = 1.0F",
    "PROJECTILE_VELOCITY = 0.8F",
    "BOUNCES = 1",
    "SHATTER_DURATION_TICKS = 160",
    "BASE_COOLDOWN_SECONDS = 8",
):
    require(token in shattering, f"W6 drifted frozen Shattering Throw token: {token}")

network = read("src/main/java/com/w0of26/martialspells/network/MartialNetwork.java")
require('PROTOCOL_VERSION = "13"' in network, "W6 unexpectedly changed frozen network protocol")
require("SyncNettedVisualPacket.class" in network, "W6 removed frozen W4 Netted visual sync")

# Mortal Strike spell contract.
spell = read("src/main/java/com/w0of26/martialspells/spells/MortalStrikeSpell.java")
for token, message in (
    ('"mortal_strike"', "Mortal Strike resource id missing"),
    ("MartialTechniqueClass.WARRIOR", "Mortal Strike must be WARRIOR"),
    ("SpellRarity.EPIC", "source tier 4 must map to EPIC"),
    ("CAST_TIME_TICKS = 10", "cast time must be 10 ticks"),
    ("RANGE = 3.0F", "1.20.1 melee mechanic must resolve to 3 blocks"),
    ("DAMAGE_BONUS = 0.50F", "damage bonus must be +50%"),
    ("ATTACK_DELAY_FRACTION = 0.30F", "melee contact delay must be 30% of attack cycle"),
    ("HITBOX_LENGTH_FACTOR = 1.0F", "hitbox length factor must be 1.0"),
    ("HITBOX_WIDTH_FACTOR = 0.50F", "hitbox width factor must be 0.5"),
    ("HITBOX_HEIGHT_FACTOR = 1.50F", "hitbox height factor must be 1.5"),
    ("ARC_DEGREES = 120.0F", "melee arc must be 120 degrees"),
    ("BLEED_DURATION_TICKS = 120", "Bleed duration must be 120 ticks"),
    ("BLEED_BASE_AMPLIFIER = 1", "Bleed base amplifier must be 1"),
    ("BLEED_POWER_MULTIPLIER = 0.25D", "Bleed power amplifier multiplier must be 0.25"),
    ("BLEED_IMPACT_PARTICLES = 40", "Mortal Strike impact must emit 40 blood particles"),
    ("IMPACT_SOUND_CAP = 3", "source impact sound cap must be 3"),
    ("BASE_COOLDOWN_SECONDS = 15", "base cooldown must be 15 seconds"),
    ("baseManaCost = 0", "Mortal Strike must remain zero mana"),
    ("CastType.LONG", "Mortal Strike must use a non-partial timed cast"),
    ("two_handed_slash_vertical_windup", "windup animation id missing"),
    ("two_handed_slash_vertical_slash", "slash animation id missing"),
    ("MORTAL_STRIKE_SWING", "cast-start sound missing"),
    ("MortalStrikeAttackManager.begin(player)", "Mortal Strike delivery manager not started"),
):
    require(token in spell, message)

# Melee delivery.
manager = read("src/main/java/com/w0of26/martialspells/combat/MortalStrikeAttackManager.java")
for token, message in (
    ("20.0D / attackSpeed", "attack-cycle duration must be derived from Attack Speed"),
    ("ATTACK_DELAY_FRACTION", "source 0.3 melee delay not used"),
    ("MORTAL_STRIKE_WHOOSH", "melee whoosh sound missing"),
    ("AttributeModifier.Operation", "temporary attack damage modifier missing"),
    (".MULTIPLY_TOTAL", "Mortal Strike +50% must use MULTIPLY_TOTAL"),
    ("DAMAGE_BONUS", "Mortal Strike damage bonus constant not applied"),
    ("martialSpells$setAttackStrengthTicker(", "vanilla attack must be forced fully charged"),
    ("martialSpells$setInvulnerableTime(0)", "target iframe bypass missing"),
    ("player.attack(target)", "Mortal Strike must route through vanilla player attack"),
    ("MORTAL_STRIKE_IMPACT", "impact sound missing"),
    ("IMPACT_SOUND_CAP", "impact sound cap missing"),
    ("BLEED_BASE_AMPLIFIER", "Bleed amplifier base missing"),
    ("BLEED_POWER_MULTIPLIER", "Bleed power scaling missing"),
    ("player.getAttributeValue(", "Bleed must read live boosted Attack Damage"),
    ("MartialEffectRegistry.BLEED.get()", "Bleed application missing"),
    ("BLEED_IMPACT_PARTICLES", "40-particle impact VFX missing"),
    ("0.20D", "impact blood min speed must be 0.2"),
    ("0.40D", "impact blood max speed must be 0.4"),
):
    require(token in manager, message)

require(
    manager.index("attackDamage.addTransientModifier") < manager.index("applyBleed(player, living)")
    < manager.rindex("attackDamage.removeModifier"),
    "Bleed must be calculated while the temporary +50% Attack Damage modifier is active"
)

# Exact Spell Engine 1.10.5.034 Bleed.
bleed = read("src/main/java/com/w0of26/martialspells/effects/BleedEffect.java")
for token, message in (
    ("MobEffectCategory.HARMFUL", "Bleed must be harmful"),
    ("0xB30000", "Bleed source color drifted"),
    ("TICK_INTERVAL = 25", "Bleed must tick every 25 ticks"),
    ("MIN_MULTIPLIER = 0.5D", "stationary Bleed multiplier must be 0.5"),
    ("MAX_MULTIPLIER = 2.0D", "maximum Bleed multiplier must be 2.0"),
    ("MOVEMENT_SPEED_CAP = 2.0D", "Bleed movement cap must be 2 blocks/sec"),
    ("* 20.0D", "Bleed movement must convert blocks/tick to blocks/sec"),
    ("int stacks = amplifier + 1", "Bleed effective stack calculation missing"),
    ("entity.damageSources().magic()", "Bleed must use vanilla magic damage"),
):
    require(token in bleed, message)

require("Math.min(" in bleed and "MOVEMENT_SPEED_CAP" in bleed,
        "Bleed movement multiplier must clamp at the 2 blocks/sec cap")
require("getHealth()" not in bleed,
        "Bleed must remain lethal and must not inherit poison's 1-HP floor")

bleed_vfx = read("src/main/java/com/w0of26/martialspells/events/BleedVisualEvents.java")
for token, message in (
    ("FREQUENCY_TICKS = 5", "Bleed particle frequency must be every 5 ticks"),
    ("bleed.getAmplifier() + 1", "Bleed particle count must scale with effective stacks"),
    ("0.10D", "Bleed persistent particle min speed must be 0.1"),
    ("0.30D", "Bleed persistent particle max speed must be 0.3"),
):
    require(token in bleed_vfx, message)

# Registrations/resources.
effect_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java")
require('"bleed"' in effect_registry and "BleedEffect::new" in effect_registry,
        "effect registry missing local Bleed")

spell_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java")
require('SPELLS.register("mortal_strike", MortalStrikeSpell::new)' in spell_registry,
        "spell registry missing Mortal Strike")

sound_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java")
for name in ("mortal_strike_swing", "mortal_strike_whoosh", "mortal_strike_impact"):
    require(f'register("{name}")' in sound_registry, f"sound registry missing {name}")

sounds = json.loads(read("src/main/resources/assets/martial_spells/sounds.json"))
for name in ("mortal_strike_swing", "mortal_strike_whoosh", "mortal_strike_impact"):
    require(sounds.get(name, {}).get("sounds") == [f"martial_spells:{name}"],
            f"sounds.json mapping wrong for {name}")

warrior = json.loads(read("src/main/resources/data/martial_spells/tags/spells/warrior_techniques.json"))
require(warrior.get("values") == [
    "martial_spells:charge",
    "martial_spells:demoralizing_shout",
    "martial_spells:throw_net",
    "martial_spells:shattering_throw",
    "martial_spells:mortal_strike",
], "W6 Warrior tag must contain exactly W2-W6 techniques")

martial = json.loads(read("src/main/resources/data/martial_spells/tags/spells/martial_techniques.json"))
require("martial_spells:mortal_strike" in martial.get("values", []),
        "Mortal Strike missing from martial_techniques")

expected_assets = {
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/mortal_strike.png":
        "d3f35879c2cbeb1c3d78f5d465d510dca39db4d2",
    "src/main/resources/assets/martial_spells/textures/mob_effect/bleed.png":
        "7384b4e1ea32644b5a3cc05907e402355c25bb8c",
    "src/main/resources/assets/martial_spells/sounds/mortal_strike_swing.ogg":
        "450936a836623b4215680c8c76c7c54d43df975d",
    "src/main/resources/assets/martial_spells/sounds/mortal_strike_whoosh.ogg":
        "52f05d483959c9c856246e4ee610359fad1a56b7",
    "src/main/resources/assets/martial_spells/sounds/mortal_strike_impact.ogg":
        "1b89880a1d631872a25ee2fc539d9fd6d92234b0",
    "src/main/resources/assets/martial_spells/player_animation/two_handed_slash_vertical_windup.json":
        "68fb82550e479c3a52b1d0c3a1db86b0323e8c38",
    "src/main/resources/assets/martial_spells/player_animation/two_handed_slash_vertical_slash.json":
        "f15ceccfdafb393eea6938bedac61249d047985c",
}
text_assets = {
    "src/main/resources/assets/martial_spells/player_animation/two_handed_slash_vertical_windup.json",
    "src/main/resources/assets/martial_spells/player_animation/two_handed_slash_vertical_slash.json",
}
for path, expected in expected_assets.items():
    require(
        git_blob_sha(path, normalize_text=path in text_assets) == expected,
        f"frozen asset mismatch: {path}"
    )

windup = json.loads(read(
    "src/main/resources/assets/martial_spells/player_animation/two_handed_slash_vertical_windup.json"
))
slash = json.loads(read(
    "src/main/resources/assets/martial_spells/player_animation/two_handed_slash_vertical_slash.json"
))
require(windup.get("name") == "two_handed_slash_vertical_windup",
        "windup PlayerAnimator internal name drifted")
require(slash.get("name") == "two_handed_slash_vertical_slash",
        "slash PlayerAnimator internal name drifted")

# W7 remains locked.
require(
    not (ROOT / "src/main/java/com/w0of26/martialspells/spells/LastStandSpell.java").exists(),
    "W6 must not contain Last Stand gameplay"
)

build_gradle = read("build.gradle")
mods_toml = read("src/main/resources/META-INF/mods.toml")
for dependency_token in ("spell_engine", "spell-engine", "spell_power", "spell-power"):
    require(dependency_token not in build_gradle.lower(),
            f"W6 introduced forbidden dependency token in build.gradle: {dependency_token}")
    require(dependency_token not in mods_toml.lower(),
            f"W6 introduced forbidden dependency token in mods.toml: {dependency_token}")

if errors:
    print("Warrior W6 audit FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Warrior W6 audit PASS")
print("Mortal Strike: 10-tick windup, 3-block/120-degree vertical melee, +50% vanilla weapon damage.")
print("Contact: 30% of current melee attack cycle; fully charged vanilla attack; iframe bypass/restore.")
print("Bleed .034: 6 sec; amp 1 + floor(0.25 x boosted AD); lethal magic tick every 25 ticks.")
print("Bleed damage: 0.5x per stack stationary -> 2.0x at >=2 blocks/sec; VFX every 5 ticks.")
print("W0-W5 remain frozen; W7 remains locked.")
