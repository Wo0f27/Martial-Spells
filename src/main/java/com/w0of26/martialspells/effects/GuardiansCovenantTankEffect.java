package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class GuardiansCovenantTankEffect extends MobEffect {
    private static final int MAX_AMPLIFIER = 4;

    private static final double[] ARMOR_BONUSES = {
            4.0D,
            5.0D,
            6.0D,
            7.0D,
            8.0D
    };

    private static final double[] TOUGHNESS_BONUSES = {
            2.0D,
            2.5D,
            3.0D,
            3.5D,
            4.0D
    };

    private static final UUID ARMOR_MODIFIER_ID =
            UUID.fromString(
                    "7f783a5e-074d-4d5f-9385-1b514cb99b86"
            );

    private static final UUID TOUGHNESS_MODIFIER_ID =
            UUID.fromString(
                    "52fbeb27-c9ec-4c78-9694-67d1a6dbf3cd"
            );

    public GuardiansCovenantTankEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0xE8C96A
        );

        /*
         * The registered amount is replaced by
         * getAttributeModifierValue() according to the effect
         * amplifier.
         */
        addAttributeModifier(
                Attributes.ARMOR,
                ARMOR_MODIFIER_ID.toString(),
                0.0D,
                AttributeModifier.Operation.ADDITION
        );

        addAttributeModifier(
                Attributes.ARMOR_TOUGHNESS,
                TOUGHNESS_MODIFIER_ID.toString(),
                0.0D,
                AttributeModifier.Operation.ADDITION
        );
    }

    private static int clampAmplifier(int amplifier) {
        return Math.max(
                0,
                Math.min(amplifier, MAX_AMPLIFIER)
        );
    }

    @Override
    public double getAttributeModifierValue(
            int amplifier,
            AttributeModifier modifier
    ) {
        int index = clampAmplifier(amplifier);

        if (ARMOR_MODIFIER_ID.equals(modifier.getId())) {
            return ARMOR_BONUSES[index];
        }

        if (TOUGHNESS_MODIFIER_ID.equals(modifier.getId())) {
            return TOUGHNESS_BONUSES[index];
        }

        return super.getAttributeModifierValue(
                amplifier,
                modifier
        );
    }

    public static double getArmorBonus(int spellLevel) {
        int index = Math.max(
                0,
                Math.min(spellLevel - 1, MAX_AMPLIFIER)
        );

        return ARMOR_BONUSES[index];
    }

    public static double getToughnessBonus(int spellLevel) {
        int index = Math.max(
                0,
                Math.min(spellLevel - 1, MAX_AMPLIFIER)
        );

        return TOUGHNESS_BONUSES[index];
    }
}