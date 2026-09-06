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

@Mixin(HumanoidModel.class)
public abstract class MagicArrowHumanoidModelMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void martialSpells$rangedPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                           float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player) || !MagicArrowClientVisuals.isRangedSpellCasting(player)) return;
        RangedWeaponClassifier.Type type = MagicArrowClientVisuals.getSelectedRangedType(player);
        HumanoidArm arm = MagicArrowClientVisuals.getSelectedRangedArm(player);
        if (arm == null || type == RangedWeaponClassifier.Type.NONE) return;
        boolean right = arm == HumanoidArm.RIGHT;

        if (type == RangedWeaponClassifier.Type.BOW) {
            if (right) {
                rightArm.yRot = -0.1F + head.yRot;
                leftArm.yRot = 0.1F + head.yRot + 0.4F;
            } else {
                rightArm.yRot = -0.1F + head.yRot - 0.4F;
                leftArm.yRot = 0.1F + head.yRot;
            }
            rightArm.xRot = -((float)Math.PI / 2.0F) + head.xRot;
            leftArm.xRot = -((float)Math.PI / 2.0F) + head.xRot;
        } else if (MagicArrowClientVisuals.isCrossbowReady(player)) {
            AnimationUtils.animateCrossbowHold(rightArm, leftArm, head, right);
        } else {
            ModelPart holding = right ? rightArm : leftArm;
            ModelPart pulling = right ? leftArm : rightArm;
            float progress = MagicArrowClientVisuals.getCrossbowChargeProgress(player);
            holding.yRot = right ? -0.8F : 0.8F;
            holding.xRot = -0.97079635F;
            pulling.xRot = holding.xRot;
            pulling.yRot = Mth.lerp(progress, 0.4F, 0.85F) * (right ? 1.0F : -1.0F);
            pulling.xRot = Mth.lerp(progress, pulling.xRot, -((float)Math.PI / 2.0F));
        }

        AnimationUtils.bobModelPart(rightArm, ageInTicks, 1.0F);
        AnimationUtils.bobModelPart(leftArm, ageInTicks, -1.0F);
    }
}
