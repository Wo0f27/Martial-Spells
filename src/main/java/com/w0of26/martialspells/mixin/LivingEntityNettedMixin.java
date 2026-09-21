package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spell Engine ROOT semantics for Throw Net: movement and jumping are blocked,
 * while attacks, item use, and spell casting remain available.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityNettedMixin {
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 martialSpells$nettedBlocksTravelInput(Vec3 travelVector) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.hasEffect(MartialEffectRegistry.NET_TRAP.get())) {
            return travelVector;
        }
        return new Vec3(0.0D, travelVector.y, 0.0D);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void martialSpells$nettedBlocksJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(MartialEffectRegistry.NET_TRAP.get())) {
            ci.cancel();
        }
    }
}
