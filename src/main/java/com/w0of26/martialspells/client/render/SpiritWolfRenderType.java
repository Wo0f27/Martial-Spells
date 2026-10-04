package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Archers' translucent emissive spirit layer, including depth writes for water/shadow ordering. */
final class SpiritWolfRenderType extends RenderType {
    private static RenderType spirit;

    private SpiritWolfRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                                 int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                                 Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    static RenderType spirit(ResourceLocation texture) {
        if (spirit == null) {
            spirit = create("martial_spells_spirit_wolf", DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS, 1536, true, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setOverlayState(OVERLAY)
                            .createCompositeState(true));
        }
        return spirit;
    }
}
