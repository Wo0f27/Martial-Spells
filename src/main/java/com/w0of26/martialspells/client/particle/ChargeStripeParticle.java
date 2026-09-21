package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class ChargeStripeParticle extends TextureSheetParticle {
    private ChargeStripeParticle(ClientLevel level, double x, double y, double z,
                                 SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        friction = 0.90F;
        gravity = -0.01F;
        this.xd = xd * 0.55D;
        this.yd = 0.025D + Math.abs(yd) * 0.35D;
        this.zd = zd * 0.55D;
        quadSize = 0.28F + random.nextFloat() * 0.24F;
        lifetime = 10 + random.nextInt(7);
        rCol = 0.749F;
        gCol = 0.251F;
        bCol = 0.251F;
        alpha = 0.95F;
        setSprite(sprites.get(random));
    }

    @Override
    public void tick() {
        super.tick();
        alpha = 0.95F * Math.max(0.0F, (lifetime - age) / (float) lifetime);
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
            return new ChargeStripeParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}
