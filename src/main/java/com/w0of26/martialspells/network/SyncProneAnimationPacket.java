package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.prone.ProneClientVisuals;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncProneAnimationPacket {

    private final int entityId;
    private final boolean active;

    public SyncProneAnimationPacket(
            int entityId,
            boolean active
    ) {
        this.entityId = entityId;
        this.active = active;
    }

    public SyncProneAnimationPacket(
            FriendlyByteBuf buffer
    ) {
        this.entityId =
                buffer.readVarInt();

        this.active =
                buffer.readBoolean();
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeVarInt(
                entityId
        );

        buffer.writeBoolean(
                active
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
                                        ProneClientVisuals
                                                .setProne(
                                                        entityId,
                                                        active
                                                )
                        )
        );

        return true;
    }
}