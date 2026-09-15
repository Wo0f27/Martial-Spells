package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Martial-Spells-owned smoke sprite used by Shock Powder.
 *
 * <p>The texture set is original to Martial Spells. It recreates the broken,
 * powdery silhouette of the frozen source effect without copying Spell Engine
 * particle assets or depending on Spell Engine at runtime.</p>
 */
public final class ShockPowderSmokeParticle extends TextureSheetParticle {
    private ShockPowderSmokeParticle(ClientLevel level, double x, double y, double z,
                                     SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        friction = 0.90F;
        this.xd = xd;
        this.yd = yd + 0.01D;
        this.zd = zd;
        gravity = 0.0F;
        quadSize = 0.28F + random.nextFloat() * 0.28F;
        lifetime = 12 + random.nextInt(9);

        float shade = 0.72F + random.nextFloat() * 0.24F;
        rCol = shade;
        gCol = shade;
        bCol = shade;
        alpha = 0.86F;
        setSprite(sprites.get(random));
    }

    @Override
    public void tick() {
        super.tick();
        if (age > lifetime * 0.55F) {
            float remaining = Math.max(0.0F, (lifetime - age) / (lifetime * 0.45F));
            alpha = 0.86F * remaining;
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
            return new ShockPowderSmokeParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}
