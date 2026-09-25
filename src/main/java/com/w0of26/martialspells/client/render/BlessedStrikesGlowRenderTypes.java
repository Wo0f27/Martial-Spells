package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.w0of26.martialspells.MartialSpells;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blessed Strikes weapon glow translated from Spell Engine 1.10.5.
 *
 * <p>The frozen source uses a POSITION_TEX glint pass, Holy #FFFFCC tint,
 * gain 3, a scrolling scale-8 texture matrix, additive ONE/ONE blending and
 * an EQUAL depth mask. Forge 1.20.1 can expose the generated item's complete
 * front/back quad to that opaque streak texture in first person, so this port
 * adds one target-side safety rule: the glow shader also samples the item's
 * real block-atlas texel and discards pixels whose item alpha is below 0.1.
 * The visible glow remains source-authored; the extra sample only supplies
 * the silhouette mask that the frozen source expected from depth.</p>
 */
public final class BlessedStrikesGlowRenderTypes
        extends RenderType {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "martial_spells",
                    "textures/misc/paladin_item_glow.png"
            );

    private static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "blessed_strikes_glow"
            );

    private static final float GAIN = 3.0F;
    private static final float HOLY_BLUE =
            204.0F / 255.0F;
    private static final int OPACITY_STEPS = 5;

    private static final Map<Integer, RenderType> LAYERS =
            new ConcurrentHashMap<>();
    private static final Set<RenderType> GLOW_LAYERS =
            ConcurrentHashMap.newKeySet();

    private static ShaderInstance glowShader;

    /*
     * RenderSystem color/glint state is global. Source Spell Engine restores
     * the values it found instead of assuming vanilla defaults; do the same
     * here so Blessed Strikes does not alter unrelated enchantment glints.
     */
    private static final float[] SHADER_COLOR_TO_RESTORE =
            new float[4];
    private static float glintAlphaToRestore = 1.0F;

    private static final RenderStateShard.ShaderStateShard GLOW_SHADER =
            new RenderStateShard.ShaderStateShard(
                    () -> glowShader
            );

    /*
     * Sampler0 is the normal item/block atlas and therefore carries the real
     * sword alpha. Sampler1 is the exact frozen Spell Engine streak texture.
     */
    private static final RenderStateShard.MultiTextureStateShard GLOW_TEXTURES =
            RenderStateShard.MultiTextureStateShard
                    .builder()
                    .add(
                            TextureAtlas.LOCATION_BLOCKS,
                            false,
                            false
                    )
                    .add(
                            TEXTURE,
                            true,
                            false
                    )
                    .build();

    private static final RenderStateShard.TransparencyStateShard ADDITIVE =
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

    public static void registerShader(
            RegisterShadersEvent event
    ) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        SHADER_ID,
                        DefaultVertexFormat.POSITION_TEX
                ),
                shader -> glowShader = shader
        );
    }

    public static RenderType itemGlow(
            float opacity
    ) {
        float clampedOpacity =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                opacity
                        )
                );

        int opacityStep =
                Math.max(
                        1,
                        Math.min(
                                OPACITY_STEPS,
                                Math.round(
                                        clampedOpacity
                                                * OPACITY_STEPS
                                )
                        )
                );

        return LAYERS.computeIfAbsent(
                opacityStep,
                BlessedStrikesGlowRenderTypes::createItemGlowLayer
        );
    }

    private static RenderType createItemGlowLayer(
            int opacityStep
    ) {
        float opacity =
                opacityStep
                        / (float) OPACITY_STEPS;

        RenderStateShard.TexturingStateShard texturing =
                new RenderStateShard.TexturingStateShard(
                        "martial_spells_blessed_strikes_texturing_"
                                + opacityStep,
                        () -> {
                            float[] shaderColor =
                                    RenderSystem.getShaderColor();

                            System.arraycopy(
                                    shaderColor,
                                    0,
                                    SHADER_COLOR_TO_RESTORE,
                                    0,
                                    SHADER_COLOR_TO_RESTORE.length
                            );

                            glintAlphaToRestore =
                                    RenderSystem.getShaderGlintAlpha();

                            RenderSystem.setTextureMatrix(
                                    BlessedStrikesItemGlow
                                            .textureMatrix()
                            );

                            /*
                             * Source Color.HOLY is #FFFFCC. Opacity is folded
                             * into RGB because ONE/ONE additive blending does
                             * not use source alpha. Gain 3 matches Spell
                             * Engine's item glow.
                             */
                            float intensity =
                                    opacity * GAIN;

                            RenderSystem.setShaderColor(
                                    intensity,
                                    intensity,
                                    HOLY_BLUE * intensity,
                                    1.0F
                            );

                            /*
                             * Blessed Strikes is gameplay state, so it must
                             * not disappear with the cosmetic Glint Strength
                             * option.
                             */
                            RenderSystem.setShaderGlintAlpha(
                                    1.0F
                            );
                        },
                        () -> {
                            RenderSystem.resetTextureMatrix();

                            RenderSystem.setShaderColor(
                                    SHADER_COLOR_TO_RESTORE[0],
                                    SHADER_COLOR_TO_RESTORE[1],
                                    SHADER_COLOR_TO_RESTORE[2],
                                    SHADER_COLOR_TO_RESTORE[3]
                            );

                            RenderSystem.setShaderGlintAlpha(
                                    glintAlphaToRestore
                            );
                        }
                );

        RenderType layer =
                create(
                        "martial_spells_blessed_strikes_glow_"
                                + opacityStep,
                        DefaultVertexFormat.POSITION_TEX,
                        VertexFormat.Mode.QUADS,
                        1536,
                        false,
                        false,
                        CompositeState.builder()
                                .setShaderState(
                                        GLOW_SHADER
                                )
                                .setTextureState(
                                        GLOW_TEXTURES
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
                                        ADDITIVE
                                )
                                .setTexturingState(
                                        texturing
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
