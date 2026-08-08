package com.w0of26.martialspells.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.w0of26.martialspells.client.visual.ClientDeflectMissilesVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    private static final float
            DEFLECT_SPIN_DEGREES_PER_TICK =
            90.0F;

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/renderer/ItemInHandRenderer;"
                                    + "renderItem("
                                    + "Lnet/minecraft/world/entity/LivingEntity;"
                                    + "Lnet/minecraft/world/item/ItemStack;"
                                    + "Lnet/minecraft/world/item/ItemDisplayContext;"
                                    + "Z"
                                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                                    + "I)V"
            )
    )
    private void martialSpells$spinDeflectMissilesStaff(
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
         * Only alter the item while the player is actively
         * channeling Deflect Missiles with a quarterstaff.
         */
        if (!ClientDeflectMissilesVisuals
                .shouldSpinQuarterstaff(
                        player
                )) {
            return;
        }

        /*
         * Only alter the main-hand item.
         */
        if (arm != player.getMainArm()) {
            return;
        }

        /*
         * Smooth interpolation between game ticks.
         */
        float partialTick =
                Minecraft.getInstance()
                        .getFrameTime();

        float spinDegrees =
                (
                        player.tickCount
                                + partialTick
                )
                        * DEFLECT_SPIN_DEGREES_PER_TICK;

        /*
         * First rotate the quarterstaff into an upright stance,
         * then spin it in a vertical plane.
         */
        /*
         * Correct the quarterstaff's model orientation first.
         */
        /*
         * Spin around the original held-item Z axis first.
         * This preserves the outward-facing defensive spin plane.
         */
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        spinDegrees
                )
        );

        /*
         * Then correct the quarterstaff's own orientation
         * without changing the plane of the spin.
         */
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
                )
        );
    }
}