"""AR4 static source/target gate. Minecraft behavior remains a developer runtime gate."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/w0of26/martialspells"
ASSETS = ROOT / "src/main/resources/assets/martial_spells"
ARCHERS = Path(r"C:\Modding\RPG-Ports\upstream\Archers")
ENGINE = Path(r"C:\Modding\RPG-Ports\upstream\SpellEngine")
errors = []


def read(path):
    return path.read_text(encoding="utf-8")


def check(ok, message):
    if not ok:
        errors.append(message)


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)


def require_tokens(path, *tokens):
    source = read(path)
    for token in tokens:
        check(token in source, f"{path.relative_to(ROOT)} lacks {token}")
    return source


def source_blob(repo, revision, path):
    return subprocess.check_output(["git", "-c", f"safe.directory={repo.as_posix()}",
                                    "show", f"{revision}:{path}"], cwd=repo)


contracts = ["ARCHERS_RANGER_SPELL_PORT.md", "ARCHERS_RANGER_AR1.md",
             "ARCHERS_RANGER_AR2.md", "ARCHERS_RANGER_AR3.md"]
for name in contracts:
    check((ROOT / name).exists(), f"frozen contract missing: {name}")
contract = read(ROOT / contracts[0])
for revision in ("ee8fb4081422ba0ab8ced57fe773a1c048d7bf74",
                 "d057c13b434cadc8eeaea6df8deab6992f8bda7e",
                 "5ea25f9d906de0ba87536243db60aea07a77f5b8"):
    check(revision in contract or revision in read(ROOT / "ARCHERS_RANGER_AR3.md"),
          f"frozen source correction missing {revision}")
check(git("merge-base", "HEAD", "a3541a94ef9a9d8b18489a8b1f8a288b45fb6505").strip()
      == "a3541a94ef9a9d8b18489a8b1f8a288b45fb6505", "wrong AR4 base")

registry = require_tokens(JAVA / "registry/MartialSpellRegistry.java",
                          'SPELLS.register("rain_of_arrows"', 'SPELLS.register("magic_arrow"')
for spell_id in ("rain_of_arrows", "magic_arrow"):
    check(len(re.findall(rf'SPELLS\.register\("{spell_id}"', registry)) == 1,
          f"{spell_id} not registered exactly once")
for old in ("power_shot", "entangling_roots", "barrage", "entangling_arrow", "caltrops"):
    check(f'SPELLS.register("{old}"' in registry, f"existing Ranger ID lost: {old}")
check('SPELLS.register("spirit_wolf"' not in registry, "Spirit Wolf exposed early")
for tag in ("ranger_techniques", "martial_techniques"):
    values = json.loads(read(ROOT / f"src/main/resources/data/martial_spells/tags/spells/{tag}.json"))["values"]
    for spell_id in ("rain_of_arrows", "magic_arrow"):
        check(values.count(f"martial_spells:{spell_id}") == 1, f"{tag}: {spell_id} count")

rain = require_tokens(JAVA / "spells/RainOfArrowsSpell.java",
                      "MartialTechniqueClass.RANGER", "SpellRarity.EPIC", "setMaxLevel(1)",
                      "baseManaCost = 0", "manaCostPerLevel = 0", "CastType.LONG",
                      "CAST_TICKS = 10", "COOLDOWN_SECONDS = 15", "LAUNCH_COUNT = 21",
                      "LAUNCH_DELAY_TICKS = 5", "LAUNCH_RADIUS = 2.0D",
                      "LAUNCH_HEIGHT = 10.0D", "VELOCITY = 1.5D",
                      "IMPACT_RADIUS = 1.5D", "DAMAGE_COEFFICIENT = 0.4D",
                      "KNOCKBACK = 0.25D", "RangerAim.entity", "STICKY_AIM",
                      "NominalPhysicalRangedPower.get(player)", "RainOfArrowsPending.begin")
pending = require_tokens(JAVA / "combat/RainOfArrowsPending.java",
                         "extends SavedData", "nextIndex", "nextDeadline",
                         "getGameTime() + RainOfArrowsSpell.LAUNCH_DELAY_TICKS",
                         "job.nextDeadline += RainOfArrowsSpell.LAUNCH_DELAY_TICKS",
                         "job.nextIndex >= RainOfArrowsSpell.LAUNCH_COUNT",
                         "tag.putUUID(\"Caster\"", "tag.putUUID(\"Target\"",
                         "tag.putDouble(\"Power\"", "tag.putInt(\"NextIndex\"",
                         "tag.putLong(\"Deadline\"")
meteor = require_tokens(JAVA / "entity/RainMeteorProjectile.java",
                        "extends Entity", "index == 0 ? targetId : null",
                        "nextFloat() * Math.PI * 2.0D", "Math.cos(angle) * RainOfArrowsSpell.LAUNCH_RADIUS",
                        "Math.sin(angle) * RainOfArrowsSpell.LAUNCH_RADIUS",
                        "RainOfArrowsSpell.LAUNCH_HEIGHT * 0.98D", "distanceTraveled >= threshold",
                        "server.hasChunkAt", "tickCount > 1200", "RainOfArrowsSpell.canAreaDamage",
                        "RainOfArrowsSpell.DAMAGE_COEFFICIENT * sampledPower",
                        "0.4D * RainOfArrowsSpell.KNOCKBACK",
                        "RANGER_RAIN_AREA", "RAIN_OF_ARROWS_IMPACT",
                        "tag.putDouble(\"DistanceTraveled\"", "tag.putDouble(\"SampledPower\"")
check("AbstractArrow" not in meteor and "ArchersPhysicalArrow" not in meteor,
      "Rain incorrectly uses physical-arrow pipeline")
check("nextFloat() * Math.PI * 2.0D" in meteor and "nextDouble() * RainOfArrowsSpell.LAUNCH_RADIUS" not in meteor,
      "Rain delayed sample became a disk")

magic = require_tokens(JAVA / "spells/RangerMagicArrowSpell.java",
                       "MartialTechniqueClass.RANGER", "SpellRarity.EPIC", "setMaxLevel(1)",
                       "baseManaCost = 0", "manaCostPerLevel = 0", "CastType.LONG",
                       "COOLDOWN_SECONDS = 8", "REFERENCE_CHARGE_TICKS = 20",
                       "MIN_RELEASE_RATIO = 0.2F", "REFERENCE_CHARGE_TICKS / castHaste",
                       "Mth.clamp(data.getCastCompletionPercent()", "ratio < MIN_RELEASE_RATIO",
                       "BASE_RANGE = 16.0F", "RANGE_BONUS = 48.0F",
                       "VELOCITY = 1.5F", "HOMING_DEGREES_PER_TICK = 2.0F",
                       "PIERCE = 99999", "DAMAGE_COEFFICIENT = 1.2D",
                       "KNOCKBACK_COEFFICIENT = 2.0D", "stack.is(Items.ARROW)",
                       "arrowCost(player).shrink(1)", "RangerAim.entity",
                       "NominalPhysicalRangedPower.get(player)", "RangerSourceVfx.magicCharge",
                       "RangerSourceVfx.magicRelease")
projectile = require_tokens(JAVA / "entity/RangerMagicArrowProjectile.java",
                            "extends Projectile", "distanceTraveled >= maxRange",
                            "tickCount > 1200", "server.hasChunkAt", "successfulHits.contains",
                            "ForgeEventFactory.onProjectileImpact", "RangerProjectileMath.turnTowards",
                            "successfulHits.add", "pierceRemaining--",
                            "RangerMagicArrowSpell.DAMAGE_COEFFICIENT * sampledPower * chargeRatio",
                            "RangerMagicArrowSpell.KNOCKBACK_COEFFICIENT * chargeRatio",
                            "onHitBlock", "discard()", "tag.putDouble(\"MaxRange\"",
                            "tag.putDouble(\"DistanceTraveled\"",
                            "tag.putInt(\"PierceRemaining\"", "SuccessfulHits", "Followed",
                            "super.readAdditionalSaveData(tag)", "super.addAdditionalSaveData(tag)")
check(projectile.index("distanceTraveled >= maxRange") < projectile.index("getHitResultOnMoveVector")
      < projectile.index("RangerProjectileMath.turnTowards") < projectile.index("setPos(getX() + velocity.x"),
      "Magic projectile tick order changed")
check("extends AbstractArrow" not in projectile and "ArchersPhysicalArrow." not in projectile
      and "ARROW_DAMAGE" not in projectile and "MartialPower" not in projectile,
      "Magic projectile uses a forbidden physical-arrow/martial scaling path")

render = require_tokens(JAVA / "client/render/RangerSourceProjectileRenderer.java",
                        "spell_projectile/volley_arrow", "spell_projectile/magic_arrow",
                        "PaladinSpellModelRenderTypes.radiate()", "PaladinSpellModelRenderTypes.glow()",
                        "InventoryMenu.BLOCK_ATLAS")
require_tokens(JAVA / "client/MartialClientEvents.java", "RangerSourceProjectileRenderer.RAIN_MODEL",
               "RangerSourceProjectileRenderer.MAGIC_MODEL", "MartialEntityRegistry.RAIN_METEOR",
               "MartialEntityRegistry.RANGER_MAGIC_ARROW", "RANGER_RAIN_AREA")
require_tokens(JAVA / "client/visual/MagicArrowClientVisuals.java",
               '"irons_spellbooks:magic_arrow"', '"martial_spells:magic_arrow"',
               '"martial_spells:rain_of_arrows"')
vfx = require_tokens(JAVA / "client/visual/RangerSourceVfx.java",
                     "rainRelease", "rainTravel", "rainDirectImpact", "rainAreaImpact",
                     "magicCharge", "magicRelease", "magicTravel", "magicImpact",
                     "Math.floorMod(meteor.tickCount, 4) == 0", "outward.scale(14)",
                     "outward.reverse()", "i < 50", "i < 25", "strand < 2",
                     "i < 5", "i < 10", "projectile.tickCount * 18D + strand * 180D",
                     "Vec3 axis = lookAxis(projectile)")
check("RangerSourceVfx.rainTravel" in meteor and "RangerSourceVfx.rainAreaImpact" in meteor,
      "Rain source travel/impact batches not wired")
check("RangerSourceVfx.magicTravel" in projectile and "RangerSourceVfx.magicImpact" in projectile,
      "Magic source travel/impact batches not wired")
check("RangerSourceVfx.rainRelease" in rain and "archery_upwards_pull" in rain
      and "archery_upwards_release" in rain, "Rain source animation/release VFX missing")
check("archery_pull" in magic and "archery_release" in magic,
      "Magic source archery animations missing")
require_tokens(JAVA / "client/visual/RangerCastingSound.java",
               "AbstractTickableSoundInstance", "isRangerMagicArrowCasting",
               "isRainOfArrowsCasting", "this.looping = true", "GENERIC_WIND_CHARGING", "BOW_PULL")
check("isSourceAnimatedRangerCast" in read(JAVA / "mixin/client/MagicArrowHumanoidModelMixin.java")
      and "isSourceAnimatedRangerCast" in read(JAVA / "mixin/client/MagicArrowPlayerRendererMixin.java"),
      "generic ranged arm pose still overrides Rain/Magic source animation")
for asset in ("textures/gui/spell_icons/rain_of_arrows.png",
              "textures/gui/spell_icons/magic_arrow.png",
              "textures/spell_projectile/volley_arrow.png",
              "textures/spell_projectile/magic_arrow.png",
              "models/spell_projectile/volley_arrow.json",
              "models/spell_projectile/magic_arrow.json",
              "sounds/rain_of_arrows_release.ogg", "sounds/rain_of_arrows_impact.ogg",
              "sounds/magic_arrow_release.ogg", "sounds/magic_arrow_impact.ogg",
              "sounds/generic_wind_charging.ogg", "particles/ranger_rain_area.json"):
    check((ASSETS / asset).is_file(), f"missing source presentation: {asset}")
archer_pairs = {
    "textures/spell/rain_of_arrows.png": "textures/gui/spell_icons/rain_of_arrows.png",
    "textures/spell/magic_arrow.png": "textures/gui/spell_icons/magic_arrow.png",
    "textures/spell_projectile/volley_arrow.png": "textures/spell_projectile/volley_arrow.png",
    "textures/spell_projectile/magic_arrow.png": "textures/spell_projectile/magic_arrow.png",
    "sounds/rain_of_arrows_release.ogg": "sounds/rain_of_arrows_release.ogg",
    "sounds/rain_of_arrows_impact.ogg": "sounds/rain_of_arrows_impact.ogg",
    "sounds/magic_arrow_release.ogg": "sounds/magic_arrow_release.ogg",
    "sounds/magic_arrow_impact.ogg": "sounds/magic_arrow_impact.ogg",
}
for source, dest in archer_pairs.items():
    blob = source_blob(ARCHERS, "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74",
                       "common/src/main/resources/assets/archers/" + source)
    check(hashlib.sha256(blob).digest() == hashlib.sha256((ASSETS / dest).read_bytes()).digest(),
          f"Archers binary differs from frozen source: {dest}")
for name in ("magic_arrow", "volley_arrow"):
    source = source_blob(ARCHERS, "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74",
                         f"common/src/main/resources/assets/archers/models/spell_projectile/{name}.json")
    check(source.replace(b"archers:", b"martial_spells:")
          == (ASSETS / f"models/spell_projectile/{name}.json").read_bytes(),
          f"{name} model differs beyond namespace translation")
for name in ("archery_upwards_pull", "archery_upwards_release", "archery_pull", "archery_release"):
    source = source_blob(ENGINE, "d057c13b434cadc8eeaea6df8deab6992f8bda7e",
                         f"common/src/main/resources/assets/spell_engine/player_animations/{name}.json")
    check(json.loads(source) == json.loads(read(ASSETS / f"player_animation/{name}.json")),
          f"{name} player animation data differs from frozen Spell Engine source")
wind = source_blob(ENGINE, "d057c13b434cadc8eeaea6df8deab6992f8bda7e",
                   "common/src/main/resources/assets/spell_engine/sounds/generic_wind_charging.ogg")
check(wind == (ASSETS / "sounds/generic_wind_charging.ogg").read_bytes(),
      "charging wind sound differs from frozen Spell Engine source")
for i in range(16):
    source = source_blob(ENGINE, "d057c13b434cadc8eeaea6df8deab6992f8bda7e",
                         f"common/src/main/resources/assets/spell_engine/textures/particle/zone/effect_658_{i}.png")
    check(source == (ASSETS / f"textures/particle/ranger_source/zone/effect_658_{i}.png").read_bytes(),
          f"Rain decal frame {i} differs from source")

dependencies = read(ROOT / "gradle.properties") + read(ROOT / "build.gradle")
check("ranged_weapon_api_version=1.1.4-forge-private.1" in dependencies, "RWA upgraded")
check("spell_engine" not in dependencies.lower() and "spell_power" not in dependencies.lower(),
      "Spell Engine/Spell Power dependency added")
changed = set(git("diff", "--name-only").splitlines())
frozen_paths = [str(p.relative_to(ROOT)).replace("\\", "/") for p in
                [ROOT / name for name in contracts if name != "ARCHERS_RANGER_AR2.md"] +
                [JAVA / f"spells/{name}.java" for name in
                 ("PowerShotSpell", "EntanglingRootsSpell", "BarrageSpell", "EntanglingArrowSpell", "CaltropsSpell")]]
check(not (changed & set(frozen_paths)), "frozen contract or existing Ranger gameplay changed")
check(not git("diff", "--cached", "--name-only").strip(), "AR4 staged before runtime PASS")
status = git("status", "--porcelain", "-uall").splitlines()
private = [line for line in status if line[3:].startswith("src/main/resources/assets/martial_spells/")
           and any(term in line.lower() for term in ("battle_banner", "divine_", "holy_", "lightwell",
                                                       "paladin_source", "penance", "priest_", "barrier"))]
check(private and all(line.startswith("?? ") for line in private),
      "unrelated Paladin/Priest private assets no longer untracked")
check("armory" not in rain.lower() and "armory" not in magic.lower(), "Armory set logic added")

if errors:
    for error in errors:
        print("AR4 AUDIT FAIL:", error, file=sys.stderr)
    sys.exit(1)
print("AR4 static audit PASS: 21 persisted Rain launches, held Magic charge, saved projectiles, exact source assets, Ranger regressions")
