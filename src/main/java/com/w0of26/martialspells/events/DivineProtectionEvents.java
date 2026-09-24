package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.spells.PaladinVfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Source-faithful Divine Protection pop behavior.
 *
 * Each incoming attack is fully negated and consumes one effect stack.
 */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID)
public final class DivineProtectionEvents {
    private DivineProtectionEvents() {
    }

    @SubscribeEvent(
            priority = EventPriority.HIGHEST,
            receiveCanceled = false
    )
    public static void onLivingAttack(
            LivingAttackEvent event
    ) {
        LivingEntity target =
                event.getEntity();

        if (target.level().isClientSide) {
            return;
        }

        MobEffectInstance protection =
                target.getEffect(
                        MartialEffectRegistry.DIVINE_PROTECTION.get()
                );

        if (protection == null) {
            return;
        }

        event.setCanceled(true);

        int remainingDuration =
                protection.getDuration();

        int amplifier =
                protection.getAmplifier();

        target.removeEffect(
                MartialEffectRegistry.DIVINE_PROTECTION.get()
        );

        if (amplifier > 0) {
            target.addEffect(
                    new MobEffectInstance(
                            MartialEffectRegistry.DIVINE_PROTECTION.get(),
                            remainingDuration,
                            amplifier - 1,
                            false,
                            true,
                            true
                    )
            );
        }

        if (target.level() instanceof ServerLevel serverLevel) {
            PaladinVfx.divineProtectionPop(
                    serverLevel,
                    target
            );

            serverLevel.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    MartialSoundRegistry.DIVINE_PROTECTION_IMPACT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }
}
