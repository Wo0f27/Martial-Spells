package com.w0of26.martialspells.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Animated area-effect particle matching Spell Engine's GROUND and CAMERA
 * facing modes. The spawn velocity payload is repurposed as:
 * x = scale, y = alpha, z = unused.
 */
public final class PaladinSourceAreaParticle
        extends TextureSheetParticle {
    public enum Facing {
        GROUND,
        CAMERA
    }

    private final SpriteSet sprites;
    private final Facing facing;
    private final Entity followEntity;
    private final Vec3 followOffset;

    private PaladinSourceAreaParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites,
            Facing facing,
            int frames,
            double encodedScale,
            double encodedAlpha
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
        this.facing = facing;

        int followId =
                facing == Facing.CAMERA
                        ? (int) Math.round(zd)
                        : -1;
        followEntity =
                followId > 0
                        ? level.getEntity(followId)
                        : null;
        followOffset =
                followEntity != null
                        ? new Vec3(
                                x - followEntity.getX(),
                                y - followEntity.getY(),
                                z - followEntity.getZ()
                        )
                        : Vec3.ZERO;

        xd = 0.0D;
        yd = 0.0D;
        zd = 0.0D;
        gravity = 0.0F;
        friction = 1.0F;
        hasPhysics = false;

        quadSize =
                Math.max(
                        0.01F,
                        (float) Math.abs(encodedScale)
                );
        alpha =
                Mth.clamp(
                        (float) encodedAlpha,
                        0.0F,
                        1.0F
                );

        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 0.80F;

        lifetime =
                Math.max(
                        1,
                        frames
                );

        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        if (!removed) {
            if (followEntity != null
                    && !followEntity.isRemoved()) {
                setPos(
                        followEntity.getX() + followOffset.x,
                        followEntity.getY() + followOffset.y,
                        followEntity.getZ() + followOffset.z
                );
            }
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

    @Override
    public void render(
            VertexConsumer consumer,
            Camera camera,
            float partialTick
    ) {
        if (facing == Facing.CAMERA) {
            super.render(
                    consumer,
                    camera,
                    partialTick
            );
            return;
        }

        var cameraPos =
                camera.getPosition();

        float x =
                (float) (
                        Mth.lerp(
                                partialTick,
                                xo,
                                this.x
                        ) - cameraPos.x()
                );
        float y =
                (float) (
                        Mth.lerp(
                                partialTick,
                                yo,
                                this.y
                        ) - cameraPos.y()
                );
        float z =
                (float) (
                        Mth.lerp(
                                partialTick,
                                zo,
                                this.z
                        ) - cameraPos.z()
                );

        Quaternionf top =
                new Quaternionf()
                        .rotationX(
                                (float) Math.toRadians(
                                        -90.0D
                                )
                        );

        renderQuad(
                consumer,
                top,
                x,
                y,
                z,
                partialTick,
                false
        );

        // Spell Engine's ground decals are explicitly double sided.
        renderQuad(
                consumer,
                new Quaternionf(top)
                        .rotateX(
                                (float) Math.PI
                        ),
                x,
                y,
                z,
                partialTick,
                true
        );
    }

    private void renderQuad(
            VertexConsumer consumer,
            Quaternionf rotation,
            float x,
            float y,
            float z,
            float partialTick,
            boolean reverse
    ) {
        float size =
                getQuadSize(partialTick);
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        int light =
                getLightColor(partialTick);

        Vector3f[] corners = {
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.mul(size);
            corner.add(
                    x,
                    y,
                    z
            );
        }

        if (!reverse) {
            vertex(consumer, corners[0], u1, v1, light);
            vertex(consumer, corners[1], u1, v0, light);
            vertex(consumer, corners[2], u0, v0, light);
            vertex(consumer, corners[3], u0, v1, light);
        } else {
            vertex(consumer, corners[3], u0, v1, light);
            vertex(consumer, corners[2], u0, v0, light);
            vertex(consumer, corners[1], u1, v0, light);
            vertex(consumer, corners[0], u1, v1, light);
        }
    }

    private void vertex(
            VertexConsumer consumer,
            Vector3f position,
            float u,
            float v,
            int light
    ) {
        consumer.vertex(
                        position.x(),
                        position.y(),
                        position.z()
                )
                .uv(u, v)
                .color(
                        rCol,
                        gCol,
                        bCol,
                        alpha
                )
                .uv2(light)
                .endVertex();
    }

    public static final class Provider
            implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Facing facing;
        private final int frames;

        public Provider(
                SpriteSet sprites,
                Facing facing,
                int frames
        ) {
            this.sprites = sprites;
            this.facing = facing;
            this.frames = frames;
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
            return new PaladinSourceAreaParticle(
                    level,
                    x,
                    y,
                    z,
                    sprites,
                    facing,
                    frames,
                    xd,
                    yd
            );
        }
    }
}
