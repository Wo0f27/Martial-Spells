package com.w0of26.martialspells.network;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Registers and sends Martial Spells network packets. */
public final class MartialNetwork {
    /* Increment whenever the packet protocol changes. */
    private static final String PROTOCOL_VERSION = "11";

    private static SimpleChannel instance;
    private static int packetId;

    private MartialNetwork() {
    }

    public static void register() {
        instance = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "messages"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        instance.messageBuilder(
                        SyncKiPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncKiPacket::new)
                .encoder(SyncKiPacket::toBytes)
                .consumerMainThread(SyncKiPacket::handle)
                .add();

        instance.messageBuilder(
                        SyncFlurryVisualPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncFlurryVisualPacket::new)
                .encoder(SyncFlurryVisualPacket::toBytes)
                .consumerMainThread(SyncFlurryVisualPacket::handle)
                .add();

        instance.messageBuilder(
                        SyncStunningStrikeAnimationPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncStunningStrikeAnimationPacket::new)
                .encoder(SyncStunningStrikeAnimationPacket::toBytes)
                .consumerMainThread(SyncStunningStrikeAnimationPacket::handle)
                .add();

        /* Explicit Mutilate animation synchronization. */
        instance.messageBuilder(
                        SyncMutilateAnimationPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncMutilateAnimationPacket::new)
                .encoder(SyncMutilateAnimationPacket::toBytes)
                .consumerMainThread(SyncMutilateAnimationPacket::handle)
                .add();

        instance.messageBuilder(
                        SyncDeflectMissilesAnimationPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncDeflectMissilesAnimationPacket::new)
                .encoder(SyncDeflectMissilesAnimationPacket::toBytes)
                .consumerMainThread(SyncDeflectMissilesAnimationPacket::handle)
                .add();

        instance.messageBuilder(
                        SyncStepOfWindSurfacePacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncStepOfWindSurfacePacket::new)
                .encoder(SyncStepOfWindSurfacePacket::toBytes)
                .consumerMainThread(SyncStepOfWindSurfacePacket::handle)
                .add();

        instance.messageBuilder(
                        RequestStepOfWindWallJumpPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(RequestStepOfWindWallJumpPacket::new)
                .encoder(RequestStepOfWindWallJumpPacket::toBytes)
                .consumerMainThread(RequestStepOfWindWallJumpPacket::handle)
                .add();

        instance.messageBuilder(
                        SyncHeavenfallTargetPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncHeavenfallTargetPacket::new)
                .encoder(SyncHeavenfallTargetPacket::toBytes)
                .consumerMainThread(SyncHeavenfallTargetPacket::handle)
                .add();

        instance.messageBuilder(
                        RequestHeavenfallDivePacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(RequestHeavenfallDivePacket::new)
                .encoder(RequestHeavenfallDivePacket::toBytes)
                .consumerMainThread(RequestHeavenfallDivePacket::handle)
                .add();

        instance.messageBuilder(
                        SyncHeavenfallAnimationPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncHeavenfallAnimationPacket::new)
                .encoder(SyncHeavenfallAnimationPacket::toBytes)
                .consumerMainThread(SyncHeavenfallAnimationPacket::handle)
                .add();

        instance.messageBuilder(
                        SyncProneAnimationPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncProneAnimationPacket::new)
                .encoder(SyncProneAnimationPacket::toBytes)
                .consumerMainThread(SyncProneAnimationPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        if (instance == null) {
            throw new IllegalStateException(
                    "Martial Spells network has not been registered."
            );
        }
        instance.sendToServer(message);
    }

    private static int nextPacketId() {
        return packetId++;
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (instance == null) {
            throw new IllegalStateException(
                    "Martial Spells network has not been registered."
            );
        }
        instance.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToTrackingAndSelf(MSG message, ServerPlayer player) {
        if (instance == null) {
            throw new IllegalStateException(
                    "Martial Spells network has not been registered."
            );
        }
        instance.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                message
        );
    }

    public static <MSG> void sendToTrackingEntityAndSelf(
            MSG message,
            net.minecraft.world.entity.Entity entity
    ) {
        if (instance == null) {
            throw new IllegalStateException(
                    "Martial Spells network has not been registered."
            );
        }
        instance.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                message
        );
    }
}
