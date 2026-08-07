package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.animation.StunningStrikeClientAnimations;
import com.w0of26.martialspells.combat.StunningStrikeWeaponStyle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class SyncStunningStrikeAnimationPacket {

    private final UUID playerId;

    private final StunningStrikeWeaponStyle
            weaponStyle;

    public SyncStunningStrikeAnimationPacket(
            UUID playerId,
            StunningStrikeWeaponStyle weaponStyle
    ) {
        this.playerId = playerId;
        this.weaponStyle = weaponStyle;
    }

    public SyncStunningStrikeAnimationPacket(
            FriendlyByteBuf buffer
    ) {
        playerId =
                buffer.readUUID();

        weaponStyle =
                buffer.readEnum(
                        StunningStrikeWeaponStyle.class
                );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(playerId);
        buffer.writeEnum(weaponStyle);
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
                                        StunningStrikeClientAnimations
                                                .play(
                                                        playerId,
                                                        weaponStyle
                                                )
                        )
        );

        return true;
    }
}