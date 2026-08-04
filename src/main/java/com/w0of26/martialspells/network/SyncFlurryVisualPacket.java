package com.w0of26.martialspells.network;

import com.w0of26.martialspells.client.visual.ClientFlurryVisuals;
import com.w0of26.martialspells.visual.FlurryVisualMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Synchronizes Flurry's temporary weapon-rendering state.
 */
public final class SyncFlurryVisualPacket {
    private final UUID playerId;
    private final boolean active;
    private final FlurryVisualMode mode;
    private final ItemStack displayedWeapon;
    private final int durationTicks;

    private SyncFlurryVisualPacket(
            UUID playerId,
            boolean active,
            FlurryVisualMode mode,
            ItemStack displayedWeapon,
            int durationTicks
    ) {
        this.playerId = playerId;
        this.active = active;
        this.mode = mode;
        this.displayedWeapon =
                displayedWeapon.copy();
        this.durationTicks = durationTicks;
    }

    public static SyncFlurryVisualPacket start(
            UUID playerId,
            FlurryVisualMode mode,
            ItemStack displayedWeapon,
            int durationTicks
    ) {
        return new SyncFlurryVisualPacket(
                playerId,
                true,
                mode,
                displayedWeapon,
                durationTicks
        );
    }

    public static SyncFlurryVisualPacket stop(
            UUID playerId
    ) {
        return new SyncFlurryVisualPacket(
                playerId,
                false,
                FlurryVisualMode.EMPTY_HAND,
                ItemStack.EMPTY,
                0
        );
    }

    public SyncFlurryVisualPacket(
            FriendlyByteBuf buffer
    ) {
        playerId = buffer.readUUID();
        active = buffer.readBoolean();

        if (active) {
            mode = buffer.readEnum(
                    FlurryVisualMode.class
            );

            displayedWeapon =
                    buffer.readItem();

            durationTicks =
                    buffer.readVarInt();
        } else {
            mode =
                    FlurryVisualMode.EMPTY_HAND;

            displayedWeapon =
                    ItemStack.EMPTY;

            durationTicks = 0;
        }
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(playerId);
        buffer.writeBoolean(active);

        if (!active) {
            return;
        }

        buffer.writeEnum(mode);
        buffer.writeItem(displayedWeapon);
        buffer.writeVarInt(durationTicks);
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
                                () -> () -> {
                                    if (active) {
                                        ClientFlurryVisuals
                                                .start(
                                                        playerId,
                                                        mode,
                                                        displayedWeapon,
                                                        durationTicks
                                                );
                                    } else {
                                        ClientFlurryVisuals
                                                .stop(
                                                        playerId
                                                );
                                    }
                                }
                        )
        );

        return true;
    }
}