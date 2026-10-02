package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.combat.RangerProjectileMath;
import com.w0of26.martialspells.client.visual.RangerSourceVfx;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialParticleRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.RainOfArrowsSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** Tracked FALL projectile; its own distance crossing performs the server impact. */
public final class RainMeteorProjectile extends Entity {
    private UUID ownerId;
    private UUID followedTargetId;
    private double sampledPower;
    private double distanceTraveled;

    public RainMeteorProjectile(EntityType<? extends RainMeteorProjectile> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public static void launch(ServerLevel level, ServerPlayer caster, UUID targetId, Vec3 anchor,
                              double sampledPower, int index) {
        RainMeteorProjectile meteor = new RainMeteorProjectile(MartialEntityRegistry.RAIN_METEOR.get(), level);
        meteor.ownerId = caster.getUUID();
        meteor.followedTargetId = index == 0 ? targetId : null;
        meteor.sampledPower = sampledPower;
        Vec3 offset = Vec3.ZERO;
        if (index > 0) {
            double angle = level.random.nextFloat() * Math.PI * 2.0D;
            offset = new Vec3(Math.cos(angle) * RainOfArrowsSpell.LAUNCH_RADIUS, 0,
                    Math.sin(angle) * RainOfArrowsSpell.LAUNCH_RADIUS);
        }
        meteor.setPos(anchor.x + offset.x, anchor.y + RainOfArrowsSpell.LAUNCH_HEIGHT,
                anchor.z + offset.z);
        meteor.setDeltaMovement(0, -RainOfArrowsSpell.VELOCITY, 0);
        level.addFreshEntity(meteor);
    }

    @Override protected void defineSynchedData() {}

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            RangerSourceVfx.rainTravel(level(), this);
            Vec3 visualStep = getDeltaMovement();
            setPos(getX() + visualStep.x, getY() + visualStep.y, getZ() + visualStep.z);
            return;
        }
        if (tickCount > 1200 || !server.hasChunkAt(blockPosition())) { discard(); return; }
        Entity owner = ownerId == null ? null : server.getEntity(ownerId);
        if (!(owner instanceof LivingEntity livingOwner) || owner.isRemoved()) { discard(); return; }
        double threshold = RainOfArrowsSpell.LAUNCH_HEIGHT * 0.98D;
        if (distanceTraveled >= threshold) {
            Vec3 velocity = getDeltaMovement();
            double excess = distanceTraveled - threshold;
            if (excess > 0 && velocity.lengthSqr() > 1.0E-8D)
                setPos(position().subtract(velocity.normalize().scale(excess)));
            impact(server, livingOwner);
            discard();
            return;
        }
        // Spell Engine FALL ignores regular block/entity ray hits. The optional reverse collision
        // interface has no target-stack counterpart; terrain does not terminate this delivery.
        if (followedTargetId != null) {
            Entity followed = server.getEntity(followedTargetId);
            if (followed != null) {
                Vec3 to = followed.position().add(0, followed.getBbHeight() * 0.5D, 0)
                        .subtract(position().add(0, getBbHeight() * 0.5D, 0));
                setDeltaMovement(RangerProjectileMath.turnTowards(getDeltaMovement(), to, 1.0D));
            }
        }
        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        distanceTraveled += movement.length();
    }

    private void impact(ServerLevel server, LivingEntity caster) {
        Vec3 center = position().add(0, 1, 0);
        double radius = RainOfArrowsSpell.IMPACT_RADIUS;
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius + 0.5D))) {
            if (!RainOfArrowsSpell.canAreaDamage(caster, target)
                    || target.getBoundingBox().getCenter().distanceToSqr(center) > radius * radius)
                continue;
            Vec3 nearest = target.getBoundingBox().getCenter();
            if (server.clip(new ClipContext(center, nearest, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.BLOCK) continue;
            target.hurt(MartialSpellRegistry.RAIN_OF_ARROWS.get().getDamageSource(caster, this),
                    (float) (RainOfArrowsSpell.DAMAGE_COEFFICIENT * sampledPower));
            Vec3 away = target.position().subtract(center);
            if (away.horizontalDistanceSqr() > 1.0E-8D)
                target.knockback(0.4D * RainOfArrowsSpell.KNOCKBACK,
                        center.x - target.getX(), center.z - target.getZ());
            RangerSourceVfx.rainDirectImpact(server, target);
        }
        RangerSourceVfx.rainAreaImpact(server, position().add(0, getBbHeight() * 0.5D, 0));
        // Spell Engine area_effect_658: one 16-frame ground decal, scale 1.5, gold tint.
        Vec3 ground = RangerSourceVfx.rainGround(server, this);
        server.sendParticles(MartialParticleRegistry.RANGER_RAIN_AREA.get(),
                ground.x, ground.y, ground.z, 0,
                RainOfArrowsSpell.IMPACT_RADIUS, 1.0D, 0.0D, 1.0D);
        server.playSound(null, getX(), getY(), getZ(), MartialSoundRegistry.RAIN_OF_ARROWS_IMPACT.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        followedTargetId = tag.hasUUID("Followed") ? tag.getUUID("Followed") : null;
        sampledPower = tag.getDouble("SampledPower");
        distanceTraveled = tag.getDouble("DistanceTraveled");
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        if (followedTargetId != null) tag.putUUID("Followed", followedTargetId);
        tag.putDouble("SampledPower", sampledPower);
        tag.putDouble("DistanceTraveled", distanceTraveled);
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
