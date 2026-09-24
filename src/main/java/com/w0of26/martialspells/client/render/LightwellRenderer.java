package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.model.LightwellModel;
import com.w0of26.martialspells.entity.LightwellEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class LightwellRenderer
        extends EntityRenderer<LightwellEntity> {
    public static final ResourceLocation BASE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "textures/entity/lightwell_base.png"
            );
    public static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "textures/entity/lightwell_glow.png"
            );

    private final LightwellModel model;

    public LightwellRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        model =
                new LightwellModel(
                        context.bakeLayer(
                                LightwellModel.LAYER
                        )
                );
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            LightwellEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float scale =
                entity.lifecycleScale(
                        partialTick
                );
        if (scale <= 0.0F) {
            return;
        }

        float age =
                entity.tickCount + partialTick;
        float bob =
                0.225F
                        + Mth.sin(
                                age
                                        * ((float) Math.PI / 20.0F)
                        ) * 0.10F;

        poseStack.pushPose();
        poseStack.translate(
                0.0D,
                bob,
                0.0D
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        -entity.getYRot() + 180.0F
                )
        );
        poseStack.scale(
                -1.0F,
                -1.0F,
                1.0F
        );
        poseStack.translate(
                0.0D,
                -1.5D,
                0.0D
        );
        poseStack.scale(
                scale,
                scale,
                scale
        );

        model.setupAnim(
                entity,
                0.0F,
                0.0F,
                age,
                0.0F,
                0.0F
        );

        VertexConsumer base =
                bufferSource.getBuffer(
                        RenderType.entityCutout(
                                BASE_TEXTURE
                        )
                );
        model.renderToBuffer(
                poseStack,
                base,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        VertexConsumer glow =
                bufferSource.getBuffer(
                        RenderType.entityTranslucentEmissive(
                                GLOW_TEXTURE
                        )
                );
        model.renderToBuffer(
                poseStack,
                glow,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        poseStack.popPose();

        super.render(
                entity,
                entityYaw,
                partialTick,
                poseStack,
                bufferSource,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            LightwellEntity entity
    ) {
        return BASE_TEXTURE;
    }
}
