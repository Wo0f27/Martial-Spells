package com.w0of26.martialspells.mixin.client;

import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets Magic Arrow use Minecraft's native bow/crossbow arm poses without
 * putting the player into real item-use state.
 */
@Mixin(PlayerRenderer.class)
public abstract class MagicArrowPlayerRendererMixin {

    @Inject(
            method = "getArmPose",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void martialSpells$magicArrowNativeRangedPose(
            AbstractClientPlayer player,
            InteractionHand hand,
            CallbackInfoReturnable<HumanoidModel.ArmPose> cir
    ) {
        if (!MagicArrowClientVisuals.isMagicArrowCasting(player)) {
            return;
        }

        InteractionHand selectedHand =
                MagicArrowClientVisuals.getSelectedRangedHand(player);
        if (selectedHand != hand) {
            return;
        }

        RangedWeaponClassifier.Type type =
                MagicArrowClientVisuals.getSelectedRangedType(player);

        if (type == RangedWeaponClassifier.Type.BOW) {
            cir.setReturnValue(HumanoidModel.ArmPose.BOW_AND_ARROW);
        } else if (type == RangedWeaponClassifier.Type.CROSSBOW) {
            cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_CHARGE);
        }
    }
}
