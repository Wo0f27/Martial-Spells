package com.w0of26.martialspells.prone;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows external systems to modify the recovery duration
 * applied after an entity leaves Prone.
 *
 * Martial Spells owns the Prone mechanic itself.
 * Other mods may register generic recovery multipliers
 * without Martial Spells needing to know what caused them.
 */
public final class ProneRecoveryModifierService {

    @FunctionalInterface
    public interface Provider {

        /**
         * Returns the recovery-duration multiplier for
         * the supplied entity.
         *
         * Examples:
         *
         * 1.0 = unchanged
         * 0.5 = half recovery duration
         * 0.0 = no recovery penalty
         * 2.0 = double recovery duration
         */
        double getRecoveryMultiplier(
                LivingEntity entity
        );
    }

    private static final Map<ResourceLocation, Provider>
            PROVIDERS =
            new ConcurrentHashMap<>();

    private ProneRecoveryModifierService() {
    }

    public static void register(
            ResourceLocation id,
            Provider provider
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Prone recovery provider id cannot be null"
            );
        }

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Prone recovery provider cannot be null"
            );
        }

        PROVIDERS.put(
                id,
                provider
        );
    }

    public static void unregister(
            ResourceLocation id
    ) {
        if (id == null) {
            return;
        }

        PROVIDERS.remove(
                id
        );
    }

    public static double getRecoveryMultiplier(
            LivingEntity entity
    ) {
        if (entity == null) {
            return 1.0D;
        }

        double totalMultiplier =
                1.0D;

        for (Map.Entry<ResourceLocation, Provider> entry
                : PROVIDERS.entrySet()) {

            double multiplier;

            try {
                multiplier =
                        entry.getValue()
                                .getRecoveryMultiplier(
                                        entity
                                );
            } catch (RuntimeException exception) {

                MartialSpells.LOGGER.warn(
                        "Prone recovery provider {} failed",
                        entry.getKey(),
                        exception
                );

                continue;
            }

            if (!Double.isFinite(multiplier)
                    || multiplier < 0.0D) {

                MartialSpells.LOGGER.warn(
                        "Prone recovery provider {} returned invalid multiplier {}",
                        entry.getKey(),
                        multiplier
                );

                continue;
            }

            totalMultiplier *=
                    multiplier;
        }

        return totalMultiplier;
    }

    public static int getRecoveryTicks(
            LivingEntity entity,
            int baseTicks
    ) {
        int safeBaseTicks =
                Math.max(
                        0,
                        baseTicks
                );

        if (safeBaseTicks == 0) {
            return 0;
        }

        double modifiedTicks =
                safeBaseTicks
                        * getRecoveryMultiplier(
                        entity
                );

        if (modifiedTicks <= 0.0D) {
            return 0;
        }

        if (!Double.isFinite(modifiedTicks)
                || modifiedTicks
                >= Integer.MAX_VALUE) {

            return Integer.MAX_VALUE;
        }

        return Math.max(
                1,
                (int) Math.round(
                        modifiedTicks
                )
        );
    }
}