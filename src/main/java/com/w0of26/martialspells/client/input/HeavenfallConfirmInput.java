package com.w0of26.martialspells.client.input;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.HeavenfallClientTargetState;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.RequestHeavenfallDivePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
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

    @SubscribeEvent
    public static void onInteraction(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        if (!event.isAttack()) {
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
         * The server-synchronized target ID doubles as the
         * client's indication that Heavenfall is currently
         * accepting a confirmation.
         */
        if (HeavenfallClientTargetState
                .getTargetEntityId() < 0) {

            return;
        }

        /*
         * This click belongs to Heavenfall.
         *
         * Do not let vanilla also perform a normal attack or
         * play its normal hand swing.
         */
        event.setSwingHand(false);
        event.setCanceled(true);

        MartialNetwork.sendToServer(
                new RequestHeavenfallDivePacket()
        );
    }
}