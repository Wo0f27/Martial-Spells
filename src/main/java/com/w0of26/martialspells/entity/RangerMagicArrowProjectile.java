package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.combat.RangerProjectileMath;
import com.w0of26.martialspells.client.visual.RangerSourceVfx;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.registry.MartialParticleRegistry;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.RangerMagicArrowSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Dedicated non-AbstractArrow projectile with integer pierce and saved flight history. */
public final class RangerMagicArrowProjectile extends Projectile {
    private UUID ownerId;
    private UUID followedTargetId;
    private final Set<UUID> successfulHits = new HashSet<>();
    private double sampledPower;
    private float chargeRatio;
    private double maxRange;
    private double distanceTraveled;
    private int pierceRemaining = RangerMagicArrowSpell.PIERCE;

    public RangerMagicArrowProjectile(EntityType<? extends RangerMagicArrowProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
    }

    public RangerMagicArrowProjectile(EntityType<? extends RangerMagicArrowProjectile> type,
                                      ServerLevel level, LivingEntity owner, UUID followedTargetId,
                                      double sampledPower, float chargeRatio) {
        this(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        this.followedTargetId = followedTargetId;
        this.sampledPower = sampledPower;
        this.chargeRatio = chargeRatio;
        maxRange = RangerMagicArrowSpell.BASE_RANGE + RangerMagicArrowSpell.RANGE_BONUS * chargeRatio;
    }

    @Override protected void defineSynchedData() {}

    @Override public void tick() {
        if (!(level() instanceof ServerLevel server)) {
            super.tick();
            RangerSourceVfx.magicTravel(level(), this);
            Vec3 visualStep = getDeltaMovement();
            setPos(getX() + visualStep.x, getY() + visualStep.y, getZ() + visualStep.z);
            updateRotation();
            return;
        }
        if (distanceTraveled >= maxRange || tickCount > 1200 || !server.hasChunkAt(blockPosition())) {
            discard(); return;
        }
        Entity owner = ownerId == null ? null : server.getEntity(ownerId);
        if (!(owner instanceof LivingEntity) || owner.isRemoved()) { discard(); return; }
        super.tick();
        if (isRemoved()) return;

        // SpellProjectile checks range/age, then collision, then homing, then movement.
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            onHit(hit);
            if (isRemoved()) return;
        }
        if (followedTargetId != null) {
            Entity followed = server.getEntity(followedTargetId);
            if (followed != null && !followed.isRemoved()) {
                Vec3 to = followed.position().add(0, followed.getBbHeight() * 0.5D, 0)
                        .subtract(position().add(0, getBbHeight() * 0.5D, 0));
                setDeltaMovement(RangerProjectileMath.turnTowards(getDeltaMovement(), to,
                        RangerMagicArrowSpell.HOMING_DEGREES_PER_TICK));
            } else followedTargetId = null;
        }
        Vec3 velocity = getDeltaMovement();
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        distanceTraveled += velocity.length();
        updateRotation();
    }

    @Override protected boolean canHitEntity(Entity candidate) {
        if (!super.canHitEntity(candidate) || !(candidate instanceof LivingEntity living)
                || !living.isAlive() || living.isSpectator() || successfulHits.contains(candidate.getUUID())) return false;
        Entity owner = getOwner();
        return owner instanceof LivingEntity caster && candidate != caster && !caster.isAlliedTo(candidate);
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(level() instanceof ServerLevel server) || !(hit.getEntity() instanceof LivingEntity target)
                || !(getOwner() instanceof LivingEntity caster) || successfulHits.contains(target.getUUID())) return;
        followedTargetId = null;
        float damage = (float) (RangerMagicArrowSpell.DAMAGE_COEFFICIENT * sampledPower * chargeRatio);
        target.hurt(MartialSpellRegistry.MAGIC_ARROW.get().getDamageSource(caster, this), damage);
        // Source damage impact reports success even when target.damage itself rejects the hit.
        successfulHits.add(target.getUUID());
        Vec3 direction = target.position().subtract(position());
        if (direction.horizontalDistanceSqr() > 1.0E-8D)
            target.knockback(0.4D * RangerMagicArrowSpell.KNOCKBACK_COEFFICIENT * chargeRatio,
                    getX() - target.getX(), getZ() - target.getZ());
        RangerSourceVfx.magicImpact(server, target);
        server.playSound(null, target.getX(), target.getY(), target.getZ(),
                MartialSoundRegistry.MAGIC_ARROW_IMPACT.get(), SoundSource.PLAYERS, 1, 1);
        if (pierceRemaining <= 0) { discard(); return; }
        pierceRemaining--;
        // SpellProjectile makes a tiny post-hit velocity adjustment to force a tracking update.
        double tiny = 0.01D * (-1D * (pierceRemaining % 2));
        setDeltaMovement(getDeltaMovement().scale(1.0D + tiny));
        hasImpulse = true;
    }

    @Override protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) discard();
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("OwnerId") ? tag.getUUID("OwnerId") : null;
        followedTargetId = tag.hasUUID("Followed") ? tag.getUUID("Followed") : null;
        sampledPower = tag.getDouble("SampledPower");
        chargeRatio = tag.getFloat("ChargeRatio");
        maxRange = tag.getDouble("MaxRange");
        distanceTraveled = tag.getDouble("DistanceTraveled");
        pierceRemaining = tag.getInt("PierceRemaining");
        successfulHits.clear();
        for (Tag item : tag.getList("SuccessfulHits", Tag.TAG_STRING)) {
            try { successfulHits.add(UUID.fromString(item.getAsString())); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("OwnerId", ownerId);
        if (followedTargetId != null) tag.putUUID("Followed", followedTargetId);
        tag.putDouble("SampledPower", sampledPower);
        tag.putFloat("ChargeRatio", chargeRatio);
        tag.putDouble("MaxRange", maxRange);
        tag.putDouble("DistanceTraveled", distanceTraveled);
        tag.putInt("PierceRemaining", pierceRemaining);
        ListTag hits = new ListTag();
        for (UUID hit : successfulHits) hits.add(StringTag.valueOf(hit.toString()));
        tag.put("SuccessfulHits", hits);
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
