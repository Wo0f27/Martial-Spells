package com.w0of26.martialspells.mixin.client;

import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Magic Arrow's final ranged pose at the end of HumanoidModel
 * setup. This is intentionally later than the ordinary arm-pose pass so
 * Better Combat weapon poses (notably Archers longbows) cannot leave the
 * weapon in their idle two-handed pose while the spell is aiming.
 */
@Mixin(HumanoidModel.class)
public abstract class MagicArrowHumanoidModelMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void martialSpells$magicArrowRangedPose(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)
                || !MagicArrowClientVisuals.isMagicArrowCasting(player)) {
            return;
        }

        RangedWeaponClassifier.Type type =
                MagicArrowClientVisuals.getSelectedRangedType(player);
        HumanoidArm arm = MagicArrowClientVisuals.getSelectedRangedArm(player);

        if (arm == null || type == RangedWeaponClassifier.Type.NONE) {
            return;
        }

        boolean right = arm == HumanoidArm.RIGHT;

        if (type == RangedWeaponClassifier.Type.BOW) {
            applyBowAim(right);
        } else if (type == RangedWeaponClassifier.Type.CROSSBOW) {
            if (MagicArrowClientVisuals.isCrossbowReady(player)) {
                AnimationUtils.animateCrossbowHold(
                        rightArm,
                        leftArm,
                        head,
                        right
                );
            } else {
                applyCrossbowCharge(
                        right,
                        MagicArrowClientVisuals
                                .getCrossbowChargeProgress(player)
                );
            }
        }

        AnimationUtils.bobModelPart(rightArm, ageInTicks, 1.0F);
        AnimationUtils.bobModelPart(leftArm, ageInTicks, -1.0F);
    }

    /**
     * Exact vanilla BOW_AND_ARROW arm math, applied explicitly after
     * external weapon-pose animation layers have run.
     */
    private void applyBowAim(boolean right) {
        if (right) {
            rightArm.yRot = -0.1F + head.yRot;
            leftArm.yRot = 0.1F + head.yRot + 0.4F;
        } else {
            rightArm.yRot = -0.1F + head.yRot - 0.4F;
            leftArm.yRot = 0.1F + head.yRot;
        }

        rightArm.xRot = -((float) Math.PI / 2.0F) + head.xRot;
        leftArm.xRot = -((float) Math.PI / 2.0F) + head.xRot;
    }

    /**
     * Vanilla crossbow-charge arm math with spell progress substituted
     * for getTicksUsingItem(), because Magic Arrow deliberately never
     * enters real crossbow item-use state.
     */
    private void applyCrossbowCharge(boolean right, float progress) {
        ModelPart holdingArm = right ? rightArm : leftArm;
        ModelPart pullingArm = right ? leftArm : rightArm;

        holdingArm.yRot = right ? -0.8F : 0.8F;
        holdingArm.xRot = -0.97079635F;
        pullingArm.xRot = holdingArm.xRot;

        pullingArm.yRot = Mth.lerp(progress, 0.4F, 0.85F)
                * (right ? 1.0F : -1.0F);
        pullingArm.xRot = Mth.lerp(
                progress,
                pullingArm.xRot,
                -((float) Math.PI / 2.0F)
        );
    }
}
