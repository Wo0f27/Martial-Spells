package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderLivingEvent;

/**
 * Frozen Paladins Divine Protection custom-model effect.
 *
 * <p>Upstream renders amplifier + 1 orbiting shields, evenly spaced around the
 * protected entity. Every shield is the Paladins base model plus its emissive
 * glow model, at scale 1.0, radius 0.35, rotating 2.25 degrees per tick from
 * an initial -45 degree phase.</p>
 */
public final class DivineProtectionRenderer {
    public static final ResourceLocation BASE_MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_effect/divine_protection"
            );
    public static final ResourceLocation GLOW_MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_effect/divine_protection_glow"
            );

    private static final float SCALE = 1.0F;
    private static final float HORIZONTAL_OFFSET = 0.35F;
    private static final float ORBITING_SPEED = 2.25F;

    private static final RenderType BASE_LAYER =
            RenderType.entityTranslucent(
                    TextureAtlas.LOCATION_BLOCKS
            );
    private static final RenderType GLOW_LAYER =
            PaladinSpellModelRenderTypes.radiate();

    private DivineProtectionRenderer() {}

    public static void onRenderLivingPost(
            RenderLivingEvent.Post<?, ?> event
    ) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect =
                entity.getEffect(
                        MartialEffectRegistry
                                .DIVINE_PROTECTION
                                .get()
                );

        if (effect == null) {
            return;
        }

        render(
                entity,
                effect.getAmplifier(),
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight()
        );
    }

    private static void render(
            LivingEntity entity,
            int amplifier,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();
        BakedModel baseModel =
                minecraft.getModelManager()
                        .getModel(BASE_MODEL);
        BakedModel glowModel =
                minecraft.getModelManager()
                        .getModel(GLOW_MODEL);

        int stacks = amplifier + 1;
        float initialAngle =
                (entity.tickCount + partialTick)
                        * ORBITING_SPEED
                        - 45.0F;
        float turnAngle =
                360.0F / stacks;
        float verticalOffset =
                entity.getBbHeight() / 2.0F;

        poseStack.pushPose();

        for (int i = 0; i < stacks; i++) {
            float angle =
                    initialAngle
                            + turnAngle * i;

            poseStack.pushPose();
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(angle)
            );
            poseStack.translate(
                    0.0D,
                    verticalOffset,
                    -HORIZONTAL_OFFSET
            );
            poseStack.scale(
                    SCALE,
                    SCALE,
                    SCALE
            );
            poseStack.translate(
                    -0.5D,
                    -0.5D,
                    -0.5D
            );

            renderModel(
                    minecraft,
                    baseModel,
                    BASE_LAYER,
                    poseStack,
                    bufferSource,
                    packedLight
            );
            renderModel(
                    minecraft,
                    glowModel,
                    GLOW_LAYER,
                    poseStack,
                    bufferSource,
                    packedLight
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void renderModel(
            Minecraft minecraft,
            BakedModel model,
            RenderType renderType,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        VertexConsumer vertices =
                bufferSource.getBuffer(renderType);

        minecraft.getItemRenderer()
                .renderModelLists(
                        model,
                        ItemStack.EMPTY,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        vertices
                );
    }
}
