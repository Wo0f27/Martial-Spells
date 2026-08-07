package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.effects.DiamondBodyEffect;
import com.w0of26.martialspells.effects.DiamondHeartEffect;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID
)
public final class DiamondBodyDamageEvents {

    private DiamondBodyDamageEvents() {
    }

    /*
     * Cancel attacks during Diamond Heart's first
     * two seconds before armor or absorption is consumed.
     */
    @SubscribeEvent(
            priority = EventPriority.HIGHEST
    )
    public static void blockDamageDuringDiamondHeart(
            LivingAttackEvent event
    ) {
        LivingEntity entity =
                event.getEntity();

        if (entity.level().isClientSide) {
            return;
        }

        var diamondHeart =
                entity.getEffect(
                        MartialEffectRegistry
                                .DIAMOND_HEART
                                .get()
                );

        if (DiamondHeartEffect.hasImmunity(
                diamondHeart
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST
    )
    public static void reduceDamage(
            LivingDamageEvent event
    ) {
        LivingEntity entity =
                event.getEntity();

        if (entity.level().isClientSide) {
            return;
        }

        if (event.getAmount() <= 0.0F) {
            return;
        }

        var diamondBody =
                entity.getEffect(
                        MartialEffectRegistry
                                .DIAMOND_BODY
                                .get()
                );

        if (diamondBody == null) {
            return;
        }

        float multiplier =
                DiamondBodyEffect
                        .getDamageMultiplier(
                                diamondBody
                                        .getAmplifier()
                        );

        float reducedDamage =
                event.getAmount()
                        * multiplier;

        /*
         * The hit is not lethal after Diamond Body's
         * normal damage reduction.
         */
        if (reducedDamage
                < entity.getHealth()) {

            event.setAmount(
                    reducedDamage
            );

            return;
        }

        triggerDiamondHeart(
                entity,
                event
        );
    }

    private static void triggerDiamondHeart(
            LivingEntity entity,
            LivingDamageEvent event
    ) {
        /*
         * Preserve exactly one health point while still
         * allowing the hit to register as damage.
         */
        if (entity.getHealth() < 1.0F) {
            entity.setHealth(1.0F);
        }

        event.setAmount(
                Math.max(
                        0.0F,
                        entity.getHealth()
                                - 1.0F
                )
        );

        /*
         * Removing Diamond Body naturally guarantees that
         * Diamond Heart can trigger only once per cast.
         */
        entity.removeEffect(
                MartialEffectRegistry
                        .DIAMOND_BODY
                        .get()
        );

        entity.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry
                                .DIAMOND_HEART
                                .get(),
                        DiamondHeartEffect
                                .DURATION_TICKS,
                        0,
                        false,
                        false,
                        true
                )
        );
    }
}