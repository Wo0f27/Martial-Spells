package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Stores Blessed Strikes seals. Amplifier + 1 equals the current seal count.
 */
public final class BlessedStrikesEffect extends MobEffect {
    public BlessedStrikesEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0xFFFFCC
        );
    }
}
