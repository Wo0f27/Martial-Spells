package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MartialParticleRegistry {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MartialSpells.MOD_ID);

    public static final RegistryObject<SimpleParticleType> BARRAGE_TRAIL =
            PARTICLES.register("barrage_trail", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SHOCK_POWDER_SMOKE =
            PARTICLES.register("shock_powder_smoke", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SHOCK_POWDER_ARC =
            PARTICLES.register("shock_powder_arc", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> CHARGE_STRIPE =
            PARTICLES.register("charge_stripe", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> CHARGE_SPARK =
            PARTICLES.register("charge_spark", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> DEMORALIZE_SMOKE =
            PARTICLES.register("demoralize_smoke", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SHATTER_BLOOD =
            PARTICLES.register("shatter_blood", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> LAST_STAND_SPARK =
            PARTICLES.register("last_stand_spark", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> LAST_STAND_SMOKE =
            PARTICLES.register("last_stand_smoke", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> LAST_STAND_AURA =
            PARTICLES.register("last_stand_aura", () -> new SimpleParticleType(false));

    // Frozen Paladins / Spell Engine visual subset. These remain Martial
    // Spells-owned particle types; no Spell Engine runtime dependency exists.
    public static final RegistryObject<SimpleParticleType> PALADIN_SPARK_FLOAT =
            PARTICLES.register("paladin_spark_float", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_SPARK_DECELERATE =
            PARTICLES.register("paladin_spark_decelerate", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_SPARK_ASCEND =
            PARTICLES.register("paladin_spark_ascend", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_HOLY_FLOAT =
            PARTICLES.register("paladin_holy_float", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_HOLY_DECELERATE =
            PARTICLES.register("paladin_holy_decelerate", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_HOLY_BURST =
            PARTICLES.register("paladin_holy_burst", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_HEAL_ASCEND =
            PARTICLES.register("paladin_heal_ascend", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_SPELL_FLOAT =
            PARTICLES.register("paladin_spell_float", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_SPELL_DECELERATE =
            PARTICLES.register("paladin_spell_decelerate", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_STRIPE_FLOAT =
            PARTICLES.register("paladin_stripe_float", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_AREA_553_CAMERA =
            PARTICLES.register("paladin_area_553_camera", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_AREA_637_GROUND =
            PARTICLES.register("paladin_area_637_ground", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PALADIN_AREA_676_CAMERA =
            PARTICLES.register("paladin_area_676_camera", () -> new SimpleParticleType(false));

    private MartialParticleRegistry() {}

    public static void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }
}
