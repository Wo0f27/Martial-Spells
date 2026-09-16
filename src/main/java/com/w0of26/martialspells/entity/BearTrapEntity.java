package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.mixin.EntityInvulnerabilityAccessor;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Dedicated Forge-side translation of frozen Rogues' SpellCloud Bear Trap.
 *
 * <p>The entity preserves the source lifecycle: placement delay, 20-tick spawn
 * warm-up, 20 seconds ACTIVE, then 15 ticks of normal despawn. A successful
 * one-shot impact instead enters the source BearTrapEntity's bespoke 30-tick
 * sprung/despawn phase.</p>
 */
public final class BearTrapEntity extends Entity {
    public static final int PHASE_PENDING = 0;
    public static final int PHASE_SPAWNING = 1;
    public static final int PHASE_ACTIVE = 2;
    public static final int PHASE_DESPAWNING = 3;

    public static final int SPAWN_TICKS = 20;
    public static final int ACTIVE_TICKS = 20 * 20;
    public static final int NORMAL_DESPAWN_TICKS = 15;
    public static final int ATTACK_TICKS = 30;
    public static final int IMPACT_INTERVAL_TICKS = 2;
    public static final int ROOT_DURATION_TICKS = 3 * 20;
    public static final float RADIUS = 0.6F;
    public static final float VERTICAL_RANGE_MULTIPLIER = 0.5F;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(BearTrapEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(BearTrapEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SPRUNG =
            SynchedEntityData.defineId(BearTrapEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PLACEMENT_DELAY =
            SynchedEntityData.defineId(BearTrapEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE_POWER =
            SynchedEntityData.defineId(BearTrapEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CONTROL_LIMIT =
            SynchedEntityData.defineId(BearTrapEntity.class, EntityDataSerializers.FLOAT);

    private int phaseTicks;
    private int clientPhaseStartTick;

    public BearTrapEntity(EntityType<? extends BearTrapEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void configure(
            ServerPlayer owner,
            Vec3 position,
            float yaw,
            int placementDelayTicks,
            float damagePower,
            float controlLimit
    ) {
        setPos(position.x, position.y, position.z);
        setYRot(yaw);
        setXRot(0.0F);
        entityData.set(OWNER_ID, Optional.of(owner.getUUID()));
        entityData.set(PLACEMENT_DELAY, Math.max(0, placementDelayTicks));
        entityData.set(DAMAGE_POWER, Math.max(0.0F, damagePower));
        entityData.set(CONTROL_LIMIT, Math.max(0.0F, controlLimit));
        entityData.set(PHASE, PHASE_PENDING);
        phaseTicks = 0;
    }

    /** Call after addFreshEntity so delay-0 placement matches source timing. */
    public void beginImmediatePlacementIfNeeded() {
        if (!level().isClientSide && getPlacementDelayTicks() == 0 && getPhase() == PHASE_PENDING) {
            beginSpawning();
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, Optional.empty());
        entityData.define(PHASE, PHASE_PENDING);
        entityData.define(SPRUNG, false);
        entityData.define(PLACEMENT_DELAY, 0);
        entityData.define(DAMAGE_POWER, 0.0F);
        entityData.define(CONTROL_LIMIT, 0.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PHASE.equals(key)) {
            clientPhaseStartTick = tickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        phaseTicks++;
        switch (getPhase()) {
            case PHASE_PENDING -> {
                if (phaseTicks >= getPlacementDelayTicks()) {
                    beginSpawning();
                }
            }
            case PHASE_SPAWNING -> {
                if (phaseTicks >= SPAWN_TICKS) {
                    setPhase(PHASE_ACTIVE);
                }
            }
            case PHASE_ACTIVE -> {
                if (phaseTicks >= ACTIVE_TICKS) {
                    beginDespawning(false, NORMAL_DESPAWN_TICKS);
                } else if ((phaseTicks % IMPACT_INTERVAL_TICKS) == 0) {
                    attemptImpact();
                }
            }
            case PHASE_DESPAWNING -> {
                int duration = isSprung() ? ATTACK_TICKS : NORMAL_DESPAWN_TICKS;
                if (phaseTicks >= duration) {
                    discard();
                }
            }
            default -> discard();
        }
    }

    private void beginSpawning() {
        setPhase(PHASE_SPAWNING);
        playSoundAtSelf(MartialSoundRegistry.BEAR_TRAP_SPAWN.get());
    }

    private void beginDespawning(boolean sprung, int ticks) {
        if (getPhase() == PHASE_DESPAWNING) {
            return;
        }
        entityData.set(SPRUNG, sprung);
        setPhase(PHASE_DESPAWNING);
        playSoundAtSelf(MartialSoundRegistry.BEAR_TRAP_DESPAWN.get());
        if (ticks <= 0) {
            discard();
        }
    }

    private void setPhase(int phase) {
        entityData.set(PHASE, phase);
        phaseTicks = 0;
    }

    private void attemptImpact() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer owner = getOwnerPlayer(serverLevel);
        if (owner == null) {
            return;
        }

        double horizontal = RADIUS;
        double vertical = RADIUS * VERTICAL_RANGE_MULTIPLIER;
        AABB search = getBoundingBox().inflate(
                horizontal + 0.5D,
                vertical + 0.5D,
                horizontal + 0.5D
        );

        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                search,
                target -> isEligibleTarget(owner, target) && withinSourceRadius(target)
        );

        if (candidates.isEmpty()) {
            return;
        }

        LivingEntity target = candidates.stream()
                .min((a, b) -> Double.compare(distanceToBoxSqr(a), distanceToBoxSqr(b)))
                .orElse(null);
        if (target == null) {
            return;
        }

        performImpact(owner, target, serverLevel);
        entityData.set(SPRUNG, true);
        beginDespawning(true, ATTACK_TICKS);
    }

    private boolean isEligibleTarget(ServerPlayer owner, LivingEntity target) {
        return target != owner
                && target.isAlive()
                && !target.isDeadOrDying()
                && !target.isRemoved()
                && !target.isSpectator()
                && target.isAttackable()
                && !owner.isAlliedTo(target);
    }

    /**
     * Spell Engine uses closest-hitbox distance for sub-one-block areas. The
     * 0.5 vertical multiplier limits the candidate box but does not replace the
     * source 0.6-block distance test.
     */
    private boolean withinSourceRadius(LivingEntity target) {
        return distanceToBoxSqr(target) <= RADIUS * RADIUS;
    }

    private double distanceToBoxSqr(LivingEntity target) {
        AABB box = target.getBoundingBox();
        Vec3 origin = position();
        double x = Mth.clamp(origin.x, box.minX, box.maxX);
        double y = Mth.clamp(origin.y, box.minY, box.maxY);
        double z = Mth.clamp(origin.z, box.minZ, box.maxZ);
        return origin.distanceToSqr(x, y, z);
    }

    private void performImpact(ServerPlayer owner, LivingEntity target, ServerLevel level) {
        Vec3 originalVelocity = target.getDeltaMovement();
        EntityInvulnerabilityAccessor invulnerability = (EntityInvulnerabilityAccessor) target;
        int originalInvulnerableTime = invulnerability.martialSpells$getInvulnerableTime();

        try {
            // Spell Engine damage impacts bypass normal hurt i-frames by default.
            invulnerability.martialSpells$setInvulnerableTime(0);
            target.hurt(
                    MartialDamageTypes.bearTrap(owner, this),
                    entityData.get(DAMAGE_POWER)
            );
        } finally {
            // Source restores the pre-impact i-frame timer and its configured
            // knockback is exactly zero. Restore both pieces explicitly.
            invulnerability.martialSpells$setInvulnerableTime(originalInvulnerableTime);
            target.setDeltaMovement(originalVelocity);
            target.hurtMarked = true;
        }

        if (target.getMaxHealth() <= entityData.get(CONTROL_LIMIT)) {
            target.addEffect(
                    new MobEffectInstance(
                            MartialEffectRegistry.BEAR_TRAP.get(),
                            ROOT_DURATION_TICKS,
                            0,
                            false,
                            true,
                            true
                    ),
                    owner
            );
        }

        level.playSound(
                null,
                getX(),
                getY(),
                getZ(),
                MartialSoundRegistry.BEAR_TRAP_IMPACT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        // Frozen source: 10 white magic_spark particles in a sphere. Keep the
        // exact count/topology with vanilla CRIT rather than importing Spell Engine.
        level.sendParticles(
                ParticleTypes.CRIT,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                10,
                0.35D,
                Math.max(0.2D, target.getBbHeight() * 0.25D),
                0.35D,
                0.22D
        );
    }

    private ServerPlayer getOwnerPlayer(ServerLevel level) {
        Optional<UUID> ownerId = entityData.get(OWNER_ID);
        if (ownerId.isEmpty()) {
            return null;
        }
        var player = level.getPlayerByUUID(ownerId.get());
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private void playSoundAtSelf(SoundEvent sound) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    getX(), getY(), getZ(),
                    sound,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    public int getPhase() {
        return entityData.get(PHASE);
    }

    public boolean isPlacementPending() {
        return getPhase() == PHASE_PENDING;
    }

    public boolean isSprung() {
        return entityData.get(SPRUNG);
    }

    public int getPlacementDelayTicks() {
        return entityData.get(PLACEMENT_DELAY);
    }

    public float getClientPhaseAge(float partialTick) {
        return Math.max(0.0F, tickCount - clientPhaseStartTick + partialTick);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER_ID, Optional.of(tag.getUUID("Owner")));
        }
        entityData.set(PHASE, tag.getInt("Phase"));
        entityData.set(SPRUNG, tag.getBoolean("Sprung"));
        entityData.set(PLACEMENT_DELAY, tag.getInt("PlacementDelay"));
        entityData.set(DAMAGE_POWER, tag.getFloat("DamagePower"));
        entityData.set(CONTROL_LIMIT, tag.getFloat("ControlLimit"));
        phaseTicks = tag.getInt("PhaseTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        entityData.get(OWNER_ID).ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putInt("Phase", getPhase());
        tag.putBoolean("Sprung", isSprung());
        tag.putInt("PlacementDelay", getPlacementDelayTicks());
        tag.putFloat("DamagePower", entityData.get(DAMAGE_POWER));
        tag.putFloat("ControlLimit", entityData.get(CONTROL_LIMIT));
        tag.putInt("PhaseTicks", phaseTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
