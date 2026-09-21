package com.w0of26.martialspells.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.w0of26.martialspells.client.render.NettedEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Source-equivalent hook for persistent Netted model FX.
 *
 * <p>Spell Engine renders CustomModelStatusEffect entries from the tail of
 * LivingEntityRenderer.render. W4 uses the same integration point so the
 * frozen net-trap model is rendered in the same entity-space pose stack.</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityNettedRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL")
    )
    private void martialSpells$renderNetted(
            LivingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        NettedEffectRenderer.render(
                entity,
                partialTick,
                poseStack,
                bufferSource,
                packedLight
        );
    }
}
