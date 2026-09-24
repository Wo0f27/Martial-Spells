from pathlib import Path
import json
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
errors = []

p2 = subprocess.run(
    [sys.executable, str(root / "tools" / "audit-paladin-p2.py")],
    cwd=root,
    text=True,
    capture_output=True,
)
output = (p2.stdout + p2.stderr).strip()
if output:
    print(output)
if p2.returncode != 0:
    errors.append("P2 Retribution/protection audit failed")

spell_dir = root / "src/main/java/com/w0of26/martialspells/spells"
effect_dir = root / "src/main/java/com/w0of26/martialspells/effects"
entity_dir = root / "src/main/java/com/w0of26/martialspells/entity"

channel = (spell_dir / "PaladinChannelSupport.java").read_text(encoding="utf-8")
for token in (
    "nextImpactIndex + 0.5F",
    "getEffectiveSpellCooldown",
    "fullEffectiveCooldown * progress",
    "getManaCost(spellLevel) * progress",
    "SpellOnCastEvent",
    "SpellCooldownAddedEvent.Pre",
    "SpellCooldownAddedEvent.Post",
    "ServerConfigs.CREATIVE_MANA_COST",
    "ServerConfigs.CREATIVE_COOLDOWN",
    "SyncManaPacket",
    "settleFullCast",
):
    if token not in channel:
        errors.append(f"PaladinChannelSupport missing {token}")

channel_compact = "".join(channel.split())
if "effectiveCastTicks/(float)channelTicks" not in channel_compact:
    errors.append("PaladinChannelSupport missing effectiveCastTicks / (float) channelTicks")

holy_beam = (spell_dir / "PaladinHolyBeamSpell.java").read_text(encoding="utf-8")
for token in (
    '"holy_beam"',
    "SpellRarity.RARE",
    "SchoolRegistry.HOLY_RESOURCE",
    "RANGE = 32",
    "CAST_TIME_TICKS = 5 * 20",
    "CHANNEL_TICKS = 25",
    "CHANNEL_VALUE_MULTIPLIER = 0.20F",
    "BASE_COOLDOWN_SECONDS = 10",
    "HEAL_COEFFICIENT = 0.40F",
    "DAMAGE_COEFFICIENT = 0.80F",
    "KNOCKBACK_STRENGTH = 0.50D",
    "BASE_MANA_COST = 40",
    "HEAL_COEFFICIENT",
    "* CHANNEL_VALUE_MULTIPLIER",
    "DAMAGE_COEFFICIENT",
    "damageBypassingIframes",
    "targetsOnBeam(",
    "Utils.checkEntityIntersecting",
    "target.getPickRadius()",
    "level.getEntities(",
    "ClipContext.Block.COLLIDER",
    "Comparator.comparingDouble",
    "Utils.shouldHealEntity",
    "HOLY_BEAM_START_CASTING",
    "HOLY_BEAM_CASTING",
    "HOLY_BEAM_RELEASE",
    "HOLY_BEAM_HEAL",
    "HOLY_BEAM_DAMAGE",
):
    if token not in holy_beam:
        errors.append(f"PaladinHolyBeamSpell missing {token}")

if (
    "RaycastBuilder.begin(" in holy_beam
    or "import io.redspace.ironsspellbooks.api.util.RaycastBuilder;" in holy_beam
):
    errors.append("Holy Light must not collapse Spell Engine BEAM into a first-hit RaycastBuilder result")
if "EntityHitResult" in holy_beam:
    errors.append("Holy Light BEAM must not use a single EntityHitResult target")

for token in (
    "HolyBeamVisualEntity",
    "MartialEntityRegistry",
    ".HOLY_BEAM_VISUAL",
    "BEAM_VISUALS",
    "PaladinVfx.holyBeam(",
):
    if token not in holy_beam:
        errors.append(f"Holy Light presentation missing {token}")

