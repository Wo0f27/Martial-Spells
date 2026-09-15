package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes Martial Stealth participate in vanilla's synchronized invisibility
 * state without depending on Spell Engine's visibility mixin.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityStealthMixin {
    @Inject(method = "updateInvisibilityStatus", at = @At("TAIL"))
    private void martialSpells$keepStealthInvisible(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.hasEffect(MartialEffectRegistry.STEALTH.get())) {
            entity.setInvisible(true);
        }
    }
}
