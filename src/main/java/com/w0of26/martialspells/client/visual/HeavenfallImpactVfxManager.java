package com.w0of26.martialspells.visual;

import com.w0of26.martialspells.MartialSpells;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HeavenfallImpactVfxManager {

    /*
     * Short, violent Heavenfall camera shake.
     *
     * Unlike Earthquake, this is not a sustained effect.
     */
    private static final int CAMERA_SHAKE_TICKS = 10;

    private static final float CAMERA_SHAKE_RADIUS = 15.0F;

    /*
     * Number of server ticks the ground wave needs
     * to travel from the center to its maximum radius.
     */
    private static final int WAVE_TICKS = 6;

    /*
     * Slightly stronger than the normal Earthquake
     * tremor impulse because Heavenfall is a single
     * concentrated impact.
     */
    private static final float MIN_TREMOR_IMPULSE = 0.20F;
    private static final float MAX_TREMOR_IMPULSE = 0.35F;

    private static final int MIN_RING_SAMPLES = 8;
    private static final int MAX_RING_SAMPLES = 24;

    private static final List<ImpactWave> ACTIVE_WAVES =
            new ArrayList<>();

    private HeavenfallImpactVfxManager() {
    }

    public static void begin(
            ServerLevel level,
            Vec3 impactCenter,
            int spellLevel
    ) {
        float maximumRadius =
                getRadius(
                        spellLevel
                );

        /*
         * Use Iron's actual Earthquake impact sound.
         */
        level.playSound(
                null,
                BlockPos.containing(
                        impactCenter
                ),
                SoundRegistry.EARTHQUAKE_IMPACT.get(),
                SoundSource.PLAYERS,
                1.5F,
                0.90F
                        + Utils.random.nextFloat()
                        * 0.10F
        );

        /*
         * Use Iron's existing synchronized
         * camera-shake system.
         */
        CameraShakeManager.addCameraShake(
                new CameraShakeData(
                        CAMERA_SHAKE_TICKS,
                        impactCenter,
                        CAMERA_SHAKE_RADIUS
                )
        );

        /*
         * Immediate eruption at the point of impact.
         */
        createCenterBurst(
                level,
                impactCenter
        );

        /*
         * Then expand outward for several ticks.
         */
        ACTIVE_WAVES.add(
                new ImpactWave(
                        level.dimension(),
                        impactCenter,
                        maximumRadius
                )
        );
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || ACTIVE_WAVES.isEmpty()) {

            return;
        }

        Iterator<ImpactWave> iterator =
                ACTIVE_WAVES.iterator();

        while (iterator.hasNext()) {
            ImpactWave wave =
                    iterator.next();

            ServerLevel level =
                    event.getServer()
                            .getLevel(
                                    wave.dimension
                            );

            if (level == null) {
                iterator.remove();
                continue;
            }

            wave.ageTicks++;

            float progress =
                    Mth.clamp(
                            wave.ageTicks
                                    / (float) WAVE_TICKS,
                            0.0F,
                            1.0F
                    );

            double currentRadius =
                    wave.maximumRadius
                            * progress;

            createRing(
                    level,
                    wave.center,
                    currentRadius
            );

            if (wave.ageTicks >= WAVE_TICKS) {
                iterator.remove();
            }
        }
    }

    private static void createCenterBurst(
            ServerLevel level,
            Vec3 center
    ) {
        /*
         * Center + eight surrounding points.
         */
        createTremorAt(
                level,
                center
        );

        final int points = 8;
        final double radius = 1.25D;

        for (int i = 0; i < points; i++) {
            double angle =
                    Math.PI
                            * 2.0D
                            * i
                            / points;

            Vec3 sample =
                    center.add(
                            Math.cos(angle)
                                    * radius,
                            0.0D,
                            Math.sin(angle)
                                    * radius
                    );

            createTremorAt(
                    level,
                    sample
            );
        }
    }

    private static void createRing(
            ServerLevel level,
            Vec3 center,
            double radius
    ) {
        if (radius <= 0.0D) {
            return;
        }

        /*
         * Approximately one visual block per block of
         * circumference, within reasonable limits.
         */
        int samples =
                Mth.clamp(
                        (int) Math.ceil(
                                Math.PI
                                        * 2.0D
                                        * radius
                        ),
                        MIN_RING_SAMPLES,
                        MAX_RING_SAMPLES
                );

        /*
         * Small radii can cause multiple samples to resolve
         * to the same BlockPos. Avoid spawning duplicates.
         */
        Set<BlockPos> usedPositions =
                new HashSet<>();

        for (int i = 0; i < samples; i++) {
            double angle =
                    Math.PI
                            * 2.0D
                            * i
                            / samples;

            Vec3 sample =
                    center.add(
                            Math.cos(angle)
                                    * radius,
                            0.0D,
                            Math.sin(angle)
                                    * radius
                    );

            BlockPos ground =
                    findGround(
                            level,
                            sample
                    );

            if (!usedPositions.add(
                    ground
            )) {
                continue;
            }

            createTremorBlock(
                    level,
                    ground
            );
        }
    }

    private static void createTremorAt(
            ServerLevel level,
            Vec3 position
    ) {
        createTremorBlock(
                level,
                findGround(
                        level,
                        position
                )
        );
    }

    private static BlockPos findGround(
            ServerLevel level,
            Vec3 position
    ) {
        /*
         * Same relative-ground helper used by
         * Iron's Earthquake.
         */
        Vec3 groundPosition =
                Utils.moveToRelativeGroundLevel(
                        level,
                        position,
                        4
                );

        /*
         * Earthquake passes the block BELOW the returned
         * ground-level position into createTremorBlock().
         */
        return BlockPos.containing(
                groundPosition
        ).below();
    }

    private static void createTremorBlock(
            ServerLevel level,
            BlockPos blockPos
    ) {
        float impulse =
                MIN_TREMOR_IMPULSE
                        + Utils.random.nextFloat()
                        * (
                        MAX_TREMOR_IMPULSE
                                - MIN_TREMOR_IMPULSE
                );

        /*
         * Iron's Earthquake visual falling-block system.
         *
         * This creates temporary visual block entities.
         * It does NOT remove/destroy the actual terrain.
         */
        Utils.createTremorBlock(
                level,
                blockPos,
                impulse
        );
    }

    private static float getRadius(
            int spellLevel
    ) {
        int level =
                Mth.clamp(
                        spellLevel,
                        1,
                        5
                );

        /*
         * Match Heavenfall's actual damaging shockwave:
         *
         * I   2.5
         * II  3.0
         * III 3.5
         * IV  4.0
         * V   4.5
         */
        return 2.0F
                + level
                * 0.5F;
    }

    private static final class ImpactWave {

        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final float maximumRadius;

        private int ageTicks;

        private ImpactWave(
                ResourceKey<Level> dimension,
                Vec3 center,
                float maximumRadius
        ) {
            this.dimension = dimension;
            this.center = center;
            this.maximumRadius = maximumRadius;
        }
    }
}