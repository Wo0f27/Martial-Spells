from pathlib import Path
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

p3 = subprocess.run(
    [sys.executable, str(root / "tools" / "audit-paladin-p3.py")],
    cwd=root,
    text=True,
    capture_output=True,
)
output = (p3.stdout + p3.stderr).strip()
if output:
    print(output)
if p3.returncode != 0:
    errors.append("P3 Priest channel/control audit failed")

spell_dir = root / "src/main/java/com/w0of26/martialspells/spells"
entity_dir = root / "src/main/java/com/w0of26/martialspells/entity"
effect_dir = root / "src/main/java/com/w0of26/martialspells/effects"
event_dir = root / "src/main/java/com/w0of26/martialspells/events"
client_render_dir = root / "src/main/java/com/w0of26/martialspells/client/render"
client_model_dir = root / "src/main/java/com/w0of26/martialspells/client/model"

registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java").read_text(encoding="utf-8")
entity_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialEntityRegistry.java").read_text(encoding="utf-8")
effect_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java").read_text(encoding="utf-8")
sound_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java").read_text(encoding="utf-8")
client_events = (root / "src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java").read_text(encoding="utf-8")
lang = json.loads((root / "src/main/resources/assets/martial_spells/lang/en_us.json").read_text(encoding="utf-8"))
sounds = json.loads((root / "src/main/resources/assets/martial_spells/sounds.json").read_text(encoding="utf-8"))

spell_contract = {
    "PaladinBarrierSpell.java": (
        '"barrier"',
        "SpellRarity.LEGENDARY",
        "SchoolRegistry.HOLY_RESOURCE",
        "RANGE = 4",
        "CAST_TIME_TICKS = 10",
        "BASE_COOLDOWN_SECONDS = 40",
        "MartialEntityRegistry",
        ".PALADIN_BARRIER",
        "PaladinVfx.barrierSpawn(",
        "HOLY_BARRIER_ACTIVATE",
    ),
    "PaladinBattleBannerSpell.java": (
        '"battle_banner"',
        "SpellRarity.LEGENDARY",
        "SchoolRegistry.HOLY_RESOURCE",
        "BASE_RADIUS = 3.0F",
        "EXTRA_RADIUS_POWER_COEFFICIENT = 1.0F",
        "EXTRA_RADIUS_POWER_CAP = 4.0F",
        "Math.min(",
        "getSourceEquivalentPower(",
        "BASE_COOLDOWN_SECONDS = 45",
        "aheadOnGround(",
        "2.0D",
        "caster.getYRot() + 20.0F",
        ".BATTLE_BANNER",
    ),
    "PaladinLightwellSpell.java": (
        '"lightwell"',
        "SpellRarity.LEGENDARY",
        "SchoolRegistry.HOLY_RESOURCE",
        "BASE_COOLDOWN_SECONDS = 45",
        "WELL_BASE_HEALING_POWER = 1.0F",
        "OWNER_POWER_COEFFICIENT = 0.50F",
        "getSourceEquivalentPower(",
        "aheadOnGround(",
        "1.5D",
        "lightwell.setYRot(",
        ".LIGHTWELL",
    ),
    "PaladinLightwellOrbSpell.java": (
        '"lightwell_orb"',
        "SpellRarity.COMMON",
        "SchoolRegistry.HOLY_RESOURCE",
        ".setCooldownSeconds(1.5F)",
        "baseManaCost = 0",
        "Internal helper only",
    ),
}
for filename, tokens in spell_contract.items():
    path = spell_dir / filename
    if not path.is_file():
        errors.append(f"missing P4 spell class: {filename}")
        continue
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{filename}: missing contract token {token!r}")

for spell_id, class_name in (
    ("barrier", "PaladinBarrierSpell::new"),
    ("battle_banner", "PaladinBattleBannerSpell::new"),
    ("lightwell", "PaladinLightwellSpell::new"),
    ("lightwell_orb", "PaladinLightwellOrbSpell::new"),
):
    if f'SPELLS.register("{spell_id}", {class_name})' not in registry:
        errors.append(f"spell registry missing {spell_id}")

