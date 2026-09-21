package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.NettedClientVisuals;
import com.w0of26.martialspells.spells.ThrowNetSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderLivingEvent;

/**
 * Frozen Rogues Netted model-FX translation.
 *
 * <p>The source effect starts one body-height above the victim, drops by 0.6
 * blocks over six ticks with EASE_IN_QUAD, and snaps from zero to full scale
 * over eight ticks with EASE_OUT_BACK. Playback is ONCE, so the final net
 * remains around the target until Netted expires.</p>
 *
 * <p>On Forge 1.20.1 this renderer is driven by RenderLivingEvent.Post,
 * registered explicitly from FMLClientSetupEvent. Visual timing comes from
 * NettedClientVisuals, a minimal S2C mirror of Spell Engine 1.20.1-modern's
 * synchronized status-effect record (entity/effect application world time),
 * rather than from the vanilla client MobEffect map.</p>
 */
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

    private static final RenderType NETTED_RENDER_TYPE =
            RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);

    private static boolean renderEventObserved;
    private static boolean nettedDiagnosticLogged;

    private NettedEffectRenderer() {}

    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!renderEventObserved) {
            renderEventObserved = true;
            MartialSpells.LOGGER.info(
                    "W4 Netted Forge render event observed"
            );
        }

        LivingEntity entity = event.getEntity();
        NettedClientVisuals.State state =
                NettedClientVisuals.stateFor(entity.getId());
        if (state == null) {
            return;
        }

        render(
                entity,
                state,
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight()
        );
    }

    private static void render(
            LivingEntity entity,
            NettedClientVisuals.State state,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        long clientWorldTime = entity.level().getGameTime();
        float age = state.age(clientWorldTime, partialTick);

        float dropT = Mth.clamp(age / DROP_END_TICK, 0.0F, 1.0F);
        float dropProgress = dropT * dropT;

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

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft
                .getModelManager()
                .getModel(MODEL);

        if (!nettedDiagnosticLogged) {
            nettedDiagnosticLogged = true;
            MartialSpells.LOGGER.info(
                    "W4 Netted entity render active: entity={} id={} appliedAt={} expiresAt={} age={} modelMissing={} bakedQuads={}",
                    entity.getType(),
                    entity.getId(),
                    state.appliedAtWorldTime(),
                    state.expiresAtWorldTime(),
                    age,
                    model == minecraft.getModelManager().getMissingModel(),
                    countBakedQuads(model)
            );
        }

        poseStack.pushPose();

        poseStack.scale(entityScale, entityScale, entityScale);
        poseStack.translate(
                0.0D,
                INITIAL_TRANSLATE_Y + DROP_Y * dropProgress,
                0.0D
        );
        poseStack.scale(snapProgress, snapProgress, snapProgress);

        VertexConsumer vertices =
                bufferSource.getBuffer(NETTED_RENDER_TYPE);

        poseStack.translate(-0.5D, -0.5D, -0.5D);
        minecraft
                .getItemRenderer()
                .renderModelLists(
                        model,
                        ItemStack.EMPTY,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        vertices
                );

        poseStack.popPose();
    }

    private static int countBakedQuads(BakedModel model) {
        int count = 0;
        RandomSource random = RandomSource.create(42L);

        count += model.getQuads(null, null, random).size();
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            count += model.getQuads(null, direction, random).size();
        }
        return count;
    }

    private static float easeOutBack(float x) {
        float shifted = x - 1.0F;
        return 1.0F
                + BACK_C3 * shifted * shifted * shifted
                + BACK_C1 * shifted * shifted;
    }
}