levitate = (spell_dir / "PaladinLevitateSpell.java").read_text(encoding="utf-8")
for token in (
    '"levitate"',
    "SpellRarity.RARE",
    "SchoolRegistry.HOLY_RESOURCE",
    "CAST_TIME_TICKS = 30",
    "CHANNEL_TICKS = 4",
    "UPWARD_VELOCITY = 0.15D",
    "LEVITATE_DURATION_TICKS = 5 * 20",
    "BASE_COOLDOWN_SECONDS = 24",
    "BASE_MANA_COST = 25",
    "new Vec3(",
    "0.0D,",
    "UPWARD_VELOCITY",
    "player.hasImpulse = true",
    "player.hurtMarked = true",
    "MartialEffectRegistry.LEVITATE.get()",
    "AttributeModifier.Operation.MULTIPLY_TOTAL",
    "HOLY_WARD_IMPACT",
):
    if token not in levitate:
        errors.append(f"PaladinLevitateSpell missing {token}")

levitate_effect = (effect_dir / "LevitateEffect.java").read_text(encoding="utf-8")
for token in (
    "SLOW_FALLING_TICKS = 3 * 20",
    "REFRESH_BELOW_TICKS = 20",
    "MobEffects.SLOW_FALLING",
    "remaining",
    "+ SLOW_FALLING_TICKS",
):
    if token not in levitate_effect:
        errors.append(f"LevitateEffect missing {token}")

penance = (spell_dir / "PaladinPenanceSpell.java").read_text(encoding="utf-8")
for token in (
    '"penance"',
    "SpellRarity.EPIC",
    "SchoolRegistry.HOLY_RESOURCE",
    "RANGE = 20",
    "CAST_TIME_TICKS = 30",
    "CHANNEL_TICKS = 3",
    "CHANNEL_VALUE_MULTIPLIER = 0.50F",
    "DAMAGE_COEFFICIENT = 0.55F",
    "KNOCKBACK_STRENGTH = 0.20F",
    "PROJECTILE_VELOCITY = 0.80F",
    "HOMING_DEGREES_PER_TICK = 16.0F",
    "ABSORPTION_RADIUS = 8.0D",
    "ABSORPTION_DURATION_TICKS = 6 * 20",
    "SHIELD_STACKS_PER_BOLT = 1",
    "SHIELD_POWER_COEFFICIENT = 0.10F",
    "ABSORPTION_HEALTH_PER_STACK = 2.0F",
    "caster.getEyeHeight()",
    "caster.getBbHeight()",
    "look.scale(0.5D)",
    "Vec3 direction = look",
    "BASE_COOLDOWN_SECONDS = 12",
    "BASE_MANA_COST = 45",
    "DAMAGE_COEFFICIENT",
    "* CHANNEL_VALUE_MULTIPLIER",
    "Math.floor(",
    "SHIELD_POWER_COEFFICIENT",
    "CHANNEL_TICKS",
    "* power",
    "MartialEntityRegistry",
    ".PENANCE_PROJECTILE",
    "PENANCE_RELEASE",
):
    if token not in penance:
        errors.append(f"PaladinPenanceSpell missing {token}")

projectile = (entity_dir / "PenanceProjectile.java").read_text(encoding="utf-8")
for token in (
    "applyHoming()",
    "HOMING_DEGREES_PER_TICK",
    "PROJECTILE_VELOCITY",
    "distanceTraveled",
    ">= PaladinPenanceSpell.RANGE",
    "previousInvulnerableTime =",
    "target.invulnerableTime = 0",
    "DamageSources.applyDamage",
    "target.invulnerableTime =",
    "MartialSpellRegistry.PENANCE",
    "Utils.shouldHealEntity",
    "ABSORPTION_RADIUS",
    "PRIEST_ABSORPTION",
    "ABSORPTION_HEALTH_PER_STACK",
    "PENANCE_IMPACT",
):
    if token not in projectile:
        errors.append(f"PenanceProjectile missing {token}")

