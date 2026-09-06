package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.RangedTechniqueHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Restores the two projectile perks from the original Archers Barrage:
 * each arrow bypasses normal hurt i-frames and adds a small 0.5-strength
 * knockback impulse on a successful hit.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BarrageProjectileEvents {
    private static final double BARRAGE_KNOCKBACK = 0.5D;

    private BarrageProjectileEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(
            ProjectileImpactEvent event
    ) {
        if (!(event.getProjectile()
                instanceof AbstractArrow arrow)) {
            return;
        }

        if (arrow.level().isClientSide
                || !RangedTechniqueHelper.isBarrageArrow(arrow)) {
            return;
        }

        if (event.getRayTraceResult()
                instanceof EntityHitResult hitResult
                && hitResult.getEntity()
                instanceof LivingEntity target) {
            /*
             * Reset immediately before vanilla processes the impact so
             * all three Barrage arrows can damage the same target even
             * when they arrive inside the normal hurt-cooldown window.
             */
            target.invulnerableTime = 0;
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(
            LivingHurtEvent event
    ) {
        if (!(event.getSource().getDirectEntity()
                instanceof AbstractArrow arrow)) {
            return;
        }

        if (event.getEntity().level().isClientSide
                || !RangedTechniqueHelper.isBarrageArrow(arrow)
                || event.getAmount() <= 0.0F) {
            return;
        }

        /*
         * Vanilla arrow knockback scales its horizontal motion by 0.6.
         * Applying the old Barrage perk's 0.5 strength through the same
         * shape gives a 0.3 horizontal impulse plus the vanilla Y lift.
         * Punch, when present, is still handled by the arrow itself.
         */
        Vec3 horizontal =
                arrow.getDeltaMovement().multiply(
                        1.0D,
                        0.0D,
                        1.0D
                );

        if (horizontal.lengthSqr() <= 1.0E-7D) {
            return;
        }

        Vec3 impulse = horizontal
                .normalize()
                .scale(BARRAGE_KNOCKBACK * 0.6D);

        event.getEntity().push(
                impulse.x,
                0.1D,
                impulse.z
        );
    }
}
