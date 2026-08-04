package com.w0of26.martialspells.ki;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

/**
 * Declares and registers Martial Spells capabilities.
 */
public final class MartialCapabilities {
    public static final Capability<KiData> KI =
            CapabilityManager.get(
                    new CapabilityToken<>() {
                    }
            );

    private MartialCapabilities() {
    }

    public static void register(
            RegisterCapabilitiesEvent event
    ) {
        event.register(KiData.class);
    }
}