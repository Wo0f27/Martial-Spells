package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.visual.HeavenfallClientTargetState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncHeavenfallTargetPacket {

    /*
     * -1 = no currently valid target.
     */
    private final int targetEntityId;

    public SyncHeavenfallTargetPacket(
            int targetEntityId
    ) {
        this.targetEntityId =
                targetEntityId;
    }

    public SyncHeavenfallTargetPacket(
            FriendlyByteBuf buffer
    ) {
        targetEntityId =
                buffer.readInt();
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeInt(
                targetEntityId
        );
    }

    public boolean handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        context.enqueueWork(
                () -> DistExecutor
                        .unsafeRunWhenOn(
                                Dist.CLIENT,
                                () -> () ->
                                        HeavenfallClientTargetState
                                                .setTargetEntityId(
                                                        targetEntityId
                                                )
                        )
        );

        return true;
    }
}