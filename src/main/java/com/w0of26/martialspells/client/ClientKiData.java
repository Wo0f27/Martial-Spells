package com.w0of26.martialspells.client;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.util.Mth;

/**
 * Client-side display copy of the player's Ki.
 *
 * This is not authoritative. The server controls the real Ki values.
 */
public final class ClientKiData {
    private static int currentKi;
    private static int maximumKi;

    private ClientKiData() {
    }

    public static int getCurrentKi() {
        return currentKi;
    }

    public static int getMaximumKi() {
        return maximumKi;
    }

    public static void set(
            int newCurrentKi,
            int newMaximumKi
    ) {
        maximumKi = Math.max(
                0,
                newMaximumKi
        );

        currentKi = Mth.clamp(
                newCurrentKi,
                0,
                maximumKi
        );

    }

    public static void clear() {
        currentKi = 0;
        maximumKi = 0;
    }
}