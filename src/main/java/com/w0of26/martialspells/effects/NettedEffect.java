package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Frozen Rogues net_trap status effect, displayed as "Netted". */
public final class NettedEffect extends MobEffect {
    private static final String MOVEMENT_SPEED_MODIFIER_UUID =
            "a49f4a42-d234-44d4-b982-d3c1fa555ae7";
    private static final String KNOCKBACK_RESISTANCE_MODIFIER_UUID =
            "309e25cd-2538-43b7-949a-0c05f1f81085";

    public NettedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B7355);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER_UUID,
                -2.0D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK_RESISTANCE_MODIFIER_UUID,
                100.0D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}
