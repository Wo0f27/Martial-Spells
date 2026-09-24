package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.PaladinVfx;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * Internal Lightwell Holy Mote.
 *
 * <p>Source executable values: velocity 1.0, +30 degree launch pitch,
 * 16 degrees/tick homing after 15% of the initial target distance, and one
 * terrain bounce.</p>
 */
public final class HolyMoteProjectile extends ThrowableProjectile {
    public static final float VELOCITY = 1.0F;
    public static final float HOMING_DEGREES_PER_TICK = 16.0F;
    public static final double RANGE = 12.0D;
    public static final double HOMING_START_RELATIVE_DISTANCE = 0.15D;
    public static final int MAX_AGE_TICKS = 20 * 10;

    private static final double EPSILON = 1.0E-8D;

    private UUID targetId;
    private UUID summonerId;
    private float healing;
    private float distanceTraveled;
    private float homingStartDistance;
    private int bouncesRemaining = 1;

    public HolyMoteProjectile(
            EntityType<? extends HolyMoteProjectile> type,
            Level level
    ) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
    }

    public HolyMoteProjectile(
            EntityType<? extends HolyMoteProjectile> type,
            ServerLevel level,
            Entity projectileOwner,
            LivingEntity summoner,
            LivingEntity target,
            float healing
    ) {
        this(type, level);
        setOwner(projectileOwner);
        summonerId = summoner.getUUID();
        targetId = target.getUUID();
        this.healing = healing;
    }

    @Override
    protected void defineSynchedData() {
    }

    public void launchToward(
            LivingEntity target
    ) {
        Vec3 start =
                position();
        Vec3 desired =
                target.getBoundingBox()
                        .getCenter()
                        .subtract(start);
        homingStartDistance =
                (float) (
                        desired.length()
                                * HOMING_START_RELATIVE_DISTANCE
                );

        double horizontal =
                Math.sqrt(
                        desired.x * desired.x
                                + desired.z * desired.z
                );

        if (desired.lengthSqr() <= EPSILON) {
            setDeltaMovement(
                    0.0D,
                    VELOCITY,
                    0.0D
            );
            return;
        }

        double basePitch =
                Math.atan2(
                        desired.y,
                        Math.max(
                                EPSILON,
                                horizontal
                        )
                );
        double raisedPitch =
                basePitch
                        + Math.toRadians(30.0D);

        double horizontalScale =
                Math.cos(raisedPitch);
        double horizontalX =
                horizontal <= EPSILON
                        ? 0.0D
                        : desired.x / horizontal;
        double horizontalZ =
                horizontal <= EPSILON
                        ? 0.0D
                        : desired.z / horizontal;

        setDeltaMovement(
                new Vec3(
                        horizontalX * horizontalScale,
                        Math.sin(raisedPitch),
                        horizontalZ * horizontalScale
                ).normalize().scale(VELOCITY)
        );
        hasImpulse = true;
    }

    @Override
    public void tick() {
        if (!level().isClientSide
                && tickCount > MAX_AGE_TICKS) {
            discard();
            return;
        }

        if (!level().isClientSide
                && distanceTraveled >= RANGE) {
            discard();
            return;
        }

        if (!level().isClientSide
                && distanceTraveled
                >= homingStartDistance) {
            applyHoming();
        }

        Vec3 before =
                position();
        super.tick();

        if (isRemoved()) {
            return;
        }

        if (!level().isClientSide
                && level() instanceof ServerLevel serverLevel) {
            distanceTraveled +=
                    (float) before.distanceTo(
                            position()
                    );

            PaladinVfx.holyMoteTrail(
                    serverLevel,
                    position()
            );
        }
    }

    private void applyHoming() {
        if (!(level() instanceof ServerLevel serverLevel)
                || targetId == null) {
            return;
        }

        Entity resolved =
                serverLevel.getEntity(
                        targetId
                );
        if (!(resolved instanceof LivingEntity target)
                || !target.isAlive()
                || target.isRemoved()) {
            targetId = null;
            return;
        }

        Vec3 currentVelocity =
                getDeltaMovement();
        if (currentVelocity.lengthSqr() <= EPSILON) {
            return;
        }

        Vec3 desired =
                target.getBoundingBox()
                        .getCenter()
                        .subtract(position())
                        .normalize();
        Vec3 current =
                currentVelocity.normalize();

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
                Math.toRadians(
                        HOMING_DEGREES_PER_TICK
                );

        Vec3 steered;
        if (angle <= maxTurn) {
            steered = desired;
        } else {
            double sin =
                    Math.sin(angle);
            if (Math.abs(sin) < EPSILON) {
                steered =
                        current.add(
                                desired.scale(0.01D)
                        ).normalize();
            } else {
                double t =
                        maxTurn / angle;
                steered =
                        current.scale(
                                Math.sin(
                                        (1.0D - t) * angle
                                ) / sin
                        ).add(
                                desired.scale(
                                        Math.sin(
                                                t * angle
                                        ) / sin
                                )
                        ).normalize();
            }
        }

        setDeltaMovement(
                steered.scale(VELOCITY)
        );
        hasImpulse = true;
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

        LivingEntity summoner =
                resolveSummoner();
        return summoner != null
                && Utils.shouldHealEntity(
                        summoner,
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
                || !(result.getEntity() instanceof LivingEntity target)) {
            return;
        }

        LivingEntity summoner =
                resolveSummoner();
        if (summoner == null
                || !Utils.shouldHealEntity(
                        summoner,
                        target
                )) {
            return;
        }

        MinecraftForge.EVENT_BUS.post(
                new SpellHealEvent(
                        summoner,
                        target,
                        healing,
                        MartialSpellRegistry
                                .LIGHTWELL_ORB
                                .get()
                                .getSchoolType()
                )
        );
        target.heal(
                healing
        );

        PaladinVfx.healPillar(
                serverLevel,
                target,
                15
        );
        PaladinVfx.holyGlimmer(
                serverLevel,
                target,
                12,
                0.20D,
                0.25D
        );

        serverLevel.playSound(
                null,
                target.blockPosition(),
                MartialSoundRegistry.HOLY_SHOCK_HEAL.get(),
                SoundSource.PLAYERS,
                0.7F,
                1.15F
        );

        discard();
    }

    @Override
    protected void onHitBlock(
            BlockHitResult result
    ) {
        if (level().isClientSide) {
            return;
        }

        if (bouncesRemaining <= 0) {
            discard();
            return;
        }

        bouncesRemaining--;

        Vec3 velocity =
                getDeltaMovement();
        Vec3 reflected =
                switch (result.getDirection().getAxis()) {
                    case X -> new Vec3(
                            -velocity.x,
                            velocity.y,
                            velocity.z
                    );
                    case Y -> new Vec3(
                            velocity.x,
                            -velocity.y,
                            velocity.z
                    );
                    case Z -> new Vec3(
                            velocity.x,
                            velocity.y,
                            -velocity.z
                    );
                };

        if (reflected.lengthSqr() <= EPSILON) {
            discard();
            return;
        }

        reflected =
                reflected.normalize()
                        .scale(VELOCITY);
        setDeltaMovement(
                reflected
        );
        setPos(
                result.getLocation()
                        .add(
                                reflected.normalize()
                                        .scale(0.08D)
                        )
        );
        hasImpulse = true;
    }

    private LivingEntity resolveSummoner() {
        if (!(level() instanceof ServerLevel serverLevel)
                || summonerId == null) {
            return null;
        }

        Entity entity =
                serverLevel.getEntity(
                        summonerId
                );
        return entity instanceof LivingEntity living
                ? living
                : null;
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(tag);

        if (targetId != null) {
            tag.putUUID(
                    "Target",
                    targetId
            );
        }
        if (summonerId != null) {
            tag.putUUID(
                    "Summoner",
                    summonerId
            );
        }
        tag.putFloat(
                "Healing",
                healing
        );
        tag.putFloat(
                "Distance",
                distanceTraveled
        );
        tag.putFloat(
                "HomingStart",
                homingStartDistance
        );
        tag.putInt(
                "Bounces",
                bouncesRemaining
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(tag);

        targetId =
                tag.hasUUID("Target")
                        ? tag.getUUID("Target")
                        : null;
        summonerId =
                tag.hasUUID("Summoner")
                        ? tag.getUUID("Summoner")
                        : null;
        healing =
                tag.getFloat("Healing");
        distanceTraveled =
                tag.getFloat("Distance");
        homingStartDistance =
                tag.contains("HomingStart")
                        ? tag.getFloat("HomingStart")
                        : 0.0F;
        bouncesRemaining =
                tag.contains("Bounces")
                        ? tag.getInt("Bounces")
                        : 1;
        noCulling = true;
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks
                .getEntitySpawningPacket(this);
    }
}
