package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Frozen Rogues Shadowstep behavior: while a mob's current target carries the
 * short Shadowstep marker, that target goal can only follow from five blocks.
 */
@Mixin(TargetGoal.class)
public abstract class TargetGoalShadowstepMixin {
    @Shadow
    protected Mob mob;

    @Inject(method = "getFollowDistance", at = @At("HEAD"), cancellable = true)
    private void martialSpells$shadowstepFollowDistance(CallbackInfoReturnable<Double> cir) {
        var target = mob.getTarget();
        if (target != null && target.hasEffect(MartialEffectRegistry.SHADOW_STEP.get())) {
            cir.setReturnValue(5.0D);
        }
    }
}
