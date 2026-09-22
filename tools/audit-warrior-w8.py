from pathlib import Path
import hashlib
import json
import subprocess
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


# W7 is the first historical audit whose expected repository state already
# contains all six finished Warrior techniques. Reuse it as the cumulative
# mechanics/presentation baseline, then add W8 final-surface checks below.
w7 = subprocess.run(
    [sys.executable, str(ROOT / "tools" / "audit-warrior-w7.py")],
    cwd=ROOT,
    text=True,
)
require(w7.returncode == 0, "W7 cumulative audit failed; fix regression before W8")


warrior_ids = [
    "charge",
    "demoralizing_shout",
    "throw_net",
    "shattering_throw",
    "mortal_strike",
    "last_stand",
]

spell_files = {
    "charge": "src/main/java/com/w0of26/martialspells/spells/ChargeSpell.java",
    "demoralizing_shout": "src/main/java/com/w0of26/martialspells/spells/DemoralizingShoutSpell.java",
    "throw_net": "src/main/java/com/w0of26/martialspells/spells/ThrowNetSpell.java",
    "shattering_throw": "src/main/java/com/w0of26/martialspells/spells/ShatteringThrowSpell.java",
    "mortal_strike": "src/main/java/com/w0of26/martialspells/spells/MortalStrikeSpell.java",
    "last_stand": "src/main/java/com/w0of26/martialspells/spells/LastStandSpell.java",
}

# Exactly six registered Warrior spell IDs, all one-level, zero-mana,
# Martial-school techniques classified as WARRIOR.
registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java")
for spell_id in warrior_ids:
    require(
        registry.count(f'SPELLS.register("{spell_id}"') == 1,
        f"Warrior registry missing/duplicates {spell_id}"
    )

for spell_id, path in spell_files.items():
    content = read(path)
    require(
        "MartialTechniqueClass.WARRIOR" in content,
        f"{spell_id} lost WARRIOR classification"
    )
    require(
        "MAX_LEVEL = 1" in content,
        f"{spell_id} must remain one-level during fidelity port"
    )
    require(
        "baseManaCost = 0" in content and "manaCostPerLevel = 0" in content,
        f"{spell_id} must remain zero-mana"
    )
    require(
        "MartialSchoolRegistry.MARTIAL_RESOURCE" in content,
        f"{spell_id} must remain in Martial school"
    )

warrior_tag = json.loads(read(
    "src/main/resources/data/martial_spells/tags/spells/warrior_techniques.json"
))
expected_warrior_tag = [f"martial_spells:{spell_id}" for spell_id in warrior_ids]
require(
    warrior_tag.get("replace") is False,
    "warrior_techniques must remain additive"
)
require(
    warrior_tag.get("values") == expected_warrior_tag,
    "warrior_techniques must contain exactly the six frozen Warrior techniques"
)

martial_tag = json.loads(read(
    "src/main/resources/data/martial_spells/tags/spells/martial_techniques.json"
))
martial_values = martial_tag.get("values", [])
for spell_id in warrior_ids:
    require(
        f"martial_spells:{spell_id}" in martial_values,
        f"{spell_id} missing from martial_techniques"
    )

# W1 shared adapters must remain narrow.
power = read("src/main/java/com/w0of26/martialspells/combat/PhysicalMeleePower.java")
require(
    "Attributes.ATTACK_DAMAGE" in power and "attackDamage.getValue()" in power,
    "PhysicalMeleePower must read current vanilla Attack Damage"
)
for forbidden in ("DualMeleePower", "MartialPowerHelper", "SPELL_POWER"):
    require(
        forbidden not in power,
        f"PhysicalMeleePower widened beyond frozen single-hand source contract: {forbidden}"
    )

release_bridge = read(
    "src/main/java/com/w0of26/martialspells/mixin/IronsReleaseUsingHelperMixin.java"
)
require(
    "instanceof ReleaseChargedTechnique" in release_bridge,
    "charged-release mixin must remain opt-in only"
)
require(
    "ci.cancel();" in release_bridge,
    "charged-release bridge no longer consumes successful opted-in release"
)

# Final source-mechanics sentinels for all six spells.
charge = read(spell_files["charge"])
for token in (
    "SpellRarity.RARE",
    "EFFECT_DURATION_TICKS = 200",
    "MOVEMENT_SPEED_BONUS = 0.50D",
    "KNOCKBACK_RESISTANCE_BONUS = 0.50D",
    "BASE_COOLDOWN_SECONDS = 12",
):
    require(token in charge, f"Charge final contract drifted: {token}")

