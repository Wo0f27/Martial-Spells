package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.combat.PowerShotImpact;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The redirect wraps only the actual vanilla arrow damage invocation. */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowPowerShotMixin {
    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean martialSpells$powerShotHurt(Entity target, DamageSource source, float amount) {
        return PowerShotImpact.hurt((AbstractArrow) (Object) this, target, source, amount);
    }
}
