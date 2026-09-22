package com.w0of26.martialspells.visual;

import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Source-shaped Last Stand casting and persistent-aura particle emission.
 */
public final class LastStandVisuals {
    public static final int CAST_SPARK_COUNT = 8;
    public static final int CAST_SMOKE_COUNT = 6;

    private LastStandVisuals() {}

    public static void spawnCasting(
            ServerLevel level,
            LivingEntity entity
    ) {
        Vec3 center = entity.position().add(
                0.0D,
                entity.getBbHeight() * 0.5D,
                0.0D
        );

        // Source: SPHERE, speed 0.2-0.3, pre_travel 6, invert=true.
        for (int i = 0; i < CAST_SPARK_COUNT; i++) {
            Vec3 outward = randomSphere(level, 0.20D, 0.30D);
            Vec3 origin = center.add(outward.scale(6.0D));
            Vec3 inward = outward.scale(-1.0D);

            level.sendParticles(
                    MartialParticleRegistry.LAST_STAND_SPARK.get(),
                    origin.x,
                    origin.y,
                    origin.z,
                    0,
                    inward.x,
                    inward.y,
                    inward.z,
                    1.0D
            );
        }

        // Source: CIRCLE at FEET (vertical_origin 0.1), speed 0.05-0.1.
        double y = entity.getY() + entity.getBbHeight() * 0.10D;
        for (int i = 0; i < CAST_SMOKE_COUNT; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double speed = 0.05D + level.random.nextDouble() * 0.05D;
            double xd = Math.sin(angle) * speed;
            double zd = Math.cos(angle) * speed;

            level.sendParticles(
                    MartialParticleRegistry.LAST_STAND_SMOKE.get(),
                    entity.getX(),
                    y,
                    entity.getZ(),
                    0,
                    xd,
                    0.0D,
                    zd,
                    1.0D
            );
        }
    }

    /**
     * The entity id is carried in the exact-velocity slot so the client aura
     * particle can follow the entity horizontally like Spell Engine's attached
     * POSITION_HORIZONTAL area effect.
     */
    public static void spawnAura(
            ServerLevel level,
            LivingEntity entity
    ) {
        level.sendParticles(
                MartialParticleRegistry.LAST_STAND_AURA.get(),
                entity.getX(),
                entity.getY() + 0.10D,
                entity.getZ(),
                0,
                entity.getId(),
                0.0D,
                0.0D,
                1.0D
        );
    }

    private static Vec3 randomSphere(
            ServerLevel level,
            double minSpeed,
            double maxSpeed
    ) {
        double speed = minSpeed
                + level.random.nextDouble() * (maxSpeed - minSpeed);
        double yaw = level.random.nextDouble() * Math.PI * 2.0D;
        double pitch = level.random.nextDouble() * Math.PI * 2.0D;

        double cosPitch = Math.cos(pitch);
        return new Vec3(
                Math.cos(yaw) * cosPitch,
                Math.sin(pitch),
                Math.sin(yaw) * cosPitch
        ).normalize().scale(speed);
    }
}
