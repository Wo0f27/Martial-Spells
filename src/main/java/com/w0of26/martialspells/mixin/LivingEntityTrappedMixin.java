package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Matches Spell Engine ROOT: movement and jumping are blocked, actions are not. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTrappedMixin {
    @Inject(method = "isImmobile", at = @At("HEAD"), cancellable = true)
    private void martialSpells$bearTrapImmobile(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(MartialEffectRegistry.BEAR_TRAP.get())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void martialSpells$bearTrapBlocksJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(MartialEffectRegistry.BEAR_TRAP.get())) {
            ci.cancel();
        }
    }
}
