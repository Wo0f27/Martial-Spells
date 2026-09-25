package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Source-faithful Paladins visual batches translated from frozen
 * Spell Engine 1.20.1 behavior.
 *
 * <p>This class reproduces only the particle vocabulary actually used by
 * Paladins. Particle motion/appearance lives in the client particle classes;
 * this class owns the authored batch geometry (SPHERE/PIPE/PILLAR/CIRCLE/LINE),
 * origins, counts, speed ranges, pre-travel and LOOK alignment.</p>
 */
public final class PaladinVfx {
    private static final double FEET = 0.10D;
    private static final double CENTER = 0.50D;
    private static final double OVER_HEAD = 1.50D;

    private PaladinVfx() {
    }

    public static void holyCasting(
            ServerLevel level,
            LivingEntity caster
    ) {
        pipe(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                caster,
                1,
                0.05D,
                0.10D,
                FEET,
                0.0D,
                2.0D
        );
    }

    public static void healPillar(
            ServerLevel level,
            LivingEntity target,
            int count
    ) {
        pillar(
                level,
                MartialParticleRegistry
                        .PALADIN_HEAL_ASCEND
                        .get(),
                target,
                count,
                0.02D,
                0.15D,
                FEET,
                0.0D,
                1.0D
        );
    }

    public static void holyBurst(
            ServerLevel level,
            LivingEntity target,
            int count,
            double maxSpeed
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_HOLY_BURST
                        .get(),
                sourceOrigin(
                        target,
                        CENTER
                ),
                count,
                0.20D,
                maxSpeed,
                0.0D,
                false
        );
    }

    public static void holyBurstAt(
            ServerLevel level,
            Vec3 center,
            int count,
            double maxSpeed
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_HOLY_BURST
                        .get(),
                center,
                count,
                0.20D,
                maxSpeed,
                0.0D,
                false
        );
    }

    public static void holyGlimmer(
            ServerLevel level,
            LivingEntity target,
            int count,
            double minSpeed,
            double maxSpeed
    ) {
        holyGlimmerAt(
                level,
                sourceOrigin(
                        target,
                        CENTER
                ),
                target.getBbWidth(),
                target.getBbHeight(),
                count,
                minSpeed,
                maxSpeed
        );
    }

    public static void holyGlimmerAt(
            ServerLevel level,
            Vec3 center,
            double width,
            double height,
            int count,
            double minSpeed,
            double maxSpeed
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_HOLY_DECELERATE
                        .get(),
                center,
                count,
                minSpeed,
                maxSpeed,
                0.0D,
                false
        );
    }

    public static void holySparksAt(
            ServerLevel level,
            Vec3 center,
            int count,
            double radius,
            double minSpeed,
            double maxSpeed
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                center,
                count,
                minSpeed,
                maxSpeed,
                0.0D,
                false
        );
    }

    public static void circleOfHealingRelease(
            ServerLevel level,
            LivingEntity caster,
            double range
    ) {
        pillar(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_DECELERATE
                        .get(),
                caster,
                100,
                0.30D,
                0.50D,
                FEET,
                range - 0.5D,
                1.0D
        );

        pillar(
                level,
                MartialParticleRegistry
                        .PALADIN_SPELL_DECELERATE
                        .get(),
                caster,
                50,
                0.10D,
                0.50D,
                FEET,
                range - 0.5D,
                1.0D
        );

        pipe(
                level,
                MartialParticleRegistry
                        .PALADIN_HOLY_FLOAT
                        .get(),
                caster,
                50,
                0.10D,
                0.20D,
                FEET,
                range,
                1.0D
        );

        area(
                level,
                MartialParticleRegistry
                        .PALADIN_AREA_637_GROUND
                        .get(),
                new Vec3(
                        caster.getX(),
                        groundY(
                                level,
                                caster.position()
                        ),
                        caster.getZ()
                ),
                range,
                0.50D,
                0
        );
    }

    public static void immolationRelease(
            ServerLevel level,
            LivingEntity caster,
            double range
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_DECELERATE
                        .get(),
                sourceOrigin(
                        caster,
                        CENTER
                ),
                60,
                0.40D,
                0.50D,
                1.0D,
                false
        );

        // Executable source uses Batches.placed rather than area()/GROUND
        // anchoring: both range-scaled sheets originate at caster centre.
        Vec3 center =
                sourceOrigin(
                        caster,
                        CENTER
                );

        area(
                level,
                MartialParticleRegistry
                        .PALADIN_AREA_637_GROUND
                        .get(),
                center,
                range,
                1.0D,
                0
        );

        area(
                level,
                MartialParticleRegistry
                        .PALADIN_AREA_676_CAMERA
                        .get(),
                center,
                range,
                1.0D,
                0
        );
    }

    public static void divineProtectionApply(
            ServerLevel level,
            LivingEntity target
    ) {
        holyGlimmer(
                level,
                target,
                40,
                0.20D,
                0.20D
        );
    }

    /**
     * Source Protection.Pop: magic_holy BURST, PIPE, widthFactor 2,
     * count 25, speed .1-.15.
     */
    public static void divineProtectionPop(
            ServerLevel level,
            LivingEntity target
    ) {
        pipe(
                level,
                MartialParticleRegistry
                        .PALADIN_HOLY_BURST
                        .get(),
                target,
                25,
                0.10D,
                0.15D,
                CENTER,
                0.0D,
                2.0D
        );
    }

    /**
     * Continuous Holy Light casting particles from Spell Engine launch point:
     * magic_spark PIPE x3 every tick plus one FIREWORK every second tick.
     */
    public static void holyBeamCasting(
            ServerLevel level,
            LivingEntity caster
    ) {
        Vec3 launch =
                launchPoint(caster);
        Vec3 look =
                caster.getLookAngle()
                        .normalize();

        orientedPipe(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                launch,
                look,
                caster.getBbWidth(),
                3,
                0.10D,
                0.20D,
                1.0D,
                0.0D
        );

        if ((level.getGameTime() & 1L) == 0L) {
            orientedPipe(
                    level,
                    ParticleTypes.FIREWORK,
                    launch,
                    look,
                    caster.getBbWidth(),
                    1,
                    0.10D,
                    0.20D,
                    1.0D,
                    0.0D
            );
        }
    }

    /**
     * Holy Light block-hit VFX. The actual beam core is rendered separately.
     */
    public static void holyBeam(
            ServerLevel level,
            LivingEntity caster,
            Vec3 end
    ) {
        Vec3 look =
                caster.getLookAngle()
                        .normalize();

        orientedCircle(
                level,
                MartialParticleRegistry
                        .PALADIN_SPELL_FLOAT
                        .get(),
                end,
                look,
                1,
                0.10D,
                0.20D
        );

        orientedCircle(
                level,
                ParticleTypes.FIREWORK,
                end,
                look,
                1,
                0.10D,
                0.20D
        );

        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                end,
                5,
                0.10D,
                0.20D,
                0.0D,
                false
        );
    }

    public static void levitateChannel(
            ServerLevel level,
            LivingEntity caster
    ) {
        pillar(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                caster,
                4,
                0.02D,
                0.12D,
                FEET,
                0.5D,
                1.0D
        );

        pipe(
                level,
                MartialParticleRegistry
                        .PALADIN_SPELL_FLOAT
                        .get(),
                caster,
                2,
                0.02D,
                0.10D,
                FEET,
                0.5D,
                1.0D
        );
    }

    public static void penanceCasting(
            ServerLevel level,
            LivingEntity caster
    ) {
        holyCasting(
                level,
                caster
        );
    }

    /**
     * Exact Batches.helix(3,.16,15,0/180): two outward-moving spark strands.
     */
    public static void penanceHelix(
            ServerLevel level,
            Vec3 position,
            Vec3 velocity,
            long age
    ) {
        if (velocity.lengthSqr() <= 1.0E-8D) {
            return;
        }

        Vec3 forward =
                velocity.normalize();
        Basis basis =
                basis(forward);

        for (int strand = 0; strand < 2; strand++) {
            double offset =
                    strand == 0
                            ? 0.0D
                            : 180.0D;
            double angle =
                    Math.toRadians(
                            (
                                    age * 15.0D
                                            + offset
                            ) % 360.0D
                    );

            Vec3 direction =
                    basis.side
                            .scale(
                                    Math.cos(angle)
                            )
                            .add(
                                    basis.up.scale(
                                            Math.sin(angle)
                                    )
                            )
                            .normalize();

            for (int i = 0; i < 3; i++) {
                double speed =
                        randomInRange(
                                level.random,
                                0.12D,
                                0.16D
                        );
                emit(
                        level,
                        MartialParticleRegistry
                                .PALADIN_SPARK_FLOAT
                                .get(),
                        position,
                        direction.scale(speed)
                );
            }
        }
    }

    public static void penanceImpact(
            ServerLevel level,
            LivingEntity target
    ) {
        holyBurst(
                level,
                target,
                18,
                0.60D
        );
    }

    public static void penanceShield(
            ServerLevel level,
            LivingEntity ally
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_DECELERATE
                        .get(),
                sourceOrigin(
                        ally,
                        CENTER
                ),
                12,
                0.20D,
                0.25D,
                0.0D,
                false
        );
    }

    public static void penanceAreaPulse(
            ServerLevel level,
            Vec3 center,
            double radius
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_HOLY_DECELERATE
                        .get(),
                center,
                40,
                0.40D,
                0.60D,
                0.0D,
                false
        );

        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                center,
                30,
                0.20D,
                0.40D,
                0.0D,
                false
        );
    }

    public static void barrierSpawn(
            ServerLevel level,
            LivingEntity caster
    ) {
        Vec3 origin =
                sourceOrigin(
                        caster,
                        CENTER
                );

        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_SPELL_DECELERATE
                        .get(),
                origin,
                50,
                1.0D,
                1.0D,
                0.0D,
                false
        );

        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_DECELERATE
                        .get(),
                origin,
                50,
                1.0D,
                1.0D,
                0.0D,
                false
        );
    }

    public static void bannerPresence(
            ServerLevel level,
            Vec3 center,
            double radius
    ) {
        // Banner entity dimensions are frozen at 6 x .5 in source.
        pillarAt(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_DECELERATE
                        .get(),
                center,
                6.0D,
                0.5D,
                15,
                0.10D,
                0.15D,
                FEET,
                0.0D,
                1.0D
        );

        pipeAt(
                level,
                MartialParticleRegistry
                        .PALADIN_STRIPE_FLOAT
                        .get(),
                center,
                6.0D,
                0.5D,
                3,
                0.05D,
                0.10D,
                FEET,
                0.0D,
                1.0D
        );
    }

    public static void lightwellSpawn(
            ServerLevel level,
            Vec3 center
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_HOLY_DECELERATE
                        .get(),
                center.add(
                        0.0D,
                        0.70D,
                        0.0D
                ),
                30,
                0.20D,
                0.40D,
                0.0D,
                false
        );
    }

    public static void lightwellAura(
            ServerLevel level,
            Vec3 center
    ) {
        pillarAt(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_ASCEND
                        .get(),
                center,
                0.9D,
                1.4D,
                2,
                0.02D,
                0.12D,
                0.10D,
                0.45D,
                1.0D
        );
    }

    public static void holyMoteTrail(
            ServerLevel level,
            Vec3 position,
            Vec3 velocity
    ) {
        if (velocity.lengthSqr() <= 1.0E-8D) {
            return;
        }

        orientedCircle(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                position,
                velocity.normalize(),
                5,
                0.0D,
                0.10D
        );
    }

    public static void judgementTrail(
            ServerLevel level,
            Vec3 point,
            Vec3 velocity
    ) {
        if (velocity.lengthSqr() <= 1.0E-8D) {
            return;
        }

        Vec3 forward =
                velocity.normalize();

        // Frozen meteor: magic_stripe PIPE widthFactor2 x5, then spark x4.
        orientedPipe(
                level,
                MartialParticleRegistry
                        .PALADIN_STRIPE_FLOAT
                        .get(),
                point,
                forward,
                0.25D,
                5,
                0.0D,
                0.20D,
                2.0D,
                0.0D
        );

        orientedPipe(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                point,
                forward,
                0.25D,
                4,
                0.0D,
                0.10D,
                2.0D,
                0.0D
        );
    }

    public static void judgementImpact(
            ServerLevel level,
            Vec3 center
    ) {
        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_HOLY_DECELERATE
                        .get(),
                center,
                100,
                0.80D,
                0.90D,
                0.0D,
                false
        );

        sphere(
                level,
                MartialParticleRegistry
                        .PALADIN_SPARK_FLOAT
                        .get(),
                center,
                100,
                0.20D,
                0.40D,
                0.0D,
                false
        );

        sphere(
                level,
                ParticleTypes.SMOKE,
                center,
                50,
                0.10D,
                0.30D,
                0.0D,
                false
        );
    }

    public static void circleAreaEffect(
            ServerLevel level,
            LivingEntity caster,
            double range,
            double alpha
    ) {
        area(
                level,
                MartialParticleRegistry
                        .PALADIN_AREA_637_GROUND
                        .get(),
                new Vec3(
                        caster.getX(),
                        groundY(
                                level,
                                caster.position()
                        ),
                        caster.getZ()
                ),
                range,
                alpha,
                0
        );
    }

    private static void pillar(
            ServerLevel level,
            ParticleOptions particle,
            LivingEntity source,
            int count,
            double minSpeed,
            double maxSpeed,
            double verticalOrigin,
            double extent,
            double widthFactor
    ) {
        pillarAt(
                level,
                particle,
                source.position(),
                source.getBbWidth(),
                source.getBbHeight(),
                count,
                minSpeed,
                maxSpeed,
                verticalOrigin,
                extent,
                widthFactor
        );
    }

    private static void pillarAt(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 sourcePosition,
            double sourceWidth,
            double sourceHeight,
            int count,
            double minSpeed,
            double maxSpeed,
            double verticalOrigin,
            double extent,
            double widthFactor
    ) {
        RandomSource random =
                level.random;
        Vec3 origin =
                sourcePosition.add(
                        0.0D,
                        sourceHeight * verticalOrigin,
                        0.0D
                );
        double radius =
                sourceWidth
                        * 0.5D
                        * widthFactor
                        + extent;

        for (int i = 0; i < count; i++) {
            double r =
                    radius
                            * random.nextDouble();
            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0D;
            Vec3 position =
                    origin.add(
                            Math.cos(angle) * r,
                            0.0D,
                            Math.sin(angle) * r
                    );
            Vec3 velocity =
                    new Vec3(
                            0.0D,
                            randomInRange(
                                    random,
                                    minSpeed,
                                    maxSpeed
                            ),
                            0.0D
                    );
            emit(
                    level,
                    particle,
                    position,
                    velocity
            );
        }
    }

    private static void pipe(
            ServerLevel level,
            ParticleOptions particle,
            LivingEntity source,
            int count,
            double minSpeed,
            double maxSpeed,
            double verticalOrigin,
            double extent,
            double widthFactor
    ) {
        pipeAt(
                level,
                particle,
                source.position(),
                source.getBbWidth(),
                source.getBbHeight(),
                count,
                minSpeed,
                maxSpeed,
                verticalOrigin,
                extent,
                widthFactor
        );
    }

    private static void pipeAt(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 sourcePosition,
            double sourceWidth,
            double sourceHeight,
            int count,
            double minSpeed,
            double maxSpeed,
            double verticalOrigin,
            double extent,
            double widthFactor
    ) {
        RandomSource random =
                level.random;
        Vec3 origin =
                sourcePosition.add(
                        0.0D,
                        sourceHeight * verticalOrigin,
                        0.0D
                );
        double radius =
                sourceWidth
                        * 0.5D
                        * widthFactor
                        + extent;

        for (int i = 0; i < count; i++) {
            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0D;
            Vec3 position =
                    origin.add(
                            Math.cos(angle) * radius,
                            0.0D,
                            Math.sin(angle) * radius
                    );
            Vec3 velocity =
                    new Vec3(
                            0.0D,
                            randomInRange(
                                    random,
                                    minSpeed,
                                    maxSpeed
                            ),
                            0.0D
                    );
            emit(
                    level,
                    particle,
                    position,
                    velocity
            );
        }
    }

    private static void sphere(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 origin,
            int count,
            double minSpeed,
            double maxSpeed,
            double preTravel,
            boolean invert
    ) {
        RandomSource random =
                level.random;

        for (int i = 0; i < count; i++) {
            double speed =
                    randomInRange(
                            random,
                            minSpeed,
                            maxSpeed
                    );

            Vec3 velocity =
                    new Vec3(
                            speed,
                            0.0D,
                            0.0D
                    )
                            .zRot(
                                    (float) (
                                            random.nextDouble()
                                                    * Math.PI
                                                    * 2.0D
                                    )
                            )
                            .yRot(
                                    (float) (
                                            random.nextDouble()
                                                    * Math.PI
                                                    * 2.0D
                                    )
                            );

            Vec3 position =
                    preTravel == 0.0D
                            ? origin
                            : origin.add(
                                    velocity.scale(
                                            preTravel
                                    )
                            );

            if (invert) {
                velocity =
                        velocity.scale(
                                -1.0D
                        );
            }

            emit(
                    level,
                    particle,
                    position,
                    velocity
            );
        }
    }

    private static void orientedCircle(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 origin,
            Vec3 forward,
            int count,
            double minSpeed,
            double maxSpeed
    ) {
        RandomSource random =
                level.random;
        Basis basis =
                basis(forward);

        for (int i = 0; i < count; i++) {
            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0D;
            double speed =
                    randomInRange(
                            random,
                            minSpeed,
                            maxSpeed
                    );
            Vec3 velocity =
                    basis.side
                            .scale(
                                    Math.cos(angle)
                            )
                            .add(
                                    basis.up.scale(
                                            Math.sin(angle)
                                    )
                            )
                            .scale(speed);

            emit(
                    level,
                    particle,
                    origin,
                    velocity
            );
        }
    }

    private static void orientedPipe(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 origin,
            Vec3 forward,
            double sourceWidth,
            int count,
            double minSpeed,
            double maxSpeed,
            double widthFactor,
            double extent
    ) {
        RandomSource random =
                level.random;
        Vec3 axis =
                forward.normalize();
        Basis basis =
                basis(axis);
        double radius =
                sourceWidth
                        * 0.5D
                        * widthFactor
                        + extent;

        for (int i = 0; i < count; i++) {
            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0D;
            Vec3 offset =
                    basis.side
                            .scale(
                                    Math.cos(angle) * radius
                            )
                            .add(
                                    basis.up.scale(
                                            Math.sin(angle) * radius
                                    )
                            );
            double speed =
                    randomInRange(
                            random,
                            minSpeed,
                            maxSpeed
                    );

            emit(
                    level,
                    particle,
                    origin.add(offset),
                    axis.scale(speed)
            );
        }
    }

    private static void area(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 position,
            double scale,
            double alpha,
            int followEntityId
    ) {
        level.sendParticles(
                particle,
                position.x,
                position.y,
                position.z,
                0,
                scale,
                alpha,
                followEntityId,
                1.0D
        );
    }

    private static void emit(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 position,
            Vec3 velocity
    ) {
        level.sendParticles(
                particle,
                position.x,
                position.y,
                position.z,
                0,
                velocity.x,
                velocity.y,
                velocity.z,
                1.0D
        );
    }

    private static Vec3 sourceOrigin(
            LivingEntity source,
            double verticalOrigin
    ) {
        return source.position()
                .add(
                        0.0D,
                        source.getBbHeight()
                                * verticalOrigin,
                        0.0D
                );
    }

    private static Vec3 launchPoint(
            LivingEntity caster
    ) {
        double launchHeight =
                caster.getEyeHeight()
                        - caster.getBbHeight()
                        * 0.15D;
        return caster.position()
                .add(
                        0.0D,
                        launchHeight,
                        0.0D
                )
                .add(
                        caster.getLookAngle()
                                .normalize()
                                .scale(0.5D)
                );
    }

    private static double groundY(
            ServerLevel level,
            Vec3 position
    ) {
        BlockPos start =
                BlockPos.containing(
                        position.x,
                        position.y + 0.5D,
                        position.z
                );

        for (int i = 0; i <= 3; i++) {
            BlockPos blockPos =
                    start.below(i);
            BlockState state =
                    level.getBlockState(
                            blockPos
                    );
            if (!state
                    .getCollisionShape(
                            level,
                            blockPos
                    )
                    .isEmpty()) {
                return blockPos.getY()
                        + 1.01D;
            }
        }

        return position.y + 0.01D;
    }

    private static double randomInRange(
            RandomSource random,
            double min,
            double max
    ) {
        return min
                + (
                max - min
        ) * random.nextDouble();
    }

    private static Basis basis(
            Vec3 forward
    ) {
        Vec3 axis =
                forward.normalize();
        Vec3 reference =
                Math.abs(axis.y) < 0.95D
                        ? new Vec3(
                                0.0D,
                                1.0D,
                                0.0D
                        )
                        : new Vec3(
                                1.0D,
                                0.0D,
                                0.0D
                        );
        Vec3 side =
                axis.cross(reference)
                        .normalize();
        Vec3 up =
                side.cross(axis)
                        .normalize();

        return new Basis(
                side,
                up
        );
    }

    private record Basis(
            Vec3 side,
            Vec3 up
    ) {
    }
}
