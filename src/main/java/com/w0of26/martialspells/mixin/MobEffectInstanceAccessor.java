package com.w0of26.martialspells.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {

    @Accessor("duration")
    void martialSpells$setDuration(
            int duration
    );
}