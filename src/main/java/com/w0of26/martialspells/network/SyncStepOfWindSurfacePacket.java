package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.visual.StepOfTheWindClientSurfaceState;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public final class SyncStepOfWindSurfacePacket {

    private final UUID playerId;

    /*
     * -1 = no active wall
     *
     * Otherwise this is Direction#get3DDataValue().
     */
    private final int directionId;

    public SyncStepOfWindSurfacePacket(
            UUID playerId,
            @Nullable Direction direction
    ) {
        this.playerId =
                playerId;

        this.directionId =
                direction == null
                        ? -1
                        : direction.get3DDataValue();
    }

    public SyncStepOfWindSurfacePacket(
            FriendlyByteBuf buffer
    ) {
        playerId =
                buffer.readUUID();

        directionId =
                buffer.readInt();
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(
                playerId
        );

        buffer.writeInt(
                directionId
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
                                        StepOfTheWindClientSurfaceState
                                                .setWallDirection(
                                                        playerId,
                                                        directionId < 0
                                                                ? null
                                                                : Direction
                                                                .from3DDataValue(
                                                                        directionId
                                                                )
                                                )
                        )
        );

        return true;
    }
}