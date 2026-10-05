package com.w0of26.martialspells.api;

import net.minecraft.world.entity.player.Player;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * Neutral ability adjustments. Register once during common setup (enqueueWork).
 * Setters replace providers; callbacks must be pure and server-thread safe.
 * External consumers own qualification. No equipment or mod identity is queried here.
 * Unsupported values fail explicitly; zero preserves the accepted ability behavior.
 */
public final class AbilityCompatibility {
    private AbilityCompatibility() {}

    private static volatile ToDoubleFunction<Player> circleOfHealingPowerProvider = player -> 0.0;

    /** Accepts only 0.0 or 0.10; replacement registration, never stacking. */
    public static void setCircleOfHealingPowerAdjustmentProvider(ToDoubleFunction<Player> provider) {
        circleOfHealingPowerProvider = Objects.requireNonNull(provider);
    }

    /** Server-only query at the ability's documented evaluation point. */
    public static double getCircleOfHealingPowerAdjustment(Player player) {
        requireServer(player);
        double value = circleOfHealingPowerProvider.applyAsDouble(player);
        if (value != 0.0 && value != 0.10) {
            throw new IllegalArgumentException("CircleOfHealingPower adjustment must be 0.0 or 0.10");
        }
        return value == 0.0 ? 0.0 : value;
    }

    private static volatile ToDoubleFunction<Player> penanceCriticalChanceProvider = player -> 0.0;

    /** Accepts only 0.0 or 0.05; replacement registration, never stacking. */
    public static void setPenanceCriticalChanceAdjustmentProvider(ToDoubleFunction<Player> provider) {
        penanceCriticalChanceProvider = Objects.requireNonNull(provider);
    }

    /** Server-only query at the ability's documented evaluation point. */
    public static double getPenanceCriticalChanceAdjustment(Player player) {
        requireServer(player);
        double value = penanceCriticalChanceProvider.applyAsDouble(player);
        if (value != 0.0 && value != 0.05) {
            throw new IllegalArgumentException("PenanceCriticalChance adjustment must be 0.0 or 0.05");
        }
        return value == 0.0 ? 0.0 : value;
    }

    private static volatile ToIntFunction<Player> divineProtectionCooldownProvider = player -> 0;

    /** Accepts only 0 or -100; replacement registration, never stacking. */
    public static void setDivineProtectionCooldownAdjustmentProvider(ToIntFunction<Player> provider) {
        divineProtectionCooldownProvider = Objects.requireNonNull(provider);
    }

    /** Server-only query at the ability's documented evaluation point. */
    public static int getDivineProtectionCooldownAdjustment(Player player) {
        requireServer(player);
        int value = divineProtectionCooldownProvider.applyAsInt(player);
        if (value != 0 && value != -100) {
            throw new IllegalArgumentException("DivineProtectionCooldown adjustment must be 0 or -100");
        }
        return value == 0 ? 0 : value;
    }

    private static volatile ToIntFunction<Player> judgementCooldownProvider = player -> 0;

    /** Accepts only 0 or -60; replacement registration, never stacking. */
    public static void setJudgementCooldownAdjustmentProvider(ToIntFunction<Player> provider) {
        judgementCooldownProvider = Objects.requireNonNull(provider);
    }

    /** Server-only query at the ability's documented evaluation point. */
    public static int getJudgementCooldownAdjustment(Player player) {
        requireServer(player);
        int value = judgementCooldownProvider.applyAsInt(player);
        if (value != 0 && value != -60) {
            throw new IllegalArgumentException("JudgementCooldown adjustment must be 0 or -60");
        }
        return value == 0 ? 0 : value;
    }

    private static volatile ToIntFunction<Player> chargeCooldownProvider = player -> 0;

    /** Accepts only 0 or -40; replacement registration, never stacking. */
    public static void setChargeCooldownAdjustmentProvider(ToIntFunction<Player> provider) {
        chargeCooldownProvider = Objects.requireNonNull(provider);
    }

    /** Server-only query at the ability's documented evaluation point. */
    public static int getChargeCooldownAdjustment(Player player) {
        requireServer(player);
        int value = chargeCooldownProvider.applyAsInt(player);
        if (value != 0 && value != -40) {
            throw new IllegalArgumentException("ChargeCooldown adjustment must be 0 or -40");
        }
        return value == 0 ? 0 : value;
    }

    private static volatile ToDoubleFunction<Player> shoutDamageMultiplierProvider = player -> 0.0;

    /** Accepts only 0.0 or 4.0; replacement registration, never stacking. */
    public static void setShoutDamageMultiplierAdjustmentProvider(ToDoubleFunction<Player> provider) {
        shoutDamageMultiplierProvider = Objects.requireNonNull(provider);
    }

    /** Server-only query at the ability's documented evaluation point. */
    public static double getShoutDamageMultiplierAdjustment(Player player) {
        requireServer(player);
        double value = shoutDamageMultiplierProvider.applyAsDouble(player);
        if (value != 0.0 && value != 4.0) {
            throw new IllegalArgumentException("ShoutDamageMultiplier adjustment must be 0.0 or 4.0");
        }
        return value == 0.0 ? 0.0 : value;
    }

    private static volatile ToIntFunction<Player> shadowStepCooldownProvider = player -> 0;

    /** Accepts only 0 or -60; replacement registration, never stacking. */
    public static void setShadowStepCooldownAdjustmentProvider(ToIntFunction<Player> provider) {
        shadowStepCooldownProvider = Objects.requireNonNull(provider);
    }

    /** Server-only query at the ability's documented evaluation point. */
    public static int getShadowStepCooldownAdjustment(Player player) {
        requireServer(player);
        int value = shadowStepCooldownProvider.applyAsInt(player);
        if (value != 0 && value != -60) {
            throw new IllegalArgumentException("ShadowStepCooldown adjustment must be 0 or -60");
        }
        return value == 0 ? 0 : value;
    }

    private static volatile ToDoubleFunction<Player> bearTrapPowerProvider = player -> 0.0;

    /** Accepts only 0.0 or 0.10; replacement registration, never stacking. */
    public static void setBearTrapPowerAdjustmentProvider(ToDoubleFunction<Player> provider) {
        bearTrapPowerProvider = Objects.requireNonNull(provider);
    }

    /** Server-only query at the ability's documented evaluation point. */
    public static double getBearTrapPowerAdjustment(Player player) {
        requireServer(player);
        double value = bearTrapPowerProvider.applyAsDouble(player);
        if (value != 0.0 && value != 0.10) {
            throw new IllegalArgumentException("BearTrapPower adjustment must be 0.0 or 0.10");
        }
        return value == 0.0 ? 0.0 : value;
    }

    private static void requireServer(Player player) {
        Objects.requireNonNull(player);
        if (player.level().isClientSide) {
            throw new IllegalStateException("Ability compatibility is server-only");
        }
    }
}