barrier = (entity_dir / "PaladinBarrierEntity.java").read_text(encoding="utf-8")
for token in (
    "LIFE_TICKS = 10 * 20",
    "CHECK_INTERVAL_TICKS = 4",
    "PROTECTION_TICKS = CHECK_INTERVAL_TICKS + 1",
    "KNOCKBACK_STRENGTH = 0.30F",
    "EXPIRATION_TICKS = 20",
    "BARRIER_PROTECTED",
    "Utils.shouldHealEntity",
    "living.knockback(",
    "blockHostileProjectiles(",
    "canBeCollidedWith()",
    "HOLY_BARRIER_IMPACT",
):
    if token not in barrier:
        errors.append(f"PaladinBarrierEntity missing {token}")

banner = (entity_dir / "BattleBannerEntity.java").read_text(encoding="utf-8")
for token in (
    "SPAWN_TICKS = 43",
    "ACTIVE_TICKS = 10 * 20",
    "DESPAWN_TICKS = 43",
    "IMPACT_INTERVAL_TICKS = 10",
    "EFFECT_DURATION_TICKS = 2 * 20",
    "radius * 0.30D",
    "BATTLE_BANNER.get()",
    "PaladinVfx.bannerPresence(",
    "BATTLE_BANNER_PRESENCE",
):
    if token not in banner:
        errors.append(f"BattleBannerEntity missing {token}")

banner_effect = (effect_dir / "BattleBannerEffect.java").read_text(encoding="utf-8")
for token in (
    "SOURCE_MULTIPLIER = 0.40D",
    "Attributes.ATTACK_SPEED",
    "Attributes.KNOCKBACK_RESISTANCE",
    "AttributeRegistry.CAST_TIME_REDUCTION.get()",
    "AttributeRegistry.COOLDOWN_REDUCTION.get()",
    "ALObjects.Attributes.DRAW_SPEED.get()",
    "AttributeModifier.Operation.MULTIPLY_BASE",
):
    if token not in banner_effect:
        errors.append(f"BattleBannerEffect missing {token}")

lightwell = (entity_dir / "LightwellEntity.java").read_text(encoding="utf-8")
for token in (
    "SPAWN_TICKS = 20",
    "ACTIVE_TICKS = 12 * 20",
    "DESPAWN_TICKS = 20",
    "HEAL_RANGE = 12.0D",
    "BASE_MOTE_COOLDOWN_TICKS = 30",
    "target.getHealth()",
    "target.getMaxHealth()",
    "Utils.shouldHealEntity(",
    "Comparator.comparingDouble(",
    "healingPower * 0.35F",
    "AttributeRegistry",
    ".COOLDOWN_REDUCTION",
    "Utils.softCapFormula(",
    "SoundRegistry.HOLY_CAST.get()",
    "PaladinVfx.lightwellAura(",
):
    if token not in lightwell:
        errors.append(f"LightwellEntity missing {token}")

# Existence particles must be emitted only after the ACTIVE-phase guard.
active_guard = lightwell.find("if (!isActive())")
aura_call = lightwell.find("PaladinVfx.lightwellAura(")
if active_guard < 0 or aura_call < active_guard:
    errors.append("Lightwell existence VFX must be restricted to ACTIVE phase")

mote = (entity_dir / "HolyMoteProjectile.java").read_text(encoding="utf-8")
mote_compact = "".join(mote.split())
for token in (
    "VELOCITY=1.0F",
    "HOMING_DEGREES_PER_TICK=16.0F",
    "RANGE=12.0D",
    "HOMING_START_RELATIVE_DISTANCE=0.15D",
    "bouncesRemaining=1",
    "setNoGravity(true)",
    "distanceTraveled>=RANGE",
    "desired.length()*HOMING_START_RELATIVE_DISTANCE",
    "PaladinVfx.holyMoteTrail(",
    "newSpellHealEvent(",
    "target.heal(",
):
    if token not in mote_compact:
        errors.append(f"HolyMoteProjectile missing {token}")

