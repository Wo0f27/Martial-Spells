package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Small, server-thread deadline queue for Ranger release work. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RangerTickScheduler {
    private static final List<Job> JOBS = new ArrayList<>();

    private RangerTickScheduler() {}

    @FunctionalInterface
    public interface Action {
        void run(ServerLevel level, UUID casterId);
    }

    /** The caller must invoke this on the server thread. Deadlines use world game time. */
    public static UUID schedule(ServerLevel level, UUID casterId, int delayTicks, Action action) {
        if (delayTicks < 0) throw new IllegalArgumentException("delayTicks must be nonnegative");
        UUID id = UUID.randomUUID();
        JOBS.add(new Job(id, level.getServer(), level.dimension(), casterId,
                level.getGameTime() + delayTicks, Objects.requireNonNull(action)));
        return id;
    }

    public static void cancel(UUID jobId) {
        JOBS.removeIf(job -> job.id.equals(jobId));
    }

    public static void cancelCaster(ResourceKey<Level> dimension, UUID casterId) {
        JOBS.removeIf(job -> job.dimension.equals(dimension) && job.casterId.equals(casterId));
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        List<Job> due = new ArrayList<>();
        Iterator<Job> iterator = JOBS.iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
            if (job.server == level.getServer() && job.dimension.equals(level.dimension())
                    && job.deadline <= level.getGameTime()) {
                iterator.remove();
                due.add(job);
            }
        }
        for (Job job : due) job.action.run(level, job.casterId);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.removeIf(job -> job.server == event.getServer());
    }

    private record Job(UUID id, MinecraftServer server, ResourceKey<Level> dimension,
                       UUID casterId, long deadline, Action action) {}
}
