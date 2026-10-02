"""Static AR3 gate. Runtime timing/collision still requires the developer's game test."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/w0of26/martialspells"
ASSETS = ROOT / "src/main/resources/assets/martial_spells"
errors = []


def read(path):
    return path.read_text(encoding="utf-8")


def check(ok, message):
    if not ok:
        errors.append(message)


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, check=True,
                          capture_output=True, text=True).stdout


contract = read(ROOT / "ARCHERS_RANGER_SPELL_PORT.md")
check((ROOT / "ARCHERS_RANGER_AR1.md").exists() and (ROOT / "ARCHERS_RANGER_AR2.md").exists(),
      "frozen AR1/AR2 notes missing")
for token in ("ee8fb4081422ba0ab8ced57fe773a1c048d7bf74",
              "d057c13b434cadc8eeaea6df8deab6992f8bda7e",
              "extra_launch_delay = 2", "critical = true", "0/2/4-tick", "supersedes"):
    check(token in contract, f"corrected frozen contract missing {token}")

registry = read(JAVA / "registry/MartialSpellRegistry.java")
check(len(re.findall(r'SPELLS\.register\("barrage"', registry)) == 1,
      "Barrage must have exactly one existing registry ID")
for absent in ("rain_of_arrows", "spirit_wolf", 'SPELLS.register("magic_arrow"'):
    check(absent not in registry, f"future Ranger spell registered: {absent}")
for existing in ('SPELLS.register("power_shot"', 'SPELLS.register("entangling_roots"',
                 'SPELLS.register("entangling_arrow"', 'SPELLS.register("caltrops"'):
    check(existing in registry, f"existing Ranger registry missing {existing}")

barrage = read(JAVA / "spells/BarrageSpell.java")
for token in ("MAX_LEVEL = 1", "CAST_TIME_TICKS = 10", "COOLDOWN_SECONDS = 10",
              "EXTRA_LAUNCH_COUNT = 2", "EXTRA_LAUNCH_DELAY_TICKS = 2",
              "DAMAGE_MULTIPLIER = 0.75F", "VELOCITY = 3.15F",
              "FOLLOW_UP_DIVERGENCE = 5.0F", "baseManaCost = 0",
              "getSpellCooldown() { return COOLDOWN_SECONDS * 20; }",
              "getMinRarity() { return SpellRarity.RARE.getValue(); }",
              "manaCostPerLevel = 0", "CastType.LONG", "MartialTechniqueClass.RANGER",
              "RangerTickScheduler.schedule", "index * EXTRA_LAUNCH_DELAY_TICKS",
              "launch(server, player, 0)", "launch(scheduledLevel, scheduledPlayer, launchIndex)",
              "scheduledPlayer.isRemoved()", "!scheduledPlayer.isAlive()",
              "scheduledPlayer.level() != scheduledLevel", "findHeldRangedWeapon(player)",
              "ArchersPhysicalArrow.findAmmunition(player, weapon)", "ammunition.shrink(1)",
              "ammunition.is(Items.ARROW)", "Enchantments.INFINITY_ARROWS",
              "ammunition.copyWithCount(1)", "RangerArrowMetadata.markBarrage(arrow)",
              "index == 0 ? 0.0F : FOLLOW_UP_DIVERGENCE", "return false;"):
    check(token in barrage, f"Barrage executor missing {token}")
check(bool(re.search(r"DAMAGE_MULTIPLIER,\s*0,\s*0,\s*true,\s*true", barrage)),
      "Barrage does not request critical arrows and iframe bypass")
check("onServerCastTick" not in barrage and "SHOT_INTERVAL_TICKS" not in barrage
      and "getProjectileCount" not in barrage and "BarrageArrow" not in barrage,
      "old timed/level-scaled Barrage executor remains")
check("NominalPhysicalRangedPower" not in barrage and "MartialPower" not in barrage,
      "Barrage incorrectly uses non-arrow or Martial power")

arrow = read(JAVA / "combat/ArchersPhysicalArrow.java")
for token in ("arrowItem.createArrow(level, ammunition, shooter)",
              "bow.customArrow(arrow)", "arrow.setCritArrow(critical)",
              "arrow.setShotFromCrossbow(true)", "arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT)",
              "Enchantments.POWER_ARROWS", "Enchantments.PUNCH_ARROWS",
              "Enchantments.FLAMING_ARROWS", "Enchantments.PIERCING",
              "innatePierce + extraPierceTargets", "Byte.MAX_VALUE",
              "ScalingUtil.arrowDamageMultiplier", "ratio * techniqueMultiplier",
              "RangerArrowMetadata.markEligibleShot(arrow)",
              "RangerArrowMetadata.setIframeBypass(arrow, bypassIframes)"):
    check(token in arrow, f"physical arrow path missing {token}")
check(arrow.count("arrow.setBaseDamage(") == 2, "unexpected physical-arrow damage scaling site")
check("arrow.shoot(look.x, look.y, look.z, velocity, inaccuracy)" in arrow
      and "arrow.shootFromRotation(" not in arrow.split("public static void launch", 1)[1],
      "source look-vector launch was replaced with shooter-motion inheritance")
check("MartialPower" not in arrow and "NominalPhysicalRangedPower" not in arrow
      and "ARROW_DAMAGE" not in arrow and "ARROW_VELOCITY" not in arrow,
      "Archers arrow path applies forbidden or duplicate scaling")

metadata = read(JAVA / "combat/RangerArrowMetadata.java")
impact = read(JAVA / "combat/PowerShotImpact.java")
knockback = read(JAVA / "combat/BarrageKnockbackScope.java")
join = read(JAVA / "events/PowerShotArrowEvents.java")
for token in ('state.putBoolean("barrage", true)', 'state(arrow).getBoolean("barrage")',
              'state.putBoolean("iframeBypass", enabled)',
              'state.putInt("extraPierceTargets", extraTargets)'):
    check(token in metadata, f"persistent Ranger arrow state missing {token}")
check("previousIframes = living.invulnerableTime" in impact
      and "living.invulnerableTime = 0" in impact
      and "living.invulnerableTime = previousIframes" in impact
      and "finally" in impact and "BarrageKnockbackScope.exit" in impact,
      "Barrage iframe/knockback scope is not restored")
check("transferred && RangerArrowMetadata.markTransferredHit" in impact
      and "markShotEvaluated(arrow)" in join
      and "stampTransfer(arrow, PowerShotSpell.SPELL_ID" in join,
      "Power Shot one-transfer-per-spawn guard changed")
check("LivingKnockBackEvent" in knockback and "ACTIVE_TARGET.get()" in knockback
      and "event.getStrength() * 0.5F" in knockback,
      "source fractional damage-time knockback is not scoped")
check("target.invulnerableTime = 0" not in "".join(
      read(path) for path in (JAVA / "events/PowerShotArrowEvents.java",)),
      "pre-impact permanent iframe reset remains")

cooldown = read(JAVA / "events/RangedTechniqueCooldownEvents.java")
check("BARRAGE" not in cooldown and "ENTANGLING_ARROW" in cooldown,
      "old Barrage fixed cooldown remains or Entangling Arrow floor changed")
for path in (JAVA / "entity/BarrageArrow.java", JAVA / "events/BarrageProjectileEvents.java",
             JAVA / "client/render/BarrageArrowRenderer.java",
             JAVA / "client/particle/BarrageTrailParticle.java",
             ASSETS / "particles/barrage_trail.json"):
    check(not path.exists(), f"obsolete custom Barrage presentation still active: {path.name}")
check("BARRAGE_ARROW" not in read(JAVA / "registry/MartialEntityRegistry.java")
      and "BARRAGE_TRAIL" not in read(JAVA / "registry/MartialParticleRegistry.java")
      and "BARRAGE_ARROW" not in read(JAVA / "client/MartialClientEvents.java"),
      "dead custom Barrage entity/trail remains registered")

mixins = read(ROOT / "src/main/resources/martial_spells.mixins.json")
level = read(JAVA / "mixin/BarrageSpellDataLevelMixin.java")
check("BarrageSpellDataLevelMixin" in mixins and "spellLevel > 1" in level
      and "BarrageSpell.SPELL_ID.equals" in level and "spellLevel = 1" in level,
      "old level-2–5 Barrage bindings are not normalized")
check("BOW_PULL" in read(JAVA / "registry/MartialSoundRegistry.java")
      and "bow_pull" in json.loads(read(ASSETS / "sounds.json"))
      and (ASSETS / "sounds/bow_pull.ogg").exists(),
      "Barrage bow-pull sound missing")
check((ASSETS / "textures/gui/spell_icons/barrage.png").exists(), "Barrage icon missing")
upstream = Path(r"C:\Modding\RPG-Ports\upstream\Archers\common\src\main\resources\assets\archers")
for source, target in ((upstream / "sounds/bow_pull.ogg", ASSETS / "sounds/bow_pull.ogg"),
                       (upstream / "textures/spell/barrage.png",
                        ASSETS / "textures/gui/spell_icons/barrage.png")):
    if source.exists() and target.exists():
        check(hashlib.sha256(source.read_bytes()).digest() == hashlib.sha256(target.read_bytes()).digest(),
              f"source asset differs: {target.name}")

changed_paths = set(git("diff", "--name-only", "HEAD").splitlines())
check("build.gradle" not in changed_paths and "gradle.properties" not in changed_paths,
      "dependency architecture or RWA version changed")
for relative in ("spells/PowerShotSpell.java", "spells/EntanglingRootsSpell.java",
                 "spells/EntanglingArrowSpell.java", "spells/CaltropsSpell.java",
                 "entity/EntanglingRootsCloud.java", "entity/EntanglingArrow.java",
                 "entity/CaltropFieldEntity.java", "entity/CaltropBundleProjectile.java",
                 "mixin/LivingEntityHuntersMarkMixin.java", "mixin/AbstractArrowPowerShotMixin.java"):
    path = f"src/main/java/com/w0of26/martialspells/{relative}"
    check(path not in changed_paths, f"accepted ability gameplay changed: {relative}")

dependency_text = read(ROOT / "build.gradle") + read(ROOT / "gradle.properties")
check("ranged_weapon_api_version=1.1.4-forge-private.1" in dependency_text,
      "RWA private version changed")
check(not re.search(r"(?:implementation|compileOnly|runtimeOnly|fg\.deobf)\s*\(?[\"'][^\"']*(?:spell.engine|spell.power)",
                    dependency_text, re.I), "Spell Engine/Spell Power dependency added")
new_text = "\n".join((barrage, arrow, metadata, impact, knockback, level))
check(not re.search(r"(?i)riftstalker|armory|strider[_ ]set", new_text),
      "Armory equipment detection added")
staged = git("diff", "--cached", "--name-only").splitlines()
check(not staged, "AR3 files or unrelated private assets are staged before runtime PASS")
status = git("status", "--porcelain", "-uall").splitlines()
private = [line for line in status if line[3:].startswith("src/main/resources/assets/martial_spells/")
           and any(x in line[3:].lower() for x in
                   ("paladin", "priest", "holy_", "divine_", "penance", "battle_banner",
                    "lightwell", "judgement", "barrier", "immolation", "blessed_strikes"))]
check(private and all(line.startswith("?? ") for line in private),
      "pre-existing Paladin/Priest private assets lost untracked status")

if errors:
    for error in errors:
        print("FAIL:", error)
    sys.exit(1)
print("AR3 static audit PASS: corrected 0/2/4 critical volley, physical-arrow pipeline, legacy-level migration, regressions and private-asset boundary")
