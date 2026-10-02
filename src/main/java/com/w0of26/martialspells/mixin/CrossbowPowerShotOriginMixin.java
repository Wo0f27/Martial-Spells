package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.combat.RangerArrowMetadata;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Each vanilla crossbow multishot call creates and marks its own arrow. */
@Mixin(CrossbowItem.class)
public abstract class CrossbowPowerShotOriginMixin {
    @Redirect(method = "getArrow", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ArrowItem;createArrow(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/entity/projectile/AbstractArrow;"))
    private static AbstractArrow martialSpells$markCrossbowShot(ArrowItem item, Level level,
                                                                 ItemStack ammunition, LivingEntity shooter) {
        AbstractArrow arrow = item.createArrow(level, ammunition, shooter);
        if (!level.isClientSide && shooter instanceof Player) {
            RangerArrowMetadata.markEligibleShot(arrow);
        }
        return arrow;
    }
}
