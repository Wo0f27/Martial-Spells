package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.client.visual.RangerSourceVfx;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/** The Archers direwolf's Forge-owned lifecycle and combat translation. */
public final class SpiritWolfEntity extends PathfinderMob implements OwnableEntity {
    public static final int SPAWN_TICKS = 20;
    public static final int ACTIVE_TICKS = 600;
    public static final int DESPAWN_TICKS = 20;
    public static final int TOTAL_TICKS = SPAWN_TICKS + ACTIVE_TICKS + DESPAWN_TICKS;
    public static final int BITE_TICKS = 14;
    public static final int BITE_WINDUP_TICKS = 7;

    public static final byte SPAWNING = 0;
    public static final byte ACTIVE = 1;
    public static final byte DESPAWNING = 2;

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(SpiritWolfEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(SpiritWolfEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> LIFE_AGE =
            SynchedEntityData.defineId(SpiritWolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_START =
            SynchedEntityData.defineId(SpiritWolfEntity.class, EntityDataSerializers.INT);

    public final AnimationState spawnAnimationState = new AnimationState();
    public final AnimationState despawnAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();

    private double rangedPowerSnapshot;
    private int lifeAge;
    @Nullable private UUID savedTargetId;
    private int targetReloadChecks;
    private int ownerLastHurtSeen;
    private int ownerLastAttackSeen;
    private int selfLastHurtSeen;
    @Nullable private Vec3 lastOwnerForward;

    public SpiritWolfEntity(EntityType<? extends SpiritWolfEntity> type, Level level) {
        super(type, level);
        setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        spawnAnimationState.startIfStopped(0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER, Optional.empty());
        entityData.define(PHASE, SPAWNING);
        entityData.define(LIFE_AGE, 0);
        entityData.define(ATTACK_START, -1);
    }

    public void prepare(UUID ownerId, double nominalRangedPower) {
        entityData.set(OWNER, Optional.of(ownerId));
        rangedPowerSnapshot = Math.max(0.0D, nominalRangedPower);
        applyScaling();
        setHealth(getMaxHealth());
    }

    private void applyScaling() {
        double p = rangedPowerSnapshot;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D + p);
        getAttribute(Attributes.ARMOR).setBaseValue(10.0D + 0.1D * p);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0D + 0.3D * p);
        getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0.1D * p);
        // The source adds 5 + .05D. Vanilla's effective attribute clamps this to its cap.
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(5.0D + 0.05D * p);
    }

    @Override @Nullable public UUID getOwnerUUID() { return entityData.get(OWNER).orElse(null); }

    @Override @Nullable public LivingEntity getOwner() {
        UUID ownerId = getOwnerUUID();
        if (ownerId == null) return null;
        if (level() instanceof ServerLevel server) return server.getPlayerByUUID(ownerId);
        return level().players().stream().filter(player -> ownerId.equals(player.getUUID())).findFirst().orElse(null);
    }

