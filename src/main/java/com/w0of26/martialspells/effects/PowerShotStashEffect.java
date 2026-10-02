package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** The amplifier is one less than the number of remaining shot transfers. */
public final class PowerShotStashEffect extends MobEffect {
    public PowerShotStashEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB53F46);
    }
}
