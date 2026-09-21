package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class ChargeSpeedSignParticle extends TextureSheetParticle {
    private ChargeSpeedSignParticle(ClientLevel level, double x, double y, double z,
                                    SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        friction = 0.82F;
        gravity = 0.0F;
        this.xd = xd * 0.10D;
        this.yd = 0.055D + Math.abs(yd) * 0.10D;
        this.zd = zd * 0.10D;
        quadSize = 0.42F;
        lifetime = 28;
        rCol = 0.749F;
        gCol = 0.251F;
        bCol = 0.251F;
        alpha = 0.92F;
        setSprite(sprites.get(random));
    }

    @Override
    public void tick() {
        super.tick();
        if (age > 10) {
            alpha = 0.92F * Math.max(0.0F, (lifetime - age) / (float) (lifetime - 10));
        }
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
            return new ChargeSpeedSignParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}
