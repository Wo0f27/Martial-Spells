package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.animation.LastStandClientAnimations;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Synchronizes Last Stand's early CHANNEL-style release animation. */
public final class SyncLastStandReleaseAnimationPacket {
    private final UUID playerId;

    public SyncLastStandReleaseAnimationPacket(UUID playerId) {
        this.playerId = playerId;
    }

    public SyncLastStandReleaseAnimationPacket(FriendlyByteBuf buffer) {
        this.playerId = buffer.readUUID();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> LastStandClientAnimations.playRelease(playerId)
        ));
        return true;
    }
}
