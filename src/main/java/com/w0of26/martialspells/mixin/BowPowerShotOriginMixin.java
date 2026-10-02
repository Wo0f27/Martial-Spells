package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.combat.RangerArrowMetadata;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Marks the final ammo-created bow arrow, after bow customization and before spawn. */
@Mixin(BowItem.class)
public abstract class BowPowerShotOriginMixin {
    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/item/BowItem;customArrow(Lnet/minecraft/world/entity/projectile/AbstractArrow;)Lnet/minecraft/world/entity/projectile/AbstractArrow;"))
    private AbstractArrow martialSpells$markBowShot(BowItem bow, AbstractArrow original) {
        AbstractArrow arrow = bow.customArrow(original);
        if (!arrow.level().isClientSide && arrow.getOwner() instanceof Player) {
            RangerArrowMetadata.markEligibleShot(arrow);
        }
        return arrow;
    }
}
