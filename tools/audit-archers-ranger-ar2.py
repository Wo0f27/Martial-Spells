#!/usr/bin/env python3
"""Static source and scope checks for the accepted AR2 checkpoint."""

from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys
from ranger_audit_support import accepted_gameplay_unchanged, private_asset_errors

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/w0of26/martialspells"
ASSETS = ROOT / "src/main/resources/assets/martial_spells"
ARCHERS = Path(r"C:\Modding\RPG-Ports\upstream\Archers")
ENGINE = Path(r"C:\Modding\RPG-Ports\upstream\SpellEngine")
ARCHERS_SHA = "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74"
ENGINE_SHA = "d057c13b434cadc8eeaea6df8deab6992f8bda7e"
AR1_BASE = "31d57ca19613c70d7ee0c89e17de81de43976918"
failures = []


def check(ok, message):
    if not ok:
        failures.append(message)


def read(path):
    check(path.exists(), f"missing {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8") if path.exists() else ""


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, check=True,
                          capture_output=True, text=True, encoding="utf-8").stdout


def frozen_blob(repo, revision, path):
    result = subprocess.run(["git", "-c", f"safe.directory={repo}", "-C", str(repo),
                             "show", f"{revision}:{path}"], capture_output=True)
    check(result.returncode == 0, f"frozen source missing: {repo.name}@{revision}:{path}")
    return result.stdout


check(git("merge-base", AR1_BASE, "HEAD").strip() == AR1_BASE,
      "AR2 does not descend from the accepted AR1 base")
for name in ("ARCHERS_RANGER_SPELL_PORT.md", "ARCHERS_RANGER_AR1.md"):
    # AR3 accepted a separate exact-source Barrage correction after AR2.
    check(read(ROOT / name) == git("show", f"HEAD:{name}"), f"frozen contract changed: {name}")

gradle = read(ROOT / "build.gradle")
props = read(ROOT / "gradle.properties")
check("ranged_weapon_api_version=1.1.4-forge-private.1" in props, "RWA version changed")
check(not re.search(r"(?im)^\s*(?:implementation|compileOnly|runtimeOnly|api)\s+[^\n]*(?:spell[_-]?engine|spell[_-]?power)", gradle),
      "Spell Engine/Spell Power dependency added")

registry = read(JAVA / "registry/MartialSpellRegistry.java")
for name in ("power_shot", "entangling_roots"):
    check(len(re.findall(rf'SPELLS\.register\("{name}"', registry)) == 1,
          f"{name} must be registered once")
for name in ("barrage", "entangling_arrow", "caltrops"):
    check(f'SPELLS.register("{name}"' in registry, f"existing Ranger ID lost: {name}")
check(registry.count('SPELLS.register("spirit_wolf"') == 1,
      "accepted AR5 Spirit Wolf registration lost or duplicated")

tag = json.loads(read(ROOT / "src/main/resources/data/martial_spells/tags/spells/ranger_techniques.json"))
martial_tag = json.loads(read(ROOT / "src/main/resources/data/martial_spells/tags/spells/martial_techniques.json"))
lang = json.loads(read(ASSETS / "lang/en_us.json"))
expected_ranger = ["power_shot", "entangling_roots", "barrage"]
if 'SPELLS.register("rain_of_arrows"' in registry:
    expected_ranger += ["rain_of_arrows", "magic_arrow"]
expected_ranger += ["spirit_wolf"]
expected_ranger += ["entangling_arrow", "caltrops"]
check(tag.get("values") == [f"martial_spells:{name}" for name in expected_ranger],
      "Ranger catalogue lost or reordered accepted AR2 abilities")
for ability in ("power_shot", "entangling_roots"):
    check(f"martial_spells:{ability}" in martial_tag.get("values", []),
          f"{ability} missing general Martial catalogue")
    check(all(f"spell.martial_spells.{ability}{suffix}" in lang
              for suffix in ("", ".description", ".guide")),
          f"{ability} language entry missing")

