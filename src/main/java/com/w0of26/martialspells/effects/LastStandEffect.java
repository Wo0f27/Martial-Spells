package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frozen Rogues Last Stand effect.
 *
 * <p>The executable upstream config grants +20% base max health and +20% base
 * knockback resistance per effective stack. A stale upstream comment mentions
 * damage reduction, but no damage-taken modifier is actually registered; this
 * translation follows the executable config.</p>
 */
public final class LastStandEffect extends MobEffect {
    private static final String MAX_HEALTH_MODIFIER_UUID =
            "73b1d943-6c77-4e8f-a622-390b1eeedc89";
    private static final String KNOCKBACK_RESISTANCE_MODIFIER_UUID =
            "b86f5f4a-133d-4a74-b3f9-3986e2258347";

    public LastStandEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xCC0000);

        addAttributeModifier(
                Attributes.MAX_HEALTH,
                MAX_HEALTH_MODIFIER_UUID,
                0.20D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK_RESISTANCE_MODIFIER_UUID,
                0.20D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}
