package com.w0of26.martialspells.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.w0of26.martialspells.client.render.BlessedStrikesItemGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
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
 * Gives Blessed Strikes the same proven Forge 1.20.1 render path used by an
 * ordinary enchanted item. No custom shader, custom glow quad, or custom
 * buffer ordering participates in this baseline.
 */
@Mixin(ItemRenderer.class)
public abstract class BlessedStrikesItemRendererMixin {
    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("HEAD")
    )
    private void martialSpells$beginBlessedStrikesGlint(
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
    private void martialSpells$endBlessedStrikesGlint(
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
    private static void martialSpells$forceBlessedStrikesGlint(
            MultiBufferSource buffers,
            RenderType renderType,
            boolean isItem,
            boolean hasFoil,
            CallbackInfoReturnable<VertexConsumer> cir
    ) {
        if (!BlessedStrikesItemGlow.active()
                || hasFoil) {
            return;
        }

        VertexConsumer glint;
        if (Minecraft.useShaderTransparency()
                && renderType
                == Sheets.translucentItemSheet()) {
            glint =
                    buffers.getBuffer(
                            RenderType.glintTranslucent()
                    );
        } else {
            glint =
                    buffers.getBuffer(
                            isItem
                                    ? RenderType.glint()
                                    : RenderType.entityGlint()
                    );
        }

        cir.setReturnValue(
                VertexMultiConsumer.create(
                        glint,
                        cir.getReturnValue()
                )
        );
    }

    @Inject(
            method = "getFoilBufferDirect",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void martialSpells$forceBlessedStrikesGlintDirect(
            MultiBufferSource buffers,
            RenderType renderType,
            boolean noEntity,
            boolean hasFoil,
            CallbackInfoReturnable<VertexConsumer> cir
    ) {
        if (!BlessedStrikesItemGlow.active()
                || hasFoil) {
            return;
        }

        VertexConsumer glint =
                buffers.getBuffer(
                        noEntity
                                ? RenderType.glintDirect()
                                : RenderType.entityGlintDirect()
                );

        cir.setReturnValue(
                VertexMultiConsumer.create(
                        glint,
                        cir.getReturnValue()
                )
        );
    }
}
