package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

/**
 * Blessed Strikes held-item glow state.
 *
 * <p>Source semantics retained: Holy #FFFFCC, opacity 0.2 per blessing,
 * maximum five blessings, source glow texture scale 8 and gain 3.</p>
 */
public final class BlessedStrikesItemGlow {
    private static final float OPACITY_PER_STACK =
            0.20F;
    private static final float TEXTURE_SCALE =
            8.0F;
    private static final int MAX_STACKS =
            5;

    private static float currentOpacity;

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

        int stacks =
                Math.max(
                        1,
                        Math.min(
                                MAX_STACKS,
                                effect.getAmplifier() + 1
                        )
                );

        currentOpacity =
                Math.min(
                        1.0F,
                        OPACITY_PER_STACK
                                * stacks
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
                        LightTexture.block(
                                packedLight
                        ),
                        Math.round(
                                15.0F
                                        * currentOpacity
                        )
                ),
                LightTexture.sky(
                        packedLight
                )
        );
    }

    public static VertexConsumer glowing(
            MultiBufferSource buffers,
            VertexConsumer original
    ) {
        if (currentOpacity <= 0.0F) {
            return original;
        }

        VertexConsumer glow =
                new BlessedStrikesGlowVertexConsumer(
                        buffers.getBuffer(
                                BlessedStrikesGlowRenderTypes
                                        .itemGlow()
                        ),
                        currentOpacity
                );

        return VertexMultiConsumer.create(
                glow,
                original
        );
    }

    public static Matrix4f textureMatrix() {
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
}
