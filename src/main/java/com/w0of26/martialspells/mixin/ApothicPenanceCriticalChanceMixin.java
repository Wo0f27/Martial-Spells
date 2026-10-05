package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.api.AbilityCompatibility;
import com.w0of26.martialspells.combat.PenanceCritContext;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import dev.shadowsoffire.attributeslib.impl.AttributeEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pinned to Apothic Attributes 1.20.1-1.3.7: ordinal 0 is CRIT_CHANCE, not CRIT_DAMAGE. */
@Mixin(value = AttributeEvents.class, remap = false)
public abstract class ApothicPenanceCriticalChanceMixin {
    @Redirect(
            method = "apothCriticalStrike(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D",
                    ordinal = 0, remap = true),
            remap = false, require = 1, expect = 1, allow = 1)
    private double martialSpells$penanceChance(LivingEntity attacker, Attribute attribute,
                                               LivingHurtEvent event) {
        if (attribute != ALObjects.Attributes.CRIT_CHANCE.get()) {
            throw new IllegalStateException("Pinned Apothic first attribute read is no longer CRIT_CHANCE");
        }
        double base = attacker.getAttributeValue(attribute);
        if (attacker instanceof Player player
                && PenanceCritContext.matches(event.getSource(), event.getEntity(), attacker)) {
            return base + AbilityCompatibility.getPenanceCriticalChanceAdjustment(player);
        }
        return base;
    }
}
