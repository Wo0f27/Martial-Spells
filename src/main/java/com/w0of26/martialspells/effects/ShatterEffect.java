package com.w0of26.martialspells.effects;

import com.w0of26.martialspells.visual.ShatterBloodVfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Frozen Rogues Shatter / "Shattered Armor" status effect. */
public final class ShatterEffect extends MobEffect {
    private static final String ARMOR_MODIFIER_UUID =
            "44ad4a91-f833-4d99-a582-3a72405ad47a";

    public ShatterEffect() {
        super(MobEffectCategory.HARMFUL, 0x800000);
        addAttributeModifier(
                Attributes.ARMOR,
                ARMOR_MODIFIER_UUID,
                -0.30D,
                AttributeModifier.Operation.MULTIPLY_BASE
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
        if (entity.level() instanceof ServerLevel serverLevel) {
            ShatterBloodVfx.spawn(
                    serverLevel,
                    entity,
                    Math.max(1, amplifier + 1),
                    0.10D,
                    0.30D
            );
        }
    }
}
