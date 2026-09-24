package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge-safe Blessed Strikes glow layer.
 *
 * <p>The previous GLINT_PROGRAM port could blacken the base item on Forge.
 * This layer keeps the source streak texture, additive blend, EQUAL depth
 * mask and Holy tint, but uses the color-capable emissive entity shader so the
 * overlay can never replace/darken the already-rendered item.</p>
 */
public final class BlessedStrikesGlowRenderTypes
        extends RenderType {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "martial_spells",
                    "textures/misc/paladin_item_glow.png"
            );

    private static final Set<RenderType> GLOW_LAYERS =
            ConcurrentHashMap.newKeySet();

    private static RenderType layer;

    private BlessedStrikesGlowRenderTypes(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            Runnable setupState,
            Runnable clearState
    ) {
        super(
                name,
                format,
                mode,
                bufferSize,
                affectsCrumbling,
                sortOnUpload,
                setupState,
                clearState
        );
    }

    public static RenderType itemGlow() {
        if (layer != null) {
            return layer;
        }

        RenderStateShard.TransparencyStateShard additive =
                new RenderStateShard.TransparencyStateShard(
                        "martial_spells_blessed_strikes_additive",
                        () -> {
                            RenderSystem.enableBlend();
                            RenderSystem.blendFunc(
                                    GlStateManager.SourceFactor.ONE,
                                    GlStateManager.DestFactor.ONE
                            );
                        },
                        () -> {
                            RenderSystem.disableBlend();
                            RenderSystem.defaultBlendFunc();
                        }
                );

        layer =
                create(
                        "martial_spells_blessed_strikes_glow",
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.QUADS,
                        1536,
                        false,
                        false,
                        CompositeState.builder()
                                .setShaderState(
                                        RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER
                                )
                                .setTextureState(
                                        new RenderStateShard.TextureStateShard(
                                                TEXTURE,
                                                true,
                                                false
                                        )
                                )
                                .setWriteMaskState(
                                        COLOR_WRITE
                                )
                                .setCullState(
                                        CULL
                                )
                                .setDepthTestState(
                                        EQUAL_DEPTH_TEST
                                )
                                .setTransparencyState(
                                        additive
                                )
                                .setOverlayState(
                                        OVERLAY
                                )
                                .setLightmapState(
                                        LIGHTMAP
                                )
                                .createCompositeState(false)
                );

        GLOW_LAYERS.add(layer);
        return layer;
    }

    public static boolean isGlowLayer(
            RenderType renderType
    ) {
        return GLOW_LAYERS.contains(
                renderType
        );
    }
}
