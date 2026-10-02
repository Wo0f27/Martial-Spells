package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
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
}
