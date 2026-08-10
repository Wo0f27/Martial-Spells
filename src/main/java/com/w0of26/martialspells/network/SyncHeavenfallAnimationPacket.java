package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.animation.HeavenfallAnimationPhase;
import com.w0of26.martialspells.client.animation.HeavenfallClientAnimations;
import com.w0of26.martialspells.combat.HeavenfallAnimationStyle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class SyncHeavenfallAnimationPacket {

    private final UUID playerId;

    private final HeavenfallAnimationPhase phase;

    private final HeavenfallAnimationStyle style;

    public SyncHeavenfallAnimationPacket(
            UUID playerId,
            HeavenfallAnimationPhase phase,
            HeavenfallAnimationStyle style
    ) {
        this.playerId =
                playerId;

        this.phase =
                phase;

        this.style =
                style;
    }

    public SyncHeavenfallAnimationPacket(
            FriendlyByteBuf buffer
    ) {
        playerId =
                buffer.readUUID();

        phase =
                buffer.readEnum(
                        HeavenfallAnimationPhase.class
                );

        style =
                buffer.readEnum(
                        HeavenfallAnimationStyle.class
                );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(
                playerId
        );

        buffer.writeEnum(
                phase
        );

        buffer.writeEnum(
                style
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
                                        HeavenfallClientAnimations
                                                .handle(
                                                        playerId,
                                                        phase,
                                                        style
                                                )
                        )
        );

        return true;
    }
}