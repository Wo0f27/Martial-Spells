package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class DiamondBodyEffect
        extends MobEffect {

    private static final UUID ARMOR_UUID =
            UUID.fromString(
                    "13bdfe7b-64e8-42ca-a1f5-a9b32d1573c1"
            );

    private static final UUID TOUGHNESS_UUID =
            UUID.fromString(
                    "d57af8e5-f5f6-4fbd-94ee-15e93bfc074b"
            );

    private static final UUID KNOCKBACK_UUID =
            UUID.fromString(
                    "269c619d-7a0f-44ac-9681-d01a889e34bb"
            );

    private static final double[] ARMOR_VALUES = {
            4.0D,
            8.0D,
            12.0D,
            16.0D,
            20.0D
    };

    private static final double[] TOUGHNESS_VALUES = {
            2.0D,
            4.0D,
            6.0D,
            9.0D,
            12.0D
    };

    private static final double[] KNOCKBACK_VALUES = {
            0.08D,
            0.16D,
            0.24D,
            0.32D,
            0.40D
    };

    private static final float[] DAMAGE_REDUCTION_VALUES = {
            0.20F,
            0.25F,
            0.30F,
            0.35F,
            0.40F
    };

    public DiamondBodyEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0x8E6B00
        );
    }

    private static int clampAmplifier(
            int amplifier
    ) {
        return Math.max(
                0,
                Math.min(
                        amplifier,
                        ARMOR_VALUES.length - 1
                )
        );
    }

    @Override
    public void addAttributeModifiers(
            LivingEntity entity,
            net.minecraft.world.entity.ai.attributes.AttributeMap attributes,
            int amplifier
    ) {
        int index = clampAmplifier(amplifier);

        addTransientModifier(
                entity,
                Attributes.ARMOR,
                ARMOR_UUID,
                "Diamond Body armor",
                ARMOR_VALUES[index]
        );

        addTransientModifier(
                entity,
                Attributes.ARMOR_TOUGHNESS,
                TOUGHNESS_UUID,
                "Diamond Body toughness",
                TOUGHNESS_VALUES[index]
        );

        addTransientModifier(
                entity,
                Attributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK_UUID,
                "Diamond Body knockback resistance",
                KNOCKBACK_VALUES[index]
        );
    }

    private static void addTransientModifier(
            LivingEntity entity,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID uuid,
            String name,
            double amount
    ) {
        var instance =
                entity.getAttribute(attribute);

        if (instance == null) {
            return;
        }

        instance.removeModifier(uuid);

        instance.addTransientModifier(
                new AttributeModifier(
                        uuid,
                        name,
                        amount,
                        AttributeModifier.Operation.ADDITION
                )
        );
    }

    @Override
    public void removeAttributeModifiers(
            LivingEntity entity,
            net.minecraft.world.entity.ai.attributes.AttributeMap attributes,
            int amplifier
    ) {
        removeModifier(
                entity,
                Attributes.ARMOR,
                ARMOR_UUID
        );

        removeModifier(
                entity,
                Attributes.ARMOR_TOUGHNESS,
                TOUGHNESS_UUID
        );

        removeModifier(
                entity,
                Attributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK_UUID
        );
    }

    private static void removeModifier(
            LivingEntity entity,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID uuid
    ) {
        var instance =
                entity.getAttribute(attribute);

        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }

    public static float getDamageReduction(
            int amplifier
    ) {
        int index = clampAmplifier(amplifier);

        return DAMAGE_REDUCTION_VALUES[index];
    }

    public static float getDamageMultiplier(
            int amplifier
    ) {
        return 1.0F
                - getDamageReduction(amplifier);
    }
}