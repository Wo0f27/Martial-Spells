package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.BarrageArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BarrageProjectileEvents {
    private BarrageProjectileEvents() {}

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof BarrageArrow arrow) || arrow.level().isClientSide) return;
        if (event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target) {
            target.invulnerableTime = 0;
        }
    }
}
