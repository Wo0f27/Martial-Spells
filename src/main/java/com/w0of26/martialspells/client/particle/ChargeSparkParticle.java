package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class ChargeSparkParticle extends TextureSheetParticle {
    private ChargeSparkParticle(ClientLevel level, double x, double y, double z,
                                SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        friction = 0.72F;
        gravity = 0.0F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        quadSize = 0.13F + random.nextFloat() * 0.12F;
        lifetime = 7 + random.nextInt(6);
        rCol = 0.749F;
        gCol = 0.251F;
        bCol = 0.251F;
        alpha = 0.88F;
        setSprite(sprites.get(random));
    }

    @Override
    public void tick() {
        super.tick();
        alpha = 0.88F * Math.max(0.0F, (lifetime - age) / (float) lifetime);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new ChargeSparkParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}
