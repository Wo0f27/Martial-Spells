package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.spells.PaladinJudgementSpell;
import com.w0of26.martialspells.spells.PaladinVfx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * Presentation-only Judgement meteor.
 *
 * <p>Damage and stun remain owned by JudgementImpactManager so P2 mechanics
 * stay frozen. This entity exists only to restore the source projectile model,
 * one-degree-per-tick homing and travel VFX.</p>
 */
public final class JudgementVisualEntity extends Entity {
    private static final double EPSILON = 1.0E-8D;

    private UUID followedTargetId;

    public JudgementVisualEntity(
            EntityType<? extends JudgementVisualEntity> type,
            Level level
    ) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public JudgementVisualEntity(
            EntityType<? extends JudgementVisualEntity> type,
            ServerLevel level,
            LivingEntity target,
            Vec3 launch
    ) {
        this(type, level);
        setPos(
                launch.x,
                launch.y,
                launch.z
        );
        followedTargetId =
                target.getUUID();
        setDeltaMovement(
                0.0D,
                -PaladinJudgementSpell.METEOR_VELOCITY,
                0.0D
        );
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            applyHoming();

            if (level() instanceof ServerLevel serverLevel) {
                PaladinVfx.judgementTrail(
                        serverLevel,
                        position(),
                        getDeltaMovement()
                );
            }
        }

        Vec3 velocity =
                getDeltaMovement();
        setPos(
                getX() + velocity.x,
                getY() + velocity.y,
                getZ() + velocity.z
        );

        if (!level().isClientSide
                && tickCount
                >= PaladinJudgementSpell.METEOR_TRAVEL_TICKS) {
            discard();
        }
    }

    private void applyHoming() {
        if (!(level() instanceof ServerLevel serverLevel)
                || followedTargetId == null) {
            return;
        }

        Entity resolved =
                serverLevel.getEntity(
                        followedTargetId
                );
        if (!(resolved instanceof LivingEntity target)
                || !target.isAlive()
                || target.isRemoved()) {
            followedTargetId = null;
            return;
        }

        Vec3 velocity =
                getDeltaMovement();
        if (velocity.lengthSqr() <= EPSILON) {
            return;
        }

        Vec3 current =
                velocity.normalize();
        Vec3 desired =
                target.getBoundingBox()
                        .getCenter()
                        .subtract(position())
                        .normalize();

        double dot =
                Math.max(
                        -1.0D,
                        Math.min(
                                1.0D,
                                current.dot(desired)
                        )
                );
        double angle =
                Math.acos(dot);
        double maxTurn =
                Math.toRadians(1.0D);

        Vec3 steered;
        if (angle <= maxTurn) {
            steered = desired;
        } else {
            double sinAngle =
                    Math.sin(angle);
            if (Math.abs(sinAngle) < EPSILON) {
                steered =
                        current.add(
                                desired.scale(0.001D)
                        ).normalize();
            } else {
                double t =
                        maxTurn / angle;
                steered =
                        current
                                .scale(
                                        Math.sin(
                                                (1.0D - t) * angle
                                        ) / sinAngle
                                )
                                .add(
                                        desired.scale(
                                                Math.sin(t * angle)
                                                        / sinAngle
                                        )
                                )
                                .normalize();
            }
        }

        setDeltaMovement(
                steered.scale(
                        PaladinJudgementSpell.METEOR_VELOCITY
                )
        );
        hasImpulse = true;
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        followedTargetId =
                tag.hasUUID("JudgementTarget")
                        ? tag.getUUID("JudgementTarget")
                        : null;
        noPhysics = true;
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        if (followedTargetId != null) {
            tag.putUUID(
                    "JudgementTarget",
                    followedTargetId
            );
        }
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks
                .getEntitySpawningPacket(this);
    }
}
