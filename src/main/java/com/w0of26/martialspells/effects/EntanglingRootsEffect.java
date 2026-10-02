package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One-second SET effect. Jump motion fallback is scoped separately to entities without Jump Strength. */
public final class EntanglingRootsEffect extends MobEffect {
    public EntanglingRootsEffect() {
        super(MobEffectCategory.HARMFUL, 0x466934);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "7b23e64c-470a-4e84-8aca-67a1eb733010",
                -0.5D, AttributeModifier.Operation.MULTIPLY_BASE);
        addAttributeModifier(Attributes.JUMP_STRENGTH, "702bce16-a843-4589-8934-b8f1531b217c",
                -0.5D, AttributeModifier.Operation.MULTIPLY_BASE);
    }
}
