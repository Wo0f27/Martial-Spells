package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Damage entry scaling is handled at LivingEntity.hurt HEAD, before Forge hooks. */
public final class HuntersMarkEffect extends MobEffect {
    public HuntersMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0xC13D43);
    }
}
