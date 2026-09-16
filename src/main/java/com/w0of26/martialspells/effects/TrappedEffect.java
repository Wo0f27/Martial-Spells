package com.w0of26.martialspells.effects;

import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Frozen Rogues Bear Trap root effect: harmful, grey, and movement-locking. */
public final class TrappedEffect extends MobEffect {
    private static final UUID MOVEMENT_SPEED_MODIFIER_ID =
            UUID.fromString("7f94ec43-a714-4e31-8c38-3d13eb919f57");

    public TrappedEffect() {
        super(MobEffectCategory.HARMFUL, 0x6E6E6E);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER_ID.toString(),
                -2.0D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}
