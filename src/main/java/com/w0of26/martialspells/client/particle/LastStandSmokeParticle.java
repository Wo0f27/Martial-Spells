package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Spell Engine .034 smoke_medium with Last Stand's 50%-alpha PHYSICAL_BLUE
 * payload.
 */
public final class LastStandSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private LastStandSmokeParticle(
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
        this.sprites = sprites;

        friction = 0.80F;
        gravity = -0.01F;
        hasPhysics = true;

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        float darken =
                1.0F - random.nextFloat() * 0.65F;

        rCol = (122.0F / 255.0F) * darken;
        gCol = (197.0F / 255.0F) * darken;
        bCol = darken;

        // smoke_medium opacity .8 * Last Stand payload alpha .5.
        alpha = 0.40F;

        quadSize = 0.15F
                * (1.0F
                + (random.nextFloat() * 2.0F - 1.0F) * 0.33F);

        float variance =
                1.0F
                        + (random.nextFloat() * 2.0F - 1.0F)
                        * 0.55F;

        // 9 animated frames at playback_speed .46.
        lifetime = Math.max(
                1,
                Math.round((9.0F / 0.46F) * variance)
        );

        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
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
            return new LastStandSmokeParticle(
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