p4_events = (event_dir / "PaladinP4Events.java").read_text(encoding="utf-8")
for token in (
    "LivingAttackEvent",
    "BARRIER_PROTECTED",
    "DamageTypes.PLAYER_ATTACK",
    "DamageTypeTags.IS_EXPLOSION",
    "DamageTypes.MAGIC",
    "DamageTypes.INDIRECT_MAGIC",
    '"irons_spellbooks", "holy_magic"',
    "ProjectileImpactEvent",
    "PaladinBarrierEntity",
    "event.setCanceled(true)",
    "SpellCooldownAddedEvent.Pre",
    "LIGHTWELL_FIXED_COOLDOWN_TICKS",
):
    if token not in p4_events:
        errors.append(f"PaladinP4Events missing {token}")

support = (spell_dir / "PaladinHolySpellSupport.java").read_text(encoding="utf-8")
for token in (
    "getSourceEquivalentPower(",
    "/ SOURCE_POWER_REFERENCE",
):
    if token not in support:
        errors.append(f"Paladin Holy source-power translation missing {token}")

# P4 effects.
for effect_id, class_name in (
    ("barrier_protected", "BarrierProtectedEffect::new"),
    ("battle_banner", "BattleBannerEffect::new"),
):
    if f'"{effect_id}"' not in effect_registry or class_name not in effect_registry:
        errors.append(f"effect registry missing {effect_id}")

# P4 entities and target dimensions/tracking.
entity_contract = {
    "paladin_barrier": (
        "PaladinBarrierEntity::new",
        ".sized(8.0F, 4.0F)",
        ".clientTrackingRange(128)",
    ),
    "battle_banner": (
        "BattleBannerEntity::new",
        ".sized(6.0F, 0.5F)",
        ".clientTrackingRange(128)",
    ),
    "lightwell": (
        "LightwellEntity::new",
        ".sized(0.9F, 1.4F)",
        ".clientTrackingRange(64)",
        ".updateInterval(3)",
    ),
    "holy_mote": (
        "HolyMoteProjectile::new",
        ".sized(0.25F, 0.25F)",
        ".clientTrackingRange(128)",
        ".updateInterval(2)",
    ),
}
for entity_id, tokens in entity_contract.items():
    if f'ENTITY_TYPES.register("{entity_id}"' not in entity_registry:
        errors.append(f"entity registry missing {entity_id}")
        continue
    for token in tokens:
        if token not in entity_registry:
            errors.append(f"{entity_id} entity registration missing {token}")

# Client presentation.
client_contract = (
    "PaladinBarrierRenderer::new",
    "BattleBannerRenderer::new",
    "LightwellRenderer::new",
    "HolyMoteRenderer::new",
    "BattleBannerModel.LAYER",
    "BattleBannerModel::createBodyLayer",
    "LightwellModel.LAYER",
    "LightwellModel::createBodyLayer",
)
for token in client_contract:
    if token not in client_events:
        errors.append(f"P4 client registration missing {token}")

barrier_renderer = (client_render_dir / "PaladinBarrierRenderer.java").read_text(encoding="utf-8")
for token in (
    "RANGE = 4.0F",
    "RADIUS = RANGE * 0.8F",
    "Z_SLANT",
    "LIGHT_UP_ORDER",
    "entity.isExpiring()",
    "item/barrier",
    "renderConnector(",
    "currentPose",
    "previousPose",
):
    if token not in barrier_renderer:
        errors.append(f"Barrier source renderer missing {token}")
if barrier_renderer.count("renderConnector(") < 2:
    errors.append("Barrier renderer must define and call the source seam-connector wedge; panel-only dome has visible gaps")

banner_animation_path = client_model_dir.parent / "animation" / "BattleBannerAnimations.java"
banner_animations = banner_animation_path.read_text(encoding="utf-8")
for token in (
    "AnimationDefinition idle",
    "withLength(2.5F).looping()",
    "AnimationDefinition place",
    "withLength(2.15F)",
    'addAnimation("battle_flag"',
    "scaleVec(0.0F, 0.0F, 0.0F)",
    "posVec(0.0F, 16.0F, 0.0F)",
):
    if token not in banner_animations:
        errors.append(f"Battle Banner frozen keyframe data missing {token}")

banner_model = (client_model_dir / "BattleBannerModel.java").read_text(encoding="utf-8")
for token in (
    "extends HierarchicalModel<BattleBannerEntity>",
    '"battle_flag"',
    '"flag_part"',
    '"flag_part_2"',
    '"flag_part_3"',
    '"flag_part_4"',
    "entity.spawnAnimationState",
    "BattleBannerAnimations.place",
    "entity.idleAnimationState",
    "BattleBannerAnimations.idle",
    "entity.despawnAnimationState",
    "-1.0F",
):
    if token not in banner_model:
        errors.append(f"Battle Banner exact model/animation integration missing {token}")

