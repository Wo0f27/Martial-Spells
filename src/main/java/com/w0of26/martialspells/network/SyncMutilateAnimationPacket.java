package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.animation.MutilateClientAnimations;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Synchronizes Mutilate's player animation to the caster and tracking clients. */
public final class SyncMutilateAnimationPacket {
    private final UUID playerId;

    public SyncMutilateAnimationPacket(UUID playerId) {
        this.playerId = playerId;
    }

    public SyncMutilateAnimationPacket(FriendlyByteBuf buffer) {
        this.playerId = buffer.readUUID();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> MutilateClientAnimations.play(playerId)
        ));
        return true;
    }
}
