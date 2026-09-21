package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Rage-tinted smoke used by Demoralizing Shout release and impact presentation. */
public final class DemoralizeSmokeParticle extends TextureSheetParticle {
    private DemoralizeSmokeParticle(
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
        friction = 0.82F;
        gravity = -0.01F;
        this.xd = xd;
        this.yd = yd + 0.015D;
        this.zd = zd;
        quadSize = 0.20F + random.nextFloat() * 0.22F;
        lifetime = 8 + random.nextInt(7);

        // Spell Engine Color.RAGE = #BF4040.
        rCol = 0.749F;
        gCol = 0.251F;
        bCol = 0.251F;
        alpha = 0.78F;
        setSprite(sprites.get(random));
    }

    @Override
    public void tick() {
        super.tick();
        alpha = 0.78F * Math.max(0.0F, (lifetime - age) / (float) lifetime);
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
            return new DemoralizeSmokeParticle(
                    level, x, y, z, sprites, xd, yd, zd
            );
        }
    }
}
