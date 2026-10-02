package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.spells.BarrageSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Normalizes legacy saved/bound Barrage levels when Iron's materializes SpellData. */
@Mixin(value = SpellData.class, remap = false)
public abstract class BarrageSpellDataLevelMixin {
    @Shadow @Final @Mutable protected int spellLevel;

    @Inject(method = "<init>(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IZ)V",
            at = @At("RETURN"), remap = false)
    private void martialSpells$clampLegacyBarrage(AbstractSpell spell, int level,
                                                   boolean locked, CallbackInfo ci) {
        if (spell != null && BarrageSpell.SPELL_ID.equals(spell.getSpellResource()) && spellLevel > 1) {
            spellLevel = 1;
        }
    }
}
