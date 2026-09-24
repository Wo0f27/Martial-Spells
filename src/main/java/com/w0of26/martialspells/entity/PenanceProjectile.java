package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.PaladinPenanceSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative Penance bolt.
 *
 * <p>The final source orbiting-orb model is intentionally P5 presentation
 * work. P3 preserves the frozen projectile velocity, 16-degree/tick homing,
 * hostile impact, and no-falloff eight-block ally absorption pulse.</p>
 */
public final class PenanceProjectile
        extends ThrowableProjectile {
    private static final double EPSILON = 1.0E-8D;
    private static final int SOURCE_AGE_CAP_TICKS = 1200;

    private float damage;
    private float knockback;
    private int shieldStacksPerBolt;
    private int shieldAmplifierCap;
    private UUID followedTargetId;

    public PenanceProjectile(
            EntityType<? extends PenanceProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
        setNoGravity(true);
    }

    public PenanceProjectile(
            EntityType<? extends PenanceProjectile> entityType,
            Level level,
            LivingEntity owner,
            float damage,
            float knockback,
            int shieldStacksPerBolt,
            int shieldAmplifierCap,
            UUID followedTargetId
    ) {
        this(entityType, level);
        setOwner(owner);
        this.damage = damage;
        this.knockback = knockback;
        this.shieldStacksPerBolt =
                Math.max(1, shieldStacksPerBolt);
        this.shieldAmplifierCap =
                Math.max(0, shieldAmplifierCap);
        this.followedTargetId = followedTargetId;
    }

    @Override
    protected void defineSynchedData() {
        // P3 uses particles for presentation; no custom tracked render data.
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (tickCount > SOURCE_AGE_CAP_TICKS) {
                discard();
                return;
            }

            applyHoming();
        }

        normalizeVelocity();
        super.tick();

        if (!isRemoved()) {
            normalizeVelocity();

            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        getX(),
                        getY(),
                        getZ(),
                        2,
                        0.04D,
                        0.04D,
                        0.04D,
                        0.01D
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
                        .scale(
                                PaladinPenanceSpell
                                        .PROJECTILE_VELOCITY
                        )
        );
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

        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() <= EPSILON) {
            return;
        }

        Vec3 current = velocity.normalize();
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
        double angle = Math.acos(dot);
        double maxTurn =
                Math.toRadians(
                        PaladinPenanceSpell
                                .HOMING_DEGREES_PER_TICK
                );

        if (angle <= maxTurn) {
            setDeltaMovement(
                    desired.scale(
                            PaladinPenanceSpell
                                    .PROJECTILE_VELOCITY
                    )
            );
            return;
        }

        double sinAngle = Math.sin(angle);
        if (Math.abs(sinAngle) < EPSILON) {
            Vec3 fallback =
                    current
                            .add(
                                    desired.scale(0.01D)
                            )
                            .normalize();

            setDeltaMovement(
                    fallback.scale(
                            PaladinPenanceSpell
                                    .PROJECTILE_VELOCITY
                    )
            );
            return;
        }

        double t = maxTurn / angle;
        Vec3 steered =
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

        setDeltaMovement(
                steered.scale(
                        PaladinPenanceSpell
                                .PROJECTILE_VELOCITY
                )
        );
    }

    @Override
    protected boolean canHitEntity(
            Entity entity
    ) {
        if (!super.canHitEntity(entity)
                || !(entity instanceof LivingEntity target)
                || !target.isAlive()
                || target.isSpectator()) {
            return false;
        }

        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return true;
        }

        return target != livingOwner
                && !Utils.shouldHealEntity(
                        livingOwner,
                        target
                );
    }

    @Override
    protected void onHitEntity(
            EntityHitResult result
    ) {
        super.onHitEntity(result);

        if (level().isClientSide
                || !(level() instanceof ServerLevel serverLevel)
                || !(result.getEntity()
                instanceof LivingEntity target)
                || !(getOwner()
                instanceof LivingEntity owner)) {
            return;
        }

        boolean damaged =
                DamageSources.applyDamage(
                        target,
                        damage,
                        MartialSpellRegistry.PENANCE
                                .get()
                                .getDamageSource(owner)
                );

        if (!damaged) {
            discard();
            return;
        }

        if (knockback > 0.0F) {
            target.knockback(
                    knockback,
                    owner.getX() - target.getX(),
                    owner.getZ() - target.getZ()
            );
        }

        serverLevel.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                MartialSoundRegistry.HOLY_SHOCK_DAMAGE.get(),
                SoundSource.PLAYERS,
                0.8F,
                1.0F
        );

        applyAbsorptionPulse(
                serverLevel,
                owner,
                target
        );

        serverLevel.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                MartialSoundRegistry.PENANCE_IMPACT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                target.getX(),
                target.getY()
                        + target.getBbHeight() * 0.5D,
                target.getZ(),
                18,
                0.35D,
                0.25D,
                0.35D,
                0.10D
        );

        discard();
    }

    private void applyAbsorptionPulse(
            ServerLevel level,
            LivingEntity owner,
            LivingEntity impactTarget
    ) {
        List<LivingEntity> allies =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        impactTarget
                                .getBoundingBox()
                                .inflate(
                                        PaladinPenanceSpell
                                                .ABSORPTION_RADIUS
                                ),
                        candidate ->
                                candidate.isAlive()
                                        && !candidate.isSpectator()
                                        && candidate.distanceToSqr(
                                                impactTarget
                                        )
                                        <= PaladinPenanceSpell
                                        .ABSORPTION_RADIUS
                                        * PaladinPenanceSpell
                                        .ABSORPTION_RADIUS
                                        && Utils.shouldHealEntity(
                                                owner,
                                                candidate
                                        )
                );

        for (LivingEntity ally : allies) {
            MobEffectInstance current =
                    ally.getEffect(
                            MartialEffectRegistry
                                    .PRIEST_ABSORPTION
                                    .get()
                    );

            int currentStacks =
                    current == null
                            ? 0
                            : current.getAmplifier() + 1;

            int cappedNewStacks =
                    Math.min(
                            currentStacks
                                    + shieldStacksPerBolt,
                            shieldAmplifierCap + 1
                    );

            // A weaker caster must never reduce a stronger existing shield.
            int resultingStacks =
                    Math.max(
                            currentStacks,
                            cappedNewStacks
                    );

            if (resultingStacks <= 0) {
                continue;
            }

            int amplifier =
                    resultingStacks - 1;

            ally.addEffect(
                    new MobEffectInstance(
                            MartialEffectRegistry
                                    .PRIEST_ABSORPTION
                                    .get(),
                            PaladinPenanceSpell
                                    .ABSORPTION_DURATION_TICKS,
                            amplifier,
                            false,
                            true,
                            true
                    ),
                    owner
            );

            ally.setAbsorptionAmount(
                    Math.max(
                            ally.getAbsorptionAmount(),
                            PaladinPenanceSpell
                                    .ABSORPTION_HEALTH_PER_STACK
                                    * resultingStacks
                    )
            );

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    ally.getX(),
                    ally.getY()
                            + ally.getBbHeight() * 0.5D,
                    ally.getZ(),
                    8,
                    0.25D,
                    0.20D,
                    0.25D,
                    0.05D
            );
        }
    }

    @Override
    protected void onHitBlock(
            BlockHitResult result
    ) {
        super.onHitBlock(result);

        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("PenanceDamage", damage);
        tag.putFloat("PenanceKnockback", knockback);
        tag.putInt(
                "PenanceShieldStacks",
                shieldStacksPerBolt
        );
        tag.putInt(
                "PenanceShieldCap",
                shieldAmplifierCap
        );

        if (followedTargetId != null) {
            tag.putUUID(
                    "PenanceTarget",
                    followedTargetId
            );
        }
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("PenanceDamage");
        knockback = tag.getFloat("PenanceKnockback");
        shieldStacksPerBolt =
                Math.max(
                        1,
                        tag.getInt(
                                "PenanceShieldStacks"
                        )
                );
        shieldAmplifierCap =
                Math.max(
                        0,
                        tag.getInt(
                                "PenanceShieldCap"
                        )
                );
        followedTargetId =
                tag.hasUUID("PenanceTarget")
                        ? tag.getUUID(
                                "PenanceTarget"
                        )
                        : null;

        setNoGravity(true);
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks
                .getEntitySpawningPacket(this);
    }
}
