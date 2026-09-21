package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.spells.ThrowNetSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Frozen Rogues Netted model-FX translation.
 *
 * <p>The source effect starts one body-height above the victim, drops by 0.6
 * blocks over six ticks with EASE_IN_QUAD, and snaps from zero to full scale
 * over eight ticks with EASE_OUT_BACK. Playback is ONCE, so the final net
 * remains around the target until Netted expires.</p>
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class NettedEffectRenderer {
    public static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_effect/net_trap"
            );

    private static final float ENTITY_WIDTH_BASELINE = 0.5F;
    private static final float ENTITY_SCALE_MIN = 0.5F;
    private static final float ENTITY_SCALE_MAX = 3.0F;
    private static final float INITIAL_TRANSLATE_Y = 1.1F;
    private static final float DROP_Y = -0.6F;
    private static final float DROP_END_TICK = 6.0F;
    private static final float SNAP_END_TICK = 8.0F;
    private static final float BACK_C1 = 1.70158F;
    private static final float BACK_C3 = BACK_C1 + 1.0F;

    private NettedEffectRenderer() {}

    @SubscribeEvent
    public static void renderNetted(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance netted =
                entity.getEffect(MartialEffectRegistry.NET_TRAP.get());
        if (netted == null) {
            return;
        }

        float age = Math.max(
                0.0F,
                ThrowNetSpell.NETTED_DURATION_TICKS
                        - netted.getDuration()
                        + event.getPartialTick()
        );

        float dropT = Mth.clamp(age / DROP_END_TICK, 0.0F, 1.0F);
        float dropProgress = dropT * dropT; // EASE_IN_QUAD

        float snapT = Mth.clamp(age / SNAP_END_TICK, 0.0F, 1.0F);
        float snapProgress = easeOutBack(snapT);
        if (snapProgress <= 1.0E-4F) {
            return;
        }

        float entityScale = Mth.clamp(
                (float) Math.sqrt(entity.getBbWidth() / ENTITY_WIDTH_BASELINE),
                ENTITY_SCALE_MIN,
                ENTITY_SCALE_MAX
        );

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // ModelFxEffectRenderer.entityScaling(WIDTH, 0.5F).
        poseStack.scale(entityScale, entityScale, entityScale);

        // Initial + animated source transforms.
        poseStack.translate(
                0.0D,
                INITIAL_TRANSLATE_Y + DROP_Y * dropProgress,
                0.0D
        );
        poseStack.scale(snapProgress, snapProgress, snapProgress);

        BakedModel model = Minecraft.getInstance()
                .getModelManager()
                .getModel(MODEL);
        VertexConsumer vertices = event.getMultiBufferSource()
                .getBuffer(Sheets.translucentCullBlockSheet());

        // Spell Engine CustomModels.renderModel raw-model centering.
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance()
                .getItemRenderer()
                .renderModelLists(
                        model,
                        ItemStack.EMPTY,
                        event.getPackedLight(),
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        vertices
                );

        poseStack.popPose();
    }

    private static float easeOutBack(float x) {
        float shifted = x - 1.0F;
        return 1.0F
                + BACK_C3 * shifted * shifted * shifted
                + BACK_C1 * shifted * shifted;
    }
}
