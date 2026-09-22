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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Ground-facing, horizontally attached translation of Spell Engine .034
 * area_effect_700 used by Last Stand's persistent aura.
 */
public final class LastStandAuraParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Entity followEntity;
    private final double groundY;

    private LastStandAuraParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites,
            double encodedEntityId,
            double yd,
            double zd
    ) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.groundY = y;

        int entityId = (int) Math.round(encodedEntityId);
        this.followEntity = level.getEntity(entityId);

        xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;

        quadSize = 1.5F;
        lifetime = 22;

        rCol = 122.0F / 255.0F;
        gCol = 197.0F / 255.0F;
        bCol = 1.0F;
        alpha = 0.50F;

        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        if (removed) {
            return;
        }

        if (followEntity == null || followEntity.isRemoved()) {
            remove();
            return;
        }

        // Source Attachment.POSITION_HORIZONTAL: follow X/Z, stay ground-pinned.
        setPos(
                followEntity.getX(),
                groundY,
                followEntity.getZ()
        );

        setSpriteFromAge(sprites);
    }

    @Override
    public int getLightColor(float partialTick) {
        // area_effect entries default glow=true.
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(
            VertexConsumer consumer,
            Camera camera,
            float partialTick
    ) {
        var cameraPos = camera.getPosition();

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

        Quaternionf rotation =
                new Quaternionf()
                        .rotationX(
                                (float) Math.toRadians(-90.0D)
                        );

        renderGroundQuad(
                consumer,
                rotation,
                x,
                y,
                z,
                partialTick,
                false
        );

        // Spell Engine renders ground-facing area particles double-sided.
        renderGroundQuad(
                consumer,
                new Quaternionf(rotation)
                        .rotateX((float) Math.PI),
                x,
                y,
                z,
                partialTick,
                true
        );
    }

    private void renderGroundQuad(
            VertexConsumer consumer,
            Quaternionf rotation,
            float x,
            float y,
            float z,
            float partialTick,
            boolean reverse
    ) {
        float size = getQuadSize(partialTick);
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        int light = getLightColor(partialTick);

        Vector3f[] corners = {
                new Vector3f(1.0F, -1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(-1.0F, -1.0F, 0.0F)
        };

        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.mul(size);
            corner.add(x, y, z);
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
                .color(rCol, gCol, bCol, alpha)
                .uv2(light)
                .endVertex();
    }

    public static final class Provider
            implements ParticleProvider<SimpleParticleType> {
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
            return new LastStandAuraParticle(
                    level,
                    x,
                    y,
                    z,
                    sprites,
                    xd,
                    yd,
                    zd
            );
        }
    }
}
