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

    public static final RegistryObject<SoundEvent> SLICE_AND_DICE =
            register("slice_and_dice");

    public static final RegistryObject<SoundEvent> VANISH_COMBINED =
            register("vanish_combined");

    public static final RegistryObject<SoundEvent> STEALTH_LEAVE =
            register("stealth_leave");

    public static final RegistryObject<SoundEvent> MUTILATE_IMPACT =
            register("mutilate_impact");

    public static final RegistryObject<SoundEvent> BEAR_TRAP_RELEASE =
            register("bear_trap_release");

    public static final RegistryObject<SoundEvent> BEAR_TRAP_SPAWN =
            register("bear_trap_spawn");

    public static final RegistryObject<SoundEvent> BEAR_TRAP_IMPACT =
            register("bear_trap_impact");

    public static final RegistryObject<SoundEvent> BEAR_TRAP_DESPAWN =
            register("bear_trap_despawn");

    public static final RegistryObject<SoundEvent> CHARGE_ACTIVATE =
            register("charge_activate");

    public static final RegistryObject<SoundEvent> SHOUT_RELEASE =
            register("shout_release");

    public static final RegistryObject<SoundEvent> DEMORALIZE_IMPACT =
            register("demoralize_impact");

    public static final RegistryObject<SoundEvent> NET_CASTING =
            register("net_casting");

    public static final RegistryObject<SoundEvent> THROW =
            register("throw");

    public static final RegistryObject<SoundEvent> NET_TRAVEL =
            register("net_travel");

    public static final RegistryObject<SoundEvent> NET_IMPACT =
            register("net_impact");

    private MartialSoundRegistry() {}

    private static RegistryObject<SoundEvent> register(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, path);
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
