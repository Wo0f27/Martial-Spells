package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.spells.ThrowNetSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * Source-faithful flying net delivery: constant velocity, 1 degree/tick homing,
 * 8-tick travel sound, charged flight range, and independent damage/root impacts.
 */
public final class ThrowNetProjectile extends ThrowableProjectile {
    private static final double EPSILON = 1.0E-8D;
    private static final int SOURCE_AGE_CAP_TICKS = 1200;
    private static final int TRAVEL_SOUND_INTERVAL = 8;
    private static final float VANILLA_KNOCKBACK_BASE = 0.4F;

    private float damage;
    private double controlHealthLimit;
    private float maxRange = ThrowNetSpell.BASE_RANGE;
    private float knockbackCoefficient;
    private float distanceTraveled;
    private UUID followedTargetId;

    public ThrowNetProjectile(
            EntityType<? extends ThrowNetProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
        setNoGravity(true);
    }

    public ThrowNetProjectile(
            EntityType<? extends ThrowNetProjectile> entityType,
            Level level,
            LivingEntity owner,
            float damage,
            double controlHealthLimit,
            float maxRange,
            float knockbackCoefficient,
            UUID followedTargetId
    ) {
        this(entityType, level);
        setOwner(owner);
        this.damage = damage;
        this.controlHealthLimit = controlHealthLimit;
        this.maxRange = maxRange;
        this.knockbackCoefficient = knockbackCoefficient;
        this.followedTargetId = followedTargetId;
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (distanceTraveled >= maxRange || tickCount > SOURCE_AGE_CAP_TICKS) {
                discard();
                return;
            }

            applyHoming();

            if (tickCount > 0 && tickCount % TRAVEL_SOUND_INTERVAL == 0) {
                level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        MartialSoundRegistry.NET_TRAVEL.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
        }

        normalizeVelocity();
        double stepDistance = getDeltaMovement().length();
        super.tick();

        if (!isRemoved()) {
            normalizeVelocity();
            if (!level().isClientSide) {
                distanceTraveled += (float) stepDistance;
            }
        }
    }

    private void normalizeVelocity() {
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() <= EPSILON) {
            return;
        }
        setDeltaMovement(
                velocity.normalize().scale(ThrowNetSpell.PROJECTILE_VELOCITY)
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

        Vec3 currentVelocity = getDeltaMovement();
        if (currentVelocity.lengthSqr() <= EPSILON) {
            return;
        }

        Vec3 desired = target.getBoundingBox().getCenter()
                .subtract(position())
                .normalize();
        Vec3 current = currentVelocity.normalize();
        double dot = Math.max(-1.0D, Math.min(1.0D, current.dot(desired)));
        double angle = Math.acos(dot);
        double maxTurn = Math.toRadians(
                ThrowNetSpell.HOMING_DEGREES_PER_TICK
        );

        if (angle <= maxTurn) {
            setDeltaMovement(
                    desired.scale(ThrowNetSpell.PROJECTILE_VELOCITY)
            );
            return;
        }

        double sinAngle = Math.sin(angle);
        if (Math.abs(sinAngle) < EPSILON) {
            Vec3 fallback = current.add(desired.scale(0.01D)).normalize();
            setDeltaMovement(
                    fallback.scale(ThrowNetSpell.PROJECTILE_VELOCITY)
            );
            return;
        }

        double t = maxTurn / angle;
        Vec3 steered = current
                .scale(Math.sin((1.0D - t) * angle) / sinAngle)
                .add(desired.scale(Math.sin(t * angle) / sinAngle))
                .normalize();
        setDeltaMovement(
                steered.scale(ThrowNetSpell.PROJECTILE_VELOCITY)
        );
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
        boolean damaged = target.hurt(
                MartialDamageTypes.throwNet(owner, this),
                damage
        );

        if (damaged) {
            target.setDeltaMovement(velocityBeforeImpact);
            float strength =
                    VANILLA_KNOCKBACK_BASE * knockbackCoefficient;
            if (strength > 0.0F) {
                target.knockback(
                        strength,
                        owner.getX() - target.getX(),
                        owner.getZ() - target.getZ()
                );
            }
        }

        if (target.getMaxHealth() <= controlHealthLimit) {
            target.addEffect(
                    new MobEffectInstance(
                            MartialEffectRegistry.NET_TRAP.get(),
                            ThrowNetSpell.NETTED_DURATION_TICKS,
                            0,
                            false,
                            true,
                            true
                    ),
                    owner
            );
            level().playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    MartialSoundRegistry.NET_IMPACT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ThrowNetDamage", damage);
        tag.putDouble("ThrowNetControlLimit", controlHealthLimit);
        tag.putFloat("ThrowNetRange", maxRange);
        tag.putFloat("ThrowNetKnockback", knockbackCoefficient);
        tag.putFloat("ThrowNetDistance", distanceTraveled);
        if (followedTargetId != null) {
            tag.putUUID("ThrowNetTarget", followedTargetId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("ThrowNetDamage");
        controlHealthLimit = tag.getDouble("ThrowNetControlLimit");
        maxRange = tag.getFloat("ThrowNetRange");
        knockbackCoefficient = tag.getFloat("ThrowNetKnockback");
        distanceTraveled = tag.getFloat("ThrowNetDistance");
        followedTargetId = tag.hasUUID("ThrowNetTarget")
                ? tag.getUUID("ThrowNetTarget")
                : null;
        setNoGravity(true);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
