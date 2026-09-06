package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.w0of26.martialspells.entity.BarrageArrow;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Barrage keeps its own physical AbstractArrow entity and gameplay,
 * but delegates projectile visuals to Iron's proven Magic Arrow model.
 */
public final class BarrageArrowRenderer extends EntityRenderer<BarrageArrow> {

    public BarrageArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
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
            float xRot = -((float) (
                    Mth.atan2(motion.horizontalDistance(), motion.y)
                            * (180.0D / Math.PI)
            ) - 90.0F);

            float yRot = -((float) (
                    Mth.atan2(motion.z, motion.x)
                            * (180.0D / Math.PI)
            ) + 90.0F);

            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        }

        // Use Iron's exact Magic Arrow render path and original texture.
        MagicArrowRenderer.renderModel(poseStack, bufferSource);

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

    @Override
    public ResourceLocation getTextureLocation(BarrageArrow entity) {
        return MagicArrowRenderer.getTextureLocation();
    }
}
