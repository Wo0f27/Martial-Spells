package com.w0of26.martialspells.effects;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public final class DiamondHeartEffect extends MobEffect {

    public static final int DURATION_TICKS = 20 * 5;
    public static final int IMMUNITY_TICKS = 20 * 2;

    private static final float ABSORPTION_PERCENT = 0.20F;

    private static final String REMAINING_ABSORPTION_KEY =
            MartialSpells.MOD_ID
                    + ".diamond_heart_remaining_absorption";

    private static final String LAST_ABSORPTION_KEY =
            MartialSpells.MOD_ID
                    + ".diamond_heart_last_absorption";

    public DiamondHeartEffect() {
        super(
                MobEffectCategory.BENEFICIAL,
                0xB9F2FF
        );
    }

    public static boolean hasImmunity(
            MobEffectInstance effect
    ) {
        if (effect == null) {
            return false;
        }

        /*
         * The effect lasts 100 ticks.
         * Durations 100 through 61 are the first 40 ticks.
         */
        return effect.getDuration()
                > DURATION_TICKS - IMMUNITY_TICKS;
    }

    @Override
    public void addAttributeModifiers(
            LivingEntity entity,
            AttributeMap attributes,
            int amplifier
    ) {
        super.addAttributeModifiers(
                entity,
                attributes,
                amplifier
        );

        if (entity.level().isClientSide) {
            return;
        }

        float grantedAbsorption =
                entity.getMaxHealth()
                        * ABSORPTION_PERCENT;

        float newAbsorption =
                entity.getAbsorptionAmount()
                        + grantedAbsorption;

        CompoundTag data =
                entity.getPersistentData();

        data.putFloat(
                REMAINING_ABSORPTION_KEY,
                grantedAbsorption
        );

        data.putFloat(
                LAST_ABSORPTION_KEY,
                newAbsorption
        );

        entity.setAbsorptionAmount(
                newAbsorption
        );
    }

    @Override
    public boolean isDurationEffectTick(
            int duration,
            int amplifier
    ) {
        return true;
    }

    @Override
    public void applyEffectTick(
            LivingEntity entity,
            int amplifier
    ) {
        if (entity.level().isClientSide) {
            return;
        }

        updateRemainingAbsorption(entity);
    }

    @Override
    public void removeAttributeModifiers(
            LivingEntity entity,
            AttributeMap attributes,
            int amplifier
    ) {
        super.removeAttributeModifiers(
                entity,
                attributes,
                amplifier
        );

        if (entity.level().isClientSide) {
            return;
        }

        updateRemainingAbsorption(entity);

        CompoundTag data =
                entity.getPersistentData();

        float remainingAbsorption =
                data.getFloat(
                        REMAINING_ABSORPTION_KEY
                );

        entity.setAbsorptionAmount(
                Math.max(
                        0.0F,
                        entity.getAbsorptionAmount()
                                - remainingAbsorption
                )
        );

        data.remove(
                REMAINING_ABSORPTION_KEY
        );

        data.remove(
                LAST_ABSORPTION_KEY
        );
    }

    private static void updateRemainingAbsorption(
            LivingEntity entity
    ) {
        CompoundTag data =
                entity.getPersistentData();

        if (!data.contains(
                REMAINING_ABSORPTION_KEY
        )) {
            return;
        }

        float currentAbsorption =
                entity.getAbsorptionAmount();

        float previousAbsorption =
                data.getFloat(
                        LAST_ABSORPTION_KEY
                );

        float remainingGrant =
                data.getFloat(
                        REMAINING_ABSORPTION_KEY
                );

        /*
         * Attribute any absorption lost since the previous
         * tick to Diamond Heart's temporary barrier first.
         */
        if (currentAbsorption
                < previousAbsorption) {

            float consumed =
                    previousAbsorption
                            - currentAbsorption;

            remainingGrant =
                    Math.max(
                            0.0F,
                            remainingGrant
                                    - consumed
                    );

            data.putFloat(
                    REMAINING_ABSORPTION_KEY,
                    remainingGrant
            );
        }

        data.putFloat(
                LAST_ABSORPTION_KEY,
                currentAbsorption
        );
    }
}