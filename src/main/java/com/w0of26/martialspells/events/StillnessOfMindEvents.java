package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StillnessOfMindEvents {

    public static final int WEAKNESS_SECONDS = 60;

    public static final int WEAKNESS_TICKS =
            WEAKNESS_SECONDS * 20;

    private static final String ACTIVE_TRACKER_TAG =
            MartialSpells.MOD_ID
                    + "_stillness_of_mind_active";

    private StillnessOfMindEvents() {
    }

    /**
     * Tracks Stillness independently from the effect itself.
     *
     * When the effect disappears naturally or through an
     * external removal such as milk/effect clearing, the
     * post-Stillness Weakness is applied safely on the
     * following server tick.
     */
    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player
                instanceof ServerPlayer player)) {
            return;
        }

        boolean active =
                player.hasEffect(
                        MartialEffectRegistry
                                .STILLNESS_OF_MIND
                                .get()
                );

        boolean wasActive =
                player.getPersistentData()
                        .getBoolean(
                                ACTIVE_TRACKER_TAG
                        );

        if (active) {
            player.getPersistentData()
                    .putBoolean(
                            ACTIVE_TRACKER_TAG,
                            true
                    );

            return;
        }

        if (!wasActive) {
            return;
        }

        player.getPersistentData()
                .remove(
                        ACTIVE_TRACKER_TAG
                );

        /*
         * Death already carries its own consequence.
         * Stillness must not cause Weakness after respawn.
         */
        if (!player.isAlive()
                || player.isDeadOrDying()) {
            return;
        }

        applyWeakness(player);
    }

    /**
     * Explicitly terminates Stillness when the Monk Codex
     * is unequipped.
     *
     * This applies the normal exhaustion penalty.
     */
    public static void terminateForCodexRemoval(
            ServerPlayer player
    ) {
        if (!player.hasEffect(
                MartialEffectRegistry
                        .STILLNESS_OF_MIND
                        .get()
        )) {
            return;
        }

        /*
         * Clear first so the tick tracker cannot apply
         * Weakness twice.
         */
        player.getPersistentData()
                .remove(
                        ACTIVE_TRACKER_TAG
                );

        player.removeEffect(
                MartialEffectRegistry
                        .STILLNESS_OF_MIND
                        .get()
        );

        if (player.isAlive()
                && !player.isDeadOrDying()) {
            applyWeakness(player);
        }
    }

    /**
     * Prevent the active-state tracker from surviving death.
     */
    @SubscribeEvent
    public static void onPlayerDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        player.getPersistentData()
                .remove(
                        ACTIVE_TRACKER_TAG
                );
    }

    /**
     * Extra respawn safeguard.
     */
    @SubscribeEvent
    public static void onPlayerClone(
            PlayerEvent.Clone event
    ) {
        event.getEntity()
                .getPersistentData()
                .remove(
                        ACTIVE_TRACKER_TAG
                );
    }

    private static void applyWeakness(
            ServerPlayer player
    ) {
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.WEAKNESS,
                        WEAKNESS_TICKS,
                        0,
                        false,
                        false,
                        true
                )
        );
    }
}