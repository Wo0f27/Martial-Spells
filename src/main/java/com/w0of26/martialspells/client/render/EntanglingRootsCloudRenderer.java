package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.w0of26.martialspells.entity.EntanglingRootsCloud;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** Cloud visuals are server-broadcast particles; no unapproved model asset is rendered. */
public final class EntanglingRootsCloudRenderer extends EntityRenderer<EntanglingRootsCloud> {
    public EntanglingRootsCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntanglingRootsCloud entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntanglingRootsCloud entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
