package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Source-faithful Blessed Strikes held-item glow state.
 *
 * <p>Frozen Paladins resolves Holy #FFFFCC glow at 0.2 opacity per stored
 * blessing, capped at full intensity at five stacks. Render-layer construction
 * lives in BlessedStrikesGlowRenderTypes so Forge 1.20.1's protected vanilla
 * render-state constants are accessed legally.</p>
 */
public final class BlessedStrikesItemGlow {
    private static final float OPACITY_PER_STACK =
            0.20F;
    private static final int MAX_STACKS =
            5;

    private static int currentStackIndex = -1;
    private static float currentOpacity;

    private BlessedStrikesItemGlow() {
    }

    public static void begin(
            @Nullable LivingEntity holder,
            ItemStack stack
    ) {
        currentStackIndex = -1;
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

        currentStackIndex =
                stacks - 1;
        currentOpacity =
                Math.min(
                        1.0F,
                        OPACITY_PER_STACK
                                * stacks
                );
    }

    public static void end() {
        currentStackIndex = -1;
        currentOpacity = 0.0F;
    }

    /**
     * Same source behavior as Spell Engine ItemGlowRendering.light: increase
     * only block light, proportionally to effect opacity, and preserve skylight.
     */
    public static int light(
            int packedLight
    ) {
        if (currentStackIndex < 0) {
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

    /**
     * Add the source glow consumer beside the normal item consumer. The fixed
     * buffer ordering mixin ensures the EQUAL-depth glow flushes after the item.
     */
    public static VertexConsumer glowing(
            MultiBufferSource buffers,
            VertexConsumer original
    ) {
        if (currentStackIndex < 0) {
            return original;
        }

        return VertexMultiConsumer.create(
                buffers.getBuffer(
                        BlessedStrikesGlowRenderTypes
                                .itemGlow(
                                        currentStackIndex
                                )
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
}
