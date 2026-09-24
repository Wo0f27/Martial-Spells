package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect for the frozen Priest absorption shield.
 *
 * <p>The shield amount is granted explicitly when Penance applies or refreshes
 * the effect because Minecraft 1.20.1 has no generic maximum-absorption
 * attribute. This mirrors the frozen Paladins 1.20.1 implementation.</p>
 */
public final class PriestAbsorptionEffect
        extends MobEffect {
    public PriestAbsorptionEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0xFFFFCC
        );
    }
}
