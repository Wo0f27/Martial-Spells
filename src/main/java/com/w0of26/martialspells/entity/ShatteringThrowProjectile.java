package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.visual.ShatterBloodVfx;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.spells.ShatteringThrowSpell;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * Frozen Rogues Shattering Throw projectile:
 * held-item visual, 0.8 speed, 2 degree/tick homing, one block bounce.
 */
public final class ShatteringThrowProjectile extends Projectile {
    private static final double EPSILON = 1.0E-8D;
    private static final int SOURCE_AGE_CAP_TICKS = 1200;
    private static final int TRAVEL_SOUND_INTERVAL = 8;
    private static final float VANILLA_KNOCKBACK_BASE = 0.4F;

    private static final EntityDataAccessor<String> ITEM_MODEL_ID =
            SynchedEntityData.defineId(
                    ShatteringThrowProjectile.class,
                    EntityDataSerializers.STRING
            );

    private float damage;
    private double controlHealthLimit;
    private float maxRange = ShatteringThrowSpell.BASE_RANGE;
    private float knockbackCoefficient;
    private float distanceTraveled;
    private int bouncesRemaining = ShatteringThrowSpell.BOUNCES;
    private UUID followedTargetId;

    public ShatteringThrowProjectile(
            EntityType<? extends ShatteringThrowProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
        setNoGravity(true);
    }

