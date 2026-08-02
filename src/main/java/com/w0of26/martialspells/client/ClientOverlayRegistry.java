package com.w0of26.martialspells.client;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers Martial Spells client HUD overlays.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientOverlayRegistry {
    private static final ResourceLocation IRONS_MANA_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(
                    "irons_spellbooks",
                    "mana_overlay"
            );

    private ClientOverlayRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(
            RegisterGuiOverlaysEvent event
    ) {
        event.registerAbove(
                IRONS_MANA_OVERLAY,
                "ki_overlay",
                KiBarOverlay.INSTANCE
        );
    }
}