projectile_compact = "".join(projectile.split())
for token in (
    "currentStacks+shieldStacksPerBolt",
    "shieldAmplifierCap+1",
    "NetworkHooks.getEntitySpawningPacket(this)",
):
    if token not in projectile_compact:
        errors.append(f"PenanceProjectile missing {token}")

holy_support = (spell_dir / "PaladinHolySpellSupport.java").read_text(encoding="utf-8")
for token in (
    "damageBypassingIframes(",
    "previousInvulnerableTime",
    "target.invulnerableTime = 0",
):
    if token not in holy_support:
        errors.append(f"PaladinHolySpellSupport missing P3 iframe-bypass token: {token}")

absorption_effect = (effect_dir / "PriestAbsorptionEffect.java").read_text(encoding="utf-8")
if "MobEffectCategory.BENEFICIAL" not in absorption_effect:
    errors.append("PriestAbsorptionEffect must be beneficial")

spell_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java").read_text(encoding="utf-8")
for spell_id, class_name in (
    ("holy_beam", "PaladinHolyBeamSpell::new"),
    ("levitate", "PaladinLevitateSpell::new"),
    ("penance", "PaladinPenanceSpell::new"),
):
    if f'SPELLS.register("{spell_id}", {class_name})' not in spell_registry:
        errors.append(f"spell registry missing {spell_id}")

effect_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialEffectRegistry.java").read_text(encoding="utf-8")
for effect_id, class_name in (
    ("levitate", "LevitateEffect::new"),
    ("priest_absorption", "PriestAbsorptionEffect::new"),
):
    if f'"{effect_id}"' not in effect_registry or class_name not in effect_registry:
        errors.append(f"effect registry missing {effect_id}")

entity_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialEntityRegistry.java").read_text(encoding="utf-8")
for token in (
    'ENTITY_TYPES.register("penance_projectile"',
    ".sized(0.25F, 0.25F)",
    ".fireImmune()",
    ".clientTrackingRange(128)",
    ".updateInterval(2)",
):
    if token not in entity_registry:
        errors.append(f"Penance entity registration missing {token}")

for token in (
    'ENTITY_TYPES.register("judgement_visual"',
    ".updateInterval(1)",
):
    if token not in entity_registry:
        errors.append(f"Judgement visual entity registration missing {token}")

for token in (
    'ENTITY_TYPES.register("holy_beam_visual"',
    ".sized(0.10F, 0.10F)",
):
    if token not in entity_registry:
        errors.append(f"Holy Light visual entity registration missing {token}")

client_events = (root / "src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java").read_text(encoding="utf-8")
for token in (
    "PenanceProjectileRenderer::new",
    "JudgementVisualRenderer::new",
    "HolyBeamVisualRenderer::new",
    "event.register(PenanceProjectileRenderer.MODEL)",
    "event.register(JudgementVisualRenderer.MODEL)",
):
    if token not in client_events:
        errors.append(f"Paladin projectile presentation registration missing {token}")

penance_renderer = (root / "src/main/java/com/w0of26/martialspells/client/render/PenanceProjectileRenderer.java").read_text(encoding="utf-8")
for token in (
    "extends EntityRenderer<PenanceProjectile>",
    '"spell_projectile/lightwell_orb"',
    "ORBIT_RADIUS = 0.6F",
    "ORBIT_DEGREES_PER_TICK = 15.0F",
    "SCALE = 0.9F",
    "LightTexture.FULL_BRIGHT",
):
    if token not in penance_renderer:
        errors.append(f"Penance source orb renderer missing {token}")
if "ThrownItemRenderer" in penance_renderer:
    errors.append("Penance renderer must not substitute a placeholder item cube")

judgement_visual = (entity_dir / "JudgementVisualEntity.java").read_text(encoding="utf-8")
for token in (
    "PaladinJudgementSpell.METEOR_VELOCITY",
    "Math.toRadians(1.0D)",
    "METEOR_TRAVEL_TICKS",
    "PaladinVfx.judgementTrail",
):
    if token not in judgement_visual:
        errors.append(f"Judgement visual entity missing {token}")

