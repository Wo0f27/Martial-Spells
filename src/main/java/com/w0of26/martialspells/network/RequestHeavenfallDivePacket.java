package com.w0of26.martialspells.network;

import com.w0of26.martialspells.movement.HeavenfallStrikeEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RequestHeavenfallDivePacket {

    public RequestHeavenfallDivePacket() {
    }

    public RequestHeavenfallDivePacket(
            FriendlyByteBuf buffer
    ) {
    }

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
                () -> HeavenfallStrikeEvents
                        .tryCommitDive(
                                sender
                        )
        );

        return true;
    }
}