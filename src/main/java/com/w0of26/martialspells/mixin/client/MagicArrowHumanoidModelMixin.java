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
 * Replays vanilla's crossbow-charge arm math using Magic Arrow cast
 * progress instead of real item-use ticks.
 */
@Mixin(HumanoidModel.class)
public abstract class MagicArrowHumanoidModelMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void martialSpells$magicArrowCrossbowCharge(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)
                || !MagicArrowClientVisuals.isMagicArrowCasting(player)
                || MagicArrowClientVisuals.getSelectedRangedType(player)
                != RangedWeaponClassifier.Type.CROSSBOW) {
            return;
        }

        HumanoidArm arm = MagicArrowClientVisuals.getSelectedRangedArm(player);
        if (arm == null) {
            return;
        }

        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart holdingArm = right ? rightArm : leftArm;
        ModelPart pullingArm = right ? leftArm : rightArm;

        holdingArm.yRot = right ? -0.8F : 0.8F;
        holdingArm.xRot = -0.97079635F;
        pullingArm.xRot = holdingArm.xRot;

        float progress = MagicArrowClientVisuals.getCastProgress(player);
        pullingArm.yRot = Mth.lerp(progress, 0.4F, 0.85F)
                * (right ? 1.0F : -1.0F);
        pullingArm.xRot = Mth.lerp(
                progress,
                pullingArm.xRot,
                -((float) Math.PI / 2.0F)
        );

        AnimationUtils.bobModelPart(rightArm, ageInTicks, 1.0F);
        AnimationUtils.bobModelPart(leftArm, ageInTicks, -1.0F);
    }
}
