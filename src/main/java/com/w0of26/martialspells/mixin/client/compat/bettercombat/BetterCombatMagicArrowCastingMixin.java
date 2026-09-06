package com.w0of26.martialspells.mixin.client.compat.bettercombat;

import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Better Combat 1.20.1 already has a "casting spell" escape hatch in its
 * player-pose controller, but the Forge implementation returns false for
 * every player. That leaves Better Combat's two-handed bow/crossbow idle
 * pose active while Iron's Magic Arrow is channeling.
 *
 * Report only Martial Spells' Magic Arrow cast as a casting activity here.
 * Better Combat then follows its own normal special-activity path and fades
 * its weapon pose out, allowing the vanilla-style bow draw/crossbow charge
 * pose supplied by Martial Spells to become visible.
 *
 * This is client-only, optional Better Combat compatibility. It does not
 * alter weapon attributes, item-use state, projectiles, or server gameplay.
 */
@Pseudo
@Mixin(
        targets = "net.bettercombat.forge.PlatformImpl",
        remap = false
)
public abstract class BetterCombatMagicArrowCastingMixin {

    @Inject(
            method = "isCastingSpell",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void martialSpells$reportMagicArrowCast(
            Player player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (MagicArrowClientVisuals.isMagicArrowCasting(player)) {
            cir.setReturnValue(true);
        }
    }
}
