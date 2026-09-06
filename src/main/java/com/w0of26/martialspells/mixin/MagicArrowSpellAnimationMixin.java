package com.w0of26.martialspells.mixin;

import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.spells.ender.MagicArrowSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents Iron's generic PlayerAnimator bow-charge animation from
 * competing with the held weapon's native bow/crossbow preparation
 * presentation supplied by Martial Spells' client render hooks.
 */
@Mixin(
        value = MagicArrowSpell.class,
        remap = false
)
public abstract class MagicArrowSpellAnimationMixin {

    @Inject(
            method = "getCastStartAnimation",
            at = @At("HEAD"),
            cancellable = true
    )
    private void martialSpells$replaceMagicArrowCastAnimation(
            CallbackInfoReturnable<AnimationHolder> cir
    ) {
        cir.setReturnValue(AnimationHolder.none());
    }
}
