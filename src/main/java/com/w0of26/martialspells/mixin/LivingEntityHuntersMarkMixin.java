package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scales the incoming amount before Forge's LivingAttack hook, iframes and defenses. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHuntersMarkMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float martialSpells$huntersMarkIncomingDamage(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return amount;
        MobEffectInstance mark = self.getEffect(MartialEffectRegistry.HUNTERS_MARK.get());
        return mark == null ? amount : amount * (1.0F + 0.1F * (mark.getAmplifier() + 1));
    }
}
