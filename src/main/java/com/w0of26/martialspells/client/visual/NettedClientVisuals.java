package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/**
 * Client mirror of the minimum synchronized state upstream Spell Engine keeps
 * for model-backed status-effect visuals.
 */
public final class NettedClientVisuals {
    public record State(long appliedAtWorldTime, long expiresAtWorldTime) {
        public float age(long clientWorldTime, float partialTick) {
            return Math.max(
                    0.0F,
                    (clientWorldTime - appliedAtWorldTime) + partialTick
            );
        }
    }

    private static final Map<Integer, State> ACTIVE = new HashMap<>();

    private NettedClientVisuals() {}

    public static void activate(
            int entityId,
            long appliedAtWorldTime,
            int durationTicks
    ) {
        long expiresAt = appliedAtWorldTime + durationTicks;
        State existing = ACTIVE.get(entityId);

        /*
         * Upstream Synchronized preserves the original appliedAtWorldTime while
         * an effect is already active. Do the same so refreshing Netted does not
         * restart the drop/snap animation.
         */
        if (existing != null
                && existing.expiresAtWorldTime() > appliedAtWorldTime) {
            ACTIVE.put(
                    entityId,
                    new State(
                            existing.appliedAtWorldTime(),
                            Math.max(existing.expiresAtWorldTime(), expiresAt)
                    )
            );
        } else {
            ACTIVE.put(
                    entityId,
                    new State(appliedAtWorldTime, expiresAt)
            );
        }

        MartialSpells.LOGGER.info(
                "W4 Netted visual sync received: entityId={} appliedAt={} duration={}",
                entityId,
                appliedAtWorldTime,
                durationTicks
        );
    }

    public static State stateFor(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.remove(entityId);
            return null;
        }

        State state = ACTIVE.get(entityId);
        if (state == null) {
            return null;
        }

        long now = minecraft.level.getGameTime();
        if (now >= state.expiresAtWorldTime()) {
            ACTIVE.remove(entityId);
            return null;
        }

        return state;
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
