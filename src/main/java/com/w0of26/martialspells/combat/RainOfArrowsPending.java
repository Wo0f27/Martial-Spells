package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.RainMeteorProjectile;
import com.w0of26.martialspells.spells.RainOfArrowsSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Saved per-dimension Rain deadlines. One job owns indices 1..20; index zero fires at release. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RainOfArrowsPending extends SavedData {
    private static final String DATA_ID = "martial_spells_rain_of_arrows_pending";
    private final List<Job> jobs = new ArrayList<>();

    public static RainOfArrowsPending get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RainOfArrowsPending::load,
                RainOfArrowsPending::new, DATA_ID);
    }

    public static void begin(ServerLevel level, ServerPlayer caster, UUID targetId, Vec3 anchor,
                             double sampledPower) {
        RainMeteorProjectile.launch(level, caster, targetId, anchor, sampledPower, 0);
        Job job = new Job(caster.getUUID(), targetId, anchor, sampledPower, 1,
                level.getGameTime() + RainOfArrowsSpell.LAUNCH_DELAY_TICKS);
        RainOfArrowsPending data = get(level);
        data.jobs.add(job);
        data.setDirty();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        RainOfArrowsPending data = get(level);
        Iterator<Job> it = data.jobs.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (level.getGameTime() < job.nextDeadline) continue;
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(job.casterId);
            if (caster == null || caster.isRemoved() || !caster.isAlive() || caster.level() != level) {
                it.remove();
                data.setDirty();
                continue;
            }
            Entity target = job.targetId == null ? null : level.getEntity(job.targetId);
            if (target != null) job.lastAnchor = target.position();
            // The captured source target reference is never replaced. A removed or unloaded target
            // leaves its last accessible position; no fresh target is selected.
            RainMeteorProjectile.launch(level, caster, job.targetId, job.lastAnchor,
                    job.sampledPower, job.nextIndex);
            job.nextIndex++;
            job.nextDeadline += RainOfArrowsSpell.LAUNCH_DELAY_TICKS;
            if (job.nextIndex >= RainOfArrowsSpell.LAUNCH_COUNT) it.remove();
            data.setDirty();
        }
    }

    private static RainOfArrowsPending load(CompoundTag root) {
        RainOfArrowsPending data = new RainOfArrowsPending();
        ListTag list = root.getList("Jobs", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (!tag.hasUUID("Caster")) continue;
            UUID target = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
            Vec3 anchor = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
            int index = tag.getInt("NextIndex");
            if (index > 0 && index < RainOfArrowsSpell.LAUNCH_COUNT)
                data.jobs.add(new Job(tag.getUUID("Caster"), target, anchor,
                        tag.getDouble("Power"), index, tag.getLong("Deadline")));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (Job job : jobs) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Caster", job.casterId);
            if (job.targetId != null) tag.putUUID("Target", job.targetId);
            tag.putDouble("X", job.lastAnchor.x);
            tag.putDouble("Y", job.lastAnchor.y);
            tag.putDouble("Z", job.lastAnchor.z);
            tag.putDouble("Power", job.sampledPower);
            tag.putInt("NextIndex", job.nextIndex);
            tag.putLong("Deadline", job.nextDeadline);
            list.add(tag);
        }
        root.put("Jobs", list);
        return root;
    }

    private static final class Job {
        private final UUID casterId;
        private final UUID targetId;
        private Vec3 lastAnchor;
        private final double sampledPower;
        private int nextIndex;
        private long nextDeadline;

        private Job(UUID casterId, UUID targetId, Vec3 lastAnchor, double sampledPower,
                    int nextIndex, long nextDeadline) {
            this.casterId = casterId;
            this.targetId = targetId;
            this.lastAnchor = lastAnchor;
            this.sampledPower = sampledPower;
            this.nextIndex = nextIndex;
            this.nextDeadline = nextDeadline;
        }
    }
}
