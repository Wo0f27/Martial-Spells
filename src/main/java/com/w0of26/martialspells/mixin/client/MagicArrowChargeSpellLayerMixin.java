package com.w0of26.martialspells.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import io.redspace.ironsspellbooks.render.ChargeSpellLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iron's ChargeSpellLayer normally renders a full Magic Arrow model in
 * the caster's hand for the entire channel. Ranger-style Magic Arrow
 * intentionally uses only weapon preparation plus Ender particles, so
 * suppress that preview for player casts while leaving mob rendering and
 * the real released projectile untouched.
 */
@Mixin(
        value = ChargeSpellLayer.Vanilla.class,
        remap = false
)
public abstract class MagicArrowChargeSpellLayerMixin {

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void martialSpells$hideMagicArrowChargePreview(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (entity instanceof Player
                && MagicArrowClientVisuals.isMagicArrowCasting(entity)) {
            ci.cancel();
        }
    }
}
