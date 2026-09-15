package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Source-faithful Slice & Dice attack-damage stack.
 *
 * <p>Vanilla MobEffect attribute modifiers scale their configured amount by
 * (amplifier + 1), so 0.1 MULTIPLY_BASE gives +10% at amplifier 0 through
 * +100% at amplifier 9, matching frozen Rogues.</p>
 */
public final class SliceAndDiceEffect extends MobEffect {
    private static final String ATTACK_DAMAGE_MODIFIER_UUID =
            "7c79e8cb-20f9-4d48-a883-9b3f9af51b12";

    public SliceAndDiceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x993333);
        addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ATTACK_DAMAGE_MODIFIER_UUID,
                0.1D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}
