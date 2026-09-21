package com.w0of26.martialspells.visual;

import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Source-shaped dripping-blood particle batches used by Shattering Throw and
 * the Shattered Armor status effect.
 */
public final class ShatterBloodVfx {
    private ShatterBloodVfx() {}

    public static void spawnImpact(
            ServerLevel level,
            LivingEntity target
    ) {
        spawn(level, target, 10, 0.05D, 0.30D);
    }

    public static void spawn(
            ServerLevel level,
            LivingEntity target,
            int count,
            double minSpeed,
            double maxSpeed
    ) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5D;
        double z = target.getZ();

        for (int i = 0; i < count; i++) {
            Vec3 direction = randomSphere(
                    level,
                    minSpeed,
                    maxSpeed
            );
            level.sendParticles(
                    MartialParticleRegistry.SHATTER_BLOOD.get(),
                    x,
                    y,
                    z,
                    0,
                    direction.x,
                    direction.y,
                    direction.z,
                    1.0D
            );
        }
    }

    private static Vec3 randomSphere(
            ServerLevel level,
            double minSpeed,
            double maxSpeed
    ) {
        double speed = minSpeed
                + level.random.nextDouble()
                * (maxSpeed - minSpeed);
        double yaw =
                level.random.nextDouble() * Math.PI * 2.0D;
        double pitch =
                level.random.nextDouble() * Math.PI * 2.0D;

        double cosPitch = Math.cos(pitch);
        return new Vec3(
                Math.cos(yaw) * cosPitch,
                Math.sin(pitch),
                Math.sin(yaw) * cosPitch
        ).normalize().scale(speed);
    }
}
