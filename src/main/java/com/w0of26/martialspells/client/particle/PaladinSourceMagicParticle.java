package com.w0of26.martialspells.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Dependency-free reproduction of the Spell Engine 1.20.1 magic-particle
 * defaults used by the frozen Paladins spell set.
 *
 * <p>This intentionally implements only the motion/appearance subset CP11
 * needs: FLOAT, DECELERATE, ASCEND and BURST. Batch geometry remains
 * server-side in PaladinVfx.</p>
 */
public final class PaladinSourceMagicParticle
        extends TextureSheetParticle {
    public enum Motion {
        FLOAT,
        DECELERATE,
        ASCEND,
        BURST
    }

    private static final float BASE_SCALE = 0.11F;
    private static final float SCALE_VARIANCE = 0.33F;
    private static final float COLOR_VARIANCE = 0.65F;
    private static final float LIFETIME_VARIANCE = 0.50F;

    private final SpriteSet sprites;

    private PaladinSourceMagicParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            SpriteSet sprites,
            Motion motion,
            float red,
            float green,
            float blue,
            float opacity
    ) {
        super(
                level,
                x,
                y,
                z,
                0.0D,
                0.0D,
                0.0D
        );

        this.sprites = sprites;
        hasPhysics = false;

        switch (motion) {
            case FLOAT -> {
                friction = 0.96F;
                gravity = 0.0F;
                this.xd =
                        xd + randomSigned(0.005F);
                this.yd =
                        yd + randomSigned(0.005F);
                this.zd =
                        zd + randomSigned(0.005F);
                setPos(
                        x + randomSigned(0.05F),
                        y + randomSigned(0.05F),
                        z + randomSigned(0.05F)
                );
            }
            case DECELERATE -> {
                friction = 0.768F;
                gravity = 0.0F;
                this.xd =
                        xd + randomSigned(0.005F);
                this.yd =
                        yd + randomSigned(0.005F);
                this.zd =
                        zd + randomSigned(0.005F);
                setPos(
                        x + randomSigned(0.05F),
                        y + randomSigned(0.05F),
                        z + randomSigned(0.05F)
                );
            }
            case ASCEND -> {
                friction = 0.96F;
                gravity = -0.10F;
                speedUpWhenYMotionIsBlocked = true;
                this.xd =
                        xd * (
                                xd == 0.0D && zd == 0.0D
                                        ? 0.10D
                                        : 1.0D
                        )
                                + randomSigned(0.005F);
                this.yd =
                        yd * 0.20D
                                + 0.02D;
                this.zd =
                        zd * (
                                xd == 0.0D && zd == 0.0D
                                        ? 0.10D
                                        : 1.0D
                        )
                                + randomSigned(0.005F);
            }
            case BURST -> {
                friction = 0.70F;
                gravity = 0.50F;
                this.xd =
                        xd * 0.40D
                                + randomSigned(0.02F);
                this.yd =
                        yd * 0.40D
                                + randomSigned(0.02F);
                this.zd =
                        zd * 0.40D
                                + randomSigned(0.02F);
            }
        }

        float darken =
                1.0F
                        - random.nextFloat()
                        * COLOR_VARIANCE;
        rCol = red * darken;
        gCol = green * darken;
        bCol = blue * darken;
        alpha = opacity;

        quadSize =
                BASE_SCALE
                        * (
                        1.0F
                                + (
                                random.nextFloat() * 2.0F
                                        - 1.0F
                        ) * SCALE_VARIANCE
                );

        float lifetimeRoll =
                1.0F
                        + (
                        random.nextFloat() * 2.0F
                                - 1.0F
                ) * LIFETIME_VARIANCE;
        int naturalLifetime =
                motion == Motion.BURST
                        ? 8
                        : 16;
        lifetime =
                Math.max(
                        1,
                        Math.round(
                                naturalLifetime
                                        * lifetimeRoll
                        )
                );

        setSpriteFromAge(sprites);
    }

    private double randomSigned(
            float amount
    ) {
        return (
                random.nextFloat()
                        - random.nextFloat()
        ) * amount;
    }

    @Override
    public void tick() {
        super.tick();

        if (!removed) {
            setSpriteFromAge(sprites);
        }
    }

    @Override
    public int getLightColor(
            float partialTick
    ) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType
                .PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider
            implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Motion motion;
        private final float red;
        private final float green;
        private final float blue;
        private final float opacity;

        public Provider(
                SpriteSet sprites,
                Motion motion,
                float red,
                float green,
                float blue,
                float opacity
        ) {
            this.sprites = sprites;
            this.motion = motion;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.opacity = opacity;
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
            return new PaladinSourceMagicParticle(
                    level,
                    x,
                    y,
                    z,
                    xd,
                    yd,
                    zd,
                    sprites,
                    motion,
                    red,
                    green,
                    blue,
                    opacity
            );
        }
    }
}