shout = read(spell_files["demoralizing_shout"])
for token in (
    "SpellRarity.RARE",
    "RANGE = 12.0F",
    "VERTICAL_RANGE_MULTIPLIER = 0.5F",
    "EFFECT_DURATION_TICKS = 160",
    "AMPLIFIER_CAP = 5",
    "ATTACK_DAMAGE_REDUCTION_PER_LEVEL = 0.20D",
    "DAMAGE_COEFFICIENT = 0.05D",
    "CONTROL_HEALTH_BASE = 50.0D",
    "CONTROL_POWER_MULTIPLIER = 2.0D",
    "BASE_COOLDOWN_SECONDS = 12",
):
    require(token in shout, f"Demoralizing Shout final contract drifted: {token}")

throw_net = read(spell_files["throw_net"])
for token in (
    "SpellRarity.UNCOMMON",
    "CAST_TIME_TICKS = 9",
    "MIN_RELEASE_RATIO = 0.20F",
    "OUTPUT_SCALING = 0.50F",
    "BASE_RANGE = 10.0F",
    "CHARGE_RANGE_BONUS = 12.0F",
    "PROJECTILE_VELOCITY = 1.0F",
    "HOMING_DEGREES_PER_TICK = 1.0F",
    "SPIN_DEGREES_PER_TICK = 12.0F",
    "DAMAGE_COEFFICIENT = 0.10D",
    "KNOCKBACK_COEFFICIENT = 0.10F",
    "NETTED_DURATION_TICKS = 60",
    "CONTROL_HEALTH_BASE = 100.0D",
    "CONTROL_POWER_MULTIPLIER = 2.0D",
    "BASE_COOLDOWN_SECONDS = 12",
):
    require(token in throw_net, f"Throw Net final contract drifted: {token}")

shattering = read(spell_files["shattering_throw"])
for token in (
    "SpellRarity.UNCOMMON",
    "CAST_TIME_TICKS = 10",
    "MIN_RELEASE_RATIO = 0.20F",
    "OUTPUT_SCALING = 1.0F",
    "BASE_RANGE = 12.0F",
    "CHARGE_RANGE_BONUS = 12.0F",
    "PROJECTILE_VELOCITY = 0.8F",
    "HOMING_DEGREES_PER_TICK = 2.0F",
    "SPIN_DEGREES_PER_TICK = -36.0F",
    "BOUNCES = 1",
    "DAMAGE_COEFFICIENT = 1.0D",
    "KNOCKBACK_COEFFICIENT = 1.0F",
    "SHATTER_DURATION_TICKS = 160",
    "CONTROL_HEALTH_BASE = 100.0D",
    "CONTROL_POWER_MULTIPLIER = 2.0D",
    "BASE_COOLDOWN_SECONDS = 8",
):
    require(token in shattering, f"Shattering Throw final contract drifted: {token}")

mortal = read(spell_files["mortal_strike"])
for token in (
    "SpellRarity.EPIC",
    "CAST_TIME_TICKS = 10",
    "RANGE = 3.0F",
    "DAMAGE_BONUS = 0.50F",
    "ATTACK_DELAY_FRACTION = 0.30F",
    "HITBOX_LENGTH_FACTOR = 1.0F",
    "HITBOX_WIDTH_FACTOR = 0.50F",
    "HITBOX_HEIGHT_FACTOR = 1.50F",
    "ARC_DEGREES = 120.0F",
    "BLEED_DURATION_TICKS = 120",
    "BASE_COOLDOWN_SECONDS = 15",
):
    require(token in mortal, f"Mortal Strike final contract drifted: {token}")

mortal_manager = read(
    "src/main/java/com/w0of26/martialspells/combat/MortalStrikeAttackManager.java"
)
require(
    "player.attack(target)" in mortal_manager,
    "Mortal Strike must continue routing contact through vanilla ServerPlayer#attack"
)

bleed = read("src/main/java/com/w0of26/martialspells/effects/BleedEffect.java")
for token in (
    "TICK_INTERVAL = 25",
    "MIN_MULTIPLIER = 0.5D",
    "MAX_MULTIPLIER = 2.0D",
    "MOVEMENT_SPEED_CAP = 2.0D",
    "entity.damageSources().magic()",
):
    require(token in bleed, f"Spell Engine .034 Bleed translation drifted: {token}")

last_stand = read(spell_files["last_stand"])
for token in (
    "SpellRarity.EPIC",
    "CAST_TIME_TICKS = 50",
    "CHANNEL_TICKS = 5",
    "{5, 15, 25, 35, 45}",
    "EFFECT_DURATION_TICKS = 200",
    "AMPLIFIER_CAP = 4",
    "HEAL_CURRENT_MAX_HEALTH_FRACTION = 0.10F",
    "BASE_COOLDOWN_SECONDS = 60",
    "fullEffectiveCooldown * progress",
):
    require(token in last_stand, f"Last Stand final contract drifted: {token}")

