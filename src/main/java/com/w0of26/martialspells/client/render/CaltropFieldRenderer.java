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

    /*
     * Visual-only scatter inside each occupied terrain cell.
     *
     * 0.28 keeps the scaled 0.35-wide placeholder fully inside the
     * owning block while removing the rigid centered-grid appearance.
     */
    private static final double MAX_VISUAL_OFFSET = 0.28D;

    private static final long X_OFFSET_SALT =
            0x6A09E667F3BCC909L;
    private static final long Z_OFFSET_SALT =
            0xBB67AE8584CAA73BL;

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

        for (int cellX = 0; cellX < size; cellX++) {
            for (int cellZ = 0; cellZ < size; cellZ++) {
                double surfaceY =
                        entity.getCaltropSurfaceY(
                                cellX,
                                cellZ
                        );

                /*
                 * No valid support block in this cell's three-block
                 * vertical search means no caltrop is rendered here.
                 */
                if (Double.isNaN(surfaceY)) {
                    continue;
                }

                double offsetX =
                        entity.getCellOffset(cellX)
                                + getVisualOffset(
                                entity,
                                cellX,
                                cellZ,
                                X_OFFSET_SALT
                        );
                double offsetZ =
                        entity.getCellOffset(cellZ)
                                + getVisualOffset(
                                entity,
                                cellX,
                                cellZ,
                                Z_OFFSET_SALT
                        );
                double offsetY =
                        surfaceY - entity.getY();

                poseStack.pushPose();

                poseStack.translate(
                        offsetX
                                - PLACEHOLDER_SCALE
                                / 2.0D,
                        offsetY,
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

    /**
     * Produces a stable pseudo-random visual offset for one field cell.
     *
     * The field UUID and local cell coordinates make the placement
     * deterministic across frames and clients. This method is used by
     * the renderer only; caltrop terrain checks and hazard mechanics
     * remain centered on their original cells.
     */
    private static double getVisualOffset(
            CaltropFieldEntity entity,
            int cellX,
            int cellZ,
            long salt
    ) {
        long seed =
                entity.getUUID().getMostSignificantBits()
                        ^ Long.rotateLeft(
                        entity.getUUID()
                                .getLeastSignificantBits(),
                        21
                )
                        ^ (long) cellX
                        * 0x9E3779B97F4A7C15L
                        ^ (long) cellZ
                        * 0xC2B2AE3D27D4EB4FL
                        ^ salt;

        long mixed = mix64(seed);

        /*
         * Convert the upper 53 bits to the same [0, 1) precision used
         * by a Java double, then remap into the configured scatter.
         */
        double unit =
                (mixed >>> 11)
                        * 0x1.0p-53;

        return (
                unit * 2.0D - 1.0D
        ) * MAX_VISUAL_OFFSET;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    @Override
    public ResourceLocation getTextureLocation(
            CaltropFieldEntity entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
