package com.w0of26.martialspells.visual;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class StillnessOfMindVfx {

    private static final int ACTIVE_INTERVAL_TICKS = 8;

    private StillnessOfMindVfx() {
    }

    /**
     * Initial burst when Stillness successfully activates.
     */
    public static void spawnActivation(
            ServerPlayer player
    ) {
        ServerLevel level = player.serverLevel();

        level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                24,
                0.45D,
                0.75D,
                0.45D,
                0.04D
        );
    }

    /**
     * Sparse aura while Stillness is active.
     *
     * Called every player tick, but only actually emits
     * particles once every ACTIVE_INTERVAL_TICKS.
     */
    public static void tickActive(
            ServerPlayer player
    ) {
        if (player.tickCount
                % ACTIVE_INTERVAL_TICKS != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();

        level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + 0.9D,
                player.getZ(),
                3,
                0.35D,
                0.65D,
                0.35D,
                0.015D
        );
    }

    /**
     * Short visual cue when Stillness ends normally,
     * is cleansed, or is terminated by Codex removal.
     */
    public static void spawnTermination(
            ServerPlayer player
    ) {
        ServerLevel level = player.serverLevel();

        level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                12,
                0.60D,
                0.55D,
                0.60D,
                0.025D
        );
    }
}