package com.w0of26.martialspells.mixin.client;

import com.w0of26.martialspells.spells.AbstractMonkTechniqueSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents Iron's generic spell cooldown reduction from modifying
 * Monk technique cooldowns.
 *
 * Monk techniques use their configured cooldown as the final
 * authoritative cooldown.
 */
@Mixin(
        value = MagicManager.class,
        remap = false
)
public abstract class MagicManagerMixin {

    @Inject(
            method = "getEffectiveSpellCooldown",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void martialSpells$useFixedMonkCooldown(
            AbstractSpell spell,
            Player player,
            CastSource castSource,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!(spell
                instanceof AbstractMonkTechniqueSpell)) {
            return;
        }

        cir.setReturnValue(
                spell.getSpellCooldown()
        );
    }
}