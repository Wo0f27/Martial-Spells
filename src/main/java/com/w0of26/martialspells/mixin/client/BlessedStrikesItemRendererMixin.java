package com.w0of26.martialspells.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.client.render.BlessedStrikesItemGlow;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mirrors the 1.20.1 Spell Engine item-glow integration used by Paladins.
 */
@Mixin(ItemRenderer.class)
public abstract class BlessedStrikesItemRendererMixin {
    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("HEAD")
    )
    private void martialSpells$beginBlessedStrikesGlow(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext context,
            boolean leftHanded,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Level level,
            int packedLight,
            int packedOverlay,
            int seed,
            CallbackInfo ci
    ) {
        BlessedStrikesItemGlow.begin(
                entity,
                stack
        );
    }

    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("RETURN")
    )
    private void martialSpells$endBlessedStrikesGlow(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext context,
            boolean leftHanded,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Level level,
            int packedLight,
            int packedOverlay,
            int seed,
            CallbackInfo ci
    ) {
        BlessedStrikesItemGlow.end();
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private int martialSpells$raiseBlessedStrikesLight(
            int packedLight
    ) {
        return BlessedStrikesItemGlow.light(
                packedLight
        );
    }

    @Inject(
            method = "getFoilBuffer",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void martialSpells$addBlessedStrikesGlow(
            MultiBufferSource buffers,
            RenderType renderType,
            boolean isItem,
            boolean hasFoil,
            CallbackInfoReturnable<VertexConsumer> cir
    ) {
        cir.setReturnValue(
                BlessedStrikesItemGlow.glowing(
                        buffers,
                        cir.getReturnValue()
                )
        );
    }

    @Inject(
            method = "getFoilBufferDirect",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void martialSpells$addBlessedStrikesGlowDirect(
            MultiBufferSource buffers,
            RenderType renderType,
            boolean noEntity,
            boolean hasFoil,
            CallbackInfoReturnable<VertexConsumer> cir
    ) {
        cir.setReturnValue(
                BlessedStrikesItemGlow.glowing(
                        buffers,
                        cir.getReturnValue()
                )
        );
    }
}