for name, cooldown in (("PowerShotSpell", 8), ("EntanglingRootsSpell", 18)):
    src = read(JAVA / f"spells/{name}.java")
    for token in ("implements MartialTechnique", "MartialTechniqueClass.RANGER", "MartialSchoolRegistry.MARTIAL_RESOURCE",
                  "MAX_LEVEL = 1", "baseManaCost = 0", "manaCostPerLevel = 0", "CastType.INSTANT",
                  f"COOLDOWN_SECONDS = {cooldown}"):
        check(token in src, f"{name} missing {token}")

shot = read(JAVA / "spells/PowerShotSpell.java")
join = read(JAVA / "events/PowerShotArrowEvents.java")
metadata = read(JAVA / "combat/RangerArrowMetadata.java")
impact = read(JAVA / "combat/PowerShotImpact.java")
damage = read(JAVA / "mixin/LivingEntityHuntersMarkMixin.java")
arrow_mixin = read(JAVA / "mixin/AbstractArrowPowerShotMixin.java")
bow_origin = read(JAVA / "mixin/BowPowerShotOriginMixin.java")
crossbow_origin = read(JAVA / "mixin/CrossbowPowerShotOriginMixin.java")
for token in ("STASH_TICKS = 240", "INITIAL_AMPLIFIER = 2"):
    check(token in shot, f"Power Shot stash missing {token}")
for token in ("EntityJoinLevelEvent", "instanceof AbstractArrow", "instanceof ServerPlayer",
              "markShotEvaluated", "stampTransfer", "amplifier - 1", "remaining"):
    check(token in join, f"shot trigger missing {token}")
for token in ("getPersistentData()", "transferredHits", "markTransferredHit", "shotEvaluated"):
    check(token in metadata, f"projectile transfer persistence missing {token}")
for token in ("MAX_AMPLIFIER = 2", "MARK_TICKS = 240", "Math.min(MAX_AMPLIFIER",
              "markTransferredHit", "previousIframes", "finally", "target.hurt(source, amount)"):
    check(token in impact, f"mark/iframe handling missing {token}")
check("RangerArrowMetadata.hasTransfer" in impact and "RangerArrowMetadata.bypassesIframes" in impact,
      "iframe bypass is not tagged-arrow scoped")
check('method = "hurt"' in damage and 'at = @At("HEAD")' in damage
      and "HUNTERS_MARK" in damage and "amount * (1.0F + 0.1F" in damage,
      "all-source damage entry hook missing")
check('method = "onHitEntity"' in arrow_mixin and "Entity;hurt" in arrow_mixin,
      "arrow hit bridge missing")
check('method = "releaseUsing"' in bow_origin and "customArrow" in bow_origin
      and "markEligibleShot" in bow_origin, "vanilla bow provenance missing")
check('method = "getArrow"' in crossbow_origin and "createArrow" in crossbow_origin
      and "markEligibleShot" in crossbow_origin, "vanilla crossbow provenance missing")
check("RangedTechniqueHelper.findHeldRangedWeapon" not in join,
      "shot eligibility uses a held-weapon timing guess")
check("instanceof BarrageArrow" not in join and "instanceof EntanglingArrow" in join,
      "current Barrage iframe path was coupled to Power Shot before AR3")

cloud = read(JAVA / "entity/EntanglingRootsCloud.java")
roots = read(JAVA / "effects/EntanglingRootsEffect.java")
jump = read(JAVA / "events/EntanglingRootsJumpEvents.java")
for token in ("LIFETIME_TICKS = 160", "PULSE_INTERVAL_TICKS = 15", "LAST_PULSE_AGE = 150",
              "EFFECT_TICKS = 20", "RADIUS = 3.5D", "RADIUS * 0.3D + 0.5D",
              "ageTicks++", "ageTicks >= LIFETIME_TICKS", "ageTicks % PULSE_INTERVAL_TICKS == 0",
              "PhysicalMeleePower.controlHealthLimit", "target.getMaxHealth() > healthLimit",
              "ClipContext.Block.COLLIDER", 'tag.putUUID("Owner"', 'tag.putInt("AgeTicks"',
              'tag.getInt("AgeTicks"'):
    check(token in cloud, f"Roots cloud missing {token}")
