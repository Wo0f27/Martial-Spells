package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.model.BattleBannerModel;
import com.w0of26.martialspells.entity.BattleBannerEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class BattleBannerRenderer
        extends EntityRenderer<BattleBannerEntity> {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "textures/entity/battle_banner.png"
            );

    private final BattleBannerModel model;

    public BattleBannerRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        model =
                new BattleBannerModel(
                        context.bakeLayer(
                                BattleBannerModel.LAYER
                        )
                );
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            BattleBannerEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
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
        model.setupAnim(
                entity,
                0.0F,
                0.0F,
                entity.tickCount + partialTick,
                0.0F,
                0.0F
        );

        VertexConsumer vertices =
                bufferSource.getBuffer(
                        RenderType.entityCutout(
                                TEXTURE
                        )
                );
        model.renderToBuffer(
                poseStack,
                vertices,
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
                LightTexture.FULL_BRIGHT
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            BattleBannerEntity entity
    ) {
        return TEXTURE;
    }
}