holy_beam_visual = (entity_dir / "HolyBeamVisualEntity.java").read_text(encoding="utf-8")
for token in (
    "OWNER_ID",
    "SAFETY_LIFETIME_TICKS = 120",
    "noCulling = true",
    "NetworkHooks",
):
    if token not in holy_beam_visual:
        errors.append(f"Holy Light beam visual entity missing {token}")

holy_beam_renderer = (root / "src/main/java/com/w0of26/martialspells/client/render/HolyBeamVisualRenderer.java").read_text(encoding="utf-8")
for token in (
    '"textures/entity/beacon_beam.png"',
    "RenderType.beaconBeam(",
    "WIDTH = 0.10F",
    "FLOW = 1.50F",
    "OUTER_RED = 255",
    "OUTER_GREEN = 204",
    "OUTER_BLUE = 102",
    "WIDTH * 1.50F",
    "WIDTH * 2.0F",
    "absoluteTime * 2.25F",
    "owner.getEyeHeight()",
    "owner.getBbHeight() * 0.15F",
    "look.scale(0.50D)",
):
    if token not in holy_beam_renderer:
        errors.append(f"Holy Light source beam renderer missing {token}")
if "RenderType.lightning()" in holy_beam_renderer:
    errors.append("Holy Light must not fall back to the old crossed lightning-beam approximation")

for token in (
    "sourceBeamStart(",
    "caster.getEyeHeight()",
    "caster.getBbHeight()",
    "look.scale(0.50D)",
    "PaladinVfx.holyBeamCasting(",
):
    if token not in holy_beam:
        errors.append(f"Holy Light source launch/presentation missing {token}")

judgement_renderer = (root / "src/main/java/com/w0of26/martialspells/client/render/JudgementVisualRenderer.java").read_text(encoding="utf-8")
for token in (
    '"spell_projectile/judgement"',
    "SCALE = 1.2F",
    "LightTexture.FULL_BRIGHT",
):
    if token not in judgement_renderer:
        errors.append(f"Judgement source model renderer missing {token}")

particle_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialParticleRegistry.java").read_text(encoding="utf-8")
for token in (
    "PALADIN_SPARK_FLOAT",
    "PALADIN_SPARK_DECELERATE",
    "PALADIN_SPARK_ASCEND",
    "PALADIN_HOLY_FLOAT",
    "PALADIN_HOLY_DECELERATE",
    "PALADIN_HOLY_BURST",
    "PALADIN_HEAL_ASCEND",
    "PALADIN_SPELL_FLOAT",
    "PALADIN_SPELL_DECELERATE",
    "PALADIN_STRIPE_FLOAT",
    "PALADIN_AREA_553_CAMERA",
    "PALADIN_AREA_637_GROUND",
    "PALADIN_AREA_676_CAMERA",
):
    if token not in particle_registry:
        errors.append(f"source-style Paladin particle registry missing {token}")

source_magic_particle = (root / "src/main/java/com/w0of26/martialspells/client/particle/PaladinSourceMagicParticle.java").read_text(encoding="utf-8")
for token in (
    "enum Motion",
    "FLOAT",
    "DECELERATE",
    "ASCEND",
    "BURST",
):
    if token not in source_magic_particle:
        errors.append(f"PaladinSourceMagicParticle missing Spell Engine motion {token}")

source_area_particle = (root / "src/main/java/com/w0of26/martialspells/client/particle/PaladinSourceAreaParticle.java").read_text(encoding="utf-8")
for token in (
    "enum Facing",
    "GROUND",
    "CAMERA",
    "encodedFollowId",
    "followEntity",
    "followOffset",
):
    if token not in source_area_particle:
        errors.append(f"PaladinSourceAreaParticle missing {token}")

