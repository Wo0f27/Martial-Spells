package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 1.20.1 render layer matching Spell Engine's itemGlow layer.
 *
 * <p>Extending RenderType is deliberate: the vanilla render-state constants
 * used by the source implementation are protected on 1.20.1.</p>
 */
public final class BlessedStrikesGlowRenderTypes
        extends RenderType {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "martial_spells",
                    "textures/misc/paladin_item_glow.png"
            );

    private static final float TEXTURE_SCALE = 8.0F;
    private static final float GAIN = 3.0F;
    private static final float OPACITY_PER_STACK = 0.20F;
    private static final int MAX_STACKS = 5;

    private static final RenderType[] LAYERS =
            new RenderType[MAX_STACKS];
    private static final Set<RenderType> GLOW_LAYERS =
            ConcurrentHashMap.newKeySet();

    private static final float[] SHADER_COLOR_RESTORE =
            new float[4];
    private static float glintAlphaRestore = 1.0F;

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

    public static RenderType itemGlow(
            int stackIndex
    ) {
        int index =
                Math.max(
                        0,
                        Math.min(
                                MAX_STACKS - 1,
                                stackIndex
                        )
                );

        RenderType cached =
                LAYERS[index];
        if (cached != null) {
            return cached;
        }

        float opacity =
                OPACITY_PER_STACK
                        * (index + 1);

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

        RenderStateShard.TexturingStateShard texturing =
                new RenderStateShard.TexturingStateShard(
                        "martial_spells_blessed_strikes_texturing",
                        () -> setupTexturing(opacity),
                        BlessedStrikesGlowRenderTypes::clearTexturing
                );

        RenderType layer =
                create(
                        "martial_spells_blessed_strikes_glow_"
                                + (index + 1),
                        DefaultVertexFormat.POSITION_TEX,
                        VertexFormat.Mode.QUADS,
                        1536,
                        false,
                        false,
                        CompositeState.builder()
                                .setShaderState(
                                        RENDERTYPE_GLINT_SHADER
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
                                        NO_CULL
                                )
                                .setDepthTestState(
                                        EQUAL_DEPTH_TEST
                                )
                                .setTransparencyState(
                                        additive
                                )
                                .setTexturingState(
                                        texturing
                                )
                                .createCompositeState(false)
                );

        LAYERS[index] = layer;
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

    private static void setupTexturing(
            float opacity
    ) {
        float[] current =
                RenderSystem.getShaderColor();
        System.arraycopy(
                current,
                0,
                SHADER_COLOR_RESTORE,
                0,
                4
        );
        glintAlphaRestore =
                RenderSystem.getShaderGlintAlpha();

        RenderSystem.setTextureMatrix(
                textureMatrix()
        );

        // Frozen Blessed Strikes Holy color #FFFFCC, source gain 3.
        float intensity =
                opacity * GAIN;

        RenderSystem.setShaderColor(
                intensity,
                intensity,
                0.80F * intensity,
                1.0F
        );
        RenderSystem.setShaderGlintAlpha(
                1.0F
        );
    }

    private static void clearTexturing() {
        RenderSystem.resetTextureMatrix();
        RenderSystem.setShaderColor(
                SHADER_COLOR_RESTORE[0],
                SHADER_COLOR_RESTORE[1],
                SHADER_COLOR_RESTORE[2],
                SHADER_COLOR_RESTORE[3]
        );
        RenderSystem.setShaderGlintAlpha(
                glintAlphaRestore
        );
    }

    private static Matrix4f textureMatrix() {
        long time =
                (long) (
                        Util.getMillis()
                                * Minecraft.getInstance()
                                .options
                                .glintSpeed()
                                .get()
                                * 8.0D
                );

        float x =
                (float) (time % 110000L)
                        / 110000.0F;
        float y =
                (float) (time % 30000L)
                        / 30000.0F;

        return new Matrix4f()
                .translation(
                        -x,
                        y,
                        0.0F
                )
                .rotateZ(
                        (float) (
                                Math.PI / 18.0D
                        )
                )
                .scale(TEXTURE_SCALE);
    }
}
