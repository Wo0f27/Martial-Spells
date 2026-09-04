package com.w0of26.martialspells.client.prone;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ProneClientMovementEvents {

    private ProneClientMovementEvents() {
    }

    @SubscribeEvent
    public static void onMovementInput(
            MovementInputUpdateEvent event
    ) {
        if (!event.getEntity()
                .hasEffect(
                        MartialEffectRegistry
                                .PRONE
                                .get()
                )) {
            return;
        }

        Input input =
                event.getInput();

        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;

        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;

        input.jumping = false;
        input.shiftKeyDown = false;
    }
}