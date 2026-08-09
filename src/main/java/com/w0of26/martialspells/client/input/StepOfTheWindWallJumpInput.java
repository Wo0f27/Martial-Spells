package com.w0of26.martialspells.client.input;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.StepOfTheWindClientSurfaceState;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.RequestStepOfWindWallJumpPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StepOfTheWindWallJumpInput {

    /*
     * Used for rising-edge detection.
     *
     * Holding Jump should perform only one wall jump.
     * The player must release and press Jump again before
     * another request is sent.
     */
    private static boolean jumpWasDown;

    private StepOfTheWindWallJumpInput() {
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {

            jumpWasDown = false;
            return;
        }

        /*
         * Use Minecraft's actual configured Jump mapping.
         *
         * This respects the player's remapped Jump key.
         */
        boolean jumpDown =
                minecraft.options
                        .keyJump
                        .isDown();

        boolean jumpPressed =
                jumpDown
                        && !jumpWasDown;

        jumpWasDown =
                jumpDown;

        if (!jumpPressed) {
            return;
        }

        /*
         * Do not trigger gameplay input through an open
         * inventory/menu.
         */
        if (minecraft.screen != null) {
            return;
        }

        /*
         * The existing surface packet already tells this
         * client whether the player is visually attached
         * to a Step wall.
         */
        if (StepOfTheWindClientSurfaceState
                .getWallDirection(
                        minecraft.player.getUUID()
                ) == null) {

            return;
        }

        MartialNetwork.sendToServer(
                new RequestStepOfWindWallJumpPacket()
        );
    }
}