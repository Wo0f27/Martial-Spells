package com.w0of26.martialspells.client.render;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.model.SpiritWolfModel;
import com.w0of26.martialspells.entity.SpiritWolfEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Frozen Archers Direwolf model, texture and emissive depth-writing presentation. */
public final class SpiritWolfRenderer extends MobRenderer<SpiritWolfEntity, SpiritWolfModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "textures/entity/spirit_wolf.png");

    public SpiritWolfRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiritWolfModel(context.bakeLayer(SpiritWolfModel.LAYER)), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(SpiritWolfEntity entity) {
        return TEXTURE;
    }

    @Override @Nullable protected RenderType getRenderType(SpiritWolfEntity entity,
                                                            boolean bodyVisible,
                                                            boolean translucent,
                                                            boolean glowing) {
        return bodyVisible ? SpiritWolfRenderType.spirit(TEXTURE) : null;
    }
}
