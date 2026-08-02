package com.w0of26.martialspells.ki;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.item.MonkCodexItem;
import com.w0of26.martialspells.registry.MartialItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Derives a player's maximum Ki from their equipped Monk Codex.
 *
 * The value is only changed when the desired maximum differs from
 * the stored maximum, so no packet is sent during ordinary ticks.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MonkCodexKiEvents {
    private MonkCodexKiEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean codexEquipped =
                MartialItemRegistry.MONK_CODEX
                        .get()
                        .isEquippedBy(serverPlayer);

        int desiredMaximumKi =
                codexEquipped
                        ? MonkCodexItem.MAXIMUM_KI
                        : 0;

        int currentMaximumKi =
                KiHelper.getMaximumKi(serverPlayer);

        if (currentMaximumKi == desiredMaximumKi) {
            return;
        }

        KiHelper.setMaximumKi(
                serverPlayer,
                desiredMaximumKi
        );
    }
}