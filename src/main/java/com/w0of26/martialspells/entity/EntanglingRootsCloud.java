package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.client.visual.RangerSourceVfx;
import com.w0of26.martialspells.combat.PhysicalMeleePower;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** Fixed-position server cloud with saved age and caster identity. */
public final class EntanglingRootsCloud extends Entity {
    public static final int LIFETIME_TICKS = 160;
    public static final int PULSE_INTERVAL_TICKS = 15;
    public static final int LAST_PULSE_AGE = 150;
    public static final int EFFECT_TICKS = 20;
    public static final double RADIUS = 3.5D;
    public static final double VERTICAL_EXPANSION = RADIUS * 0.3D + 0.5D;
    public static final double HEALTH_BASE = 50.0D;
    public static final double MELEE_POWER_MULTIPLIER = 5.0D;

    private UUID ownerUuid;
    private int ageTicks;

    public EntanglingRootsCloud(EntityType<? extends EntanglingRootsCloud> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public EntanglingRootsCloud(ServerLevel level, LivingEntity owner) {
        this(MartialEntityRegistry.ENTANGLING_ROOTS_CLOUD.get(), level);
        ownerUuid = owner.getUUID();
    }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        ageTicks++;
        if (ageTicks >= LIFETIME_TICKS) {
            discard();
            return;
        }
        RangerSourceVfx.rootsCloudTick(server, position(), getBbHeight(), getBbWidth());
        if (ageTicks >= PULSE_INTERVAL_TICKS && ageTicks <= LAST_PULSE_AGE
                && ageTicks % PULSE_INTERVAL_TICKS == 0) pulse(server);
    }

    private void pulse(ServerLevel server) {
        Entity ownerEntity = ownerUuid == null ? null : server.getEntity(ownerUuid);
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) return;
        // cloud.volume.sound is an area-impact sound, emitted on every due pulse.
        server.playSound(null, blockPosition(), MartialSoundRegistry.ENTANGLING_ROOTS.get(),
                SoundSource.PLAYERS, 1.0F, 0.9F + server.getRandom().nextFloat() * 0.2F);
        double healthLimit = PhysicalMeleePower.controlHealthLimit(owner, HEALTH_BASE, MELEE_POWER_MULTIPLIER);
        AABB candidates = getBoundingBox().inflate(0.0D, VERTICAL_EXPANSION, 0.0D);
        Vec3 origin = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, candidates)) {
            if (target.getBoundingBox().getCenter().distanceToSqr(position()) > RADIUS * RADIUS
                    || !canAffect(owner, target) || target.getMaxHealth() > healthLimit
                    || !visible(server, origin, target)) continue;
            target.addEffect(new MobEffectInstance(MartialEffectRegistry.ENTANGLING_ROOTS.get(),
                    EFFECT_TICKS, 0, false, true, true));
            RangerSourceVfx.rootsImpact(server, target);
        }
    }

    private static boolean canAffect(LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || owner.isAlliedTo(target)) return false;
        // Spell Engine's default harmful AREA relation admits NEUTRAL/HOSTILE/MIXED,
        // but treats these player-protected entity classes as FRIENDLY.
        if (target instanceof Player || target instanceof Villager || target instanceof Allay
                || target instanceof IronGolem || target instanceof Cat) return false;
        return !(target instanceof TamableAnimal tameable && tameable.isTame());
    }

    private static boolean visible(ServerLevel level, Vec3 origin, LivingEntity target) {
        AABB box = target.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 nearest = new Vec3(Mth.clamp(origin.x, box.minX, box.maxX),
                Mth.clamp(origin.y, box.minY, box.maxY),
                Mth.clamp(origin.z, box.minZ, box.maxZ));
        return clearRay(level, origin, center, target)
                || clearRay(level, origin, nearest, target);
    }

    private static boolean clearRay(ServerLevel level, Vec3 origin, Vec3 end, LivingEntity target) {
        if (origin.distanceToSqr(end) < 1.0E-8D) return true;
        return level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ageTicks = Mth.clamp(tag.getInt("AgeTicks"), 0, LIFETIME_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
        tag.putInt("AgeTicks", ageTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
