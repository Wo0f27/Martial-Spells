package com.w0of26.martialspells.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses vanilla's melee recharge timer for source-faithful Mutilate hits. */
@Mixin(LivingEntity.class)
public interface LivingEntityAttackStrengthAccessor {
    @Accessor("attackStrengthTicker")
    int martialSpells$getAttackStrengthTicker();

    @Accessor("attackStrengthTicker")
    void martialSpells$setAttackStrengthTicker(int value);
}
