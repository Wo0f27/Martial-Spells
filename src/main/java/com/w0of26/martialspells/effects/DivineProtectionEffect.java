package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Stores Divine Protection charges. Amplifier + 1 equals remaining protected hits.
 */
public final class DivineProtectionEffect extends MobEffect {
    public DivineProtectionEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0x66CCFF
        );
    }
}
