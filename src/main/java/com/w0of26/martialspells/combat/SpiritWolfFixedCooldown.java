package com.w0of26.martialspells.combat;

import net.minecraft.world.entity.player.Player;
import java.util.Objects;
import java.util.function.ToIntFunction;

/** Fixed target cooldown arithmetic; later equipment integrations supply only the adjustment. */
public final class SpiritWolfFixedCooldown {
    public static final int BASE_TICKS = 900;
    private static volatile ToIntFunction<Player> adjustmentProvider = player -> 0;

    private SpiritWolfFixedCooldown() {}

    /** A future equipment adapter may supply 100; ordinary CDR never enters this calculation. */
    public static int ticks(int equipmentReductionTicks) {
        if (equipmentReductionTicks != 0 && equipmentReductionTicks != 100) {
            throw new IllegalArgumentException("Only the neutral or 100-tick equipment adjustment is supported");
        }
        return BASE_TICKS - equipmentReductionTicks;
    }

    /** Neutral Armory-facing seam: 0 normally, -100 for a future authorized bonus. */
    public static void setAdjustmentProvider(ToIntFunction<Player> provider) {
        adjustmentProvider = Objects.requireNonNull(provider);
    }

    public static int forPlayer(Player player) {
        int adjustmentTicks = adjustmentProvider.applyAsInt(player);
        if (adjustmentTicks != 0 && adjustmentTicks != -100) {
            throw new IllegalArgumentException("Spirit Wolf adjustment must be 0 or -100 ticks");
        }
        return Math.max(1, BASE_TICKS + adjustmentTicks);
    }
}
