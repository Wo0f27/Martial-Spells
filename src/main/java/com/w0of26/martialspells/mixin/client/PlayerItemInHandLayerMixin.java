package com.w0of26.martialspells.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.w0of26.martialspells.client.visual.ClientFlurryVisuals;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemInHandLayer.class)
public abstract class PlayerItemInHandLayerMixin {
    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void martialSpells$hideStowedMainHand(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        if (!(entity instanceof Player player)) {
            return;
        }

        /*
         * The arm corresponding to getMainArm() carries the
         * player's main-hand item.
         */
        if (arm != player.getMainArm()) {
            return;
        }

        if (ClientFlurryVisuals
                .shouldStowWeapon(
                        player.getUUID()
                )) {
            callbackInfo.cancel();
        }
    }
}