    public int lifeAge() { return level().isClientSide ? entityData.get(LIFE_AGE) : lifeAge; }
    public byte phase() { return entityData.get(PHASE); }
    public int attackStartAge() { return entityData.get(ATTACK_START); }
    public boolean isActivePhase() { return phase() == ACTIVE; }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            updateAnimations();
            return;
        }
        lifeAge++;
        entityData.set(LIFE_AGE, lifeAge);
        if (lifeAge == 1 && level() instanceof ServerLevel server) {
            server.playSound(null, blockPosition(), MartialSoundRegistry.SPIRIT_WOLF_SPAWN.get(),
                    SoundSource.NEUTRAL, 0.75F, 1.0F);
            RangerSourceVfx.spiritWolfSpawn(server, position(), getBbWidth(), getBbHeight());
        }
        if (lifeAge >= TOTAL_TICKS) {
            discard();
            return;
        }
        byte next = lifeAge < SPAWN_TICKS ? SPAWNING
                : lifeAge < SPAWN_TICKS + ACTIVE_TICKS ? ACTIVE : DESPAWNING;
        if (next != phase()) {
            entityData.set(PHASE, next);
            if (next == DESPAWNING && level() instanceof ServerLevel server) {
                server.playSound(null, blockPosition(), SoundEvents.WOLF_WHINE, SoundSource.NEUTRAL, 0.75F, 1.0F);
                RangerSourceVfx.spiritWolfDespawn(server, position(), getBbWidth(), getBbHeight());
                getNavigation().stop();
                setTarget(null);
            }
        }
        if (savedTargetId != null && targetReloadChecks++ < 20 && level() instanceof ServerLevel server) {
            Entity entity = server.getEntity(savedTargetId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                setTarget(living);
                savedTargetId = null;
            }
        }
    }

    private void updateAnimations() {
        spawnAnimationState.animateWhen(phase() == SPAWNING, tickCount);
        despawnAnimationState.animateWhen(phase() == DESPAWNING, tickCount);
        idleAnimationState.animateWhen(phase() == ACTIVE, tickCount);
        int attackAt = attackStartAge();
        attackAnimationState.animateWhen(attackAt >= 0 && lifeAge() - attackAt < BITE_TICKS, tickCount);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PhaseLockGoal());
        goalSelector.addGoal(2, new TeleportOwnerGoal());
        goalSelector.addGoal(10, new BiteGoal());
        goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(12, new FollowOwnerGoal());
        goalSelector.addGoal(13, new RandomStrollGoal(this, 1.0D, 1000));
        goalSelector.addGoal(14, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerDefenseGoal());
        targetSelector.addGoal(2, new RevengeCloserGoal());
        targetSelector.addGoal(3, new OwnerAttackGoal());
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10,
                true, false, this::canAutoTarget));
    }

    private boolean canTargetForOwner(@Nullable LivingEntity candidate) {
        LivingEntity owner = getOwner();
        if (owner == null || candidate == null || candidate == owner || candidate == this || !candidate.isAlive()) return false;
        if (candidate instanceof SpiritWolfEntity other && owner.getUUID().equals(other.getOwnerUUID())) return false;
        if (candidate instanceof TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID())) return false;
        return !owner.isAlliedTo(candidate) && !candidate.isAlliedTo(owner);
    }

    private boolean canAutoTarget(@Nullable LivingEntity candidate) {
        // Spell Engine's default relation classifies passives as NEUTRAL; autonomous HOSTILE
        // acquisition excludes them, while owner-directed and retaliation goals permit neutral.
        return candidate instanceof Enemy && canTargetForOwner(candidate);
    }

    @Override public boolean isAlliedTo(Entity other) {
        if (super.isAlliedTo(other)) return true;
        UUID ownerId = getOwnerUUID();
        if (ownerId == null) return false;
        if (ownerId.equals(other.getUUID())) return true;
        if (other instanceof SpiritWolfEntity wolf && ownerId.equals(wolf.getOwnerUUID())) return true;
        LivingEntity owner = getOwner();
        return owner != null && owner.isAlliedTo(other);
    }

    @Override public boolean canCollideWith(Entity other) {
        LivingEntity owner = getOwner();
        return owner != null && other instanceof LivingEntity living
                && canTargetForOwner(living) && super.canCollideWith(other);
    }

    @Override public boolean doHurtTarget(Entity target) {
        return target instanceof LivingEntity living && canTargetForOwner(living) && super.doHurtTarget(target);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WOLF_PANT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WOLF_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WOLF_DEATH; }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.WOLF_STEP, 0.1125F, 1.0F);
    }
    @Override protected float getSoundVolume() { return 0.75F; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID ownerId = getOwnerUUID();
        if (ownerId != null) tag.putUUID("SpiritOwner", ownerId);
        tag.putInt("SpiritAge", lifeAge);
        tag.putDouble("SpiritPower", rangedPowerSnapshot);
        if (getTarget() != null) tag.putUUID("SpiritTarget", getTarget().getUUID());
        tag.putInt("SpiritAttackStart", attackStartAge());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("SpiritOwner")) entityData.set(OWNER, Optional.of(tag.getUUID("SpiritOwner")));
        lifeAge = Math.max(0, tag.getInt("SpiritAge"));
        entityData.set(LIFE_AGE, lifeAge);
        entityData.set(PHASE, lifeAge < SPAWN_TICKS ? SPAWNING
                : lifeAge < SPAWN_TICKS + ACTIVE_TICKS ? ACTIVE : DESPAWNING);
        rangedPowerSnapshot = Math.max(0.0D, tag.getDouble("SpiritPower"));
        float health = getHealth();
        applyScaling();
        setHealth(Math.min(health, getMaxHealth()));
        savedTargetId = tag.hasUUID("SpiritTarget") ? tag.getUUID("SpiritTarget") : null;
        entityData.set(ATTACK_START, tag.getInt("SpiritAttackStart"));
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private final class PhaseLockGoal extends Goal {
        private PhaseLockGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return !isActivePhase(); }
        @Override public boolean canContinueToUse() { return !isActivePhase(); }
    }

    private final class TeleportOwnerGoal extends Goal {
        private TeleportOwnerGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            LivingEntity owner = getOwner();
            return owner != null && distanceToSqr(owner) > 32.0D * 32.0D;
        }
        @Override public boolean canContinueToUse() { return false; }
        @Override public void start() {
            LivingEntity owner = getOwner();
            if (owner == null) return;
            // The source teleports to owner.position() without testing terrain. Search only
            // nearby safe positions so the target stack never embeds a summon in solid blocks.
            Vec3 center = owner.position();
            int[][] offsets = {{0,0}, {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {-1,-1}};
            for (int[] offset : offsets) {
                for (int lift = 0; lift <= 1; lift++) {
                    Vec3 candidate = center.add(offset[0], lift, offset[1]);
                    BlockPos feet = BlockPos.containing(candidate);
                    if (!level().getFluidState(feet).isEmpty()) continue;
                    if (!level().noCollision(SpiritWolfEntity.this,
                            getBoundingBox().move(candidate.subtract(position())))) continue;
                    teleportTo(candidate.x, candidate.y, candidate.z);
                    setTarget(null);
                    getNavigation().stop();
                    return;
                }
            }
        }
    }

    private final class FollowOwnerGoal extends Goal {
        private FollowOwnerGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            LivingEntity owner = getOwner();
            return owner != null && !hasLiveTarget() && distanceToSqr(owner) > 100.0D;
        }
        @Override public boolean canContinueToUse() {
            LivingEntity owner = getOwner();
            return owner != null && !hasLiveTarget() && distanceToSqr(owner) > 16.0D;
        }
        @Override public void start() { tick(); }
        @Override public void tick() {
            LivingEntity owner = getOwner();
            if (owner == null) return;
            Vec3 velocity = owner.getDeltaMovement();
            double horizontal = Math.hypot(velocity.x, velocity.z);
            if (horizontal > 0.02D) lastOwnerForward = new Vec3(velocity.x / horizontal, 0, velocity.z / horizontal);
            Vec3 forward = lastOwnerForward == null ? new Vec3(0, 0, -1) : lastOwnerForward;
            Vec3 right = new Vec3(-forward.z, 0, forward.x);
            Vec3 destination = owner.position().add(right.scale(2.0D));
            getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0D);
        }
        @Override public void stop() { getNavigation().stop(); }
    }

    private boolean hasLiveTarget() { return getTarget() != null && getTarget().isAlive(); }

    private final class OwnerDefenseGoal extends Goal {
        private OwnerDefenseGoal() { setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            LivingEntity owner = getOwner();
            if (owner == null || owner.getLastHurtByMobTimestamp() == ownerLastHurtSeen) return false;
            return canTargetForOwner(owner.getLastHurtByMob());
        }
        @Override public void start() {
            LivingEntity owner = getOwner();
            if (owner != null) {
                setTarget(owner.getLastHurtByMob());
                ownerLastHurtSeen = owner.getLastHurtByMobTimestamp();
            }
        }
    }

    private final class RevengeCloserGoal extends Goal {
        private RevengeCloserGoal() { setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            LivingEntity attacker = getLastHurtByMob();
            int hitTime = getLastHurtByMobTimestamp();
            if (hitTime == selfLastHurtSeen) return false;
            if (!canTargetForOwner(attacker)) {
                selfLastHurtSeen = hitTime;
                return false;
            }
            return !hasLiveTarget() || distanceToSqr(attacker) < distanceToSqr(getTarget());
        }
        @Override public void start() {
            setTarget(getLastHurtByMob());
            selfLastHurtSeen = getLastHurtByMobTimestamp();
        }
    }

    private final class OwnerAttackGoal extends Goal {
        private OwnerAttackGoal() { setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            LivingEntity owner = getOwner();
            if (owner == null || owner.getLastHurtMobTimestamp() == ownerLastAttackSeen) return false;
            return canTargetForOwner(owner.getLastHurtMob());
        }
        @Override public void start() {
            LivingEntity owner = getOwner();
            if (owner != null) {
                setTarget(owner.getLastHurtMob());
                ownerLastAttackSeen = owner.getLastHurtMobTimestamp();
            }
        }
    }

    private final class BiteGoal extends Goal {
        private int swingTick = -1;
        private int lastSwingAge = -BITE_TICKS;
        private BiteGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return isActivePhase() && hasLiveTarget(); }
        @Override public boolean canContinueToUse() { return swingTick >= 0 || canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { swingTick = -1; getNavigation().stop(); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) { swingTick = -1; return; }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double reach = getBbWidth() * 2.0D + target.getBbWidth();
            double distance = distanceToSqr(target);
            if (swingTick < 0 && distance <= reach * reach && lifeAge - lastSwingAge >= BITE_TICKS) {
                swingTick = 0;
                lastSwingAge = lifeAge;
                entityData.set(ATTACK_START, lifeAge);
                playSound(SoundEvents.WOLF_GROWL, 0.5F, 0.9F + random.nextFloat() * 0.2F);
            }
            double moveSpeed = swingTick >= 0 ? 0.6D : 1.2D;
            Vec3 toTarget = target.position().subtract(position());
            double horizontalLength = Math.hypot(toTarget.x, toTarget.z);
            double dot = horizontalLength > 1.0E-6D
                    ? target.getDeltaMovement().dot(new Vec3(toTarget.x / horizontalLength, 0, toTarget.z / horizontalLength))
                    : 0.0D;
            double holdFraction = dot > 0.01D ? 0.5D : dot < -0.01D ? 0.9D : 0.7D;
            if (distance > reach * reach * holdFraction * holdFraction) getNavigation().moveTo(target, moveSpeed);
            else getNavigation().stop();
            if (swingTick >= 0) {
                if (swingTick == BITE_WINDUP_TICKS && distance <= reach * reach * 1.21D
                        && canTargetForOwner(target)) {
                    playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.75F, 0.9F + random.nextFloat() * 0.2F);
                    doHurtTarget(target);
                }
                if (++swingTick >= BITE_TICKS) swingTick = -1;
            }
        }
    }
}
