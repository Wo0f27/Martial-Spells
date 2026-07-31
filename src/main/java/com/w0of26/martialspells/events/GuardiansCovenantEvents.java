package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.util.GuardiansCovenantLinkData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import com.w0of26.martialspells.damage.GuardiansCovenantRedirectManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GuardiansCovenantEvents {
    /*
     * Five particle updates per second.
     */
    private static final int PARTICLE_INTERVAL_TICKS = 4;

    /*
     * Smaller values produce a denser line but create more particles.
     */
    private static final double PARTICLE_SPACING = 0.5D;

    /*
     * Warm golden Holy color.
     *
     * Vector values use the range 0.0 to 1.0.
     */
    private static final DustParticleOptions TETHER_PARTICLE =
            new DustParticleOptions(
                    new Vector3f(
                            1.0F,
                            0.82F,
                            0.32F
                    ),
                    0.7F
            );

    private GuardiansCovenantEvents() {
    }

    @SubscribeEvent
    public static void onEffectExpired(
            MobEffectEvent.Expired event
    ) {
        MobEffectInstance effectInstance =
                event.getEffectInstance();

        if (effectInstance.getEffect()
                == MartialEffectRegistry
                .GUARDIANS_COVENANT_LINKED
                .get()) {
            GuardiansCovenantLinkData.clearLink(
                    event.getEntity()
            );

            return;
        }

        if (effectInstance.getEffect()
                == MartialEffectRegistry
                .GUARDIANS_COVENANT_TANK
                .get()
                && event.getEntity()
                instanceof ServerPlayer tank) {
            GuardiansCovenantRedirectManager.stop(tank);
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(
            MobEffectEvent.Remove event
    ) {
        if (event.getEffect()
                == MartialEffectRegistry
                .GUARDIANS_COVENANT_LINKED
                .get()) {
            GuardiansCovenantLinkData.clearLink(
                    event.getEntity()
            );

            return;
        }

        if (event.getEffect()
                == MartialEffectRegistry
                .GUARDIANS_COVENANT_TANK
                .get()
                && event.getEntity()
                instanceof ServerPlayer tank) {
            GuardiansCovenantRedirectManager.stop(tank);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer tank)) {
            return;
        }

        GuardiansCovenantRedirectManager.tick(tank);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            GuardiansCovenantRedirectManager.stop(
                    player
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        GuardiansCovenantRedirectManager.clear();
    }

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer ally)) {
            return;
        }

        boolean hasLinkedEffect =
                ally.hasEffect(
                        MartialEffectRegistry
                                .GUARDIANS_COVENANT_LINKED
                                .get()
                );

        /*
         * Remove stale stored data if the effect disappeared without
         * going through the expected removal event.
         */
        if (!hasLinkedEffect) {
            if (GuardiansCovenantLinkData.hasLink(ally)) {
                GuardiansCovenantLinkData.clearLink(ally);
            }

            return;
        }

        if (!GuardiansCovenantLinkData.hasLink(ally)) {
            return;
        }

        /*
         * The tether does not need to update every server tick.
         */
        if (ally.tickCount % PARTICLE_INTERVAL_TICKS != 0) {
            return;
        }

        MinecraftServer server = ally.getServer();

        if (server == null) {
            return;
        }

        UUID casterUuid =
                GuardiansCovenantLinkData.getCasterUuid(ally);

        ServerPlayer tank =
                server.getPlayerList().getPlayer(casterUuid);

        if (!isValidTank(ally, tank)) {
            invalidateLink(ally);
            return;
        }

        double radius =
                GuardiansCovenantLinkData.getRadius(ally);

        if (radius <= 0.0D) {
            return;
        }

        /*
         * The link remains stored while the ally is outside the
         * radius, but the visible tether disappears. Re-entering the
         * radius allows it to appear again.
         */
        if (ally.distanceToSqr(tank) > radius * radius) {
            return;
        }

        spawnTetherParticles(
                (ServerLevel) ally.level(),
                ally,
                tank
        );
    }

    private static boolean isValidTank(
            ServerPlayer ally,
            ServerPlayer tank
    ) {
        if (tank == null) {
            return false;
        }

        if (!tank.isAlive() || tank.isSpectator()) {
            return false;
        }

        if (tank.level() != ally.level()) {
            return false;
        }

        return tank.hasEffect(
                MartialEffectRegistry
                        .GUARDIANS_COVENANT_TANK
                        .get()
        );
    }

    private static void invalidateLink(
            ServerPlayer ally
    ) {
        GuardiansCovenantLinkData.clearLink(ally);

        ally.removeEffect(
                MartialEffectRegistry
                        .GUARDIANS_COVENANT_LINKED
                        .get()
        );
    }

    private static void spawnTetherParticles(
            ServerLevel level,
            ServerPlayer ally,
            ServerPlayer tank
    ) {
        Vec3 start = ally.position().add(
                0.0D,
                ally.getBbHeight() * 0.65D,
                0.0D
        );

        Vec3 end = tank.position().add(
                0.0D,
                tank.getBbHeight() * 0.65D,
                0.0D
        );

        Vec3 difference = end.subtract(start);
        double length = difference.length();

        if (length <= 0.001D) {
            return;
        }

        /*
         * Shifting the first particle over time creates movement from
         * the ally toward the tank rather than a completely static
         * dotted line.
         */
        double phase =
                (
                        level.getGameTime() % 20L
                ) / 20.0D * PARTICLE_SPACING;

        for (
                double distance = phase;
                distance <= length;
                distance += PARTICLE_SPACING
        ) {
            double progress = distance / length;

            Vec3 position = start.add(
                    difference.scale(progress)
            );

            level.sendParticles(
                    TETHER_PARTICLE,
                    position.x,
                    position.y,
                    position.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }
}