check("NominalPhysicalRangedPower" not in cloud, "Roots uses ranged power")
check([age for age in range(1, 160) if age >= 15 and age <= 150 and age % 15 == 0]
      == [15, 30, 45, 60, 75, 90, 105, 120, 135, 150],
      "Roots pulse schedule is not exactly ten attempts")
check("-0.5D, AttributeModifier.Operation.MULTIPLY_BASE" in roots
      and "Attributes.MOVEMENT_SPEED" in roots and "Attributes.JUMP_STRENGTH" in roots,
      "Roots authored movement/jump attributes missing")
check("ENTANGLING_ROOTS" in jump and "LivingJumpEvent" in jump
      and "Attributes.JUMP_STRENGTH" in jump and "HALF_BASE_JUMP" in jump,
      "Roots-only jump fallback missing")

unchanged = [
    "spells/BarrageSpell.java", "spells/EntanglingArrowSpell.java", "spells/CaltropsSpell.java",
    "entity/EntanglingArrow.java", "entity/CaltropFieldEntity.java",
    "entity/CaltropBundleProjectile.java",
    "events/RangedTechniqueCooldownEvents.java", "combat/RangedTechniqueHelper.java",
]
for relative in unchanged:
    path = f"src/main/java/com/w0of26/martialspells/{relative}"
    check(accepted_gameplay_unchanged(relative), f"existing Ranger gameplay changed: {relative}")

ar2_java = "\n".join(read(path) for path in (JAVA / "events/PowerShotArrowEvents.java",
    JAVA / "combat/PowerShotImpact.java", JAVA / "mixin/LivingEntityHuntersMarkMixin.java",
    JAVA / "mixin/AbstractArrowPowerShotMixin.java", JAVA / "events/EntanglingRootsJumpEvents.java",
    JAVA / "entity/EntanglingRootsCloud.java", JAVA / "mixin/BowPowerShotOriginMixin.java",
    JAVA / "mixin/CrossbowPowerShotOriginMixin.java"))
check(not re.search(r"(?i)riftstalker|armory|strider[_ ]set", ar2_java), "Armory gear check in AR2")
check("markShotEvaluated" in join and "hasTransfer" in impact, "broad arrow handling")
check("HUNTERS_MARK" in damage and "ENTANGLING_ROOTS" in jump,
      "broad damage or jump hook")

# Pure gameplay files are byte-identical to the developer-confirmed AR2 mechanics.
mechanics_sha256 = {
    "effects/EntanglingRootsEffect.java": "7a4af2fba79067ceb6abbba517ef52d69131feb20ab9400686f6ef1fc5f5f977",
    "effects/HuntersMarkEffect.java": "df0f5d75fb498832fcfaac15939ba6dfc255c2fa6e2c8836b99ec371322d58e2",
    "effects/PowerShotStashEffect.java": "6ab07590a30b64595fb193af12e5bde2c04b036ede2f669d6c665a78a544699d",
    "events/EntanglingRootsJumpEvents.java": "37e1dfe772fb079ce9d3abce80caf09fcd8977e609b96f75fd7a9e239b0dd8cd",
    "mixin/AbstractArrowPowerShotMixin.java": "2495f62dc98ad835e35d062f578165b0fc73e38fc1de0f038dafef084a5163fd",
    "mixin/BowPowerShotOriginMixin.java": "6767106004c9273fe80c3ae9f0ae99604086423d44429cc90365e32abc9b89fa",
    "mixin/CrossbowPowerShotOriginMixin.java": "680f66ea0106d8eb18b6a0e9aef2c07d53277da54f55da982a3214f48ca7d6ce",
    "mixin/LivingEntityHuntersMarkMixin.java": "3dc8b1b1a25d14b019f6186c58a3b94a0ff179b9ac12d8be5d389f6851e0268c",
}
for relative in ("events/PowerShotArrowEvents.java", "combat/RangerArrowMetadata.java"):
    path = f"src/main/java/com/w0of26/martialspells/{relative}"
    check(read(ROOT / path) == git("show", f"HEAD:{path}"),
          f"accepted post-AR3 Power Shot integration changed: {relative}")
