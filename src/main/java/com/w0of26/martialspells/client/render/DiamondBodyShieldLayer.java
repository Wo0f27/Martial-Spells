package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.render.RenderHelper;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldModel;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldRenderer;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldTrimModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class DiamondBodyShieldLayer
        extends RenderLayer<
        AbstractClientPlayer,
        PlayerModel<AbstractClientPlayer>
        > {

    private static final int SHIELD_COUNT = 3;

    /*
     * 4.5 degrees each tick gives one complete orbit
     * every 80 ticks, or four seconds.
     */
    private static final float ORBIT_SPEED = 4.5F;

    private static final double ORBIT_RADIUS = 0.90D;

    /*
     * Render-layer coordinates begin around the upper
     * player model. Positive Y moves down toward the torso.
     */
    private static final double ORBIT_HEIGHT = 0.65D;

    private static final double BOB_AMOUNT = 0.06D;

    private static final float SHIELD_SCALE = 1.10F;

    private static final ResourceLocation SHIELD_TRIM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    IronsSpellbooks.MODID,
                    "textures/entity/shield/shield_trim.png"
            );

    private final ShieldModel shieldModel;
    private final ShieldTrimModel shieldTrimModel;

    public DiamondBodyShieldLayer(
            RenderLayerParent<
                    AbstractClientPlayer,
                    PlayerModel<AbstractClientPlayer>
                    > parent,
            EntityModelSet entityModels
    ) {
        super(parent);

        this.shieldModel =
                new ShieldModel(
                        entityModels.bakeLayer(
                                ShieldModel.LAYER_LOCATION
                        )
                );

        this.shieldTrimModel =
                new ShieldTrimModel(
                        entityModels.bakeLayer(
                                ShieldTrimModel.LAYER_LOCATION
                        )
                );
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!player.hasEffect(
                MartialEffectRegistry
                        .DIAMOND_BODY
                        .get()
        )) {
            return;
        }

        float time =
                player.tickCount
                        + partialTick;

        float baseOrbitAngle =
                time
                        * ORBIT_SPEED;

        float swirlU =
                time
                        * 0.01F;

        float swirlV =
                time
                        * 0.008F;

        VertexConsumer shieldConsumer =
                bufferSource.getBuffer(
                        RenderHelper
                                .CustomerRenderType
                                .magicSwirl(
                                        ShieldRenderer
                                                .SPECTRAL_OVERLAY_TEXTURE,
                                        swirlU,
                                        swirlV
                                )
                );

        VertexConsumer trimConsumer =
                bufferSource.getBuffer(
                        RenderType.energySwirl(
                                SHIELD_TRIM_TEXTURE,
                                0.0F,
                                0.0F
                        )
                );

        for (int index = 0;
             index < SHIELD_COUNT;
             index++) {

            float angle =
                    baseOrbitAngle
                            + (
                            360.0F
                                    / SHIELD_COUNT
                    )
                            * index;

            float bobPhase =
                    time
                            * 0.15F
                            + index
                            * (
                            (float) Math.PI
                                    * 2.0F
                                    / SHIELD_COUNT
                    );

            double bob =
                    Mth.sin(bobPhase)
                            * BOB_AMOUNT;

            poseStack.pushPose();

            /*
             * Rotate around the player first, then move
             * outward to produce an orbit.
             */
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            angle
                    )
            );

            poseStack.translate(
                    0.0D,
                    ORBIT_HEIGHT + bob,
                    ORBIT_RADIUS
            );

            /*
             * Face away from the player.
             */
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(180.0F)
            );

            /*
             * Turn the shield right-side up.
             */
            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(180.0F)
            );

            poseStack.scale(
                    SHIELD_SCALE,
                    SHIELD_SCALE,
                    SHIELD_SCALE
            );

            shieldModel.renderToBuffer(
                    poseStack,
                    shieldConsumer,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    0.65F,
                    0.65F,
                    0.65F,
                    1.0F
            );

            shieldTrimModel.renderToBuffer(
                    poseStack,
                    trimConsumer,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    1.0F,
                    1.0F,
                    1.0F,
                    0.45F
            );

            poseStack.popPose();
        }
    }
}