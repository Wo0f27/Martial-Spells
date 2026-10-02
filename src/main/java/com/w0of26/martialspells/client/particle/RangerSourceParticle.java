package com.w0of26.martialspells.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Local copies of the exact Spell Engine 1.10.5 stripe and roots particle defaults. */
public final class RangerSourceParticle extends TextureSheetParticle {
    public enum Kind { MAGIC_STRIPE, ROOTS }

    private final SpriteSet sprites;
    private final Kind kind;

    private RangerSourceParticle(ClientLevel level, double x, double y, double z,
                                 double xd, double yd, double zd, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        if (kind == Kind.MAGIC_STRIPE) {
            hasPhysics = false;
            friction = 0.96F;
            gravity = 0.0F;
            this.xd = xd + signed(0.005F);
            this.yd = yd + signed(0.005F);
            this.zd = zd + signed(0.005F);
            setPos(x + signed(0.05F), y + signed(0.05F), z + signed(0.05F));
            float darken = 1.0F - random.nextFloat() * 0.65F;
            setColor((0xBF / 255.0F) * darken, (0x40 / 255.0F) * darken,
                    (0x40 / 255.0F) * darken);
            quadSize = 0.11F * (1.0F + (random.nextFloat() * 2.0F - 1.0F) * 0.33F);
            lifetime = Math.max(1, Math.round(16.0F * (1.0F
                    + (random.nextFloat() * 2.0F - 1.0F) * 0.5F)));
        } else {
            hasPhysics = true;
            friction = 0.95F;
            gravity = 0.225F;
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            quadSize = 0.25F;
            lifetime = Math.max(1, Math.round((14.0F / 0.42F) * (1.0F
                    + (random.nextFloat() * 2.0F - 1.0F) * 0.64F)));
            // The source sprite has pivot=1; only its rendered quad is lifted.
        }
        setSpriteFromAge(sprites);
    }

    private double signed(float amount) {
        return (random.nextFloat() - random.nextFloat()) * amount;
    }

    @Override public void tick() {
        super.tick();
        if (!removed) setSpriteFromAge(sprites);
    }

    @Override public void render(VertexConsumer vertices, Camera camera, float partialTick) {
        if (kind != Kind.ROOTS) {
            super.render(vertices, camera, partialTick);
            return;
        }
        // Lift only the visual quad, keeping ground collision at the real position.
        double originalY = y;
        double originalYo = yo;
        y += quadSize;
        yo += quadSize;
        try {
            super.render(vertices, camera, partialTick);
        } finally {
            y = originalY;
            yo = originalYo;
        }
    }

    @Override public int getLightColor(float partialTick) {
        return kind == Kind.MAGIC_STRIPE ? 0xF000F0 : super.getLightColor(partialTick);
    }

    @Override public ParticleRenderType getRenderType() {
        return kind == Kind.ROOTS ? ParticleRenderType.PARTICLE_SHEET_OPAQUE
                : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xd, double yd, double zd) {
            return new RangerSourceParticle(level, x, y, z, xd, yd, zd, sprites, kind);
        }
    }
}