for relative, expected in mechanics_sha256.items():
    path = JAVA / relative
    check(path.exists() and hashlib.sha256(path.read_bytes()).hexdigest() == expected,
          f"accepted AR2 gameplay changed: {relative}")

for folder, name in (("gui/spell_icons", "power_shot"), ("gui/spell_icons", "entangling_roots"),
                     ("mob_effect", "power_shot_stash"), ("mob_effect", "hunters_mark"),
                     ("mob_effect", "entangling_roots")):
    path = ASSETS / "textures" / folder / (name + ".png")
    check(path.is_file() and path.read_bytes().startswith(b"\x89PNG"), f"local icon missing: {name}")

archers_base = "common/src/main/resources/assets/archers"
exact_assets = {
    "textures/spell/power_shot.png": "textures/gui/spell_icons/power_shot.png",
    "textures/spell/entangling_roots.png": "textures/gui/spell_icons/entangling_roots.png",
    "textures/mob_effect/hunters_mark_stash.png": "textures/mob_effect/power_shot_stash.png",
    "textures/mob_effect/hunters_mark.png": "textures/mob_effect/hunters_mark.png",
    "textures/mob_effect/entangling_roots.png": "textures/mob_effect/entangling_roots.png",
    "textures/spell_effect/hunters_mark.png": "textures/spell_effect/hunters_mark.png",
    "textures/spell_effect/entangling_roots.png": "textures/spell_effect/entangling_roots.png",
    "sounds/marker_shot.ogg": "sounds/marker_shot.ogg",
    "sounds/entangling_roots.ogg": "sounds/entangling_roots.ogg",
}
for source, target in exact_assets.items():
    dest = ASSETS / target
    check(dest.is_file() and dest.read_bytes() == frozen_blob(
          ARCHERS, ARCHERS_SHA, f"{archers_base}/{source}"),
          f"AR2 private asset differs from frozen Archers: {target}")
for name in ("entangling_roots", "hunters_mark"):
    source = json.loads(frozen_blob(ARCHERS, ARCHERS_SHA,
                        f"{archers_base}/models/spell_effect/{name}.json"))
    source["textures"] = {k: v.replace("archers:", "martial_spells:")
                          for k, v in source["textures"].items()}
    check(json.loads(read(ASSETS / f"models/spell_effect/{name}.json")) == source,
          f"{name} model differs beyond namespace translation")
for source_stem, count in (("magic/vertical_stripe", 8), ("misc/roots", 14)):
    for i in range(count):
        source = f"{source_stem}_{i}.png"
        dest = ASSETS / f"textures/particle/ranger_source/{source}"
        check(dest.is_file() and dest.read_bytes() == frozen_blob(ENGINE, ENGINE_SHA,
              f"common/src/main/resources/assets/spell_engine/textures/particle/{source}"),
              f"Ranger particle sprite differs from exact Spell Engine: {source}")

client = read(JAVA / "client/MartialClientEvents.java")
roots_renderer = read(JAVA / "client/render/EntanglingRootsEffectRenderer.java")
mark_renderer = read(JAVA / "client/render/HuntersMarkEffectRenderer.java")
mark_visual = read(JAVA / "mixin/LivingEntityHuntersMarkVisualMixin.java")
mark_visual_interface = read(JAVA / "effects/HuntersMarkVisualState.java")
particle = read(JAVA / "client/particle/RangerSourceParticle.java")
vfx = read(JAVA / "client/visual/RangerSourceVfx.java")
sound_registry = read(JAVA / "registry/MartialSoundRegistry.java")
sound_defs = json.loads(read(ASSETS / "sounds.json"))
for name, renderer, effect in (("entangling_roots", roots_renderer, "ENTANGLING_ROOTS"),
                               ("hunters_mark", mark_renderer, "HUNTERS_MARK")):
    class_name = "EntanglingRootsEffectRenderer" if name == "entangling_roots" else "HuntersMarkEffectRenderer"
    check(f"event.register({class_name}.MODEL)" in client
          and f"{class_name}::onRenderLivingPost" in client
          and f"MartialEffectRegistry.{effect}.get()" in (renderer if name == "entangling_roots" else mark_visual),
          f"{name} effect renderer not registered and effect-tied")
