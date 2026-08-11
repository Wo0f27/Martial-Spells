package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class StillnessOfMindEffect
        extends MobEffect {

    private static final UUID ARMOR_UUID =
            UUID.fromString(
                    "80605476-9318-44c3-a885-500677bbb6d6"
            );

    private static final UUID TOUGHNESS_UUID =
            UUID.fromString(
                    "d40c30bd-d46e-47e8-b382-a9929cdf83ed"
            );

    /**
     * -0.50 with MULTIPLY_TOTAL means the final value
     * is multiplied by 0.50.
     */
    private static final double DEFENSE_MULTIPLIER =
            -0.50D;

    public StillnessOfMindEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0x8E6B00
        );
    }

    @Override
    public void addAttributeModifiers(
            LivingEntity entity,
            AttributeMap attributes,
            int amplifier
    ) {
        addTransientModifier(
                entity,
                Attributes.ARMOR,
                ARMOR_UUID,
                "Stillness of Mind armor penalty"
        );

        addTransientModifier(
                entity,
                Attributes.ARMOR_TOUGHNESS,
                TOUGHNESS_UUID,
                "Stillness of Mind toughness penalty"
        );
    }

    private static void addTransientModifier(
            LivingEntity entity,
            Attribute attribute,
            UUID uuid,
            String name
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
                        DEFENSE_MULTIPLIER,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                )
        );
    }

    @Override
    public void removeAttributeModifiers(
            LivingEntity entity,
            AttributeMap attributes,
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
    }

    private static void removeModifier(
            LivingEntity entity,
            Attribute attribute,
            UUID uuid
    ) {
        var instance =
                entity.getAttribute(attribute);

        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }
}