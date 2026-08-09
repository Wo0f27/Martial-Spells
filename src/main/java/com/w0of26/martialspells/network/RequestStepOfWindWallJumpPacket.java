package com.w0of26.martialspells.network;

import com.w0of26.martialspells.movement.StepOfTheWindWallRunEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RequestStepOfWindWallJumpPacket {

    /*
     * Client-side constructor.
     *
     * The packet carries no data. The server already knows
     * which player sent it and independently verifies the
     * current Step of the Wind state.
     */
    public RequestStepOfWindWallJumpPacket() {
    }

    /*
     * Decode constructor.
     */
    public RequestStepOfWindWallJumpPacket(
            FriendlyByteBuf buffer
    ) {
    }

    /*
     * No payload needs to be encoded.
     */
    public void toBytes(
            FriendlyByteBuf buffer
    ) {
    }

    public boolean handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        ServerPlayer sender =
                context.getSender();

        if (sender == null) {
            return true;
        }

        context.enqueueWork(
                () ->
                        StepOfTheWindWallRunEvents
                                .tryWallJump(
                                        sender
                                )
        );

        return true;
    }
}