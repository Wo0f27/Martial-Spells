package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.ClientKiData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Synchronizes one player's Ki values from the server to that player's
 * client.
 */
public final class SyncKiPacket {
    private final int currentKi;
    private final int maximumKi;

    /**
     * Called on the server when constructing the outgoing packet.
     */
    public SyncKiPacket(
            int currentKi,
            int maximumKi
    ) {
        this.currentKi = currentKi;
        this.maximumKi = maximumKi;
    }

    /**
     * Called when decoding the packet on the receiving side.
     */
    public SyncKiPacket(
            FriendlyByteBuf buffer
    ) {
        currentKi = buffer.readVarInt();
        maximumKi = buffer.readVarInt();
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeVarInt(currentKi);
        buffer.writeVarInt(maximumKi);
    }

    public boolean handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        context.enqueueWork(
                () -> ClientKiData.set(
                        currentKi,
                        maximumKi
                )
        );

        return true;
    }
}