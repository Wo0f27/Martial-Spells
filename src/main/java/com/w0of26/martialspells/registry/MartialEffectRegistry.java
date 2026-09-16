package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.effects.ChargeEffect;
import com.w0of26.martialspells.effects.DiamondBodyEffect;
import com.w0of26.martialspells.effects.DiamondHeartEffect;
import com.w0of26.martialspells.effects.GuardiansCovenantLinkedEffect;
import com.w0of26.martialspells.effects.GuardiansCovenantTankEffect;
import com.w0of26.martialspells.effects.GuardiansCryActiveEffect;
import com.w0of26.martialspells.effects.GuardiansCryEffect;
import com.w0of26.martialspells.effects.ProneEffect;
import com.w0of26.martialspells.effects.ShadowstepEffect;
import com.w0of26.martialspells.effects.SliceAndDiceEffect;
import com.w0of26.martialspells.effects.StealthEffect;
import com.w0of26.martialspells.effects.StillnessOfMindEffect;
import com.w0of26.martialspells.effects.TrappedEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MartialEffectRegistry {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(
                    ForgeRegistries.MOB_EFFECTS,
                    MartialSpells.MOD_ID
            );

    public static final RegistryObject<MobEffect> GUARDIANS_CRY =
            MOB_EFFECTS.register(
                    "guardians_cry",
                    GuardiansCryEffect::new
            );

    public static final RegistryObject<MobEffect> GUARDIANS_CRY_ACTIVE =
            MOB_EFFECTS.register(
                    "guardians_cry_active",
                    GuardiansCryActiveEffect::new
            );

    public static final RegistryObject<MobEffect> GUARDIANS_COVENANT_TANK =
            MOB_EFFECTS.register(
                    "guardians_covenant_tank",
                    GuardiansCovenantTankEffect::new
            );

    public static final RegistryObject<MobEffect> GUARDIANS_COVENANT_LINKED =
            MOB_EFFECTS.register(
                    "guardians_covenant_linked",
                    GuardiansCovenantLinkedEffect::new
            );

    public static final RegistryObject<MobEffect>
            DIAMOND_BODY =
            MOB_EFFECTS.register(
                    "diamond_body",
                    DiamondBodyEffect::new
            );

    public static final RegistryObject<MobEffect>
            DIAMOND_HEART =
            MOB_EFFECTS.register(
                    "diamond_heart",
                    DiamondHeartEffect::new
            );

    public static final RegistryObject<MobEffect>
            STILLNESS_OF_MIND =
            MOB_EFFECTS.register(
                    "stillness_of_mind",
                    StillnessOfMindEffect::new
            );

    public static final RegistryObject<MobEffect>
            PRONE =
            MOB_EFFECTS.register(
                    "prone",
                    ProneEffect::new
            );

    public static final RegistryObject<MobEffect>
            SHADOW_STEP =
            MOB_EFFECTS.register(
                    "shadow_step",
                    ShadowstepEffect::new
            );

    public static final RegistryObject<MobEffect>
            SLICE_AND_DICE =
            MOB_EFFECTS.register(
                    "slice_and_dice",
                    SliceAndDiceEffect::new
            );

    public static final RegistryObject<MobEffect>
            STEALTH =
            MOB_EFFECTS.register(
                    "stealth",
                    StealthEffect::new
            );

    /** Frozen Rogues bear_trap status effect, displayed as "Trapped". */
    public static final RegistryObject<MobEffect>
            BEAR_TRAP =
            MOB_EFFECTS.register(
                    "bear_trap",
                    TrappedEffect::new
            );

    /** Frozen Warrior Charge self-buff. */
    public static final RegistryObject<MobEffect>
            CHARGE =
            MOB_EFFECTS.register(
                    "charge",
                    ChargeEffect::new
            );

    private MartialEffectRegistry() {
    }

    public static void register(
            IEventBus modEventBus
    ) {
        MOB_EFFECTS.register(
                modEventBus
        );
    }
}
