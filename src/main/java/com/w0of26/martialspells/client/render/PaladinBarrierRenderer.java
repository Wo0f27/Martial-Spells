package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.PaladinBarrierEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Forge-native reconstruction of the frozen Paladins segmented Holy Barrier.
 *
 * <p>The source dome consists of twelve translucent golden panels: six on the
 * upper half and six mirrored below, with a 0.8*range visible radius and an
 * expiration pulse during the final second.</p>
 */
public final class PaladinBarrierRenderer
        extends EntityRenderer<PaladinBarrierEntity> {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "item/barrier"
            );

    private static final int[] LIGHT_UP_ORDER =
            {0, 2, 8, 6, 4, 3, 9, 1, 5, 10, 7, 11};

    private static final float RANGE = 4.0F;
    private static final float RADIUS = RANGE * 0.8F;
    private static final float Z_SLANT =
            (float) (Math.PI / 8.0D);
    private static final float SIZE =
            (RADIUS * Mth.sqrt(3.0F)) / 3.0F;
    private static final float OFFSET =
            RADIUS * (Mth.sin(Z_SLANT) + 1.0F);

    public PaladinBarrierRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            PaladinBarrierEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        TextureAtlasSprite sprite =
                Minecraft.getInstance()
                        .getTextureAtlas(
                                InventoryMenu.BLOCK_ATLAS
                        )
                        .apply(TEXTURE);

        VertexConsumer vertices =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                InventoryMenu.BLOCK_ATLAS
                        )
                );

        double fullTime =
                entity.level().getGameTime()
                        / 20.0D;
        long wholeTime =
                entity.level().getGameTime()
                        / 20L;
        double delta =
                (fullTime - wholeTime) * 2.0D;
        if (delta > 1.0D) {
            delta =
                    2.0D - delta;
        }
        delta =
                1.0D
                        - Math.pow(
                                1.0D - delta,
                                4.0D
                        );

        poseStack.pushPose();
        poseStack.translate(
                0.0D,
                1.0D,
                0.0D
        );

        for (int half = 0; half < 2; half++) {
            for (int panel = 0; panel < 6; panel++) {
                poseStack.pushPose();

                if (half == 0) {
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    180.0F
                            )
                    );
                }

                poseStack.mulPose(
                        Axis.YP.rotation(
                                (float) (
                                        panel
                                                / 3.0D
                                                * Math.PI
                                )
                        )
                );
                poseStack.translate(
                        OFFSET,
                        0.0D,
                        0.0D
                );
                poseStack.mulPose(
                        Axis.ZP.rotation(
                                Z_SLANT
                        )
                );

                float red = 1.0F;
                float green = 0.80F;
                float blue = 0.40F;
                float alpha = 0.65F;

                if (entity.isExpiring()) {
                    float remaining =
                            PaladinBarrierEntity.LIFE_TICKS
                                    - (
                                    entity.tickCount
                                            + partialTick
                            );
                    alpha =
                            0.85F
                                    * Math.abs(
                                    Mth.cos(
                                            remaining
                                                    * 1.25F
                                                    / 10.0F
                                                    * (float) Math.PI
                                    )
                            );
                } else if (
                        wholeTime % 12L
                                == LIGHT_UP_ORDER[
                                panel + half * 6
                                ]
                ) {
                    float glow =
                            (float) (0.5D * delta);
                    red =
                            blend(
                                    red,
                                    1.0F,
                                    glow
                            );
                    green =
                            blend(
                                    green,
                                    1.0F,
                                    glow
                            );
                    blue =
                            blend(
                                    blue,
                                    1.0F,
                                    glow
                            );
                    alpha =
                            blend(
                                    alpha,
                                    0.90F,
                                    glow
                            );
                }

                renderPanel(
                        poseStack,
                        vertices,
                        sprite,
                        LightTexture.FULL_BRIGHT,
                        red,
                        green,
                        blue,
                        alpha
                );

                poseStack.popPose();
            }
        }

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

    private static void renderPanel(
            PoseStack poseStack,
            VertexConsumer vertices,
            TextureAtlasSprite sprite,
            int packedLight,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Matrix4f pose =
                poseStack.last().pose();
        Matrix3f normal =
                poseStack.last().normal();

        float u1 = sprite.getU0();
        float u2 = sprite.getU1();
        float v1 = sprite.getV0();
        float v2 = sprite.getV1();

        quad(
                pose,
                normal,
                vertices,
                sprite,
                packedLight,
                red,
                green,
                blue,
                alpha,
                0.0F,
                RADIUS,
                -SIZE,
                0.0F,
                0.0F,
                -SIZE,
                0.0F,
                0.0F,
                SIZE,
                0.0F,
                RADIUS,
                SIZE,
                u1,
                v2,
                u1,
                v1,
                u2,
                v1,
                u2,
                v2
        );

        quad(
                pose,
                normal,
                vertices,
                sprite,
                packedLight,
                red,
                green,
                blue,
                alpha,
                0.0F,
                RADIUS,
                SIZE,
                0.0F,
                0.0F,
                SIZE,
                0.0F,
                0.0F,
                -SIZE,
                0.0F,
                RADIUS,
                -SIZE,
                u1,
                v2,
                u1,
                v1,
                u2,
                v1,
                u2,
                v2
        );
    }

    private static void quad(
            Matrix4f pose,
            Matrix3f normal,
            VertexConsumer vertices,
            TextureAtlasSprite sprite,
            int packedLight,
            float red,
            float green,
            float blue,
            float alpha,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            float u1,
            float v1,
            float u2,
            float v2,
            float u3,
            float v3,
            float u4,
            float v4
    ) {
        vertex(
                pose,
                normal,
                vertices,
                packedLight,
                x1,
                y1,
                z1,
                red,
                green,
                blue,
                0.0F,
                u1,
                v1
        );
        vertex(
                pose,
                normal,
                vertices,
                packedLight,
                x2,
                y2,
                z2,
                red,
                green,
                blue,
                alpha,
                u2,
                v2
        );
        vertex(
                pose,
                normal,
                vertices,
                packedLight,
                x3,
                y3,
                z3,
                red,
                green,
                blue,
                alpha,
                u3,
                v3
        );
        vertex(
                pose,
                normal,
                vertices,
                packedLight,
                x4,
                y4,
                z4,
                red,
                green,
                blue,
                0.0F,
                u4,
                v4
        );
    }

    private static void vertex(
            Matrix4f pose,
            Matrix3f normal,
            VertexConsumer vertices,
            int packedLight,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v
    ) {
        vertices.vertex(
                        pose,
                        x,
                        y,
                        z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .uv(
                        u,
                        v
                )
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(
                        packedLight
                )
                .normal(
                        normal,
                        0.0F,
                        1.0F,
                        0.0F
                )
                .endVertex();
    }

    private static float blend(
            float min,
            float max,
            float delta
    ) {
        return min
                + (max - min) * delta;
    }

    @Override
    public ResourceLocation getTextureLocation(
            PaladinBarrierEntity entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
