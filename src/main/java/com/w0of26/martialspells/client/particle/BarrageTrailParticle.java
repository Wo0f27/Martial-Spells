package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public final class BarrageTrailParticle extends TextureSheetParticle {
    private BarrageTrailParticle(ClientLevel level, double x, double y, double z,
                                 SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        friction = 0.77F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        quadSize = 0.1F * (random.nextFloat() * 0.15F + 0.3F);
        scale(2.25F);
        lifetime = 5 + random.nextInt(25);
        gravity = 0.0F;
        setSprite(sprites.get(random));

        float f = random.nextFloat() * 0.6F + 0.4F;
        rCol = f * 0.28F;
        gCol = f * 0.95F;
        bCol = f * 0.34F;
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new BarrageTrailParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}
