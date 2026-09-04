package com.w0of26.martialspells.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class ProneEffect
        extends MobEffect {

    private static final UUID MOVEMENT_SPEED_UUID =
            UUID.fromString(
                    "f8f92879-d288-4c2d-bffc-4cd290e30ee5"
            );

    /*
     * MULTIPLY_TOTAL -1.0 reduces effective walking speed
     * to zero without modifying the entity's base attribute.
     *
     * The transient modifier is removed automatically when
     * Prone ends.
     */
    private static final double MOVEMENT_SPEED_MULTIPLIER =
            -1.0D;

    public ProneEffect() {
        super(
                MobEffectCategory.HARMFUL,
                0x6B5A45
        );
    }

    @Override
    public void addAttributeModifiers(
            LivingEntity entity,
            AttributeMap attributes,
            int amplifier
    ) {
        var instance =
                entity.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (instance == null) {
            return;
        }

        instance.removeModifier(
                MOVEMENT_SPEED_UUID
        );

        instance.addTransientModifier(
                new AttributeModifier(
                        MOVEMENT_SPEED_UUID,
                        "Prone movement suppression",
                        MOVEMENT_SPEED_MULTIPLIER,
                        AttributeModifier.Operation
                                .MULTIPLY_TOTAL
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
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_UUID
        );
    }

    private static void removeModifier(
            LivingEntity entity,
            Attribute attribute,
            UUID uuid
    ) {
        var instance =
                entity.getAttribute(
                        attribute
                );

        if (instance != null) {
            instance.removeModifier(
                    uuid
            );
        }
    }
}