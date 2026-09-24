package com.w0of26.martialspells.effects;

import com.w0of26.martialspells.spells.PaladinVfx;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Frozen Paladins Levitate effect translated for Minecraft 1.20.1.
 *
 * <p>1.20.1 has no generic gravity attribute, so the source itself uses
 * vanilla Slow Falling as the compatibility carrier. Slow Falling is kept
 * alive throughout Levitate and deliberately outlives it by three seconds for
 * the source soft-landing behavior.</p>
 */
public final class LevitateEffect extends MobEffect {
    public static final int SLOW_FALLING_TICKS = 3 * 20;
    private static final int REFRESH_BELOW_TICKS = 20;

    public LevitateEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0xFFFFCC
        );
    }

    @Override
    public boolean isDurationEffectTick(
            int duration,
            int amplifier
    ) {
        return true;
    }

    @Override
    public void applyEffectTick(
            LivingEntity entity,
            int amplifier
    ) {
        if (entity.level().isClientSide) {
            return;
        }

        MobEffectInstance levitate =
                entity.getEffect(this);
        int remaining =
                levitate == null
                        ? 0
                        : levitate.getDuration();

        MobEffectInstance slowFalling =
                entity.getEffect(
                        MobEffects.SLOW_FALLING
                );

        if (entity.level() instanceof ServerLevel serverLevel
                && serverLevel.getGameTime() % 4L == 0L) {
            PaladinVfx.levitateChannel(
                    serverLevel,
                    entity
            );
        }

        if (slowFalling == null
                || slowFalling.getDuration()
                < REFRESH_BELOW_TICKS) {
            entity.addEffect(
                    new MobEffectInstance(
                            MobEffects.SLOW_FALLING,
                            remaining
                                    + SLOW_FALLING_TICKS,
                            0,
                            false,
                            true,
                            true
                    ),
                    entity
            );
        }
    }
}
