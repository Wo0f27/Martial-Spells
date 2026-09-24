package com.w0of26.martialspells.spells;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Forge-native reconstruction of the frozen Paladins holy visual language.
 *
 * <p>The source authored its generic particles through Spell Engine. CP11 does
 * not take Spell Engine as a runtime dependency, so the same shapes/motion
 * intent are rebuilt here using vanilla particle primitives and the exact
 * Paladins color palette.</p>
 */
public final class PaladinVfx {
    private static final Vector3f HOLY_COLOR =
            new Vector3f(1.0F, 1.0F, 0.80F);
    private static final Vector3f HOLY_WARM_COLOR =
            new Vector3f(1.0F, 0.80F, 0.40F);
    private static final Vector3f HEAL_COLOR =
            new Vector3f(0.40F, 1.0F, 0.40F);

    public static final DustParticleOptions HOLY_DUST =
            new DustParticleOptions(HOLY_COLOR, 1.0F);
    public static final DustParticleOptions HOLY_WARM_DUST =
            new DustParticleOptions(HOLY_WARM_COLOR, 1.0F);
    public static final DustParticleOptions HEAL_DUST =
            new DustParticleOptions(HEAL_COLOR, 0.9F);

    private PaladinVfx() {
    }

    public static void holyCasting(
            ServerLevel level,
            LivingEntity caster
    ) {
        RandomSource random = caster.getRandom();
        double baseY = caster.getY() + 0.10D;
        double radius =
                Math.max(0.35D, caster.getBbWidth() * 0.75D);

        for (int i = 0; i < 4; i++) {
            double angle =
                    random.nextDouble() * Math.PI * 2.0D;
            double r =
                    radius * (0.35D + random.nextDouble() * 0.65D);
            double x =
                    caster.getX() + Math.cos(angle) * r;
            double z =
                    caster.getZ() + Math.sin(angle) * r;
            double y =
                    baseY + random.nextDouble() * 0.35D;

            velocityParticle(
                    level,
                    HOLY_DUST,
                    x,
                    y,
                    z,
                    -Math.cos(angle) * 0.015D,
                    0.035D + random.nextDouble() * 0.035D,
                    -Math.sin(angle) * 0.015D
            );
        }
    }

    public static void healPillar(
            ServerLevel level,
            LivingEntity target,
            int count
    ) {
        RandomSource random = target.getRandom();
        double radius =
                Math.max(0.20D, target.getBbWidth() * 0.45D);

        for (int i = 0; i < count; i++) {
            double angle =
                    random.nextDouble() * Math.PI * 2.0D;
            double r =
                    radius * Math.sqrt(random.nextDouble());
            double x =
                    target.getX() + Math.cos(angle) * r;
            double z =
                    target.getZ() + Math.sin(angle) * r;
            double y =
                    target.getY()
                            + random.nextDouble()
                            * Math.max(0.4D, target.getBbHeight() * 0.8D);

            velocityParticle(
                    level,
                    HEAL_DUST,
                    x,
                    y,
                    z,
                    0.0D,
                    0.03D + random.nextDouble() * 0.12D,
                    0.0D
            );

            if ((i & 3) == 0) {
                velocityParticle(
                        level,
                        ParticleTypes.END_ROD,
                        x,
                        y,
                        z,
                        0.0D,
                        0.025D + random.nextDouble() * 0.07D,
                        0.0D
                );
            }
        }
    }

    public static void holyBurst(
            ServerLevel level,
            LivingEntity target,
            int count,
            double maxSpeed
    ) {
        holyBurstAt(
                level,
                target.getBoundingBox().getCenter(),
                count,
                maxSpeed
        );
    }

