package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.w0of26.martialspells.entity.CaltropFieldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CaltropFieldRenderer
        extends EntityRenderer<CaltropFieldEntity> {
    private static final float PLACEHOLDER_SCALE = 0.35F;

    private static final BlockState PLACEHOLDER_CALTROP =
            Blocks.POINTED_DRIPSTONE.defaultBlockState();

    private final BlockRenderDispatcher blockRenderer;

    public CaltropFieldRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);

        blockRenderer =
                Minecraft
                        .getInstance()
                        .getBlockRenderer();

        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            CaltropFieldEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        int size = entity.getFieldSize();

        double startOffset =
                -(size - 1) / 2.0D;

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                double offsetX =
                        startOffset + x;
                double offsetZ =
                        startOffset + z;

                poseStack.pushPose();

                poseStack.translate(
                        offsetX
                                - PLACEHOLDER_SCALE
                                / 2.0D,
                        0.01D,
                        offsetZ
                                - PLACEHOLDER_SCALE
                                / 2.0D
                );

                poseStack.scale(
                        PLACEHOLDER_SCALE,
                        PLACEHOLDER_SCALE,
                        PLACEHOLDER_SCALE
                );

                blockRenderer.renderSingleBlock(
                        PLACEHOLDER_CALTROP,
                        poseStack,
                        buffer,
                        packedLight,
                        OverlayTexture.NO_OVERLAY
                );

                poseStack.popPose();
            }
        }

        super.render(
                entity,
                entityYaw,
                partialTick,
                poseStack,
                buffer,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            CaltropFieldEntity entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
