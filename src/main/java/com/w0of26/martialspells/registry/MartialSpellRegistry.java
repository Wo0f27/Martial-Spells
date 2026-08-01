package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.spells.GuardiansCrySpell;
import com.w0of26.martialspells.spells.GuardiansCovenantSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class MartialSpellRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(
                    SpellRegistry.SPELL_REGISTRY_KEY,
                    MartialSpells.MOD_ID
            );

    public static final RegistryObject<AbstractSpell> GUARDIANS_CRY =
            SPELLS.register(
                    "guardians_cry",
                    GuardiansCrySpell::new
            );

    public static final RegistryObject<AbstractSpell> GUARDIANS_COVENANT =
            SPELLS.register(
                    "guardians_covenant",
                    GuardiansCovenantSpell::new
            );

    private MartialSpellRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }
}