banner_renderer = (client_render_dir / "BattleBannerRenderer.java").read_text(encoding="utf-8")
for token in (
    '"textures/entity/battle_banner.png"',
    "LightTexture.FULL_BRIGHT",
):
    if token not in banner_renderer:
        errors.append(f"Battle Banner renderer missing {token}")
if "lifecycleScale(" in banner_renderer:
    errors.append("Battle Banner must not use the old whole-entity scale approximation; source uses place keyframes")

lightwell_animation_path = client_model_dir.parent / "animation" / "LightwellAnimations.java"
lightwell_animations = lightwell_animation_path.read_text(encoding="utf-8")
for token in (
    "AnimationDefinition idle",
    "withLength(1.5F).looping()",
    "AnimationDefinition spawn",
    "withLength(1.0F)",
    "AnimationDefinition spell_release",
    "scaleVec(0.0F, 0.0F, 0.0F)",
    "degreeVec(0.0F, -362.5F, 0.0F)",
):
    if token not in lightwell_animations:
        errors.append(f"Lightwell frozen keyframe data missing {token}")

lightwell_model = (client_model_dir / "LightwellModel.java").read_text(encoding="utf-8")
for token in (
    "extends HierarchicalModel<LightwellEntity>",
    '"root"',
    '"light"',
    "entity.spawnAnimationState",
    "LightwellAnimations.spawn",
    "entity.despawnAnimationState",
    "-1.0F",
    "entity.idleAnimationState",
    "LightwellAnimations.idle",
    "entity.spellReleaseAnimationState",
    "LightwellAnimations.spell_release",
    "20.0F / LightwellEntity.SPELL_RELEASE_ANIMATION_TICKS",
):
    if token not in lightwell_model:
        errors.append(f"Lightwell exact model/animation integration missing {token}")

lightwell_entity = (entity_dir / "LightwellEntity.java").read_text(encoding="utf-8")
for token in (
    "SPELL_RELEASE_ANIMATION_TICKS = 15",
    "SPELL_RELEASE_EVENT = 61",
    "spawnAnimationState",
    "despawnAnimationState",
    "idleAnimationState",
    "spellReleaseAnimationState",
    "broadcastEntityEvent(",
    "setupAnimationStates()",
    "despawnAnimationState.start(",
    "TOTAL_TICKS",
):
    if token not in lightwell_entity:
        errors.append(f"Lightwell animation-state lifecycle missing {token}")

lightwell_renderer = (client_render_dir / "LightwellRenderer.java").read_text(encoding="utf-8")
for token in (
    '"textures/entity/lightwell_base.png"',
    '"textures/entity/lightwell_glow.png"',
    "RenderType.entityTranslucentEmissive(",
    "LightTexture.FULL_BRIGHT",
):
    if token not in lightwell_renderer:
        errors.append(f"Lightwell renderer missing {token}")
if "lifecycleScale(" in lightwell_renderer:
    errors.append("Lightwell must not use the old whole-entity lifecycle scaling approximation")

mote_renderer = (client_render_dir / "HolyMoteRenderer.java").read_text(encoding="utf-8")
for token in (
    '"spell_projectile/lightwell_orb"',
    "PaladinSpellModelRenderTypes.glow()",
    "directionYaw",
    "directionPitch",
    "age * 2.0F",
    "packedLight",
):
    if token not in mote_renderer:
        errors.append(f"Holy Mote renderer missing {token}")

# Holy Mote must use the source magic_holy five-particle LINE trail rather
# than END_ROD or another generic substitute.
if "PaladinVfx.holyMoteTrail(" not in mote:
    errors.append("Holy Mote projectile missing source Holy trail")
if "ParticleTypes.END_ROD" in mote:
    errors.append("Holy Mote projectile must not use END_ROD trail fallback")

