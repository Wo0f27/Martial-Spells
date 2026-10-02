package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.effects.HuntersMarkVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;

/** Camera-facing source Hunter's Mark model, driven by its tracked visual flag. */
public final class HuntersMarkEffectRenderer {
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "spell_effect/hunters_mark");
    private static final RenderType MARK_LAYER =
            RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);

    private HuntersMarkEffectRenderer() {}

    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!((HuntersMarkVisualState) entity).martialSpells$hasHuntersMarkVisual()
                || !entity.isAlive()) return;
        Minecraft client = Minecraft.getInstance();
        if (entity == client.player && client.options.getCameraType().isFirstPerson()) return;
        BakedModel model = client.getModelManager().getModel(MODEL);
        if (model == client.getModelManager().getMissingModel()) return;

        // Forge's Post event occurs after the living renderer restores its pose.
        // Its stack is entity-relative, as in Spell Engine's render-TAIL hook.
        Vec3 direction = client.gameRenderer.getMainCamera().getPosition()
                .subtract(entity.position()).normalize().scale(entity.getBbWidth() * 0.5D);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(direction.x, entity.getBbHeight() * 0.75D, direction.z);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F
                + (float) Math.toDegrees(Math.atan2(direction.x, direction.z))));
        pose.mulPose(Axis.XP.rotationDegrees(client.gameRenderer.getMainCamera().getXRot()));
        pose.translate(-0.5D, -0.5D, -0.5D);
        client.getItemRenderer().renderModelLists(model, ItemStack.EMPTY,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose,
                event.getMultiBufferSource().getBuffer(MARK_LAYER));
        pose.popPose();
    }
}
