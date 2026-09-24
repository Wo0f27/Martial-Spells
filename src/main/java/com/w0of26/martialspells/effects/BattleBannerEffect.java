package com.w0of26.martialspells.effects;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frozen Paladins Battle Banner buff translated to target-system attributes.
 *
 * <p>The source applies +40% attack speed, Healing Haste, knockback
 * resistance, and ranged haste. Iron's splits spell haste into cast-time and
 * cooldown reduction, so both receive the same +40% MULTIPLY_BASE modifier.</p>
 */
public final class BattleBannerEffect extends MobEffect {
    public static final double SOURCE_MULTIPLIER = 0.40D;

    public BattleBannerEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0x66CCFF
        );

        addAttributeModifier(
                Attributes.ATTACK_SPEED,
                "8a6eef4f-6de9-4f0a-bf3c-cb39a4ff8b01",
                SOURCE_MULTIPLIER,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                "8a6eef4f-6de9-4f0a-bf3c-cb39a4ff8b02",
                SOURCE_MULTIPLIER,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        addAttributeModifier(
                AttributeRegistry.CAST_TIME_REDUCTION.get(),
                "8a6eef4f-6de9-4f0a-bf3c-cb39a4ff8b03",
                SOURCE_MULTIPLIER,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        addAttributeModifier(
                AttributeRegistry.COOLDOWN_REDUCTION.get(),
                "8a6eef4f-6de9-4f0a-bf3c-cb39a4ff8b04",
                SOURCE_MULTIPLIER,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}
