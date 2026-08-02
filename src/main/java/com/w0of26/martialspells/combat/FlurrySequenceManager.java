package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
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
public final class FlurrySequenceManager {
    private static final int SECOND_STRIKE_DELAY_TICKS =
            11;

    /*
     * Allows slight movement after starting Flurry.
     * 4.5 blocks squared.
     */
    private static final double
            CONTINUATION_RANGE_SQUARED = 20.25D;

    private static final Map<UUID, PendingStrike>
            PENDING_STRIKES = new HashMap<>();

    private FlurrySequenceManager() {
    }

    public static void begin(
            ServerPlayer player,
            LivingEntity target,
            float damage
    ) {
        performStrike(player, target, damage);

        if (!target.isAlive()) {
            return;
        }

        long executionTick =
                player.serverLevel().getGameTime()
                        + SECOND_STRIKE_DELAY_TICKS;

        PENDING_STRIKES.put(
                player.getUUID(),
                new PendingStrike(
                        target.getUUID(),
                        executionTick,
                        damage
                )
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player
                instanceof ServerPlayer player)) {
            return;
        }

        PendingStrike pending =
                PENDING_STRIKES.get(
                        player.getUUID()
                );

        if (pending == null) {
            return;
        }

        if (player.serverLevel().getGameTime()
                < pending.executionTick()) {
            return;
        }

        PENDING_STRIKES.remove(
                player.getUUID()
        );

        Entity entity =
                player.serverLevel().getEntity(
                        pending.targetId()
                );

        if (!(entity instanceof LivingEntity target)) {
            return;
        }

        if (!canContinue(player, target)) {
            return;
        }

        performStrike(
                player,
                target,
                pending.damage()
        );
    }

    @SubscribeEvent
    public static void onPlayerLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        PENDING_STRIKES.remove(
                event.getEntity().getUUID()
        );
    }

    private static boolean canContinue(
            ServerPlayer player,
            LivingEntity target
    ) {
        return player.isAlive()
                && target.isAlive()
                && !target.isSpectator()
                && !player.isAlliedTo(target)
                && player.distanceToSqr(target)
                <= CONTINUATION_RANGE_SQUARED
                && player.hasLineOfSight(target);
    }

    private static void performStrike(
            ServerPlayer player,
            LivingEntity target,
            float damage
    ) {
        player.swing(
                InteractionHand.MAIN_HAND,
                true
        );

        boolean damaged =
                target.hurt(
                        player.damageSources()
                                .playerAttack(player),
                        damage
                );

        if (!damaged) {
            return;
        }

        double effectY =
                target.getY()
                        + target.getBbHeight() * 0.5D;

        player.serverLevel().sendParticles(
                ParticleTypes.CRIT,
                target.getX(),
                effectY,
                target.getZ(),
                6,
                0.20D,
                0.20D,
                0.20D,
                0.05D
        );

        player.serverLevel().playSound(
                null,
                target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS,
                0.8F,
                1.1F
        );
    }

    private record PendingStrike(
            UUID targetId,
            long executionTick,
            float damage
    ) {
    }
}