package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.visual.NettedClientVisuals;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Minimal source-equivalent synchronization for Netted's client model FX.
 */
public final class SyncNettedVisualPacket {
    private final int entityId;
    private final long appliedAtWorldTime;
    private final int durationTicks;

    public SyncNettedVisualPacket(
            int entityId,
            long appliedAtWorldTime,
            int durationTicks
    ) {
        this.entityId = entityId;
        this.appliedAtWorldTime = appliedAtWorldTime;
        this.durationTicks = durationTicks;
    }

    public SyncNettedVisualPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.appliedAtWorldTime = buffer.readLong();
        this.durationTicks = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeLong(appliedAtWorldTime);
        buffer.writeVarInt(durationTicks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> NettedClientVisuals.activate(
                        entityId,
                        appliedAtWorldTime,
                        durationTicks
                )
        ));
        return true;
    }
}