# Effect attribute contracts.
effects = {
    "charge": read("src/main/java/com/w0of26/martialspells/effects/ChargeEffect.java"),
    "demoralized": read("src/main/java/com/w0of26/martialspells/effects/DemoralizedEffect.java"),
    "netted": read("src/main/java/com/w0of26/martialspells/effects/NettedEffect.java"),
    "shatter": read("src/main/java/com/w0of26/martialspells/effects/ShatterEffect.java"),
    "last_stand": read("src/main/java/com/w0of26/martialspells/effects/LastStandEffect.java"),
}
for token in ("Attributes.MOVEMENT_SPEED", "Attributes.KNOCKBACK_RESISTANCE", "0.5D", "MULTIPLY_BASE"):
    require(token in effects["charge"], f"Charge effect drifted: {token}")
for token in ("Attributes.ATTACK_DAMAGE", "-0.20D", "MULTIPLY_BASE"):
    require(token in effects["demoralized"], f"Demoralized effect drifted: {token}")
for token in ("Attributes.MOVEMENT_SPEED", "-2.0D", "Attributes.KNOCKBACK_RESISTANCE", "100.0D", "MULTIPLY_BASE"):
    require(token in effects["netted"], f"Netted effect drifted: {token}")
for token in ("Attributes.ARMOR", "-0.30D", "MULTIPLY_BASE"):
    require(token in effects["shatter"], f"Shatter effect drifted: {token}")
for token in ("Attributes.MAX_HEALTH", "Attributes.KNOCKBACK_RESISTANCE", "0.20D", "MULTIPLY_BASE"):
    require(token in effects["last_stand"], f"Last Stand effect drifted: {token}")
require(
    "damage_taken" not in effects["last_stand"].lower()
    and "LivingHurtEvent" not in effects["last_stand"],
    "stale Last Stand damage-reduction comment was implemented accidentally"
)

# Network final state: W4 Netted visual + W7 early-release animation are both
# present and protocol is the final Warrior protocol 14.
network = read("src/main/java/com/w0of26/martialspells/network/MartialNetwork.java")
require('PROTOCOL_VERSION = "14"' in network, "final Warrior network protocol must be 14")
for packet in ("SyncNettedVisualPacket.class", "SyncLastStandReleaseAnimationPacket.class"):
    require(packet in network, f"final Warrior packet missing: {packet}")

