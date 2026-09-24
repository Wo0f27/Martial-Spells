package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.JudgementVisualEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Renders the frozen Paladins Judgement projectile model at source scale. */
public final class JudgementVisualRenderer
        extends EntityRenderer<JudgementVisualEntity> {
    public static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_projectile/judgement"
            );

    private static final float SCALE = 1.2F;

    public JudgementVisualRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            JudgementVisualEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Vec3 velocity =
                entity.getDeltaMovement();
        if (velocity.lengthSqr() <= 1.0E-8D) {
            return;
        }

        Vec3 direction =
                velocity.normalize();
        float directionYaw =
                (float) Math.toDegrees(
                        Math.atan2(direction.x, direction.z)
                ) + 180.0F;
        float directionPitch =
                (float) Math.toDegrees(
                        Math.asin(
                                Mth.clamp(
                                        direction.y,
                                        -1.0D,
                                        1.0D
                                )
                        )
                );

        poseStack.pushPose();
        poseStack.mulPose(
                Axis.YP.rotationDegrees(directionYaw)
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(directionPitch)
        );
        poseStack.scale(
                SCALE,
                SCALE,
                SCALE
        );

        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(MODEL);
        VertexConsumer vertices =
                bufferSource.getBuffer(
                        Sheets.translucentCullBlockSheet()
                );

        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        Minecraft.getInstance()
                .getItemRenderer()
                .renderModelLists(
                        model,
                        ItemStack.EMPTY,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        vertices
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
            JudgementVisualEntity entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
