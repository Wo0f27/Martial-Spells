package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.BarrageArrow;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders Barrage's physical projectile with the same crossed-quad
 * silhouette used by Iron's Magic Arrow, but with Martial Spells' own
 * recolored texture.
 *
 * The projectile deliberately uses Minecraft's ordinary translucent
 * entity render type instead of Iron's additive energy-swirl type.
 * Full-bright lighting preserves the supernatural appearance while
 * avoiding shader/render-state compatibility issues for this custom
 * AbstractArrow entity.
 */
public final class BarrageArrowRenderer
        extends EntityRenderer<BarrageArrow> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "textures/entity/barrage_arrow.png"
            );

    private static final float MODEL_SCALE = 0.13F;

    public BarrageArrowRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
    }

    @Override
    public void render(
            BarrageArrow entity,
            float yaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light
    ) {
        poseStack.pushPose();

        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-7D) {
            float xRot = -(
                    (float) (
                            Mth.atan2(
                                    motion.horizontalDistance(),
                                    motion.y
                            ) * (180.0D / Math.PI)
                    ) - 90.0F
            );

            float yRot = -(
                    (float) (
                            Mth.atan2(
                                    motion.z,
                                    motion.x
                            ) * (180.0D / Math.PI)
                    ) + 90.0F
            );

            poseStack.mulPose(
                    Axis.YP.rotationDegrees(yRot)
            );
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(xRot)
            );
        }

        renderModel(
                poseStack,
                bufferSource
        );

        poseStack.popPose();

        super.render(
                entity,
                yaw,
                partialTicks,
                poseStack,
                bufferSource,
                light
        );
    }

    private static void renderModel(
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        poseStack.scale(
                MODEL_SCALE,
                MODEL_SCALE,
                MODEL_SCALE
        );

        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(TEXTURE)
                );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(90.0F)
        );
        poseStack.translate(
                -2.0D,
                0.0D,
                0.0D
        );

        for (int j = 0; j < 4; ++j) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(90.0F)
            );

            vertex(
                    poseMatrix,
                    normalMatrix,
                    consumer,
                    -8,
                    -2,
                    0,
                    0.0F,
                    0.0F
            );
            vertex(
                    poseMatrix,
                    normalMatrix,
                    consumer,
                    8,
                    -2,
                    0,
                    0.5F,
                    0.0F
            );
            vertex(
                    poseMatrix,
                    normalMatrix,
                    consumer,
                    8,
                    2,
                    0,
                    0.5F,
                    0.15625F
            );
            vertex(
                    poseMatrix,
                    normalMatrix,
                    consumer,
                    -8,
                    2,
                    0,
                    0.0F,
                    0.15625F
            );
        }
    }

    private static void vertex(
            Matrix4f matrix,
            Matrix3f normal,
            VertexConsumer consumer,
            int x,
            int y,
            int z,
            float u,
            float v
    ) {
        consumer.vertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .color(
                        255,
                        255,
                        255,
                        255
                )
                .uv(u, v)
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(
                        LightTexture.FULL_BRIGHT
                )
                .normal(
                        normal,
                        0.0F,
                        0.0F,
                        1.0F
                )
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(
            BarrageArrow entity
    ) {
        return TEXTURE;
    }
}
