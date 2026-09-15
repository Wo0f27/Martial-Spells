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
 * Rogue anti-tracking behavior for the frozen Shadowstep and Vanish markers.
 */
@Mixin(TargetGoal.class)
public abstract class TargetGoalShadowstepMixin {
    @Shadow
    protected Mob mob;

    @Inject(method = "getFollowDistance", at = @At("HEAD"), cancellable = true)
    private void martialSpells$rogueFollowDistance(CallbackInfoReturnable<Double> cir) {
        var target = mob.getTarget();
        if (target == null) {
            return;
        }

        // Vanish source default: hostile target goals only follow a stealthed
        // target from one block away. Keep R3's already-validated 5-block
        // Shadowstep contract unchanged.
        if (target.hasEffect(MartialEffectRegistry.STEALTH.get())) {
            cir.setReturnValue(1.0D);
        } else if (target.hasEffect(MartialEffectRegistry.SHADOW_STEP.get())) {
            cir.setReturnValue(5.0D);
        }
    }
}
