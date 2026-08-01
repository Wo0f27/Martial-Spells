package com.w0of26.martialspells.ki;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles attaching and cloning the Ki capability.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class KiCapabilityEvents {
    private KiCapabilityEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(
            AttachCapabilitiesEvent<Entity> event
    ) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }

        KiProvider provider = new KiProvider();

        event.addCapability(
                KiProvider.ID,
                provider
        );

        event.addListener(
                provider::invalidate
        );
    }

    /**
     * Players receive a new entity instance after death and in certain
     * special transitions.
     *
     * Death resets Ki. Non-death cloning preserves it.
     */
    @SubscribeEvent
    public static void onPlayerClone(
            PlayerEvent.Clone event
    ) {
        event.getOriginal().reviveCaps();

        event.getOriginal()
                .getCapability(MartialCapabilities.KI)
                .ifPresent(oldKi ->
                        event.getEntity()
                                .getCapability(
                                        MartialCapabilities.KI
                                )
                                .ifPresent(newKi -> {
                                    if (event.isWasDeath()) {
                                        newKi.reset();
                                    } else {
                                        newKi.copyFrom(oldKi);
                                    }
                                })
                );

        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer serverPlayer) {
            KiHelper.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer serverPlayer) {
            KiHelper.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer serverPlayer) {
            KiHelper.sync(serverPlayer);
        }
    }
}