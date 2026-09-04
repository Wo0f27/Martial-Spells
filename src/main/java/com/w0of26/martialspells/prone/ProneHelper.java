package com.w0of26.martialspells.prone;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class ProneHelper {

    private ProneHelper() {
    }

    public static boolean isProne(
            LivingEntity entity
    ) {
        return entity != null
                && entity.hasEffect(
                MartialEffectRegistry
                        .PRONE
                        .get()
        );
    }

    public static boolean applyProne(
            LivingEntity entity
    ) {
        return applyProne(
                entity,
                ProneConstants
                        .DEFAULT_DURATION_TICKS
        );
    }

    public static boolean applyProne(
            LivingEntity entity,
            int durationTicks
    ) {
        if (entity == null
                || !entity.isAlive()) {
            return false;
        }

        int safeDuration =
                Math.max(
                        1,
                        durationTicks
                );

        return entity.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry
                                .PRONE
                                .get(),
                        safeDuration,
                        0,
                        false,
                        true,
                        true
                )
        );
    }
}