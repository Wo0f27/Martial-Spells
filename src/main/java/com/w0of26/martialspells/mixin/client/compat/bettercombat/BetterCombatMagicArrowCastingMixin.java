package com.w0of26.martialspells.mixin.client.compat.bettercombat;

import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.bettercombat.forge.PlatformImpl", remap = false)
public abstract class BetterCombatMagicArrowCastingMixin {
    @Inject(method = "isCastingSpell", at = @At("HEAD"), cancellable = true, remap = false)
    private static void martialSpells$reportRangedCast(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (MagicArrowClientVisuals.isRangedSpellCasting(player)) cir.setReturnValue(true);
    }
}