vfx = (spell_dir / "PaladinVfx.java").read_text(encoding="utf-8")
for token in (
    "holyCasting(",
    "healPillar(",
    "holyBurst(",
    "holyGlimmer(",
    "circleOfHealingRelease(",
    "immolationRelease(",
    "divineProtectionPop(",
    "blessedGather(",
    "blessedRelease(",
    "holyBeamCasting(",
    "holyBeam(",
    "levitateChannel(",
    "penanceHelix(",
    "penanceAreaPulse(",
    "judgementTrail(",
    "judgementImpact(",
):
    if token not in vfx:
        errors.append(f"PaladinVfx missing source-style helper {token}")
for forbidden in (
    "new Vector3f(",
    "blessedWeaponAura(",
):
    if forbidden in vfx:
        errors.append(f"PaladinVfx still contains approximation-era fallback {forbidden}")

vfx_expectations = {
    "PaladinHolyShockSpell.java": (
        "PaladinVfx.holyCasting(",
        "PaladinVfx.healPillar(",
        "PaladinVfx.holyGlimmer(",
        "PaladinVfx.holyBurst(",
    ),
    "PaladinFlashHealSpell.java": (
        "PaladinVfx.holyCasting(",
        "PaladinVfx.healPillar(",
    ),
    "PaladinCircleOfHealingSpell.java": (
        "PaladinVfx.holyCasting(",
        "PaladinVfx.circleOfHealingRelease(",
        "PaladinVfx.healPillar(",
        "PaladinVfx.holyGlimmer(",
    ),
    "PaladinBlessedStrikesSpell.java": (
        "PaladinVfx.blessedGather(",
        "PaladinVfx.blessedRelease(",
    ),
    "PaladinDivineProtectionSpell.java": (
        "PaladinVfx.divineProtectionApply(",
    ),
    "PaladinJudgementSpell.java": (
        "PaladinVfx.holyCasting(",
        "PaladinVfx.judgementImpact(",
    ),
    "PaladinImmolationSpell.java": (
        "PaladinVfx.immolationRelease(",
        "PaladinVfx.healPillar(",
        "PaladinVfx.holyBurst(",
    ),
    "PaladinHolyBeamSpell.java": (
        "PaladinVfx.holyBeamCasting(",
        "PaladinVfx.holyBeam(",
        "PaladinVfx.healPillar(",
        "PaladinVfx.holyGlimmer(",
        "PaladinVfx.holyBurst(",
    ),
    "PaladinLevitateSpell.java": (
        "PaladinVfx.levitateChannel(",
    ),
    "PaladinPenanceSpell.java": (
        "PaladinVfx.penanceCasting(",
    ),
}
for filename, tokens in vfx_expectations.items():
    text = (spell_dir / filename).read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{filename} missing source-style VFX call {token}")

blessed_events = (root / "src/main/java/com/w0of26/martialspells/events/BlessedStrikesEvents.java").read_text(encoding="utf-8")
if "PaladinVfx.holyBurst(" not in blessed_events:
    errors.append("BlessedStrikesEvents missing source Holy impact burst")
if "blessedWeaponAura(" in blessed_events:
    errors.append("Blessed Strikes must not use the old particle weapon-aura approximation")

blessed_glow = (root / "src/main/java/com/w0of26/martialspells/client/render/BlessedStrikesItemGlow.java").read_text(encoding="utf-8")
for token in (
    "OPACITY_PER_STACK =",
    "0.20F",
    "MAX_STACKS =",
    "5",
    "VertexMultiConsumer.create(",
    "EquipmentSlot.MAINHAND",
    "EquipmentSlot.OFFHAND",
):
    if token not in blessed_glow:
        errors.append(f"Blessed Strikes source item-glow state missing {token}")

