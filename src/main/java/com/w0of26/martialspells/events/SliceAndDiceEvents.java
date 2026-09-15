package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.spells.SliceAndDiceSpell;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Converts successful vanilla/Better Combat player melee impacts into
 * Slice & Dice stacks after that hit's damage has already been calculated.
 */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID)
public final class SliceAndDiceEvents {
    private SliceAndDiceEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onMeleeDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }

        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        MobEffectInstance current = attacker.getEffect(MartialEffectRegistry.SLICE_AND_DICE.get());
        if (current == null || current.getAmplifier() >= SliceAndDiceSpell.MAX_AMPLIFIER) {
            return;
        }

        int remainingDuration = current.getDuration();
        int nextAmplifier = current.getAmplifier() + 1;

        attacker.addEffect(new MobEffectInstance(
                MartialEffectRegistry.SLICE_AND_DICE.get(),
                remainingDuration,
                nextAmplifier,
                false,
                true,
                true
        ));
    }
}
