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

@Mixin(PlayerRenderer.class)
public abstract class MagicArrowPlayerRendererMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void martialSpells$rangedNativePose(AbstractClientPlayer player, InteractionHand hand,
                                                        CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (!MagicArrowClientVisuals.isRangedSpellCasting(player)) return;
        InteractionHand selected = MagicArrowClientVisuals.getSelectedRangedHand(player);
        if (selected != hand) return;

        RangedWeaponClassifier.Type type = MagicArrowClientVisuals.getSelectedRangedType(player);
        if (type == RangedWeaponClassifier.Type.BOW) {
            cir.setReturnValue(HumanoidModel.ArmPose.BOW_AND_ARROW);
        } else if (type == RangedWeaponClassifier.Type.CROSSBOW) {
            cir.setReturnValue(MagicArrowClientVisuals.isCrossbowReady(player)
                    ? HumanoidModel.ArmPose.CROSSBOW_HOLD
                    : HumanoidModel.ArmPose.CROSSBOW_CHARGE);
        }
    }
}
