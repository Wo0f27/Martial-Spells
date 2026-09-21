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
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, MartialSpells.MOD_ID);

    public static final RegistryObject<AbstractSpell> GUARDIANS_CRY = SPELLS.register("guardians_cry", GuardiansCrySpell::new);
    public static final RegistryObject<AbstractSpell> GUARDIANS_COVENANT = SPELLS.register("guardians_covenant", GuardiansCovenantSpell::new);
    public static final RegistryObject<AbstractSpell> STILLWATER_MEDITATION = SPELLS.register("stillwater_meditation", StillwaterMeditationSpell::new);
    public static final RegistryObject<AbstractSpell> FLURRY_OF_BLOWS = SPELLS.register("flurry_of_blows", FlurryOfBlowsSpell::new);
    public static final RegistryObject<AbstractSpell> DIAMOND_BODY = SPELLS.register("diamond_body", DiamondBodySpell::new);
    public static final RegistryObject<AbstractSpell> STUNNING_STRIKE = SPELLS.register("stunning_strike", StunningStrikeSpell::new);
    public static final RegistryObject<AbstractSpell> DEFLECT_MISSILES = SPELLS.register("deflect_missiles", DeflectMissilesSpell::new);
    public static final RegistryObject<AbstractSpell> STEP_OF_THE_WIND = SPELLS.register("step_of_the_wind", StepOfTheWindSpell::new);
    public static final RegistryObject<AbstractSpell> HEAVENFALL_STRIKE = SPELLS.register("heavenfall_strike", HeavenfallStrikeSpell::new);
    public static final RegistryObject<AbstractSpell> STILLNESS_OF_MIND = SPELLS.register("stillness_of_mind", StillnessOfMindSpell::new);
    public static final RegistryObject<AbstractSpell> BARRAGE = SPELLS.register("barrage", BarrageSpell::new);
    public static final RegistryObject<AbstractSpell> ENTANGLING_ARROW = SPELLS.register("entangling_arrow", EntanglingArrowSpell::new);
    public static final RegistryObject<AbstractSpell> CALTROPS = SPELLS.register("caltrops", CaltropsSpell::new);
    public static final RegistryObject<AbstractSpell> SHOCK_POWDER = SPELLS.register("shock_powder", ShockPowderSpell::new);
    public static final RegistryObject<AbstractSpell> SHADOW_STEP = SPELLS.register("shadow_step", ShadowstepSpell::new);
    public static final RegistryObject<AbstractSpell> SLICE_AND_DICE = SPELLS.register("slice_and_dice", SliceAndDiceSpell::new);
    public static final RegistryObject<AbstractSpell> VANISH = SPELLS.register("vanish", VanishSpell::new);
    public static final RegistryObject<AbstractSpell> MUTILATE = SPELLS.register("mutilate", MutilateSpell::new);
    public static final RegistryObject<AbstractSpell> BEAR_TRAP = SPELLS.register("bear_trap", BearTrapSpell::new);
    public static final RegistryObject<AbstractSpell> CHARGE = SPELLS.register("charge", ChargeSpell::new);
    public static final RegistryObject<AbstractSpell> DEMORALIZING_SHOUT = SPELLS.register("demoralizing_shout", DemoralizingShoutSpell::new);
    public static final RegistryObject<AbstractSpell> THROW_NET = SPELLS.register("throw_net", ThrowNetSpell::new);
    public static final RegistryObject<AbstractSpell> SHATTERING_THROW = SPELLS.register("shattering_throw", ShatteringThrowSpell::new);

    private MartialSpellRegistry() {}

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }
}
