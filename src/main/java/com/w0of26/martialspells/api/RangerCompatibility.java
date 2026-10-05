package com.w0of26.martialspells.api;

import com.w0of26.martialspells.combat.SpiritWolfFixedCooldown;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Neutral, server-evaluated Ranger bonuses. Install providers during common
 * setup (enqueueWork); registration replaces the previous provider, not adds to
 * it. Providers must be deterministic, non-null and safe on the server thread.
 * They receive live players and must not mutate gameplay state while queried.
 */
public final class RangerCompatibility {
    private static volatile ToIntFunction<Player> barrageProvider = player -> 0;

    private RangerCompatibility() {}

    /**
     * Supply 0..127 extra distinct penetrable targets, added to the weapon's
     * baseline pierce level at each successful Barrage ammunition load. Default
     * is zero. One extra target increments pierce level once; the final level
     * saturates at 127. Multiple registrations are replacement, not stacking.
     */
    public static void setBarrageExtraPenetrableTargetsProvider(ToIntFunction<Player> provider) {
        barrageProvider = Objects.requireNonNull(provider);
    }

    /** Queried by Barrage only, on the server thread before arrow creation. */
    public static int extraBarragePenetrableTargets(Player player) {
        if (player.level().isClientSide) {
            throw new IllegalStateException("Barrage compatibility is server-only");
        }
        int extraTargets = barrageProvider.applyAsInt(player);
        if (extraTargets < 0 || extraTargets > 127) {
            throw new IllegalArgumentException("Barrage extra targets must be 0..127");
        }
        return extraTargets;
    }

    /**
     * Supply only 0 or -100 ticks. Spirit Wolf's post-reduction cooldown is
     * replaced by 900 + adjustment, yielding exactly 900 or 800 ticks, never
     * ordinary cooldown reduction. Default is zero. Evaluated at cooldown add
     * on the server thread. Registration replaces the previous provider.
     */
    public static void setSpiritWolfCooldownAdjustmentProvider(ToIntFunction<Player> provider) {
        SpiritWolfFixedCooldown.setAdjustmentProvider(Objects.requireNonNull(provider));
    }
}
