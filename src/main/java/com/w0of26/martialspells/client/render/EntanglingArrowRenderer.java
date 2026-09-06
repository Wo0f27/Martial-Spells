package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.w0of26.martialspells.entity.EntanglingArrow;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Entangling Arrow remains a physical AbstractArrow mechanically so RWA and Apothic
 * ranged scaling still apply, while delegating its projectile body to Iron's Magic Arrow visual.
 */
public final class EntanglingArrowRenderer extends EntityRenderer<EntanglingArrow> {
    public EntanglingArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public void render(EntanglingArrow entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light) {
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

        MagicArrowRenderer.renderModel(poseStack, bufferSource);
        poseStack.popPose();

        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntanglingArrow entity) {
        return MagicArrowRenderer.getTextureLocation();
    }
}
