package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.SpiritWolfEntity;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** One saved +5-tick wolf insertion, prepared from cast-time owner, yaw and power. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritWolfPending extends SavedData {
    public static final int SECOND_DELAY_TICKS = 5;
    private static final String DATA_ID = "martial_spells_spirit_wolf_pending";
    private final List<PreparedWolf> pending = new ArrayList<>();

    public static SpiritWolfPending get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SpiritWolfPending::load, SpiritWolfPending::new, DATA_ID);
    }

    public static void begin(ServerLevel level, ServerPlayer caster) {
        UUID ownerId = caster.getUUID();
        double power = NominalPhysicalRangedPower.get(caster);
        float yaw = caster.getYRot();
        Vec3 origin = caster.position();
        Vec3 front = placement(level, caster, origin, yaw, 0.0F);
        Vec3 right = placement(level, caster, origin, yaw, 90.0F);
        // Both entities are conceptually prepared at release. Source world insertion is
        // scheduled at 0/+5; the +5 descriptor deliberately survives a world save.
        PreparedWolf first = new PreparedWolf(UUID.randomUUID(), ownerId, power, front, yaw, level.getGameTime());
        PreparedWolf second = new PreparedWolf(UUID.randomUUID(), ownerId, power, right, yaw,
                level.getGameTime() + SECOND_DELAY_TICKS);
        insert(level, first);
        SpiritWolfPending data = get(level);
        data.pending.add(second);
        data.setDirty();
    }

    private static Vec3 placement(ServerLevel level, ServerPlayer caster, Vec3 origin, float yaw, float offset) {
        Vec3 desired = origin.add(Vec3.directionFromRotation(0.0F, yaw + offset));
        Vec3 search = desired;
        if (level.getBlockState(BlockPos.containing(search)).isSolid()) search = search.add(0, 2, 0);
        BlockHitResult hit = level.clip(new ClipContext(search.add(0, 1, 0), search.add(0, -20, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 ground = hit.getType() == HitResult.Type.BLOCK
                ? new Vec3(desired.x, hit.getLocation().y, desired.z) : desired;
        SpiritWolfEntity probe = MartialEntityRegistry.SPIRIT_WOLF.get().create(level);
        if (probe == null) return ground;
        for (int lift = 0; lift <= 2; lift++) {
            Vec3 candidate = ground.add(0, lift, 0);
            boolean blocked = level.getBlockCollisions(probe, probe.getDimensions(probe.getPose()).makeBoundingBox(candidate))
                    .iterator().hasNext();
            if (!blocked) return candidate;
        }
        return ground; // exact source fallback if the narrow upward lift cannot make room
    }

    private static void insert(ServerLevel level, PreparedWolf prepared) {
        if (level.getEntity(prepared.entityId) != null) return;
        SpiritWolfEntity wolf = MartialEntityRegistry.SPIRIT_WOLF.get().create(level);
        if (wolf == null) return;
        wolf.setUUID(prepared.entityId);
        wolf.prepare(prepared.ownerId, prepared.power);
        wolf.moveTo(prepared.position.x, prepared.position.y, prepared.position.z, prepared.yaw, 0.0F);
        wolf.setYHeadRot(prepared.yaw);
        wolf.setYBodyRot(prepared.yaw);
        level.addFreshEntity(wolf);
    }

    @SubscribeEvent public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        SpiritWolfPending data = get(level);
        Iterator<PreparedWolf> it = data.pending.iterator();
        while (it.hasNext()) {
            PreparedWolf prepared = it.next();
            if (level.getGameTime() < prepared.deadline) continue;
            it.remove();
            data.setDirty();
            // Spell Engine's prepared delayed insertion has no caster-alive, online or
            // same-dimension recheck. The descriptor likewise keeps its original world.
            insert(level, prepared);
        }
    }

    private static SpiritWolfPending load(CompoundTag root) {
        SpiritWolfPending data = new SpiritWolfPending();
        ListTag list = root.getList("Pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (!tag.hasUUID("Entity") || !tag.hasUUID("Owner")) continue;
            data.pending.add(new PreparedWolf(tag.getUUID("Entity"), tag.getUUID("Owner"),
                    tag.getDouble("Power"), new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                    tag.getFloat("Yaw"), tag.getLong("Deadline")));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (PreparedWolf prepared : pending) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Entity", prepared.entityId);
            tag.putUUID("Owner", prepared.ownerId);
            tag.putDouble("Power", prepared.power);
            tag.putDouble("X", prepared.position.x);
            tag.putDouble("Y", prepared.position.y);
            tag.putDouble("Z", prepared.position.z);
            tag.putFloat("Yaw", prepared.yaw);
            tag.putLong("Deadline", prepared.deadline);
            list.add(tag);
        }
        root.put("Pending", list);
        return root;
    }

    private record PreparedWolf(UUID entityId, UUID ownerId, double power, Vec3 position,
                                float yaw, long deadline) {}
}
