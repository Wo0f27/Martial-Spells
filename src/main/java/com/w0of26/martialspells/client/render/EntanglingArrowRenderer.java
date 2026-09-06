package com.w0of26.martialspells.client.render;

import com.w0of26.martialspells.entity.EntanglingArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class EntanglingArrowRenderer extends ArrowRenderer<EntanglingArrow> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/entity/projectiles/arrow.png");

    public EntanglingArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntanglingArrow entity) {
        return TEXTURE;
    }
}
