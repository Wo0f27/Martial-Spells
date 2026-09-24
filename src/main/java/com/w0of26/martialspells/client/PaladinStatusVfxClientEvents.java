package com.w0of26.martialspells.client;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Client-only persistent Paladins status presentation that the frozen mod
 * registers through Spell Engine's CustomParticleStatusEffect API.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class PaladinStatusVfxClientEvents {
    private static final ResourceLocation TURTLECORE_STUN =
            ResourceLocation.fromNamespaceAndPath(
                    "turtlecore",
                    "stunned"
            );

    private PaladinStatusVfxClientEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity =
                event.getEntity();

        if (!(entity.level() instanceof ClientLevel level)) {
            return;
        }

        if (entity.hasEffect(
                MartialEffectRegistry.LEVITATE.get()
        )) {
            levitateClouds(
                    level,
                    entity
            );
        }

        if (entity.hasEffect(
                MartialEffectRegistry
                        .PRIEST_ABSORPTION
                        .get()
        )) {
            priestAbsorptionAura(
                    level,
                    entity
            );
        }

        MobEffect stun =
                ForgeRegistries.MOB_EFFECTS
                        .getValue(TURTLECORE_STUN);

        if (stun != null
                && entity.hasEffect(stun)) {
            stunOrbit(
                    level,
                    entity
            );
        }
    }

    /**
     * Frozen Paladins:
     * cloud, CIRCLE x2, speed .01-.05, FEET origin, extent .45,
     * emitted every three entity ticks.
     */
    private static void levitateClouds(
            ClientLevel level,
            LivingEntity entity
    ) {
        if (entity.tickCount % 3 != 0) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            double angle =
                    level.random.nextDouble()
                            * Math.PI * 2.0D;
            double speed =
                    0.01D
                            + level.random.nextDouble()
                            * 0.04D;

            double vx =
                    Math.sin(angle) * speed;
            double vz =
                    Math.cos(angle) * speed;

            // Spell Engine CIRCLE applies extent to the generated direction
            // before spawning the particle.
            double x =
                    entity.getX()
                            + vx * 0.45D;
            double y =
                    entity.getY()
                            + entity.getBbHeight()
                            * 0.10D;
            double z =
                    entity.getZ()
                            + vz * 0.45D;

            level.addParticle(
                    ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    vx,
                    0.0D,
                    vz
            );
        }
    }

    /**
     * Frozen Penance absorption:
     * area_effect_553, camera-facing aura, attached POSITION_SCALED,
     * scale 1.4, Holy color at 75% opacity, one particle every 30 ticks.
     */
    private static void priestAbsorptionAura(
            ClientLevel level,
            LivingEntity entity
    ) {
        if (entity.tickCount % 30 != 0) {
            return;
        }

        level.addParticle(
                MartialParticleRegistry
                        .PALADIN_AREA_553_CAMERA
                        .get(),
                entity.getX(),
                entity.getY()
                        + entity.getBbHeight() * 0.50D,
                entity.getZ(),
                1.40D,
                0.75D,
                entity.getId()
        );
    }

    /**
     * Frozen Spell Engine StunParticleSpawner used by Paladins Judgement:
     * one CRIT particle circles at 18 degrees/tick, 1.2 entity heights high.
     */
    private static void stunOrbit(
            ClientLevel level,
            LivingEntity entity
    ) {
        double angle =
                Math.toRadians(
                        (entity.tickCount % 360)
                                * 18.0D
                );

        double radius =
                entity.getBbWidth() * 0.50D;

        double x =
                entity.getX()
                        + Math.sin(angle) * radius;
        double y =
                entity.getY()
                        + entity.getBbHeight() * 1.20D;
        double z =
                entity.getZ()
                        + Math.cos(angle) * radius;

        level.addParticle(
                ParticleTypes.CRIT,
                x,
                y,
                z,
                0.0D,
                0.0D,
                0.0D
        );
    }
}
