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

    private MartialParticleRegistry() {}

    public static void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }
}
