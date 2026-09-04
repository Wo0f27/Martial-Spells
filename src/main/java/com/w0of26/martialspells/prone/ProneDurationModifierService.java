package com.w0of26.martialspells.prone;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProneDurationModifierService {

    @FunctionalInterface
    public interface Provider {

        double getDurationMultiplier(
                LivingEntity entity
        );
    }

    private static final Map<ResourceLocation, Provider>
            PROVIDERS =
            new ConcurrentHashMap<>();

    private ProneDurationModifierService() {
    }

    public static void register(
            ResourceLocation id,
            Provider provider
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Prone duration provider id cannot be null"
            );
        }

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Prone duration provider cannot be null"
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

        PROVIDERS.remove(id);
    }

    public static double getDurationMultiplier(
            LivingEntity entity
    ) {
        if (entity == null) {
            return 1.0D;
        }

        double totalMultiplier = 1.0D;

        for (Map.Entry<ResourceLocation, Provider> entry
                : PROVIDERS.entrySet()) {

            double multiplier;

            try {
                multiplier =
                        entry.getValue()
                                .getDurationMultiplier(entity);

            } catch (RuntimeException exception) {

                MartialSpells.LOGGER.warn(
                        "Prone duration provider {} failed",
                        entry.getKey(),
                        exception
                );

                continue;
            }

            if (!Double.isFinite(multiplier)
                    || multiplier <= 0.0D) {

                MartialSpells.LOGGER.warn(
                        "Prone duration provider {} returned invalid multiplier {}",
                        entry.getKey(),
                        multiplier
                );

                continue;
            }

            totalMultiplier *= multiplier;
        }

        return totalMultiplier;
    }

    public static int getDurationTicks(
            LivingEntity entity,
            int baseTicks
    ) {
        int safeBaseTicks =
                Math.max(
                        1,
                        baseTicks
                );

        double modifiedTicks =
                safeBaseTicks
                        * getDurationMultiplier(entity);

        if (!Double.isFinite(modifiedTicks)
                || modifiedTicks >= Integer.MAX_VALUE) {

            return Integer.MAX_VALUE;
        }

        return Math.max(
                1,
                (int) Math.round(modifiedTicks)
        );
    }
}