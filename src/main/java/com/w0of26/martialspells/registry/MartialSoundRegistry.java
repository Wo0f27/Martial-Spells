package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MartialSoundRegistry {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MartialSpells.MOD_ID);

    public static final RegistryObject<SoundEvent> SHOCK_POWDER_RELEASE =
            register("shock_powder_release");

    public static final RegistryObject<SoundEvent> SHOCK_POWDER_IMPACT =
            register("shock_powder_impact");

    public static final RegistryObject<SoundEvent> SHADOW_STEP_DEPART =
            register("shadow_step_depart");

    private MartialSoundRegistry() {}

    private static RegistryObject<SoundEvent> register(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, path);
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
