package com.w0of26.martialspells.client.render;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Client-side state for the Blessed Strikes held-item presentation.
 *
 * <p>The stable Forge 1.20.1 presentation uses Minecraft's own item glint
 * render types. This class only answers whether the currently rendered held
 * item is blessed and preserves the source stack count for light intensity.
 * It intentionally does not own a custom RenderType or shader.</p>
 */
public final class BlessedStrikesItemGlow {
    private static final float OPACITY_PER_STACK =
            0.20F;
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

    public static boolean active() {
        return currentOpacity > 0.0F;
    }

    public static int light(
            int packedLight
    ) {
        if (!active()) {
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
