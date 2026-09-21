package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frozen Rogues Demoralized effect.
 *
 * <p>Each Minecraft effect level removes 20% of base Attack Damage using
 * MULTIPLY_BASE. Demoralizing Shout owns the source ADD/cap behavior.</p>
 */
public final class DemoralizedEffect extends MobEffect {
    private static final String ATTACK_DAMAGE_MODIFIER_UUID =
            "bc9219d2-8bb9-4ab1-b502-e0d3d674765d";

    public DemoralizedEffect() {
        super(MobEffectCategory.HARMFUL, 0x800000);
        addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ATTACK_DAMAGE_MODIFIER_UUID,
                -0.20D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}
