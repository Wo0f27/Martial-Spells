package com.w0of26.martialspells.ki;

import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncKiPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.w0of26.martialspells.registry.MartialEffectRegistry;

import java.util.Optional;

/**
 * Provides controlled access to player Ki.
 *
 * The logical server is responsible for modifying Ki. Successful
 * server-side changes are synchronized to the affected client.
 */
public final class KiHelper {
    private KiHelper() {
    }

    private static Optional<KiData> getData(
            Player player
    ) {
        return player.getCapability(
                        MartialCapabilities.KI
                )
                .resolve();
    }

    private static boolean hasInfiniteKi(
            Player player
    ) {
        return player != null
                && player.hasEffect(
                MartialEffectRegistry
                        .STILLNESS_OF_MIND
                        .get()
        );
    }

    public static int getCurrentKi(
            Player player
    ) {
        return getData(player)
                .map(KiData::getCurrentKi)
                .orElse(0);
    }

    public static int getMaximumKi(
            Player player
    ) {
        return getData(player)
                .map(KiData::getMaximumKi)
                .orElse(0);
    }

    public static boolean setCurrentKi(
            Player player,
            int amount
    ) {
        Optional<KiData> optional =
                getData(player);

        if (optional.isEmpty()) {
            return false;
        }

        KiData data = optional.get();
        data.setCurrentKi(amount);

        syncIfServerPlayer(
                player,
                data
        );

        return true;
    }

    public static boolean setMaximumKi(
            Player player,
            int amount
    ) {
        Optional<KiData> optional =
                getData(player);

        if (optional.isEmpty()) {
            return false;
        }

        KiData data = optional.get();
        data.setMaximumKi(amount);

        syncIfServerPlayer(
                player,
                data
        );

        return true;
    }

    public static int addKi(
            Player player,
            int amount
    ) {
        Optional<KiData> optional =
                getData(player);

        if (optional.isEmpty()) {
            return 0;
        }

        KiData data = optional.get();
        int added = data.addKi(amount);

        syncIfServerPlayer(
                player,
                data
        );

        return added;
    }

    public static int removeKi(
            Player player,
            int amount
    ) {
        Optional<KiData> optional =
                getData(player);

        if (optional.isEmpty()) {
            return 0;
        }

        KiData data = optional.get();
        int removed = data.removeKi(amount);

        syncIfServerPlayer(
                player,
                data
        );

        return removed;
    }

    public static boolean consumeKi(
            Player player,
            int amount
    ) {
        /*
         * Stillness of Mind suspends Ki expenditure.
         *
         * The underlying Ki capability is deliberately left
         * untouched so the player returns to exactly the same
         * stored Ki when Stillness ends.
         */
        if (hasInfiniteKi(player)) {
            return true;
        }

        Optional<KiData> optional =
                getData(player);

        if (optional.isEmpty()) {
            return false;
        }

        KiData data = optional.get();

        if (!data.consumeKi(amount)) {
            return false;
        }

        syncIfServerPlayer(
                player,
                data
        );

        return true;
    }

    public static boolean hasKi(
            Player player,
            int amount
    ) {
        if (hasInfiniteKi(player)) {
            return true;
        }

        return getData(player)
                .map(data -> data.hasKi(amount))
                .orElse(false);
    }

    public static boolean fillKi(
            Player player
    ) {
        Optional<KiData> optional =
                getData(player);

        if (optional.isEmpty()) {
            return false;
        }

        KiData data = optional.get();
        data.fill();

        syncIfServerPlayer(
                player,
                data
        );

        return true;
    }

    public static boolean resetKi(
            Player player
    ) {
        Optional<KiData> optional =
                getData(player);

        if (optional.isEmpty()) {
            return false;
        }

        KiData data = optional.get();
        data.reset();

        syncIfServerPlayer(
                player,
                data
        );

        return true;
    }

    /**
     * Sends the player's existing Ki values without modifying them.
     */
    public static void sync(
            ServerPlayer player
    ) {
        getData(player).ifPresent(
                data -> syncIfServerPlayer(
                        player,
                        data
                )
        );
    }

    private static void syncIfServerPlayer(
            Player player,
            KiData data
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MartialNetwork.sendToPlayer(
                new SyncKiPacket(
                        data.getCurrentKi(),
                        data.getMaximumKi()
                ),
                serverPlayer
        );
    }
}