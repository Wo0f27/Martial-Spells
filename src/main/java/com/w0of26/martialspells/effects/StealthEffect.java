package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frozen Rogues Stealth effect used by Vanish.
 *
 * <p>The source applies a -50% MULTIPLY_BASE movement-speed modifier. Enemy
 * targeting and invisibility presentation are implemented by the R5 event and
 * mixin hooks so this effect remains a small, synchronized gameplay marker.</p>
 */
public final class StealthEffect extends MobEffect {
    private static final String MOVEMENT_SPEED_MODIFIER_UUID =
            "1d55f8f1-4c62-4ca5-8bf7-8c78b82d58b5";

    public StealthEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAAAAAA);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER_UUID,
                -0.5D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}
