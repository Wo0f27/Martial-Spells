package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Short-lived pale-violet electrical fragment used by Shock Powder. */
public final class ShockPowderArcParticle extends TextureSheetParticle {
    private ShockPowderArcParticle(ClientLevel level, double x, double y, double z,
                                   SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        friction = 0.96F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        gravity = 0.0F;
        quadSize = 0.42F + random.nextFloat() * 0.24F;
        lifetime = 4 + random.nextInt(5);

        rCol = 1.0F;
        gCol = 0.78F;
        bCol = 1.0F;
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
            return new ShockPowderArcParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}
