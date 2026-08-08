package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.animation.DeflectMissilesClientAnimations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Synchronizes one Deflect Missiles hand-deflection
 * animation from the server to tracking clients.
 *
 * The actual projectile interception remains entirely
 * server-authoritative. This packet only communicates the
 * corresponding visual reaction.
 */
public final class SyncDeflectMissilesAnimationPacket {

    private final UUID playerId;

    /*
     * false = base/original swipe
     * true  = mirrored swipe
     */
    private final boolean mirrored;

    public SyncDeflectMissilesAnimationPacket(
            UUID playerId,
            boolean mirrored
    ) {
        this.playerId =
                playerId;

        this.mirrored =
                mirrored;
    }

    /**
     * Decode constructor.
     */
    public SyncDeflectMissilesAnimationPacket(
            FriendlyByteBuf buffer
    ) {
        playerId =
                buffer.readUUID();

        mirrored =
                buffer.readBoolean();
    }

    /**
     * Encode packet contents.
     */
    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(
                playerId
        );

        buffer.writeBoolean(
                mirrored
        );
    }

    /**
     * Handles the packet on the receiving client.
     */
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
                                        DeflectMissilesClientAnimations
                                                .play(
                                                        playerId,
                                                        mirrored
                                                )
                        )
        );

        return true;
    }
}