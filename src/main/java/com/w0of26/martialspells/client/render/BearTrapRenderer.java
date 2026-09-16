package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.model.BearTrapModel;
import com.w0of26.martialspells.entity.BearTrapEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Frozen Rogues Bear Trap renderer translated to Forge/Mojmap. */
public final class BearTrapRenderer extends EntityRenderer<BearTrapEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID,
            "textures/entity/bear_trap.png"
    );

    private final BearTrapModel model;

    public BearTrapRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BearTrapModel(context.bakeLayer(BearTrapModel.LAYER));
        this.shadowRadius = 0.45F;
    }

    @Override
    public ResourceLocation getTextureLocation(BearTrapEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(
            BearTrapEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (entity.isPlacementPending()) {
            return;
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + 180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.5D, 0.0D);

        model.setupAnim(
                entity,
                0.0F,
                0.0F,
                entity.getClientPhaseAge(partialTick),
                0.0F,
                0.0F
        );
        VertexConsumer vertices = bufferSource.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(
                poseStack,
                vertices,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F
        );
        poseStack.popPose();
    }
}
