package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.spells.PaladinBlessedStrikesSpell;
import com.w0of26.martialspells.spells.PaladinVfx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rebuilds Spell Engine's Blessed Strikes stash trigger.
 *
 * Every successful player melee target hit in the same tick receives the
 * bonus Holy impact. Seal consumption is deferred until the end of the
 * player tick, so a Better Combat cleave spends only one seal.
 */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID)
public final class BlessedStrikesEvents {
    private static final String PENDING_CONSUME_TAG =
            MartialSpells.MOD_ID
                    + "_blessed_strikes_pending_consume";

    private BlessedStrikesEvents() {
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST,
            receiveCanceled = false
    )
    public static void onMeleeDamage(
            LivingDamageEvent event
    ) {
        if (event.getEntity().level().isClientSide
                || event.getAmount() <= 0.0F) {
            return;
        }

        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getEntity()
                instanceof Player attacker)) {
            return;
        }

        MobEffectInstance seals =
                attacker.getEffect(
                        MartialEffectRegistry.BLESSED_STRIKES.get()
                );

        if (seals == null) {
            return;
        }

        if (!(MartialSpellRegistry.BLESSED_STRIKES.get()
                instanceof PaladinBlessedStrikesSpell spell)) {
            return;
        }

        LivingEntity target =
                event.getEntity();

        float bonusDamage =
                spell.getBonusDamage(attacker);

        if (bonusDamage > 0.0F
                && DamageSources.applyDamage(
                        target,
                        bonusDamage,
                        spell.getDamageSource(attacker)
                )) {
            target.knockback(
                    PaladinBlessedStrikesSpell.KNOCKBACK_STRENGTH,
                    attacker.getX() - target.getX(),
                    attacker.getZ() - target.getZ()
            );

            if (target.level() instanceof ServerLevel serverLevel) {
                PaladinVfx.holyBurst(
                        serverLevel,
                        target,
                        30,
                        0.70D
                );

                serverLevel.playSound(
                        null,
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        MartialSoundRegistry.HOLY_SHOCK_DAMAGE.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
        }

        attacker.getPersistentData().putBoolean(
                PENDING_CONSUME_TAG,
                true
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide) {
            return;
        }

        Player player =
                event.player;

        if (!player.getPersistentData().getBoolean(
                PENDING_CONSUME_TAG
        )) {
            return;
        }

        player.getPersistentData().remove(
                PENDING_CONSUME_TAG
        );

        MobEffectInstance current =
                player.getEffect(
                        MartialEffectRegistry.BLESSED_STRIKES.get()
                );

        if (current == null) {
            return;
        }

        int remainingDuration =
                current.getDuration();

        int currentAmplifier =
                current.getAmplifier();

        player.removeEffect(
                MartialEffectRegistry.BLESSED_STRIKES.get()
        );

        if (currentAmplifier <= 0) {
            return;
        }

        player.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry.BLESSED_STRIKES.get(),
                        remainingDuration,
                        currentAmplifier - 1,
                        false,
                        true,
                        true
                )
        );
    }
}
