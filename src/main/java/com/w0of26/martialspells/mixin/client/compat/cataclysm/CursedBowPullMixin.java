package com.w0of26.martialspells.mixin.client.compat.cataclysm;

import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soft render compatibility for Cataclysm's Cursed Bow. Its custom
 * renderer asks this static method for its draw amount instead of using
 * vanilla bow item predicates.
 */
@Pseudo
@Mixin(
        targets = "com.github.L_Ender.cataclysm.items.Cursed_bow",
        remap = false
)
public abstract class CursedBowPullMixin {
    @Inject(
            method = "getPullingAmount",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void martialSpells$magicArrowPullAmount(
            ItemStack stack,
            float partialTicks,
            CallbackInfoReturnable<Float> cir
    ) {
        Player holder =
                MagicArrowClientVisuals.findSelectedRangedStackHolder(stack);

        if (holder != null
                && RangedWeaponClassifier.isBow(stack)) {
            cir.setReturnValue(
                    MagicArrowClientVisuals.getCastProgress(holder)
            );
        }
    }
}
