package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.visual.FlurryVisualMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores short-lived client-side visual states for Flurry.
 *
 * The ItemStack stored here is cosmetic only. The player's actual
 * inventory and equipped attributes are never modified.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientFlurryVisuals {
    private static final Map<UUID, VisualState>
            ACTIVE_STATES = new HashMap<>();

    private static long clientTick;

    private ClientFlurryVisuals() {
    }

    public static void start(
            UUID playerId,
            FlurryVisualMode mode,
            ItemStack displayedWeapon,
            int durationTicks
    ) {
        long expirationTick =
                clientTick
                        + Math.max(1, durationTicks)
                        + 5L;

        ACTIVE_STATES.put(
                playerId,
                new VisualState(
                        mode,
                        displayedWeapon.copy(),
                        expirationTick
                )
        );
    }

    public static void stop(
            UUID playerId
    ) {
        ACTIVE_STATES.remove(playerId);
    }

    public static boolean isActive(
            UUID playerId
    ) {
        return ACTIVE_STATES.containsKey(playerId);
    }

    public static boolean shouldStowWeapon(
            UUID playerId
    ) {
        VisualState state =
                ACTIVE_STATES.get(playerId);

        return state != null
                && state.mode
                == FlurryVisualMode.STOWED_WEAPON;
    }

    public static ItemStack getDisplayedWeapon(
            UUID playerId
    ) {
        VisualState state =
                ACTIVE_STATES.get(playerId);

        if (state == null) {
            return ItemStack.EMPTY;
        }

        return state.displayedWeapon;
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        clientTick++;

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            ACTIVE_STATES.clear();
            return;
        }

        ACTIVE_STATES.entrySet().removeIf(
                entry ->
                        entry.getValue()
                                .expirationTick
                                <= clientTick
        );
    }

    private static final class VisualState {
        private final FlurryVisualMode mode;
        private final ItemStack displayedWeapon;
        private final long expirationTick;

        private VisualState(
                FlurryVisualMode mode,
                ItemStack displayedWeapon,
                long expirationTick
        ) {
            this.mode = mode;
            this.displayedWeapon =
                    displayedWeapon;
            this.expirationTick =
                    expirationTick;
        }
    }
}