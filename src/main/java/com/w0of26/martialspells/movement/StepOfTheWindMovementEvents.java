package com.w0of26.martialspells.movement;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StepOfTheWindMovementEvents {

    /*
     * A ground cast gets a brief window in which the dash
     * may carry the player off a ledge.
     *
     * Once the player becomes airborne, protection remains
     * active until that airborne movement reaches its next
     * landing.
     */
    private static final int AIRBORNE_GRACE_TICKS = 10;

    /*
     * Do not remove protection on the first grounded tick.
     *
     * This allows LivingFallEvent to consume the protected
     * landing before transient movement state is cleaned up.
     */
    private static final int GROUNDED_CLEANUP_TICKS = 3;

    private static final Map<UUID, FallProtectionState>
            FALL_PROTECTION = new HashMap<>();

    private StepOfTheWindMovementEvents() {
    }

    public static void armFallProtection(
            ServerPlayer player
    ) {
        /*
         * Step of the Wind begins a new protected movement.
         * Pre-existing accumulated fall distance is not
         * carried into that movement.
         */
        player.fallDistance = 0.0F;

        FALL_PROTECTION.put(
                player.getUUID(),
                new FallProtectionState(
                        !player.onGround(),
                        AIRBORNE_GRACE_TICKS
                )
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player
                instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();

        FallProtectionState state =
                FALL_PROTECTION.get(playerId);

        if (state == null) {
            return;
        }

        /*
         * Once airborne, retain protection through the
         * entire fall until LivingFallEvent consumes it.
         */
        if (state.airborne) {

            if (player.onGround()) {
                state.groundedAfterAirborneTicks++;

                if (state.groundedAfterAirborneTicks
                        >= GROUNDED_CLEANUP_TICKS) {

                    FALL_PROTECTION.remove(playerId);
                }
            } else {
                state.groundedAfterAirborneTicks = 0;
            }

            return;
        }

        /*
         * The ground dash has now carried the player into
         * the air. Protection becomes landing-based rather
         * than timer-based from this point onward.
         */
        if (!player.onGround()) {
            state.airborne = true;
            state.groundedAfterAirborneTicks = 0;

            return;
        }

        /*
         * The player is still grounded.
         *
         * If the dash never becomes airborne within the
         * grace period, remove protection so it cannot be
         * saved for an unrelated future fall.
         */
        state.groundGraceTicks--;

        if (state.groundGraceTicks <= 0) {
            FALL_PROTECTION.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(
            LivingFallEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();

        FallProtectionState state =
                FALL_PROTECTION.get(playerId);

        if (state == null
                || !state.airborne) {
            return;
        }

        /*
         * Consume the Step-of-the-Wind-protected landing.
         */
        event.setCanceled(true);

        player.fallDistance = 0.0F;

        FALL_PROTECTION.remove(playerId);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        FALL_PROTECTION.remove(
                event.getEntity().getUUID()
        );
    }

    private static final class FallProtectionState {

        private boolean airborne;

        private int groundGraceTicks;

        private int groundedAfterAirborneTicks;

        private FallProtectionState(
                boolean airborne,
                int groundGraceTicks
        ) {
            this.airborne = airborne;

            this.groundGraceTicks =
                    groundGraceTicks;

            this.groundedAfterAirborneTicks = 0;
        }
    }
}