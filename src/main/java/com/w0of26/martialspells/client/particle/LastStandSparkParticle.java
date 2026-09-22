package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Exact-shape local translation of Spell Engine .034 magic_spark with Last
 * Stand's PHYSICAL_BLUE payload and DECELERATE motion.
 */
public final class LastStandSparkParticle extends TextureSheetParticle {
    private LastStandSparkParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites,
            double xd,
            double yd,
            double zd
    ) {
        super(level, x, y, z, xd, yd, zd);

        friction = 0.768F;
        gravity = 0.0F;
        hasPhysics = false;

        this.xd = xd
                + (random.nextFloat() - random.nextFloat()) * 0.005F;
        this.yd = yd
                + (random.nextFloat() - random.nextFloat()) * 0.005F;
        this.zd = zd
                + (random.nextFloat() - random.nextFloat()) * 0.005F;

        setPos(
                x + (random.nextFloat() - random.nextFloat()) * 0.05F,
                y + (random.nextFloat() - random.nextFloat()) * 0.05F,
                z + (random.nextFloat() - random.nextFloat()) * 0.05F
        );

        float darken =
                1.0F - random.nextFloat() * 0.65F;

        // Spell Engine Color.PHYSICAL_BLUE = #7AC5FF.
        rCol = (122.0F / 255.0F) * darken;
        gCol = (197.0F / 255.0F) * darken;
        bCol = darken;
        alpha = 0.75F;

        quadSize = 0.11F
                * (1.0F
                + (random.nextFloat() * 2.0F - 1.0F) * 0.33F);

        lifetime = Math.max(
                1,
                Math.round(
                        16.0F
                                * (1.0F
                                + (random.nextFloat() * 2.0F - 1.0F)
                                * 0.50F)
                )
        );

        setSprite(sprites.get(random));
    }

    @Override
    public int getLightColor(float partialTick) {
        // Spell Engine magic_spark defaults glow=true.
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider
            implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new LastStandSparkParticle(
                    level,
                    x,
                    y,
                    z,
                    sprites,
                    xd,
                    yd,
                    zd
            );
        }
    }
}
