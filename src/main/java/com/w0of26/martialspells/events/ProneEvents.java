package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.prone.ProneConstants;
import com.w0of26.martialspells.prone.ProneHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncProneAnimationPacket;
import com.w0of26.martialspells.prone.ProneRecoveryModifierService;
import com.w0of26.martialspells.mixin.MobEffectInstanceAccessor;
import com.w0of26.martialspells.prone.ProneDurationModifierService;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraftforge.event.entity.living.MobEffectEvent;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ProneEvents {

    private ProneEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity =
                event.getEntity();

        if (entity.level().isClientSide) {
            return;
        }

        boolean active =
                ProneHelper.isProne(
                        entity
                );

        boolean wasActive =
                entity.getPersistentData()
                        .getBoolean(
                                ProneConstants
                                        .ACTIVE_TRACKER_TAG
                        );

        if (active) {

            /*
             * Only synchronize when transitioning into Prone.
             *
             * Refreshing Prone does not restart the animation.
             */
            if (!wasActive) {

                entity.getPersistentData()
                        .putBoolean(
                                ProneConstants
                                        .ACTIVE_TRACKER_TAG,
                                true
                        );

                syncProneVisual(
                        entity,
                        true
                );
            }

            enforceProneMovement(
                    entity
            );

            return;
        }

        if (!wasActive) {
            return;
        }

        entity.getPersistentData()
                .remove(
                        ProneConstants
                                .ACTIVE_TRACKER_TAG
                );

        syncProneVisual(
                entity,
                false
        );

        if (!entity.isAlive()
                || entity.isDeadOrDying()) {
            return;
        }

        applyRecoverySlowness(
                entity
        );
    }

    private static void enforceProneMovement(
            LivingEntity entity
    ) {
        entity.setSprinting(
                false
        );

        /*
         * Remove voluntary/previous horizontal momentum.
         *
         * Falling is preserved so applying Prone in mid-air
         * does not leave an entity floating.
         *
         * Positive vertical motion is cancelled so a Prone
         * entity cannot continue a jump upward.
         */
        Vec3 movement =
                entity.getDeltaMovement();

        double verticalMovement;

        if (entity.onGround()
                || entity.isNoGravity()) {
            verticalMovement = 0.0D;
        } else {
            verticalMovement =
                    Math.min(
                            movement.y,
                            0.0D
                    );
        }

        entity.setDeltaMovement(
                0.0D,
                verticalMovement,
                0.0D
        );

        if (entity instanceof Mob mob) {
            mob.getNavigation()
                    .stop();

            mob.setJumping(
                    false
            );
        }
    }

    private static void applyRecoverySlowness(
            LivingEntity entity
    ) {
        int recoveryTicks =
                ProneRecoveryModifierService
                        .getRecoveryTicks(
                                entity,
                                ProneConstants
                                        .POST_SLOWNESS_TICKS
                        );

        /*
         * Providers may completely remove the
         * post-Prone recovery penalty.
         */
        if (recoveryTicks <= 0) {
            return;
        }

        entity.addEffect(
                new MobEffectInstance(
                        MobEffects
                                .MOVEMENT_SLOWDOWN,
                        recoveryTicks,
                        ProneConstants
                                .POST_SLOWNESS_AMPLIFIER,
                        false,
                        true,
                        true
                )
        );
    }

    @SubscribeEvent
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {
        event.getEntity()
                .getPersistentData()
                .remove(
                        ProneConstants
                                .ACTIVE_TRACKER_TAG
                );
    }

    @SubscribeEvent
    public static void onPlayerClone(
            PlayerEvent.Clone event
    ) {
        event.getEntity()
                .getPersistentData()
                .remove(
                        ProneConstants
                                .ACTIVE_TRACKER_TAG
                );
    }
    private static void syncProneVisual(
            LivingEntity entity,
            boolean active
    ) {
        if (entity.level().isClientSide) {
            return;
        }

        MartialNetwork
                .sendToTrackingEntityAndSelf(
                        new SyncProneAnimationPacket(
                                entity.getId(),
                                active
                        ),
                        entity
                );
    }

    @SubscribeEvent
    public static void onStartTracking(
            PlayerEvent.StartTracking event
    ) {
        if (!(event.getEntity()
                instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }

        if (!(event.getTarget()
                instanceof LivingEntity target)) {
            return;
        }

        if (!ProneHelper.isProne(
                target
        )) {
            return;
        }

        MartialNetwork.sendToPlayer(
                new SyncProneAnimationPacket(
                        target.getId(),
                        true
                ),
                player
        );
    }
    @SubscribeEvent
    public static void onProneAdded(
            MobEffectEvent.Added event
    ) {
        LivingEntity entity =
                event.getEntity();

        /*
         * Duration is server-authoritative.
         *
         * The client will receive the already-modified
         * duration through normal effect synchronization.
         */
        if (entity.level().isClientSide) {
            return;
        }

        MobEffectInstance effect =
                event.getEffectInstance();

        if (effect.getEffect()
                != MartialEffectRegistry
                .PRONE
                .get()) {

            return;
        }

        int originalDuration =
                effect.getDuration();

        int modifiedDuration =
                ProneDurationModifierService
                        .getDurationTicks(
                                entity,
                                originalDuration
                        );

        if (modifiedDuration
                == originalDuration) {

            return;
        }

        ((MobEffectInstanceAccessor) (Object) effect)
                .martialSpells$setDuration(
                        modifiedDuration
                );
    }
}