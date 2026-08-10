package com.w0of26.martialspells.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.HeavenfallClientTargetState;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.RequestHeavenfallDivePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HeavenfallConfirmInput {

    private HeavenfallConfirmInput() {
    }

    /*
     * Intercept the physical mouse event before Minecraft
     * turns it into an Attack key-mapping action.
     *
     * This is intentionally lower-level than
     * InteractionKeyMappingTriggered.
     *
     * Better Combat also owns normal weapon attack input,
     * so allowing the click to proceed causes its regular
     * attack animation to begin before Heavenfall's DIVE
     * animation is received.
     */
    @SubscribeEvent(
            priority = EventPriority.HIGHEST
    )
    public static void onMouseButton(
            InputEvent.MouseButton.Pre event
    ) {
        /*
         * Only react to a new button press.
         *
         * RELEASE must still pass through normally.
         */
        if (event.getAction()
                != InputConstants.PRESS) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null) {

            return;
        }

        /*
         * Respect the player's configured Attack mouse
         * binding instead of assuming mouse button 0.
         */
        if (!minecraft.options
                .keyAttack
                .matchesMouse(
                        event.getButton()
                )) {

            return;
        }

        /*
         * No Heavenfall target means this click belongs to
         * ordinary Minecraft / Better Combat gameplay.
         */
        if (HeavenfallClientTargetState
                .getTargetEntityId() < 0) {

            return;
        }

        /*
         * Heavenfall owns this press.
         *
         * Cancel BEFORE Minecraft and Better Combat can
         * convert it into a normal weapon attack.
         */
        event.setCanceled(
                true
        );

        /*
         * The client sends only the confirmation action.
         *
         * Target identity remains entirely server-owned.
         */
        MartialNetwork.sendToServer(
                new RequestHeavenfallDivePacket()
        );
    }
}