    public ShatteringThrowProjectile(
            EntityType<? extends ShatteringThrowProjectile> entityType,
            Level level,
            LivingEntity owner,
            float damage,
            double controlHealthLimit,
            float maxRange,
            float knockbackCoefficient,
            int bouncesRemaining,
            UUID followedTargetId,
            String itemModelId
    ) {
        this(entityType, level);
        setOwner(owner);
        this.damage = damage;
        this.controlHealthLimit = controlHealthLimit;
        this.maxRange = maxRange;
        this.knockbackCoefficient = knockbackCoefficient;
        this.bouncesRemaining = bouncesRemaining;
        this.followedTargetId = followedTargetId;
        setItemModelId(itemModelId);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ITEM_MODEL_ID, "");
    }

    public String getItemModelId() {
        return entityData.get(ITEM_MODEL_ID);
    }

    private void setItemModelId(String itemModelId) {
        entityData.set(
                ITEM_MODEL_ID,
                itemModelId == null ? "" : itemModelId
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (isRemoved()) {
            return;
        }

        if (!level().isClientSide
                && (distanceTraveled >= maxRange
                || tickCount > SOURCE_AGE_CAP_TICKS)) {
            discard();
            return;
        }

        if (!level().isClientSide) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(
                    this,
                    this::canHitEntity
            );

            if (hit.getType() != HitResult.Type.MISS
                    && !ForgeEventFactory.onProjectileImpact(this, hit)) {
                onHit(hit);
                if (isRemoved() || hit.getType() == HitResult.Type.BLOCK) {
                    // Source bounce sets skipTravel for this tick. A terminal
                    // block/entity impact removes the projectile.
                    return;
                }
            }

            applyHoming();
        }

        normalizeVelocity();
        Vec3 movement = getDeltaMovement();

        setPos(
                getX() + movement.x,
                getY() + movement.y,
                getZ() + movement.z
        );
        updateRotation();

        if (!level().isClientSide) {
            distanceTraveled += (float) movement.length();

            if (tickCount > 0
                    && tickCount % TRAVEL_SOUND_INTERVAL == 0) {
                level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        MartialSoundRegistry.THROW.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
        }
    }

    private void normalizeVelocity() {
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() <= EPSILON) {
            return;
        }
        setDeltaMovement(
                velocity.normalize()
                        .scale(ShatteringThrowSpell.PROJECTILE_VELOCITY)
        );
    }

    private void applyHoming() {
        if (!(level() instanceof ServerLevel serverLevel)
                || followedTargetId == null) {
            return;
        }

        Entity resolved = serverLevel.getEntity(followedTargetId);
        if (!(resolved instanceof LivingEntity target)
                || !target.isAlive()
                || target.isRemoved()) {
            followedTargetId = null;
            return;
        }

        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() <= EPSILON) {
            return;
        }

        Vec3 current = velocity.normalize();
        Vec3 desired = target.getBoundingBox()
                .getCenter()
                .subtract(position())
                .normalize();

        double dot = Math.max(
                -1.0D,
                Math.min(1.0D, current.dot(desired))
        );
        double angle = Math.acos(dot);
        double maxTurn = Math.toRadians(
                ShatteringThrowSpell.HOMING_DEGREES_PER_TICK
        );

        if (angle <= maxTurn) {
            setDeltaMovement(
                    desired.scale(
                            ShatteringThrowSpell.PROJECTILE_VELOCITY
                    )
            );
            return;
        }

        double sinAngle = Math.sin(angle);
        if (Math.abs(sinAngle) < EPSILON) {
            Vec3 fallback = current
                    .add(desired.scale(0.01D))
                    .normalize();
            setDeltaMovement(
                    fallback.scale(
                            ShatteringThrowSpell.PROJECTILE_VELOCITY
                    )
            );
            return;
        }

        double t = maxTurn / angle;
        Vec3 steered = current
                .scale(Math.sin((1.0D - t) * angle) / sinAngle)
                .add(
                        desired.scale(
                                Math.sin(t * angle) / sinAngle
                        )
                )
                .normalize();

        setDeltaMovement(
                steered.scale(
                        ShatteringThrowSpell.PROJECTILE_VELOCITY
                )
        );
    }

    private void bounceFrom(
            BlockHitResult result,
            Vec3 previousDirection
    ) {
        Vec3 impactPosition = result.getLocation();
        Direction side = result.getDirection();
        Vec3 normal = new Vec3(
                side.getStepX(),
                side.getStepY(),
                side.getStepZ()
        );

        double speed = previousDirection.length();
        Vec3 reflected = previousDirection.subtract(
                normal.scale(
                        2.0D * previousDirection.dot(normal)
                )
        );

        double usedDistance =
                impactPosition.subtract(position()).length();
        double remainingDistance = Math.max(
                0.0D,
                speed - usedDistance
        );

        Vec3 reflectedDirection = reflected.lengthSqr() <= EPSILON
                ? previousDirection.scale(-1.0D).normalize()
                : reflected.normalize();

        Vec3 finalPosition = impactPosition.add(
                reflectedDirection.scale(remainingDistance)
        );

        setPos(
                finalPosition.x,
                finalPosition.y,
                finalPosition.z
        );
        setDeltaMovement(
                reflectedDirection.scale(speed)
        );

        bouncesRemaining--;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)
                || !(entity instanceof LivingEntity target)
                || !target.isAlive()
                || target.isSpectator()) {
            return false;
        }

        Entity owner = getOwner();
        return !(owner instanceof LivingEntity livingOwner)
                || !livingOwner.isAlliedTo(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (level().isClientSide
                || !(result.getEntity() instanceof LivingEntity target)
                || !(getOwner() instanceof LivingEntity owner)) {
            return;
        }

        Vec3 velocityBeforeImpact = target.getDeltaMovement();

        int previousInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        boolean damaged = target.hurt(
                MartialDamageTypes.shatteringThrow(owner, this),
                damage
        );
        target.invulnerableTime = previousInvulnerableTime;

        if (damaged) {
            target.setDeltaMovement(velocityBeforeImpact);

            float strength =
                    VANILLA_KNOCKBACK_BASE
                            * knockbackCoefficient;
            if (strength > 0.0F) {
                target.knockback(
                        strength,
                        owner.getX() - target.getX(),
                        owner.getZ() - target.getZ()
                );
            }
        }

        level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                MartialSoundRegistry.THROW_IMPACT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        if (target.getMaxHealth() <= controlHealthLimit) {
            target.addEffect(
                    new MobEffectInstance(
                            MartialEffectRegistry.SHATTER.get(),
                            ShatteringThrowSpell.SHATTER_DURATION_TICKS,
                            0,
                            false,
                            false,
                            true
                    ),
                    owner
            );

            if (level() instanceof ServerLevel serverLevel) {
                ShatterBloodVfx.spawnImpact(
                        serverLevel,
                        target
                );
            }
        }

        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (level().isClientSide) {
            return;
        }

        if (bouncesRemaining > 0) {
            bounceFrom(result, getDeltaMovement());
            return;
        }

        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ShatteringThrowDamage", damage);
        tag.putDouble(
                "ShatteringThrowControlLimit",
                controlHealthLimit
        );
        tag.putFloat("ShatteringThrowRange", maxRange);
        tag.putFloat(
                "ShatteringThrowKnockback",
                knockbackCoefficient
        );
        tag.putFloat(
                "ShatteringThrowDistance",
                distanceTraveled
        );
        tag.putInt(
                "ShatteringThrowBounces",
                bouncesRemaining
        );
        tag.putString(
                "ShatteringThrowItemModel",
                getItemModelId()
        );

        if (followedTargetId != null) {
            tag.putUUID(
                    "ShatteringThrowTarget",
                    followedTargetId
            );
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("ShatteringThrowDamage");
        controlHealthLimit =
                tag.getDouble("ShatteringThrowControlLimit");
        maxRange = tag.getFloat("ShatteringThrowRange");
        knockbackCoefficient =
                tag.getFloat("ShatteringThrowKnockback");
        distanceTraveled =
                tag.getFloat("ShatteringThrowDistance");
        bouncesRemaining =
                tag.getInt("ShatteringThrowBounces");
        setItemModelId(
                tag.getString("ShatteringThrowItemModel")
        );

        followedTargetId =
                tag.hasUUID("ShatteringThrowTarget")
                        ? tag.getUUID("ShatteringThrowTarget")
                        : null;

        setNoGravity(true);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
