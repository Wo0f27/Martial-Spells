package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.PhysicalMeleePower;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialParticleRegistry;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Forge/Iron's translation of frozen Rogues Demoralizing Shout.
 */
public final class DemoralizingShoutSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "demoralizing_shout");

    public static final int MAX_LEVEL = 1;
    public static final float RANGE = 12.0F;
    public static final float VERTICAL_RANGE_MULTIPLIER = 0.5F;
    public static final int EFFECT_DURATION_TICKS = 160;
    public static final int AMPLIFIER_INCREMENT = 1;
    public static final int AMPLIFIER_CAP = 5;
    public static final double ATTACK_DAMAGE_REDUCTION_PER_LEVEL = 0.20D;
    public static final double DAMAGE_COEFFICIENT = 0.05D;
    public static final double CONTROL_HEALTH_BASE = 50.0D;
    public static final double CONTROL_POWER_MULTIPLIER = 2.0D;
    public static final int BASE_COOLDOWN_SECONDS = 12;

    private static final double SEARCH_TOLERANCE = 0.5D;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public DemoralizingShoutSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.WARRIOR;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        // Frozen Rogues used spell_engine:one_handed_shout_release.
        // Iron's native instant-cast pose keeps the port dependency-free.
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    public static double getControlHealthLimit(LivingEntity caster) {
        return PhysicalMeleePower.controlHealthLimit(
                caster,
                CONTROL_HEALTH_BASE,
                CONTROL_POWER_MULTIPLIER
        );
    }

    public static float getDirectDamage(LivingEntity caster) {
        return (float) (PhysicalMeleePower.get(caster) * DAMAGE_COEFFICIENT);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.martial_spells.radius", RANGE),
                Component.translatable(
                        "ui.martial_spells.effect_length",
                        EFFECT_DURATION_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.demoralizing_shout_attack_reduction",
                        Math.round(ATTACK_DAMAGE_REDUCTION_PER_LEVEL * 100.0D)
                ),
                Component.translatable(
                        "ui.martial_spells.demoralizing_shout_max_effect_level",
                        AMPLIFIER_CAP + 1
                ),
                Component.translatable(
                        "ui.martial_spells.demoralizing_shout_damage",
                        Math.round(DAMAGE_COEFFICIENT * 100.0D)
                ),
                Component.translatable(
                        "ui.martial_spells.demoralizing_shout_control_limit",
                        Math.round(getControlHealthLimit(caster))
                ),
                Component.translatable(
                        "ui.martial_spells.base_cooldown",
                        BASE_COOLDOWN_SECONDS
                )
        );
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        playReleasePresentation(serverLevel, caster);

        double controlHealthLimit = getControlHealthLimit(caster);
        float directDamage = getDirectDamage(caster);
        Vec3 origin = caster.getEyePosition();
        double verticalRange = RANGE * VERTICAL_RANGE_MULTIPLIER;

        AABB searchArea = caster.getBoundingBox().inflate(
                RANGE + SEARCH_TOLERANCE,
                verticalRange + SEARCH_TOLERANCE,
                RANGE + SEARCH_TOLERANCE
        );

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                target -> isEligibleTarget(caster, target, origin, serverLevel)
        );

        for (LivingEntity target : targets) {
            // The source health gate belongs only to the status-effect impact.
            // Targets above the cap still receive the separate 5% damage impact.
            if (target.getMaxHealth() <= controlHealthLimit) {
                applyDemoralized(target, caster);
                playDemoralizeImpact(serverLevel, target);
            }

            applyDirectDamageWithoutKnockback(target, caster, directDamage);
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private static void applyDemoralized(LivingEntity target, LivingEntity caster) {
        MobEffectInstance current =
                target.getEffect(MartialEffectRegistry.DEMORALIZED.get());

        // Exact Spell Engine ADD semantics for amplifier=1:
        // first application => amplifier 0; subsequent applications => +1,
        // clamped to amplifier_cap=5 (effect level VI).
        int nextAmplifier = current == null
                ? Math.max(AMPLIFIER_INCREMENT - 1, 0)
                : Math.min(
                        current.getAmplifier() + AMPLIFIER_INCREMENT,
                        AMPLIFIER_CAP
                );

        target.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry.DEMORALIZED.get(),
                        EFFECT_DURATION_TICKS,
                        nextAmplifier,
                        false,
                        true,
                        true
                ),
                caster
        );
    }

    private static void applyDirectDamageWithoutKnockback(
            LivingEntity target,
            LivingEntity caster,
            float damage
    ) {
        if (damage <= 0.0F) {
            return;
        }

        Vec3 velocityBeforeImpact = target.getDeltaMovement();
        boolean damaged = target.hurt(
                MartialDamageTypes.demoralizingShout(caster),
                damage
        );

        if (damaged) {
            // Source damage impact declares knockback=0. Preserve any motion the
            // target already had, but discard motion introduced by hurt handling.
            target.setDeltaMovement(velocityBeforeImpact);
        }
    }

    private static boolean isEligibleTarget(
            LivingEntity caster,
            LivingEntity target,
            Vec3 origin,
            ServerLevel level
    ) {
        if (target == caster
                || !target.isAlive()
                || target.isDeadOrDying()
                || target.isRemoved()
                || target.isSpectator()
                || caster.isAlliedTo(target)) {
            return false;
        }

        Vec3 targetCenter =
                target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);

        if (targetCenter.distanceToSqr(origin) > RANGE * RANGE) {
            return false;
        }

        return hasLineOfSight(level, caster, origin, target, targetCenter);
    }

    private static boolean hasLineOfSight(
            ServerLevel level,
            LivingEntity caster,
            Vec3 origin,
            LivingEntity target,
            Vec3 targetCenter
    ) {
        if (isUnobstructed(level, caster, origin, targetCenter)) {
            return true;
        }

        AABB box = target.getBoundingBox();
        Vec3 nearest = new Vec3(
                clamp(origin.x, box.minX, box.maxX),
                clamp(origin.y, box.minY, box.maxY),
                clamp(origin.z, box.minZ, box.maxZ)
        );
        return isUnobstructed(level, caster, origin, nearest);
    }

    private static boolean isUnobstructed(
            ServerLevel level,
            LivingEntity caster,
            Vec3 start,
            Vec3 end
    ) {
        return level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        )).getType() != HitResult.Type.BLOCK;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void playReleasePresentation(
            ServerLevel level,
            LivingEntity caster
    ) {
        level.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                MartialSoundRegistry.SHOUT_RELEASE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        // Frozen Rogues used one Rage-tinted ground area texture. Recreate the
        // expanding footprint with low Rage-smoke rings while remaining free of
        // Spell Engine's particle renderer.
        spawnReleaseRing(level, caster, 4.0D, 16);
        spawnReleaseRing(level, caster, 8.0D, 24);
        spawnReleaseRing(level, caster, RANGE, 32);
    }

    private static void spawnReleaseRing(
            ServerLevel level,
            LivingEntity caster,
            double radius,
            int points
    ) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            level.sendParticles(
                    MartialParticleRegistry.DEMORALIZE_SMOKE.get(),
                    caster.getX() + Math.cos(angle) * radius,
                    caster.getY() + 0.10D,
                    caster.getZ() + Math.sin(angle) * radius,
                    1,
                    0.04D,
                    0.03D,
                    0.04D,
                    0.025D
            );
        }
    }

    private static void playDemoralizeImpact(
            ServerLevel level,
            LivingEntity target
    ) {
        level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                MartialSoundRegistry.DEMORALIZE_IMPACT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        level.sendParticles(
                MartialParticleRegistry.DEMORALIZE_SMOKE.get(),
                target.getX(),
                target.getY() + target.getBbHeight() * 0.55D,
                target.getZ(),
                25,
                0.40D,
                0.60D,
                0.40D,
                0.20D
        );
    }
}