blessed_render_types = (root / "src/main/java/com/w0of26/martialspells/client/render/BlessedStrikesGlowRenderTypes.java").read_text(encoding="utf-8")
for token in (
    "extends RenderType",
    '"textures/misc/paladin_item_glow.png"',
    "TEXTURE_SCALE = 8.0F",
    "GAIN = 3.0F",
    "OPACITY_PER_STACK = 0.20F",
    "RENDERTYPE_GLINT_SHADER",
    "EQUAL_DEPTH_TEST",
    "SourceFactor.ONE",
    "DestFactor.ONE",
    "intensity,",
    "0.80F * intensity",
    "setShaderGlintAlpha(",
):
    if token not in blessed_render_types:
        errors.append(f"Blessed Strikes source glow RenderType missing {token}")

blessed_mixin = (root / "src/main/java/com/w0of26/martialspells/mixin/client/BlessedStrikesItemRendererMixin.java").read_text(encoding="utf-8")
for token in (
    '@Mixin(ItemRenderer.class)',
    "renderStatic(",
    "getFoilBuffer",
    "getFoilBufferDirect",
    "BlessedStrikesItemGlow.begin(",
    "BlessedStrikesItemGlow.glowing(",
):
    if token not in blessed_mixin:
        errors.append(f"Blessed Strikes item-render hook missing {token}")

blessed_buffer_mixin = (root / "src/main/java/com/w0of26/martialspells/mixin/client/BlessedStrikesGlowBufferSourceMixin.java").read_text(encoding="utf-8")
for token in (
    "@Mixin(MultiBufferSource.BufferSource.class)",
    "fixedBuffers",
    "BlessedStrikesGlowRenderTypes",
    ".isGlowLayer(renderType)",
    "new BufferBuilder(",
    "renderType.bufferSize()",
):
    if token not in blessed_buffer_mixin:
        errors.append(f"Blessed Strikes glow buffer ordering missing {token}")

mixins_json = json.loads((root / "src/main/resources/martial_spells.mixins.json").read_text(encoding="utf-8"))
for mixin_id in (
    "client.BlessedStrikesItemRendererMixin",
    "client.BlessedStrikesGlowBufferSourceMixin",
):
    if mixin_id not in mixins_json.get("client", []):
        errors.append(f"Blessed Strikes client mixin is not registered: {mixin_id}")

divine_events = (root / "src/main/java/com/w0of26/martialspells/events/DivineProtectionEvents.java").read_text(encoding="utf-8")
if "PaladinVfx.divineProtectionPop(" not in divine_events:
    errors.append("DivineProtectionEvents missing source-style protection pop VFX")

divine_renderer = (root / "src/main/java/com/w0of26/martialspells/client/render/DivineProtectionRenderer.java").read_text(encoding="utf-8")
for token in (
    '"spell_effect/divine_protection"',
    '"spell_effect/divine_protection_glow"',
    "HORIZONTAL_OFFSET = 0.35F",
    "ORBITING_SPEED = 2.25F",
    "effect.getAmplifier()",
    "LightTexture.FULL_BRIGHT",
):
    if token not in divine_renderer:
        errors.append(f"Divine Protection source orbit renderer missing {token}")

client_events = (root / "src/main/java/com/w0of26/martialspells/client/MartialClientEvents.java").read_text(encoding="utf-8")
for token in (
    "DivineProtectionRenderer::onRenderLivingPost",
    "PALADIN_AREA_553_CAMERA",
    "PALADIN_AREA_637_GROUND",
    "PaladinSourceAreaParticle.Facing.GROUND",
    "PaladinSourceAreaParticle.Facing.CAMERA",
    "PaladinSourceMagicParticle.Motion.FLOAT",
    "PaladinSourceMagicParticle.Motion.DECELERATE",
    "PaladinSourceMagicParticle.Motion.ASCEND",
    "PaladinSourceMagicParticle.Motion.BURST",
):
    if token not in client_events:
        errors.append(f"Paladin source presentation registration missing {token}")