    public static void holyBurstAt(
            ServerLevel level,
            Vec3 center,
            int count,
            double maxSpeed
    ) {
        RandomSource random = level.random;

        for (int i = 0; i < count; i++) {
            Vec3 direction =
                    randomUnitVector(random);
            double speed =
                    0.20D
                            + random.nextDouble()
                            * Math.max(0.0D, maxSpeed - 0.20D);

            velocityParticle(
                    level,
                    i % 4 == 0
                            ? ParticleTypes.END_ROD
                            : HOLY_DUST,
                    center.x,
                    center.y,
                    center.z,
                    direction.x * speed,
                    direction.y * speed,
                    direction.z * speed
            );
        }
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
                target.getBoundingBox().getCenter(),
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
        RandomSource random = level.random;

        for (int i = 0; i < count; i++) {
            double x =
                    center.x
                            + (random.nextDouble() - 0.5D)
                            * Math.max(0.3D, width);
            double y =
                    center.y
                            + (random.nextDouble() - 0.5D)
                            * Math.max(0.5D, height);
            double z =
                    center.z
                            + (random.nextDouble() - 0.5D)
                            * Math.max(0.3D, width);

            Vec3 direction =
                    randomUnitVector(random);
            double speed =
                    minSpeed
                            + random.nextDouble()
                            * Math.max(0.0D, maxSpeed - minSpeed);

            velocityParticle(
                    level,
                    i % 5 == 0
                            ? ParticleTypes.FIREWORK
                            : HOLY_DUST,
                    x,
                    y,
                    z,
                    direction.x * speed * 0.20D,
                    Math.abs(direction.y) * speed * 0.20D,
                    direction.z * speed * 0.20D
            );
        }
    }

