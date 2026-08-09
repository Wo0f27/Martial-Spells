package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.spells.*;
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

    public static final RegistryObject<AbstractSpell>
            STILLWATER_MEDITATION =
            SPELLS.register(
                    "stillwater_meditation",
                    StillwaterMeditationSpell::new
            );

    public static final RegistryObject<AbstractSpell>
            FLURRY_OF_BLOWS =
            SPELLS.register(
                    "flurry_of_blows",
                    FlurryOfBlowsSpell::new
            );

    public static final RegistryObject<AbstractSpell>
            DIAMOND_BODY =
            SPELLS.register(
                    "diamond_body",
                    DiamondBodySpell::new
            );

    public static final RegistryObject<AbstractSpell>
            STUNNING_STRIKE =
            SPELLS.register(
                    "stunning_strike",
                    StunningStrikeSpell::new
            );

    public static final RegistryObject<AbstractSpell>
            DEFLECT_MISSILES =
            SPELLS.register(
                    "deflect_missiles",
                    DeflectMissilesSpell::new
            );

    public static final RegistryObject<AbstractSpell>
            STEP_OF_THE_WIND =
            SPELLS.register(
                    "step_of_the_wind",
                    StepOfTheWindSpell::new
            );

    private MartialSpellRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }
}