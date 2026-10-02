package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Spell Engine's 0.5 arrow perk halves damage-time knockback, not integer Punch. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BarrageKnockbackScope {
    private static final ThreadLocal<LivingEntity> ACTIVE_TARGET = new ThreadLocal<>();

    private BarrageKnockbackScope() {}

    static LivingEntity enter(LivingEntity target) {
        LivingEntity previous = ACTIVE_TARGET.get();
        ACTIVE_TARGET.set(target);
        return previous;
    }

    static void exit(LivingEntity previous) {
        if (previous == null) ACTIVE_TARGET.remove();
        else ACTIVE_TARGET.set(previous);
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() == ACTIVE_TARGET.get()) {
            event.setStrength(event.getStrength() * 0.5F);
        }
    }
}
