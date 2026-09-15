package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect for the short post-Shadowstep anti-tracking window.
 *
 * <p>The behavior itself is implemented in TargetGoalShadowstepMixin so it
 * affects vanilla target-goal follow distance exactly where the frozen Rogues
 * implementation did.</p>
 */
public final class ShadowstepEffect extends MobEffect {
    public ShadowstepEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x000000);
    }
}