check("RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)" in mark_renderer
      and "LightTexture.FULL_BRIGHT" in mark_renderer
      and "model == client.getModelManager().getMissingModel()" in mark_renderer
      and "martialSpells$hasHuntersMarkVisual()" in mark_renderer,
      "Hunter's Mark renderer is not effect-gated")
check("LivingEntityHuntersMarkVisualMixin" in read(ROOT / "src/main/resources/martial_spells.mixins.json")
      and "SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN)" in mark_visual
      and "getEntityData().define(MARTIAL_SPELLS_HUNTERS_MARK_VISUAL, false)" in mark_visual
      and "getEntityData().set(MARTIAL_SPELLS_HUNTERS_MARK_VISUAL" in mark_visual
      and "getEntityData().get(MARTIAL_SPELLS_HUNTERS_MARK_VISUAL)" in mark_visual
      and "boolean martialSpells$hasHuntersMarkVisual()" in mark_visual_interface,
      "Hunter's Mark observer visual state is not tracked")
for hook in ("onEffectAdded", "onEffectUpdated", "onEffectRemoved", "readAdditionalSaveData"):
    check(f'method = "{hook}"' in mark_visual,
          f"Hunter's Mark visual state misses lifecycle hook: {hook}")
check("DEBUG_GEOMETRY_PROBES" not in mark_renderer and "drawDebugQuad" not in mark_renderer,
      "Hunter's Mark temporary render probes remain in parity candidate")
for token in ("RANGER_MAGIC_STRIPE", "RANGER_ROOTS", "Kind.MAGIC_STRIPE", "Kind.ROOTS"):
    check(token in client + read(JAVA / "registry/MartialParticleRegistry.java"),
          f"Ranger source particle registration missing {token}")
for name, frames in (("ranger_magic_stripe", 8), ("ranger_roots", 14)):
    data = json.loads(read(ASSETS / f"particles/{name}.json"))
    check(len(data.get("textures", [])) == frames, f"{name} sprite frame count changed")
check("RangerSourceVfx.powerShotRelease(server, caster)" in shot and "for (int i = 0; i < 10; i++)" in vfx,
      "Power Shot source release VFX missing")
check("(16.0F / 0.5F)" in particle,
      "Power Shot stripe lifetime omits the source playback-speed divisor")
check("ParticleTypes.FIREWORK" in vfx,
      "Power Shot impact must use the source's vanilla firework particle")
check("RangerSourceVfx.huntersMarkImpact(server, living)" in impact
      and "for (int i = 0; i < 3; i++)" in vfx, "Hunter's Mark impact VFX missing")
check("MARK_TICKS, next, false, false, true" in impact,
      "Hunter's Mark gained generic status-effect particles absent upstream")
check("RangerSourceVfx.rootsCloudTick(server" in cloud
      and "RangerSourceVfx.rootsImpact(server, target)" in cloud
      and "for (int i = 0; i < 2; i++)" in vfx,
      "Roots cloud or impact VFX missing")
check("quadSize = 0.25F" in particle and "gravity = 0.225F" in particle,
      "Roots source particle motion missing")
for name in ("marker_shot", "entangling_roots"):
    check(name in sound_defs and sound_defs[name]["sounds"] == [f"martial_spells:{name}"],
          f"Ranger sound definition missing: {name}")
    check(f'register("{name}")' in sound_registry, f"Ranger sound not registered: {name}")
check("MartialSoundRegistry.MARKER_SHOT.get()" in shot
      and "MartialSoundRegistry.ENTANGLING_ROOTS.get()" in cloud
      and "MartialSoundRegistry.ENTANGLING_ROOTS.get()" not in read(JAVA / "spells/EntanglingRootsSpell.java"),
      "Ranger source sounds not used")

for error in private_asset_errors():
    check(False, error)

if failures:
    print("\n".join("FAIL: " + item for item in failures))
    sys.exit(1)
print("PASS: Archers Ranger AR2 static source/scope audit")
print("Accepted tracked Paladin/Priest private assets unchanged: 106")
