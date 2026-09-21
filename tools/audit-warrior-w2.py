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


charge_effect = read("src/main/java/com/w0of26/martialspells/effects/ChargeEffect.java")
require("MobEffectCategory.BENEFICIAL" in charge_effect, "Charge effect must be beneficial")
require("Attributes.MOVEMENT_SPEED" in charge_effect, "Charge effect is missing Movement Speed")
require("Attributes.KNOCKBACK_RESISTANCE" in charge_effect, "Charge effect is missing Knockback Resistance")
require(charge_effect.count("0.5D") >= 2, "Charge effect must use +0.5 for both frozen attribute modifiers")
require(charge_effect.count("AttributeModifier.Operation.MULTIPLY_BASE") == 2,
        "Charge effect must use MULTIPLY_BASE for both frozen modifiers")

charge_spell = read("src/main/java/com/w0of26/martialspells/spells/ChargeSpell.java")
for token, message in (
    ('"charge"', "Charge spell resource id is missing"),
    ("MartialTechniqueClass.WARRIOR", "Charge must be classified as WARRIOR"),
    ("SpellRarity.RARE", "Charge must retain frozen tier-3/RARE mapping"),
    ("EFFECT_DURATION_TICKS = 200", "Charge duration must be exactly 200 ticks / 10 seconds"),
    ("MOVEMENT_SPEED_BONUS = 0.50D", "Charge Movement Speed bonus drifted from +50%"),
    ("KNOCKBACK_RESISTANCE_BONUS = 0.50D", "Charge Knockback Resistance bonus drifted from +50%"),
    ("BASE_COOLDOWN_SECONDS = 12", "Charge base cooldown must be exactly 12 seconds"),
    ("MartialEffectRegistry.CHARGE.get()", "Charge spell must apply the registered Charge effect"),
    ("MartialSoundRegistry.CHARGE_ACTIVATE.get()", "Charge spell must play the frozen activation sound"),
    ("castTime = 0", "Charge must remain instant"),
    ("baseManaCost = 0", "Charge must remain zero-mana during the fidelity port"),
):
    require(token in charge_spell, message)
require("new MobEffectInstance" in charge_spell and ",\n                0," in charge_spell,
        "Charge must apply amplifier 0 / SET-style effect semantics")
require("Spell Engine" in charge_spell, "Charge translation note should document the removed Spell Engine dependency")
require("ParticleTypes.CRIT" not in charge_spell, "Charge must not fall back to the old vanilla CRIT approximation")
for particle_token in ("CHARGE_STRIPE", "CHARGE_SPARK"):
    require(particle_token in charge_spell, f"Charge release VFX is missing {particle_token}")

particle_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialParticleRegistry.java")
for particle_id in ("charge_stripe", "charge_spark"):
    require(f'PARTICLES.register("{particle_id}"' in particle_registry,
            f"MartialParticleRegistry is missing {particle_id}")

client_events = read("src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java")
for provider in ("ChargeStripeParticle.Provider", "ChargeSparkParticle.Provider"):
    require(provider in client_events, f"client particle provider missing: {provider}")

for path in (
    "src/main/resources/assets/martial_spells/particles/charge_stripe.json",
    "src/main/resources/assets/martial_spells/particles/charge_spark.json",
):
    try:
        json.loads(read(path))
    except json.JSONDecodeError as exc:
        errors.append(f"invalid JSON in {path}: {exc}")

spell_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java")
require('SPELLS.register("charge", ChargeSpell::new)' in spell_registry,
        "MartialSpellRegistry is missing Charge")

fx_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java")
require('MOB_EFFECTS.register(\n                    "charge",\n                    ChargeEffect::new' in fx_registry,
        "MartialEffectRegistry is missing Charge")

sound_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java")
require('CHARGE_ACTIVATE' in sound_registry and 'register("charge_activate")' in sound_registry,
        "MartialSoundRegistry is missing charge_activate")

sounds_path = "src/main/resources/assets/martial_spells/sounds.json"
try:
    sounds = json.loads(read(sounds_path))
    require(sounds.get("charge_activate", {}).get("sounds") == ["martial_spells:charge_activate"],
            "sounds.json charge_activate mapping is incorrect")
