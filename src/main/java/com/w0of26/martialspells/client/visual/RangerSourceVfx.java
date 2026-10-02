package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-broadcast geometry from frozen Archers/Spell Engine presentation data. */
public final class RangerSourceVfx {
    private RangerSourceVfx() {}

    public static void powerShotRelease(ServerLevel level, LivingEntity caster) {
        RandomSource random = level.getRandom();
        // 10 PIPE, width_factor=2. Spell Engine's packet path uses width=0.5.
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            emit(level, MartialParticleRegistry.RANGER_MAGIC_STRIPE.get(),
                    caster.getX() + Math.cos(angle) * 0.5D,
                    caster.getY() + caster.getBbHeight() * 0.1D,
                    caster.getZ() + Math.sin(angle) * 0.5D,
                    0.0D, between(random, 0.01D, 0.1D), 0.0D);
        }
    }

    public static void rootsCloudTick(ServerLevel level, Vec3 center, double cloudHeight,
                                      double cloudWidth) {
        RandomSource random = level.getRandom();
        // 2 PILLAR per client cloud tick; radius from the cloud's width/2.
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = random.nextDouble() * cloudWidth * 0.5D;
            emit(level, MartialParticleRegistry.RANGER_ROOTS.get(),
                    center.x + Math.cos(angle) * radius,
                    center.y + cloudHeight * 0.1D,
                    center.z + Math.sin(angle) * radius,
                    0.0D, 0.0D, 0.0D);
        }
    }

    public static void rootsImpact(ServerLevel level, LivingEntity target) {
        RandomSource random = level.getRandom();
        // 2 vanilla falling spores, SPHERE, speed 0.1..0.2.
        for (int i = 0; i < 2; i++) {
            Vec3 velocity = sphereDirection(random).scale(between(random, 0.1D, 0.2D));
            emit(level, ParticleTypes.FALLING_SPORE_BLOSSOM,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    velocity.x, velocity.y, velocity.z);
        }
    }

    public static void huntersMarkImpact(ServerLevel level, LivingEntity target) {
        RandomSource random = level.getRandom();
        // 3 vanilla firework particles, PILLAR, packet width=0.5.
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = random.nextDouble() * 0.25D;
            emit(level, ParticleTypes.FIREWORK,
                    target.getX() + Math.cos(angle) * radius,
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ() + Math.sin(angle) * radius,
                    0.0D, between(random, 0.01D, 0.1D), 0.0D);
        }
    }

    /** Spell Engine LINE_VERTICAL: four independent upward velocities from ENTITY center. */
    public static void rainRelease(ServerLevel level, LivingEntity caster) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 4; i++) {
            emit(level, ParticleTypes.FIREWORK, caster.getX(), caster.getY(0.5D), caster.getZ(),
                    0, between(random, 0.6D, 0.9D), 0);
        }
    }

    /** Client travel batches: CIRCLE/LOOK crit every tick; LINE/LOOK firework every fourth tick. */
    public static void rainTravel(Level level, Entity meteor) {
        RandomSource random = level.getRandom();
        Vec3 origin = meteor.position().add(0, meteor.getBbHeight() * 0.5D, 0);
        Vec3 crit = lookAligned(circle(random, 0, 0.05D), meteor, false);
        emit(level, ParticleTypes.CRIT, origin, crit);
        if (Math.floorMod(meteor.tickCount, 4) == 0) {
            Vec3 firework = lookAligned(new Vec3(0, 0, between(random, 0.1D, 0.2D)), meteor, true);
            emit(level, ParticleTypes.FIREWORK, origin, firework);
        }
    }

    public static void rainDirectImpact(ServerLevel level, LivingEntity target) {
        sphereBatch(level, ParticleTypes.CRIT, target.position().add(0, target.getBbHeight() * 0.5D, 0),
                8, 0.2D, 0.4D);
    }

    public static void rainAreaImpact(ServerLevel level, Vec3 origin) {
        sphereBatch(level, ParticleTypes.CRIT, origin, 20, 0.25D, 0.5D);
        sphereBatch(level, ParticleTypes.POOF, origin, 5, 0.02D, 0.1D);
    }

    public static Vec3 rainGround(ServerLevel level, Entity meteor) {
        Vec3 origin = meteor.position();
        HitResult hit = level.clip(new ClipContext(origin.add(0, 1, 0), origin.add(0, -2, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, meteor));
        return hit.getType() == HitResult.Type.BLOCK
                ? new Vec3(origin.x, hit.getLocation().y + 0.1D, origin.z)
                : origin.add(0, 0.1D, 0);
    }

    /** SPHERE + preTravel(14) + invert: spawn 14 velocity units out, then fly inward. */
    public static void magicCharge(ServerLevel level, LivingEntity caster) {
        RandomSource random = level.getRandom();
        Vec3 origin = launchPoint(caster);
        for (int i = 0; i < 4; i++) {
            Vec3 outward = sphereDirection(random).scale(between(random, 0.02D, 0.1D));
            emit(level, MartialParticleRegistry.RANGER_NATURE_FLOAT.get(), origin.add(outward.scale(14)),
                    outward.reverse());
        }
    }

    /** Two independent LOOK-aligned CIRCLE batches, each emitted from the launch point. */
    public static void magicRelease(ServerLevel level, LivingEntity caster) {
        RandomSource random = level.getRandom();
        Vec3 origin = launchPoint(caster);
        for (int i = 0; i < 50; i++) {
            emit(level, MartialParticleRegistry.RANGER_NATURE_DECELERATE.get(), origin,
                    lookAligned(circle(random, 0.18D, 0.20D), caster, false));
        }
        for (int i = 0; i < 25; i++) {
            emit(level, MartialParticleRegistry.RANGER_NATURE_DECELERATE.get(), origin,
                    lookAligned(circle(random, 0.28D, 0.30D), caster, false));
        }
    }

    /** Exact LINE/LOOK roll: rotate each strand's velocity around the look axis. */
    public static void magicTravel(Level level, Entity projectile) {
        RandomSource random = level.getRandom();
        Vec3 origin = projectile.position().add(0, projectile.getBbHeight() * 0.5D, 0);
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < 5; i++) {
                Vec3 base = lookAligned(new Vec3(0, 0, between(random, 0.14D, 0.15D)), projectile, true);
                Vec3 axis = lookAxis(projectile);
                Vec3 velocity = rotateAround(base, axis, Math.toRadians(projectile.tickCount * 18D + strand * 180D));
                emit(level, MartialParticleRegistry.RANGER_NATURE_DECELERATE.get(), origin, velocity);
            }
        }
        for (int i = 0; i < 2; i++) {
            emit(level, ParticleTypes.FIREWORK, origin,
                    lookAligned(circle(random, 0, 0.05D), projectile, false));
        }
        for (int i = 0; i < 10; i++) {
            emit(level, MartialParticleRegistry.RANGER_NATURE_DECELERATE.get(), origin,
                    lookAligned(circle(random, 0, 0.05D), projectile, false));
        }
    }

    public static void magicImpact(ServerLevel level, LivingEntity target) {
        sphereBatch(level, MartialParticleRegistry.RANGER_NATURE_BURST.get(),
                target.position().add(0, target.getBbHeight() * 0.5D, 0), 25, 1.2D, 1.8D);
    }

    private static void sphereBatch(ServerLevel level, ParticleOptions particle, Vec3 origin,
                                    int count, double minSpeed, double maxSpeed) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            emit(level, particle, origin, sphereDirection(random).scale(between(random, minSpeed, maxSpeed)));
        }
    }

    private static Vec3 circle(RandomSource random, double minSpeed, double maxSpeed) {
        return new Vec3(0, 0, between(random, minSpeed, maxSpeed))
                .yRot((float) (random.nextDouble() * Math.PI * 2));
    }

    private static Vec3 lookAligned(Vec3 vector, Entity entity, boolean line) {
        // ParticleHelper's LINE case inverts pitch before applying LOOK alignment.
        float pitch = line ? pitch(entity) : -pitch(entity);
        return vector.xRot((float) Math.toRadians(pitch - 90F))
                .yRot((float) Math.toRadians(-yaw(entity)));
    }

    private static Vec3 lookAxis(Entity entity) {
        double yaw = Math.toRadians(yaw(entity));
        double pitch = Math.toRadians(pitch(entity));
        return new Vec3(Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                -Math.cos(yaw) * Math.cos(pitch)).normalize();
    }

    private static float yaw(Entity entity) {
        Vec3 velocity = entity.getDeltaMovement();
        return entity instanceof LivingEntity || velocity.lengthSqr() < 1.0E-8D
                ? entity.getYRot() : (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z));
    }

    private static float pitch(Entity entity) {
        Vec3 velocity = entity.getDeltaMovement();
        return entity instanceof LivingEntity || velocity.lengthSqr() < 1.0E-8D
                ? entity.getXRot() : (float) Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance()));
    }

    private static Vec3 rotateAround(Vec3 vector, Vec3 axis, double radians) {
        return vector.scale(Math.cos(radians))
                .add(axis.cross(vector).scale(Math.sin(radians)))
                .add(axis.scale(axis.dot(vector) * (1D - Math.cos(radians))));
    }

    private static Vec3 launchPoint(LivingEntity caster) {
        double height = caster.getEyeHeight(Pose.STANDING) - caster.getBbHeight() * 0.15D;
        return caster.position().add(0, height, 0).add(caster.getLookAngle().scale(0.5D));
    }

    private static Vec3 sphereDirection(RandomSource random) {
        Vec3 direction = new Vec3(1.0D, 0.0D, 0.0D)
                .zRot((float) (random.nextDouble() * Math.PI * 2.0D))
                .yRot((float) (random.nextDouble() * Math.PI * 2.0D));
        return direction;
    }

    private static double between(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static void emit(ServerLevel level, ParticleOptions particle,
                             double x, double y, double z, double vx, double vy, double vz) {
        level.sendParticles(particle, x, y, z, 0, vx, vy, vz, 1.0D);
    }

    private static void emit(ServerLevel level, ParticleOptions particle, Vec3 origin, Vec3 velocity) {
        emit(level, particle, origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
    }

    private static void emit(Level level, ParticleOptions particle, Vec3 origin, Vec3 velocity) {
        if (level.isClientSide) level.addParticle(particle, true,
                origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
    }
}
