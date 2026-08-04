package com.w0of26.martialspells.ki;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.item.MonkCodexItem;
import com.w0of26.martialspells.registry.MartialItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Derives maximum Ki from the specific equipped Monk Codex stack.
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
        if (event.phase
                != TickEvent.Phase.END
                || !(event.player
                instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack equippedCodex =
                CuriosApi.getCuriosInventory(
                                serverPlayer
                        )
                        .resolve()
                        .flatMap(handler ->
                                handler.findFirstCurio(
                                        MartialItemRegistry
                                                .MONK_CODEX
                                                .get()
                                )
                        )
                        .map(SlotResult::stack)
                        .orElse(ItemStack.EMPTY);

        int desiredMaximumKi =
                equippedCodex.isEmpty()
                        ? 0
                        : MonkCodexItem
                        .getMaximumKi(
                                equippedCodex
                        );

        int currentMaximumKi =
                KiHelper.getMaximumKi(
                        serverPlayer
                );

        if (currentMaximumKi
                == desiredMaximumKi) {
            return;
        }

        KiHelper.setMaximumKi(
                serverPlayer,
                desiredMaximumKi
        );
    }
}