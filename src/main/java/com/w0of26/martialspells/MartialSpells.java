package com.w0of26.martialspells;

import com.mojang.logging.LogUtils;
import com.w0of26.martialspells.ki.MartialCapabilities;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.registry.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MartialSpells.MOD_ID)
public final class MartialSpells {
    public static final String MOD_ID = "martial_spells";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MartialSpells() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MartialEffectRegistry.register(modEventBus);
        MartialEntityRegistry.register(modEventBus);
        MartialParticleRegistry.register(modEventBus);
        MartialSpellRegistry.register(modEventBus);
        MartialItemRegistry.register(modEventBus);
        MartialRecipeRegistry.register(modEventBus);
        MartialAttributeRegistry.register(modEventBus);
        MartialSchoolRegistry.register(modEventBus);

        modEventBus.addListener(MartialCapabilities::register);
        MartialNetwork.register();
        LOGGER.info("Initializing Martial Spells");
    }
}
