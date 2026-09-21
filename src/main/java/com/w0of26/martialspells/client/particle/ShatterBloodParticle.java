package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Local translation of Spell Engine 1.20.1-modern dripping_blood:
 * minecraft:drip_hang, #590000, DRIFT, scale 0.11 +/- 33%, gravity 0.8.
 */
public final class ShatterBloodParticle extends TextureSheetParticle {
    private ShatterBloodParticle(
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

        friction = 1.0F;
        gravity = 0.8F;
        hasPhysics = true;

        this.xd = xd + (random.nextDouble() * 2.0D - 1.0D) * 0.05D;
        this.yd = yd + (random.nextDouble() * 2.0D - 1.0D) * 0.05D;
        this.zd = zd + (random.nextDouble() * 2.0D - 1.0D) * 0.05D;

        quadSize = 0.11F * (
                1.0F + (random.nextFloat() * 2.0F - 1.0F) * 0.33F
        );
        lifetime = 20;

        // Spell Engine Color.from(0x590000).
        rCol = 0.349F;
        gCol = 0.0F;
        bCol = 0.0F;
        alpha = 1.0F;

        setSprite(sprites.get(random));
    }

    @Override
    public void tick() {
        super.tick();

        // Spell Engine DRIFT per-axis damping.
        xd *= 0.95D;
        yd *= 0.90D;
        zd *= 0.95D;
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
            return new ShatterBloodParticle(
                    level, x, y, z, sprites, xd, yd, zd
            );
        }
    }
}