status_vfx = (root / "src/main/java/com/w0of26/martialspells/client/PaladinStatusVfxClientEvents.java").read_text(encoding="utf-8")
for token in (
    "MartialEffectRegistry.LEVITATE.get()",
    "ParticleTypes.CLOUD",
    "entity.tickCount % 3",
    "MartialEffectRegistry",
    ".PRIEST_ABSORPTION",
    "PALADIN_AREA_553_CAMERA",
    "entity.tickCount % 30",
    '"turtlecore",',
    '"stunned"',
    "ParticleTypes.CRIT",
    "* 18.0D",
):
    if token not in status_vfx:
        errors.append(f"persistent Paladin status VFX missing {token}")

if "PaladinVfx.levitateChannel(" in levitate_effect:
    errors.append("LevitateEffect must not use the old persistent Holy-particle approximation; source uses client cloud spawner")

for token in (
    "PaladinVfx.penanceHelix(",
    "PaladinVfx.penanceImpact(",
    "PaladinVfx.penanceShield(",
    "PaladinVfx.penanceAreaPulse(",
):
    if token not in projectile:
        errors.append(f"PenanceProjectile missing restored VFX call {token}")

judgement_manager = (root / "src/main/java/com/w0of26/martialspells/combat/JudgementImpactManager.java").read_text(encoding="utf-8")
for token in (
    "new JudgementVisualEntity(",
    "MartialEntityRegistry.JUDGEMENT_VISUAL.get()",
):
    if token not in judgement_manager:
        errors.append(f"JudgementImpactManager missing model-VFX integration {token}")

# Exact source-art assets owned by P2/P3 sync.
for rel in (
    "textures/spell_effect/divine_protection.png",
    "textures/spell_effect/divine_protection_glow.png",
    "textures/misc/paladin_item_glow.png",
    "textures/particle/paladin_source/magic/holy.png",
    "textures/particle/paladin_source/magic/heal.png",
):
    if not (root / "src/main/resources/assets/martial_spells" / rel).is_file():
        errors.append(f"source-faithful Paladin VFX asset missing; rerun P3 asset sync: {rel}")

for frame in range(13):
    rel = f"textures/particle/paladin_source/zone/effect_553_{frame}.png"
    if not (root / "src/main/resources/assets/martial_spells" / rel).is_file():
        errors.append(f"Priest Absorption aura frame missing; rerun P3 asset sync: {rel}")

lang = json.loads((root / "src/main/resources/assets/martial_spells/lang/en_us.json").read_text(encoding="utf-8"))
for key in (
    "spell.martial_spells.holy_beam",
    "spell.martial_spells.levitate",
    "spell.martial_spells.penance",
    "effect.martial_spells.levitate",
    "effect.martial_spells.priest_absorption",
):
    if key not in lang:
        errors.append(f"lang missing {key}")

sound_registry = (root / "src/main/java/com/w0of26/martialspells/registry/MartialSoundRegistry.java").read_text(encoding="utf-8")
sounds = json.loads((root / "src/main/resources/assets/martial_spells/sounds.json").read_text(encoding="utf-8"))

single_sounds = (
    "holy_beam_start_casting",
    "holy_beam_casting",
    "holy_beam_release",
    "holy_beam_heal",
    "holy_beam_damage",
    "holy_ward_impact",
    "penance_impact",
)
for sound in single_sounds:
    if f'register("{sound}")' not in sound_registry:
        errors.append(f"sound registry missing {sound}")
    if sounds.get(sound, {}).get("sounds") != [f"martial_spells:{sound}"]:
        errors.append(f"sounds.json mismatch for {sound}")

if 'register("penance_release")' not in sound_registry:
    errors.append("sound registry missing penance_release")
if sounds.get("penance_release", {}).get("sounds") != [
    "martial_spells:penance_release_1",
    "martial_spells:penance_release_2",
    "martial_spells:penance_release_3",
]:
    errors.append("sounds.json mismatch for penance_release variants")

