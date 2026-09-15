package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.StunService;
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
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Forge/Iron's translation of frozen Rogues Shock Powder.
 *
 * <p>The original Spell Engine implementation is an instant harmful area
 * control technique: 5-block range, half-height vertical reach, a two-second
 * true stun, and a control cap of 50 + 2x physical-melee power. In frozen
 * Rogues physical-melee power is the caster's current Attack Damage, so this
 * port reads vanilla Attack Damage directly rather than introducing Spell
 * Power as a dependency.</p>
 */
public final class ShockPowderSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "shock_powder");

    public static final int MAX_LEVEL = 1;
    public static final float RANGE = 5.0F;
    public static final float VERTICAL_RANGE_MULTIPLIER = 0.5F;
    public static final int STUN_DURATION_TICKS = 40;
    public static final int BASE_COOLDOWN_SECONDS = 16;
    public static final float CONTROL_HEALTH_BASE = 50.0F;
    public static final float CONTROL_ATTACK_DAMAGE_MULTIPLIER = 2.0F;

    private static final double SEARCH_TOLERANCE = 0.5D;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public ShockPowderSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.ROGUE;
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
        // The frozen release sound is played server-side with the translated VFX.
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        // Upstream used Spell Engine's generic dual-handed ground-release pose.
        // Do not add a Spell Engine dependency solely to reproduce that generic animation.
        return AnimationHolder.none();
    }

    public static float getControlHealthLimit(LivingEntity caster) {
        AttributeInstance attackDamage = caster == null
                ? null
                : caster.getAttribute(Attributes.ATTACK_DAMAGE);
        float physicalMeleePower = attackDamage == null
                ? 0.0F
                : (float) attackDamage.getValue();

        return CONTROL_HEALTH_BASE
                + physicalMeleePower * CONTROL_ATTACK_DAMAGE_MULTIPLIER;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.martial_spells.radius", RANGE),
                Component.translatable(
                        "ui.martial_spells.shock_powder_stun_duration",
                        STUN_DURATION_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.shock_powder_control_limit",
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

        float controlHealthLimit = getControlHealthLimit(caster);
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
                target -> isEligibleTarget(caster, target, origin, controlHealthLimit, serverLevel)
        );

        for (LivingEntity target : targets) {
            if (!StunService.apply(target, caster, STUN_DURATION_TICKS)) {
                continue;
            }

            serverLevel.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    MartialSoundRegistry.SHOCK_POWDER_IMPACT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private static boolean isEligibleTarget(
            LivingEntity caster,
            LivingEntity target,
            Vec3 origin,
            float controlHealthLimit,
            ServerLevel level
    ) {
        if (target == caster
                || !target.isAlive()
                || target.isDeadOrDying()
                || target.isRemoved()
                || target.isSpectator()
                || caster.isAlliedTo(target)
                || target.getMaxHealth() > controlHealthLimit) {
            return false;
        }

        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        if (targetCenter.distanceToSqr(origin) > RANGE * RANGE) {
            return false;
        }

        return hasLineOfSight(level, caster, origin, target, targetCenter);
    }

    /**
     * Mirrors Spell Engine's area-target obstacle rule: center LOS is enough,
     * otherwise the closest point on the target hitbox may still be visible.
     */
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

    private static void playReleasePresentation(ServerLevel level, LivingEntity caster) {
        level.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                MartialSoundRegistry.SHOCK_POWDER_RELEASE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        // The frozen source used three smoke batches and two lightning-arc
        // batches. Martial Spells recreates that silhouette with its own
        // registered sprite particles instead of importing Spell Engine assets.
        level.sendParticles(
                MartialParticleRegistry.SHOCK_POWDER_SMOKE.get(),
                caster.getX(), caster.getY() + 0.2D, caster.getZ(),
                50,
                2.3D, 0.15D, 2.3D,
                0.03D
        );
        level.sendParticles(
                MartialParticleRegistry.SHOCK_POWDER_SMOKE.get(),
                caster.getX(), caster.getY() + 0.3D, caster.getZ(),
                60,
                3.3D, 0.18D, 3.3D,
                0.025D
        );
        level.sendParticles(
                MartialParticleRegistry.SHOCK_POWDER_SMOKE.get(),
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.45D, caster.getZ(),
                50,
                1.8D, 0.9D, 1.8D,
                0.02D
        );
        level.sendParticles(
                MartialParticleRegistry.SHOCK_POWDER_ARC.get(),
                caster.getX(), caster.getY() + 1.5D, caster.getZ(),
                6,
                0.75D, 1.5D, 0.75D,
                0.02D
        );
        level.sendParticles(
                MartialParticleRegistry.SHOCK_POWDER_ARC.get(),
                caster.getX(), caster.getY() + 2.5D, caster.getZ(),
                8,
                1.0D, 2.5D, 1.0D,
                0.03D
        );
    }
}
