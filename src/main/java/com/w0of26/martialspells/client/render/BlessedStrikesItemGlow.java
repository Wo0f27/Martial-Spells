package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

/**
 * Source-faithful Blessed Strikes held-item glow.
 *
 * <p>Frozen Paladins registers Blessed Strikes through Spell Engine's
 * GlowingItemStatusEffect with Holy #FFFFCC and 0.2 opacity per stack.
 * The source item glow uses a grayscale streak texture, additive blending,
 * scale 8 and gain 3. CP11 recreates that rendering locally so Spell Engine
 * is not a runtime dependency.</p>
 */
public final class BlessedStrikesItemGlow {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "textures/misc/paladin_item_glow.png"
            );

    private static final float OPACITY_PER_STACK = 0.20F;
    private static final float GAIN = 3.0F;
    private static final float TEXTURE_SCALE = 8.0F;
    private static final int MAX_STACKS = 5;

    private static final RenderType[] GLOW_LAYERS =
            new RenderType[MAX_STACKS];

    private static float currentOpacity;
    private static final float[] shaderColorRestore =
            new float[4];
    private static float glintAlphaRestore = 1.0F;

    private BlessedStrikesItemGlow() {
    }

    public static void begin(
            @Nullable LivingEntity holder,
            ItemStack stack
    ) {
        currentOpacity = 0.0F;

        if (holder == null
                || stack.isEmpty()
                || !isHeldEquipment(stack)) {
            return;
        }

        MobEffectInstance effect =
                holder.getEffect(
                        MartialEffectRegistry
                                .BLESSED_STRIKES
                                .get()
                );

        if (effect == null) {
            return;
        }

        currentOpacity =
                Math.min(
                        1.0F,
                        OPACITY_PER_STACK
                                * (effect.getAmplifier() + 1)
                );
    }

    public static void end() {
        currentOpacity = 0.0F;
    }

    public static int light(
            int packedLight
    ) {
        if (currentOpacity <= 0.0F) {
            return packedLight;
        }

        return LightTexture.pack(
                Math.max(
                        LightTexture.block(packedLight),
                        Math.round(
                                15.0F * currentOpacity
                        )
                ),
                LightTexture.sky(packedLight)
        );
    }

    public static VertexConsumer glowing(
            MultiBufferSource buffers,
            VertexConsumer original
    ) {
        if (currentOpacity <= 0.0F) {
            return original;
        }

        int stackIndex =
                Math.min(
                        MAX_STACKS - 1,
                        Math.max(
                                0,
                                Math.round(
                                        currentOpacity
                                                / OPACITY_PER_STACK
                                ) - 1
                        )
                );

        return VertexMultiConsumer.create(
                buffers.getBuffer(
                        layer(stackIndex)
                ),
                original
        );
    }

    private static boolean isHeldEquipment(
            ItemStack stack
    ) {
        return !stack.getAttributeModifiers(
                        EquipmentSlot.MAINHAND
                ).isEmpty()
                || !stack.getAttributeModifiers(
                        EquipmentSlot.OFFHAND
                ).isEmpty();
    }

    private static RenderType layer(
            int stackIndex
    ) {
        RenderType cached =
                GLOW_LAYERS[stackIndex];
        if (cached != null) {
            return cached;
        }

        float opacity =
                OPACITY_PER_STACK
                        * (stackIndex + 1);

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
                        BlessedStrikesItemGlow::clearTexturing
                );

        RenderType created =
                RenderType.create(
                        "martial_spells_blessed_strikes_glow_"
                                + (stackIndex + 1),
                        DefaultVertexFormat.POSITION_TEX,
                        VertexFormat.Mode.QUADS,
                        1536,
                        false,
                        false,
                        RenderType.CompositeState.builder()
                                .setShaderState(
                                        RenderStateShard
                                                .RENDERTYPE_GLINT_SHADER
                                )
                                .setTextureState(
                                        new RenderStateShard.TextureStateShard(
                                                TEXTURE,
                                                true,
                                                false
                                        )
                                )
                                .setWriteMaskState(
                                        RenderStateShard.COLOR_WRITE
                                )
                                .setCullState(
                                        RenderStateShard.NO_CULL
                                )
                                .setDepthTestState(
                                        RenderStateShard.EQUAL_DEPTH_TEST
                                )
                                .setTransparencyState(additive)
                                .setTexturingState(texturing)
                                .setOutputState(
                                        RenderStateShard.ITEM_ENTITY_TARGET
                                )
                                .createCompositeState(false)
                );

        GLOW_LAYERS[stackIndex] = created;
        return created;
    }

    private static void setupTexturing(
            float opacity
    ) {
        float[] current =
                RenderSystem.getShaderColor();
        System.arraycopy(
                current,
                0,
                shaderColorRestore,
                0,
                4
        );
        glintAlphaRestore =
                RenderSystem.getShaderGlintAlpha();

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

        Matrix4f textureMatrix =
                new Matrix4f()
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

        RenderSystem.setTextureMatrix(
                textureMatrix
        );

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
                shaderColorRestore[0],
                shaderColorRestore[1],
                shaderColorRestore[2],
                shaderColorRestore[3]
        );
        RenderSystem.setShaderGlintAlpha(
                glintAlphaRestore
        );
    }
}