except json.JSONDecodeError as exc:
    errors.append(f"invalid JSON in {sounds_path}: {exc}")

warrior_tag_path = "src/main/resources/data/martial_spells/tags/spells/warrior_techniques.json"
try:
    warrior_tag = json.loads(read(warrior_tag_path))
    require(warrior_tag.get("replace") is False, "warrior_techniques tag must use replace=false")
    require(warrior_tag.get("values") == ["martial_spells:charge"],
            "W2 warrior_techniques tag must contain only Charge")
except json.JSONDecodeError as exc:
    errors.append(f"invalid JSON in {warrior_tag_path}: {exc}")

martial_tag_path = "src/main/resources/data/martial_spells/tags/spells/martial_techniques.json"
try:
    martial_tag = json.loads(read(martial_tag_path))
    require("martial_spells:charge" in martial_tag.get("values", []),
            "Charge is missing from martial_techniques")
except json.JSONDecodeError as exc:
    errors.append(f"invalid JSON in {martial_tag_path}: {exc}")

lang_path = "src/main/resources/assets/martial_spells/lang/en_us.json"
try:
    lang = json.loads(read(lang_path))
    for key in (
        "effect.martial_spells.charge",
        "spell.martial_spells.charge",
        "spell.martial_spells.charge.description",
        "spell.martial_spells.charge.guide",
        "ui.martial_spells.charge_movement_speed",
        "ui.martial_spells.charge_knockback_resistance",
    ):
        require(key in lang, f"missing Charge localization: {key}")
except json.JSONDecodeError as exc:
    errors.append(f"invalid JSON in {lang_path}: {exc}")

require(git_blob_sha("src/main/resources/assets/martial_spells/textures/gui/spell_icons/charge.png")
        == "c278a806b067c9350bf81fbf89783c289a651517",
        "Charge spell icon does not match frozen Rogues")
require(git_blob_sha("src/main/resources/assets/martial_spells/textures/mob_effect/charge.png")
        == "c278a806b067c9350bf81fbf89783c289a651517",
        "Charge effect icon does not match frozen Rogues")
require("CHARGE_SPEED_SIGN" not in charge_spell,
        "Charge must not show the removed above-head speed/Charge sign")
require(not (ROOT / "src/main/java/com/w0of26/martialspells/client/particle/ChargeSpeedSignParticle.java").exists(),
        "removed Charge speed-sign particle class must stay absent")
require(not (ROOT / "src/main/resources/assets/martial_spells/particles/charge_speed_sign.json").exists(),
        "removed Charge speed-sign particle definition must stay absent")
require(not (ROOT / "src/main/resources/assets/martial_spells/textures/particle/charge_speed_sign.png").exists(),
        "removed Charge speed-sign texture must stay absent")
require(git_blob_sha("src/main/resources/assets/martial_spells/sounds/charge_activate.ogg")
        == "77ec5f2f81300f268e686c9fe2c1800127f2936f",
        "Charge activation sound does not match frozen Rogues; run tools/sync-warrior-w2-assets.ps1")

# W2 is a single vertical slice. Later Warrior gameplay remains locked.
for filename in (
    "ShatteringThrowSpell.java",
    "ThrowNetSpell.java",
    "DemoralizingShoutSpell.java",
    "LastStandSpell.java",
    "MortalStrikeSpell.java",
):
    require(not (ROOT / "src/main/java/com/w0of26/martialspells/spells" / filename).exists(),
            f"W2 must not contain later Warrior gameplay class {filename}")

build_gradle = read("build.gradle")
mods_toml = read("src/main/resources/META-INF/mods.toml")
for dependency_token in ("spell_engine", "spell-engine", "spell_power", "spell-power"):
    require(dependency_token not in build_gradle.lower(),
            f"W2 introduced forbidden dependency token in build.gradle: {dependency_token}")
    require(dependency_token not in mods_toml.lower(),
            f"W2 introduced forbidden dependency token in mods.toml: {dependency_token}")

if errors:
    print("Warrior W2 audit FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Warrior W2 audit PASS")
print("Charge: 10 sec, +50% base Movement Speed, +50% base Knockback Resistance, 12 sec base cooldown.")
print("Only Charge is active in the Warrior technique tag; W3+ remain locked.")
