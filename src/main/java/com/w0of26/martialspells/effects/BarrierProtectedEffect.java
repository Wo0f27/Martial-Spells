package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Short-lived marker refreshed by the Paladin Barrier every four ticks.
 * Damage cancellation is handled server-side in PaladinP4Events.
 */
public final class BarrierProtectedEffect extends MobEffect {
    public BarrierProtectedEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0xFFFFCC
        );
    }
}
