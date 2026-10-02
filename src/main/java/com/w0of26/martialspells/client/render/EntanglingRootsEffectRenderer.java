package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderLivingEvent;

/** Local Forge translation of frozen Archers RootsRenderer. */
public final class EntanglingRootsEffectRenderer {
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "spell_effect/entangling_roots");
    private static final RenderType LAYER = RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS);

    private EntanglingRootsEffectRenderer() {}

    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(MartialEffectRegistry.ENTANGLING_ROOTS.get())) return;
        Minecraft client = Minecraft.getInstance();
        if (entity == client.player && client.options.getCameraType().isFirstPerson()) return;
        var model = client.getModelManager().getModel(MODEL);
        if (model == client.getModelManager().getMissingModel()) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0D, 0.5D, 0.0D);
        pose.translate(-0.5D, -0.5D, -0.5D);
        client.getItemRenderer().renderModelLists(model, ItemStack.EMPTY,
                event.getPackedLight(), OverlayTexture.NO_OVERLAY, pose,
                event.getMultiBufferSource().getBuffer(LAYER));
        pose.popPose();
    }
}
