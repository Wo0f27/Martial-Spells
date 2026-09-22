package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Exact local translation of Spell Engine 1.10.5.034 Bleed.
 *
 * <p>Bleed is lethal, ticks every 25 ticks, and scales linearly with horizontal
 * movement from 0.5x damage while stationary to 2.0x at 2 blocks/second.</p>
 */
public final class BleedEffect extends MobEffect {
    public static final int TICK_INTERVAL = 25;
    public static final double MIN_MULTIPLIER = 0.5D;
    public static final double MAX_MULTIPLIER = 2.0D;
    public static final double MOVEMENT_SPEED_CAP = 2.0D;

    public BleedEffect() {
        super(MobEffectCategory.HARMFUL, 0xB30000);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }

        Vec3 velocity = entity.getDeltaMovement();
        double horizontalBlocksPerSecond =
                Math.sqrt(
                        velocity.x * velocity.x
                                + velocity.z * velocity.z
                ) * 20.0D;

        double fraction =
                Math.min(
                        horizontalBlocksPerSecond,
                        MOVEMENT_SPEED_CAP
                ) / MOVEMENT_SPEED_CAP;

        double multiplier =
                MIN_MULTIPLIER
                        + (MAX_MULTIPLIER - MIN_MULTIPLIER)
                        * fraction;

        int stacks = amplifier + 1;
        float damage = (float) (stacks * multiplier);

        // Source uses vanilla magic damage and is intentionally lethal.
        entity.hurt(entity.damageSources().magic(), damage);
    }
}