# Exact directly imported binary assets remain frozen. Renamed Martial paths
# intentionally retain the upstream Git blob identity.
expected_assets = {
    # Icons / effect icons
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/charge.png": "c278a806b067c9350bf81fbf89783c289a651517",
    "src/main/resources/assets/martial_spells/textures/mob_effect/charge.png": "c278a806b067c9350bf81fbf89783c289a651517",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/demoralizing_shout.png": "5e0d0e332fe365dc0f1e677618e7ec33390e39c9",
    "src/main/resources/assets/martial_spells/textures/mob_effect/demoralized.png": "5e0d0e332fe365dc0f1e677618e7ec33390e39c9",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/throw_net.png": "6bffb24937a7aeddb959f05994bd846f21f8164e",
    "src/main/resources/assets/martial_spells/textures/mob_effect/net_trap.png": "6bffb24937a7aeddb959f05994bd846f21f8164e",
    "src/main/resources/assets/martial_spells/textures/spell_projectile/throw_net.png": "57dcfd0337d544ae2f51459b6d7dbb5b3a82e815",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/shattering_throw.png": "d43c529e6ba3b6970be7b68809c38a4ccab0ca0f",
    "src/main/resources/assets/martial_spells/textures/mob_effect/shatter.png": "d43c529e6ba3b6970be7b68809c38a4ccab0ca0f",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/mortal_strike.png": "d3f35879c2cbeb1c3d78f5d465d510dca39db4d2",
    "src/main/resources/assets/martial_spells/textures/mob_effect/bleed.png": "7384b4e1ea32644b5a3cc05907e402355c25bb8c",
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/last_stand.png": "5f6e39441c5368b379731ad086db332079223b18",
    "src/main/resources/assets/martial_spells/textures/mob_effect/last_stand.png": "5f6e39441c5368b379731ad086db332079223b18",

    # Rogues sounds
    "src/main/resources/assets/martial_spells/sounds/charge_activate.ogg": "77ec5f2f81300f268e686c9fe2c1800127f2936f",
    "src/main/resources/assets/martial_spells/sounds/shout_release.ogg": "353c003ba15948d8ff55c6624a939b7cbf07d09b",
    "src/main/resources/assets/martial_spells/sounds/demoralize_impact.ogg": "e78e21e8b74f6bc188de0d3f1b46bad2fb64d29d",
    "src/main/resources/assets/martial_spells/sounds/net_casting.ogg": "5495c850ccfd2ab16becefb4831eccfc3e3834d0",
    "src/main/resources/assets/martial_spells/sounds/throw.ogg": "ce5a789e74441a01f63624a608f8ba3ca1987a2d",
    "src/main/resources/assets/martial_spells/sounds/net_travel.ogg": "304fb9c0eae54244c1d05b1252241f3ba84720d1",
    "src/main/resources/assets/martial_spells/sounds/net_impact.ogg": "d0a9568906788c50def55cdaeb9a73bd51b6466d",
    "src/main/resources/assets/martial_spells/sounds/throw_impact.ogg": "4266dde3a8b9682b4dfd46342d5630534ab4a965",
    "src/main/resources/assets/martial_spells/sounds/mortal_strike_swing.ogg": "450936a836623b4215680c8c76c7c54d43df975d",
    "src/main/resources/assets/martial_spells/sounds/mortal_strike_whoosh.ogg": "52f05d483959c9c856246e4ee610359fad1a56b7",
    "src/main/resources/assets/martial_spells/sounds/mortal_strike_impact.ogg": "1b89880a1d631872a25ee2fc539d9fd6d92234b0",
    "src/main/resources/assets/martial_spells/sounds/last_stand_start.ogg": "f6ce90b0a0c89e66cc60e74bedc64b04d03871cf",
    "src/main/resources/assets/martial_spells/sounds/last_stand_casting.ogg": "ff5dafe52ce8e3f22a867238f8d4bf1e6fd7dbac",
    "src/main/resources/assets/martial_spells/sounds/last_stand_release.ogg": "19f4d8deead19c22eff36454e5f3c2e8cf1176d1",

    # Exact Spell Engine .034 animation assets used by W6/W7
    "src/main/resources/assets/martial_spells/player_animation/two_handed_slash_vertical_windup.json": "68fb82550e479c3a52b1d0c3a1db86b0323e8c38",
    "src/main/resources/assets/martial_spells/player_animation/two_handed_slash_vertical_slash.json": "f15ceccfdafb393eea6938bedac61249d047985c",
    "src/main/resources/assets/martial_spells/player_animation/one_handed_ground_charge.json": "7294d126bf932f843e82f3864f5a5b5961f35839",
    "src/main/resources/assets/martial_spells/player_animation/one_handed_shout_release.json": "292e820254a1088297ad518b1deb49f612b9739d",
}
text_assets = {
    path for path in expected_assets
    if path.endswith(".json")
}
for path, expected in expected_assets.items():
    require(
        git_blob_sha(path, normalize_text=path in text_assets) == expected,
        f"frozen final asset mismatch: {path}"
    )

# Dependency boundary remains intact.
build_gradle = read("build.gradle").lower()
mods_toml = read("src/main/resources/META-INF/mods.toml").lower()
for token in ("spell_engine", "spell-engine", "spell_power", "spell-power"):
    require(token not in build_gradle, f"forbidden Warrior runtime dependency in build.gradle: {token}")
    require(token not in mods_toml, f"forbidden Warrior runtime dependency in mods.toml: {token}")

# Accepted Rogue-complete baseline must still exist. Warrior work may touch
# shared registries/networking, but not remove the previously frozen Rogue port.
for path in (
    "src/main/java/com/w0of26/martialspells/spells/ShockPowderSpell.java",
    "src/main/java/com/w0of26/martialspells/spells/ShadowstepSpell.java",
    "src/main/java/com/w0of26/martialspells/spells/SliceAndDiceSpell.java",
    "src/main/java/com/w0of26/martialspells/spells/VanishSpell.java",
    "src/main/java/com/w0of26/martialspells/spells/MutilateSpell.java",
    "src/main/java/com/w0of26/martialspells/spells/BearTrapSpell.java",
    "src/main/resources/data/martial_spells/tags/spells/rogue_techniques.json",
):
    require((ROOT / path).is_file(), f"accepted Rogue baseline missing: {path}")

if errors:
    print("Warrior W8 FINAL AUDIT FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Warrior W8 STATIC FINAL AUDIT PASS")
print("Source inventory: exactly 6 Warrior techniques.")
print("Mechanics: W2-W7 frozen contracts preserved; Charge retains approved 10-second override.")
print("Assets: exact imported icons/sounds and W6/W7 Spell Engine .034 animations verified.")
print("Dependencies: no Spell Engine / Spell Power runtime dependency introduced.")
print("Integration: accepted Rogue baseline still present; final Warrior network protocol = 14.")
print("Runtime/build validation is still required before W8 can be frozen PASS.")
