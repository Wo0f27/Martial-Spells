package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frozen Rogues Charge buff translated to Forge 1.20.1.
 *
 * <p>At amplifier 0 this grants +50% base movement speed and +50% base
 * knockback resistance. Charge is always applied with amplifier 0, so
 * reapplication refreshes the same effect instead of stacking another copy.</p>
 */
public final class ChargeEffect extends MobEffect {
    private static final String MOVEMENT_SPEED_MODIFIER_UUID =
            "22c14c39-7739-4d38-9b76-6b8dcbef5081";
    private static final String KNOCKBACK_RESISTANCE_MODIFIER_UUID =
            "73cdbe53-7de2-4afb-b08e-3aa76a753bb4";

    public ChargeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAAAAAA);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER_UUID,
                0.5D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK_RESISTANCE_MODIFIER_UUID,
                0.5D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}
