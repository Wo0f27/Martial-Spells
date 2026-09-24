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

    public static final RegistryObject<SoundEvent> THROW_IMPACT =
            register("throw_impact");

    public static final RegistryObject<SoundEvent> MORTAL_STRIKE_SWING =
            register("mortal_strike_swing");

    public static final RegistryObject<SoundEvent> MORTAL_STRIKE_WHOOSH =
            register("mortal_strike_whoosh");

    public static final RegistryObject<SoundEvent> MORTAL_STRIKE_IMPACT =
            register("mortal_strike_impact");

    public static final RegistryObject<SoundEvent> LAST_STAND_START =
            register("last_stand_start");

    public static final RegistryObject<SoundEvent> LAST_STAND_CASTING =
            register("last_stand_casting");

    public static final RegistryObject<SoundEvent> LAST_STAND_RELEASE =
            register("last_stand_release");

    public static final RegistryObject<SoundEvent> HOLY_SHOCK_HEAL =
            register("holy_shock_heal");

    public static final RegistryObject<SoundEvent> HOLY_SHOCK_DAMAGE =
            register("holy_shock_damage");

    public static final RegistryObject<SoundEvent> BLESSED_STRIKE_START =
            register("blessed_strike_start");

    public static final RegistryObject<SoundEvent> BLESSED_STRIKE_CASTING =
            register("blessed_strike_casting");

    public static final RegistryObject<SoundEvent> BLESSED_STRIKE_RELEASE =
            register("blessed_strike_release");

    public static final RegistryObject<SoundEvent> DIVINE_PROTECTION_RELEASE =
            register("divine_protection_release");

    public static final RegistryObject<SoundEvent> DIVINE_PROTECTION_IMPACT =
            register("divine_protection_impact");

    public static final RegistryObject<SoundEvent> JUDGEMENT_IMPACT =
            register("judgement_impact");

    public static final RegistryObject<SoundEvent> IMMOLATION_RELEASE =
            register("immolation_release");

    public static final RegistryObject<SoundEvent> HOLY_BEAM_START_CASTING =
            register("holy_beam_start_casting");

    public static final RegistryObject<SoundEvent> HOLY_BEAM_CASTING =
            register("holy_beam_casting");

    public static final RegistryObject<SoundEvent> HOLY_BEAM_RELEASE =
            register("holy_beam_release");

    public static final RegistryObject<SoundEvent> HOLY_BEAM_HEAL =
            register("holy_beam_heal");

    public static final RegistryObject<SoundEvent> HOLY_BEAM_DAMAGE =
            register("holy_beam_damage");

    public static final RegistryObject<SoundEvent> HOLY_WARD_IMPACT =
            register("holy_ward_impact");

    public static final RegistryObject<SoundEvent> PENANCE_RELEASE =
            register("penance_release");

    public static final RegistryObject<SoundEvent> PENANCE_IMPACT =
            register("penance_impact");

    private MartialSoundRegistry() {}

    private static RegistryObject<SoundEvent> register(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, path);
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