    public static void holySparksAt(
            ServerLevel level,
            Vec3 center,
            int count,
            double radius,
            double minSpeed,
            double maxSpeed
    ) {
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            Vec3 direction =
                    randomUnitVector(random);
            double r =
                    random.nextDouble() * radius;
            double speed =
                    minSpeed
                            + random.nextDouble()
                            * Math.max(0.0D, maxSpeed - minSpeed);

            velocityParticle(
                    level,
                    (i & 1) == 0
                            ? HOLY_DUST
                            : ParticleTypes.END_ROD,
                    center.x + direction.x * r,
                    center.y + direction.y * r,
                    center.z + direction.z * r,
                    direction.x * speed * 0.25D,
                    direction.y * speed * 0.25D,
                    direction.z * speed * 0.25D
            );
        }
    }

    public static void circleOfHealingRelease(
            ServerLevel level,
            LivingEntity caster,
            double range
    ) {
        Vec3 center =
                new Vec3(
                        caster.getX(),
                        caster.getY() + 0.08D,
                        caster.getZ()
                );

        groundRings(
                level,
                center,
                range,
                3,
                64,
                HOLY_WARM_DUST
        );

        RandomSource random = caster.getRandom();
        for (int i = 0; i < 100; i++) {
            double angle =
                    random.nextDouble() * Math.PI * 2.0D;
            double r =
                    range * Math.sqrt(random.nextDouble());
            double x =
                    center.x + Math.cos(angle) * r;
            double z =
                    center.z + Math.sin(angle) * r;

            velocityParticle(
                    level,
                    (i & 3) == 0
                            ? ParticleTypes.END_ROD
                            : HOLY_DUST,
                    x,
                    center.y,
                    z,
                    0.0D,
                    0.08D + random.nextDouble() * 0.28D,
                    0.0D
            );
        }
    }

    public static void immolationRelease(
            ServerLevel level,
            LivingEntity caster,
            double range
    ) {
        Vec3 center =
                new Vec3(
                        caster.getX(),
                        caster.getY() + 0.10D,
                        caster.getZ()
                );

        groundRings(
                level,
                center,
                range,
                3,
                72,
                HOLY_WARM_DUST
        );

        for (int i = 0; i < 72; i++) {
            double angle =
                    Math.PI * 2.0D * i / 72.0D;
            double r =
                    range * (0.25D + 0.75D * ((i % 12) / 11.0D));
            double x =
                    center.x + Math.cos(angle) * r;
            double z =
                    center.z + Math.sin(angle) * r;
            double rise =
                    0.10D + 0.25D * (1.0D - r / range);

            velocityParticle(
                    level,
                    (i & 2) == 0
                            ? ParticleTypes.FLAME
                            : HOLY_DUST,
                    x,
                    center.y,
                    z,
                    Math.cos(angle) * 0.04D,
                    rise,
                    Math.sin(angle) * 0.04D
            );
        }
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

    public static void divineProtectionPop(
            ServerLevel level,
            LivingEntity target
    ) {
        Vec3 center =
                target.getBoundingBox().getCenter();
        RandomSource random = target.getRandom();
        double width =
                Math.max(0.5D, target.getBbWidth() * 1.2D);

        for (int i = 0; i < 25; i++) {
            double angle =
                    random.nextDouble() * Math.PI * 2.0D;
            double x =
                    center.x + Math.cos(angle) * width;
            double z =
                    center.z + Math.sin(angle) * width;
            double y =
                    target.getY()
                            + random.nextDouble() * target.getBbHeight();

            velocityParticle(
                    level,
                    (i & 3) == 0
                            ? ParticleTypes.END_ROD
                            : HOLY_DUST,
                    x,
                    y,
                    z,
                    Math.cos(angle) * 0.10D,
                    (random.nextDouble() - 0.5D) * 0.05D,
                    Math.sin(angle) * 0.10D
            );
        }
    }

    public static void blessedGather(
            ServerLevel level,
            LivingEntity caster
    ) {
        Vec3 hand =
                approximateMainHand(caster);
        RandomSource random = caster.getRandom();

        for (int i = 0; i < 4; i++) {
            Vec3 direction =
                    randomUnitVector(random);
            Vec3 start =
                    hand.add(
                            direction.scale(
                                    0.55D
                                            + random.nextDouble() * 0.35D
                            )
                    );
            Vec3 velocity =
                    hand.subtract(start)
                            .normalize()
                            .scale(0.05D + random.nextDouble() * 0.04D);

            velocityParticle(
                    level,
                    HOLY_DUST,
                    start.x,
                    start.y,
                    start.z,
                    velocity.x,
                    velocity.y,
                    velocity.z
            );
        }
    }

    public static void blessedRelease(
            ServerLevel level,
            LivingEntity caster
    ) {
        Vec3 hand =
                approximateMainHand(caster);
        holySparksAt(
                level,
                hand,
                12,
                0.15D,
                0.05D,
                0.18D
        );
    }

    public static void blessedWeaponAura(
            ServerLevel level,
            LivingEntity caster,
            int stacks
    ) {
        Vec3 hand =
                approximateMainHand(caster);
        int count =
                Mth.clamp(stacks, 1, 6);

        for (int i = 0; i < count; i++) {
            double angle =
                    (level.getGameTime() * 0.35D)
                            + Math.PI * 2.0D * i / count;
            double radius =
                    0.08D + 0.015D * count;
            velocityParticle(
                    level,
                    HOLY_WARM_DUST,
                    hand.x + Math.cos(angle) * radius,
                    hand.y + 0.03D * Math.sin(angle * 0.5D),
                    hand.z + Math.sin(angle) * radius,
                    0.0D,
                    0.01D,
                    0.0D
            );
        }
    }

    public static void holyBeam(
            ServerLevel level,
            LivingEntity caster,
            Vec3 end
    ) {
        Vec3 start =
                caster.getEyePosition()
                        .add(
                                caster.getLookAngle()
                                        .normalize()
                                        .scale(0.35D)
                        );
        Vec3 delta =
                end.subtract(start);
        double length =
                delta.length();
        if (length <= 1.0E-5D) {
            return;
        }

        Vec3 direction =
                delta.scale(1.0D / length);
        int steps =
                Math.max(
                        1,
                        (int) Math.ceil(length / 0.45D)
                );

        for (int i = 0; i <= steps; i++) {
            Vec3 point =
                    start.add(
                            direction.scale(
                                    Math.min(
                                            length,
                                            i * 0.45D
                                    )
                            )
                    );
            velocityParticle(
                    level,
                    (i % 4 == 0)
                            ? ParticleTypes.END_ROD
                            : HOLY_WARM_DUST,
                    point.x,
                    point.y,
                    point.z,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }

        holySparksAt(
                level,
                end,
                8,
                0.25D,
                0.10D,
                0.20D
        );
    }

    public static void levitateChannel(
            ServerLevel level,
            LivingEntity caster
    ) {
        RandomSource random = caster.getRandom();
        double y =
                caster.getY() + 0.05D;
        double radius =
                Math.max(0.35D, caster.getBbWidth() * 0.65D);

        for (int i = 0; i < 6; i++) {
            double angle =
                    random.nextDouble() * Math.PI * 2.0D;
            double r =
                    radius * random.nextDouble();
            double x =
                    caster.getX() + Math.cos(angle) * r;
            double z =
                    caster.getZ() + Math.sin(angle) * r;

            velocityParticle(
                    level,
                    (i & 1) == 0
                            ? HOLY_DUST
                            : ParticleTypes.END_ROD,
                    x,
                    y,
                    z,
                    0.0D,
                    0.03D + random.nextDouble() * 0.09D,
                    0.0D
            );
        }
    }

    public static void penanceCasting(
            ServerLevel level,
            LivingEntity caster
    ) {
        holyCasting(level, caster);
    }

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
        Vec3 up =
                Math.abs(forward.y) < 0.95D
                        ? new Vec3(0.0D, 1.0D, 0.0D)
                        : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side =
                forward.cross(up).normalize();
        Vec3 normal =
                side.cross(forward).normalize();

        double baseAngle =
                Math.toRadians(age * 15.0D);
        for (int strand = 0; strand < 2; strand++) {
            double strandOffset =
                    strand == 0 ? 0.0D : Math.PI;

            for (int i = 0; i < 3; i++) {
                double angle =
                        baseAngle
                                + strandOffset
                                - i * 0.35D;
                Vec3 offset =
                        side.scale(Math.cos(angle) * 0.16D)
                                .add(
                                        normal.scale(
                                                Math.sin(angle) * 0.16D
                                        )
                                )
                                .subtract(
                                        forward.scale(i * 0.10D)
                                );
                Vec3 point =
                        position.add(offset);

                velocityParticle(
                        level,
                        HOLY_DUST,
                        point.x,
                        point.y,
                        point.z,
                        0.0D,
                        0.0D,
                        0.0D
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
        holySparksAt(
                level,
                ally.getBoundingBox().getCenter(),
                12,
                Math.max(0.45D, ally.getBbWidth()),
                0.20D,
                0.25D
        );
    }

    public static void penanceAreaPulse(
            ServerLevel level,
            Vec3 center,
            double radius
    ) {
        groundRings(
                level,
                center,
                radius,
                2,
                56,
                HOLY_WARM_DUST
        );
        holySparksAt(
                level,
                center.add(0.0D, 0.35D, 0.0D),
                30,
                radius * 0.45D,
                0.20D,
                0.40D
        );
    }

    public static void judgementTrail(
            ServerLevel level,
            Vec3 point
    ) {
        holySparksAt(
                level,
                point,
                8,
                0.20D,
                0.05D,
                0.20D
        );
    }

    public static void judgementImpact(
            ServerLevel level,
            Vec3 center
    ) {
        holyGlimmerAt(
                level,
                center.add(0.0D, 0.5D, 0.0D),
                5.0D,
                3.0D,
                100,
                0.08D,
                0.18D
        );
        holySparksAt(
                level,
                center.add(0.0D, 0.5D, 0.0D),
                100,
                3.0D,
                0.20D,
                0.40D
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                center.x,
                center.y + 0.25D,
                center.z,
                50,
                2.0D,
                0.75D,
                2.0D,
                0.08D
        );
    }

    private static void groundRings(
            ServerLevel level,
            Vec3 center,
            double range,
            int rings,
            int points,
            DustParticleOptions particle
    ) {
        for (int ring = 1; ring <= rings; ring++) {
            double radius =
                    range * ring / (double) rings;

            for (int i = 0; i < points; i++) {
                double angle =
                        Math.PI * 2.0D * i / points;
                velocityParticle(
                        level,
                        particle,
                        center.x + Math.cos(angle) * radius,
                        center.y,
                        center.z + Math.sin(angle) * radius,
                        0.0D,
                        0.005D,
                        0.0D
                );
            }
        }
    }

    private static Vec3 approximateMainHand(
            LivingEntity entity
    ) {
        Vec3 look =
                entity.getLookAngle().normalize();
        Vec3 side =
                new Vec3(
                        -look.z,
                        0.0D,
                        look.x
                );
        if (side.lengthSqr() > 1.0E-8D) {
            side = side.normalize();
        }

        double handedness =
                entity.getMainArm()
                        == net.minecraft.world.entity.HumanoidArm.RIGHT
                        ? 1.0D
                        : -1.0D;

        return entity.getEyePosition()
                .add(look.scale(0.45D))
                .add(side.scale(0.30D * handedness))
                .add(0.0D, -0.35D, 0.0D);
    }

    private static Vec3 randomUnitVector(
            RandomSource random
    ) {
        double y =
                random.nextDouble() * 2.0D - 1.0D;
        double angle =
                random.nextDouble() * Math.PI * 2.0D;
        double horizontal =
                Math.sqrt(
                        Math.max(
                                0.0D,
                                1.0D - y * y
                        )
                );

        return new Vec3(
                Math.cos(angle) * horizontal,
                y,
                Math.sin(angle) * horizontal
        );
    }

    private static void velocityParticle(
            ServerLevel level,
            net.minecraft.core.particles.ParticleOptions particle,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz
    ) {
        level.sendParticles(
                particle,
                x,
                y,
                z,
                0,
                vx,
                vy,
                vz,
                1.0D
        );
    }
}
