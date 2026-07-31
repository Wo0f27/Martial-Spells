package com.w0of26.martialspells.damage;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GuardiansCovenantRedirectManager {
    /*
     * Minimum raw redirected-damage budget replenished each second.
     */
    private static final float MINIMUM_CAPACITY = 40.0F;

    /*
     * Tanks with more than 160 maximum health receive a larger
     * redirect capacity:
     *
     * capacity = max(40, maxHealth * 0.25)
     */
    private static final float MAX_HEALTH_MULTIPLIER = 0.25F;

    /*
     * Server-thread-only state. We deliberately store UUIDs rather
     * than ServerPlayer references to avoid retaining disconnected
     * player objects.
     */
    private static final Map<UUID, RedirectState> STATES =
            new HashMap<>();

    private GuardiansCovenantRedirectManager() {
    }

    /**
     * Creates or replaces the redirect state for a new Covenant.
     *
     * The bucket begins completely full.
     */
    public static void start(ServerPlayer tank) {
        float capacity = calculateCapacity(tank);

        STATES.put(
                tank.getUUID(),
                new RedirectState(capacity)
        );
    }

    public static void stop(ServerPlayer tank) {
        stop(tank.getUUID());
    }

    public static void stop(UUID tankUuid) {
        STATES.remove(tankUuid);
    }

    public static void clear() {
        STATES.clear();
    }

    public static boolean hasState(ServerPlayer tank) {
        return STATES.containsKey(tank.getUUID());
    }

    /**
     * Reserves redirect budget and queues the accepted amount for
     * one aggregated damage application at the end of this tick.
     *
     * @param tank               Covenant caster
     * @param requestedRawDamage raw damage requested by an ally hit
     * @return the amount actually reserved and queued
     */
    public static float queueRedirect(
            ServerPlayer tank,
            float requestedRawDamage
    ) {
        if (!Float.isFinite(requestedRawDamage)
                || requestedRawDamage <= 0.0F) {
            return 0.0F;
        }

        RedirectState state =
                STATES.get(tank.getUUID());

        if (state == null || !isValidTank(tank)) {
            return 0.0F;
        }

        return state.reserveAndQueue(
                requestedRawDamage
        );
    }

    /**
     * Runs once at the end of each server-side tank tick.
     *
     * It validates the Covenant, refills the token bucket, and
     * applies all redirected damage collected during the tick as
     * one mitigable damage call.
     */
    public static void tick(ServerPlayer tank) {
        RedirectState state =
                STATES.get(tank.getUUID());

        /*
         * Static manager state is cleared when the server stops, while
         * active potion effects can survive a reconnect or world restart.
         * Reconstruct the state if the tank effect still exists.
         */
        if (state == null) {
            if (isValidTank(tank)) {
                start(tank);
            }

            return;
        }

        if (!isValidTank(tank)) {
            stop(tank);
            return;
        }

        float queuedDamage =
                state.takeQueuedDamage();

        state.refill();

        if (queuedDamage <= 0.0F) {
            return;
        }

        tank.hurt(
                MartialDamageTypes
                        .guardiansCovenantRedirect(
                                tank.level()
                        ),
                queuedDamage
        );
    }

    public static float getCapacity(ServerPlayer tank) {
        RedirectState state =
                STATES.get(tank.getUUID());

        return state == null
                ? 0.0F
                : state.capacity;
    }

    public static float getAvailableBudget(
            ServerPlayer tank
    ) {
        RedirectState state =
                STATES.get(tank.getUUID());

        return state == null
                ? 0.0F
                : state.availableBudget;
    }

    public static float getQueuedDamage(
            ServerPlayer tank
    ) {
        RedirectState state =
                STATES.get(tank.getUUID());

        return state == null
                ? 0.0F
                : state.queuedDamage;
    }

    private static boolean isValidTank(
            ServerPlayer tank
    ) {
        return tank.isAlive()
                && !tank.isSpectator()
                && tank.hasEffect(
                MartialEffectRegistry
                        .GUARDIANS_COVENANT_TANK
                        .get()
        );
    }

    private static float calculateCapacity(
            ServerPlayer tank
    ) {
        float scaledCapacity =
                tank.getMaxHealth()
                        * MAX_HEALTH_MULTIPLIER;

        if (!Float.isFinite(scaledCapacity)) {
            return MINIMUM_CAPACITY;
        }

        return Math.max(
                MINIMUM_CAPACITY,
                scaledCapacity
        );
    }

    private static final class RedirectState {
        private final float capacity;
        private final float refillPerTick;

        private float availableBudget;
        private float queuedDamage;

        private RedirectState(float capacity) {
            this.capacity = capacity;
            this.refillPerTick =
                    capacity / 20.0F;

            /*
             * A new Covenant starts with a full bucket.
             */
            this.availableBudget = capacity;
            this.queuedDamage = 0.0F;
        }

        private float reserveAndQueue(
                float requestedDamage
        ) {
            float acceptedDamage =
                    Math.min(
                            requestedDamage,
                            availableBudget
                    );

            availableBudget -= acceptedDamage;
            queuedDamage += acceptedDamage;

            return acceptedDamage;
        }

        private float takeQueuedDamage() {
            float result = queuedDamage;
            queuedDamage = 0.0F;
            return result;
        }

        private void refill() {
            availableBudget =
                    Math.min(
                            capacity,
                            availableBudget
                                    + refillPerTick
                    );
        }
    }
}