sync_text = (root / "tools/sync-paladin-p3-assets.ps1").read_text(encoding="utf-8")
if "2807417a1dd9a65204c002ded487da0e6ae467a1" not in sync_text:
    errors.append("P3 asset sync is not pinned to frozen Paladins commit")
if "sync-paladin-p2-assets.ps1" not in sync_text:
    errors.append("P3 asset sync must re-assert finalized P2 assets first")

for source_texture in (
    "textures/spell_projectile/judgement.png",
    "textures/spell_projectile/lightwell_orb.png",
):
    if source_texture not in sync_text:
        errors.append(f"P3 asset sync missing VFX texture: {source_texture}")

for spell_id in ("holy_beam", "levitate", "penance"):
    path = root / f"src/main/resources/assets/martial_spells/textures/gui/spell_icons/{spell_id}.png"
    if not path.is_file():
        errors.append(f"source spell icon missing; run P3 asset sync: {spell_id}.png")

for effect_id in ("levitate", "priest_absorption"):
    path = root / f"src/main/resources/assets/martial_spells/textures/mob_effect/{effect_id}.png"
    if not path.is_file():
        errors.append(f"source effect icon missing; run P3 asset sync: {effect_id}.png")

for projectile_model in ("judgement", "lightwell_orb"):
    model_path = root / f"src/main/resources/assets/martial_spells/models/spell_projectile/{projectile_model}.json"
    texture_path = root / f"src/main/resources/assets/martial_spells/textures/spell_projectile/{projectile_model}.png"
    if not model_path.is_file():
        errors.append(f"source projectile model missing: {projectile_model}.json")
    if not texture_path.is_file():
        errors.append(f"source projectile texture missing; run P3 asset sync: {projectile_model}.png")

for sound in (
    "holy_beam_start_casting.ogg",
    "holy_beam_casting.ogg",
    "holy_beam_release.ogg",
    "holy_beam_heal.ogg",
    "holy_beam_damage.ogg",
    "holy_ward_impact.ogg",
    "penance_impact.ogg",
    "penance_release_1.ogg",
    "penance_release_2.ogg",
    "penance_release_3.ogg",
):
    path = root / "src/main/resources/assets/martial_spells/sounds" / sound
    if not path.is_file():
        errors.append(f"source sound missing; run P3 asset sync: {sound}")

# P3 is a regression audit once later CP11 phases exist. P4 registration is
# intentionally allowed here; P4's own audit is responsible for validating
# Barrier, Battle Banner, Lightwell, and the internal Holy Mote helper.

build = (root / "build.gradle").read_text(encoding="utf-8").lower()
for forbidden in ("spell_engine", "spell-engine", "spell-power", "spell_power"):
    if forbidden in build:
        errors.append(f"P3 must not add source runtime dependency: {forbidden}")

print("")
print("CP11 P3 Priest channel/control summary")
print(" - spells: Holy Light, Levitate, Penance")
print(" - source channel scheduling: equal-interval midpoints")
print(" - Holy Light: 5 sec / 25 pulses / continuous golden blocked BEAM + flow particles")
print(" - Holy Light/Penance: source damage iframe bypass preserved")
print(" - Levitate: 1.5 sec / 4 reset-velocity lifts / executable Slow Falling refresh")
print(" - Penance: 1.5 sec / 3 look-launched homing bolts / 20-block travel cap")
print(" - Penance: 0.5x per-bolt damage/knockback; 8-block no-falloff ally shield")
print(" - early release: proportional mana + effective cooldown")
print(" - target-side mana: 40 / 25 / 45")
print(" - P1-P3 holy/healing particle language restored without Spell Engine runtime")
print(" - Penance Lightwell orb model + 15 deg/tick orbit restored")
print(" - Judgement source projectile model + fullbright meteor visual restored")

if errors:
    print("CP11 P3 AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("CP11 P3 AUDIT PASSED")
