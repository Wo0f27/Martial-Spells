package com.w0of26.martialspells.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.client.render.BlessedStrikesGlowRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Gives Blessed Strikes glow layers dedicated fixed buffers so EQUAL depth
 * testing is flushed after the held item, matching Spell Engine's 1.20.1
 * ImmediateItemGlowMixin ordering.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class BlessedStrikesGlowBufferSourceMixin {
    @Shadow
    @Final
    protected Map<RenderType, BufferBuilder> fixedBuffers;

    @Inject(
            method = "getBuffer",
            at = @At("HEAD")
    )
    private void martialSpells$bufferBlessedStrikesGlow(
            RenderType renderType,
            CallbackInfoReturnable<VertexConsumer> cir
    ) {
        if (fixedBuffers.isEmpty()
                || fixedBuffers.containsKey(renderType)
                || !BlessedStrikesGlowRenderTypes
                .isGlowLayer(renderType)) {
            return;
        }

        fixedBuffers.put(
                renderType,
                new BufferBuilder(
                        renderType.bufferSize()
                )
        );
    }
}