# Source-owned sounds and mappings.
p4_sounds = (
    "holy_barrier_activate",
    "holy_barrier_idle",
    "holy_barrier_impact",
    "holy_barrier_deactivate",
    "battle_banner_release",
    "battle_banner_presence",
    "lightwell_spawn",
    "lightwell_ambient",
    "lightwell_despawn",
)
for sound in p4_sounds:
    if f'register("{sound}")' not in sound_registry:
        errors.append(f"sound registry missing {sound}")
    if sounds.get(sound, {}).get("sounds") != [f"martial_spells:{sound}"]:
        errors.append(f"sounds.json mismatch for {sound}")

# Language surface.
for key in (
    "spell.martial_spells.barrier",
    "spell.martial_spells.battle_banner",
    "spell.martial_spells.lightwell",
    "spell.martial_spells.lightwell_orb",
    "effect.martial_spells.battle_banner",
    "effect.martial_spells.barrier_protected",
    "entity.martial_spells.paladin_barrier",
    "entity.martial_spells.battle_banner",
    "entity.martial_spells.lightwell",
    "entity.martial_spells.holy_mote",
    "ui.martial_spells.lightwell_mote_heal",
):
    if key not in lang:
        errors.append(f"lang missing {key}")

# Source assets.
for spell_id in ("barrier", "battle_banner", "lightwell"):
    path = root / f"src/main/resources/assets/martial_spells/textures/gui/spell_icons/{spell_id}.png"
    if not path.is_file():
        errors.append(f"source spell icon missing; run P4 asset sync: {spell_id}.png")

for rel in (
    "textures/mob_effect/battle_banner.png",
    "textures/item/barrier.png",
    "textures/entity/battle_banner.png",
    "textures/entity/lightwell_base.png",
    "textures/entity/lightwell_glow.png",
    "textures/spell_projectile/lightwell_orb.png",
):
    path = root / "src/main/resources/assets/martial_spells" / rel
    if not path.is_file():
        errors.append(f"source P4 texture missing; run P4 asset sync: {rel}")

for sound in p4_sounds:
    path = root / "src/main/resources/assets/martial_spells/sounds" / f"{sound}.ogg"
    if not path.is_file():
        errors.append(f"source P4 sound missing; run P4 asset sync: {sound}.ogg")

sync_text = (root / "tools/sync-paladin-p4-assets.ps1").read_text(encoding="utf-8")
for token in (
    "sync-paladin-p3-assets.ps1",
    "2807417a1dd9a65204c002ded487da0e6ae467a1",
    "textures/item/barrier.png",
    "textures/entity/battle_banner.png",
    "textures/entity/lightwell_base.png",
    "textures/entity/lightwell_glow.png",
    "textures/spell_projectile/lightwell_orb.png",
):
    if token not in sync_text:
        errors.append(f"P4 asset sync missing {token}")

# Ownership boundary.
build = (root / "build.gradle").read_text(encoding="utf-8").lower()
for forbidden in (
    "spell_engine",
    "spell-engine",
    "spell_power",
    "spell-power",
):
    if forbidden in build:
        errors.append(f"P4 must not add source runtime dependency: {forbidden}")

print("")
print("CP11 P4 Constructs/summons summary")
print(" - Barrier: 4-block range / 10 sec / 4-tick ally protection refresh")
print(" - Barrier: player attack + explosion + magic immunity; hostile intrusion/projectile control")
print(" - Battle Banner: 43 spawn + 200 active + 43 despawn ticks")
print(" - Battle Banner: source-equivalent power-scaled radius; baseline 4 blocks, cap 7")
print(" - Battle Banner: +40% melee/spell/ranged haste + knockback resistance")
print(" - Lightwell: 20 spawn + 240 active + 20 despawn ticks; stationary wounded-ally healer")
print(" - Holy Mote: 12-block cap / 1.0 speed / +30-degree lob / 16 deg/tick homing / one bounce")
print(" - Holy Mote heal: 0.35x translated well healing power")
print(" - Lightwell outer cooldown: fixed 45 sec; internal mote cadence uses Iron's cooldown-reduction translation")
print(" - source Barrier/Banner/Lightwell presentation + P4-owned sounds restored")
print(" - no Spell Engine or Spell Power runtime dependency")

if errors:
    print("CP11 P4 AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("CP11 P4 AUDIT PASSED")
