package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.api.AbilityCompatibility;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Narrow additive adjustments after Iron's ordinary cooldown reduction. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbilityCompatibilityCooldownEvents {
    private AbilityCompatibilityCooldownEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCooldown(SpellCooldownAddedEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        int adjustment;
        if (event.getSpell() == MartialSpellRegistry.DIVINE_PROTECTION.get()) {
            adjustment = AbilityCompatibility.getDivineProtectionCooldownAdjustment(player);
        } else if (event.getSpell() == MartialSpellRegistry.JUDGEMENT.get()) {
            adjustment = AbilityCompatibility.getJudgementCooldownAdjustment(player);
        } else if (event.getSpell() == MartialSpellRegistry.CHARGE.get()) {
            adjustment = AbilityCompatibility.getChargeCooldownAdjustment(player);
        } else if (event.getSpell() == MartialSpellRegistry.SHADOW_STEP.get()) {
            adjustment = AbilityCompatibility.getShadowStepCooldownAdjustment(player);
        } else {
            return;
        }
        if (adjustment != 0) {
            event.setEffectiveCooldown(Math.max(0, event.getEffectiveCooldown() + adjustment));
        }
    }
}
