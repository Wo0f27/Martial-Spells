package com.w0of26.martialspells.network;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Registers and sends Martial Spells network packets.
 */
public final class MartialNetwork {

    /*
     * Increment whenever the packet protocol changes.
     *
     * Deflect Missiles adds a new S2C animation packet,
     * so protocol version 4 represents the new layout.
     */
    private static final String PROTOCOL_VERSION = "4";

    private static SimpleChannel instance;
    private static int packetId;

    private MartialNetwork() {
    }

    public static void register() {
        instance =
                NetworkRegistry.ChannelBuilder
                        .named(
                                ResourceLocation
                                        .fromNamespaceAndPath(
                                                MartialSpells.MOD_ID,
                                                "messages"
                                        )
                        )
                        .networkProtocolVersion(
                                () -> PROTOCOL_VERSION
                        )
                        .clientAcceptedVersions(
                                PROTOCOL_VERSION::equals
                        )
                        .serverAcceptedVersions(
                                PROTOCOL_VERSION::equals
                        )
                        .simpleChannel();

        /*
         * Ki synchronization.
         */
        instance.messageBuilder(
                        SyncKiPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(
                        SyncKiPacket::new
                )
                .encoder(
                        SyncKiPacket::toBytes
                )
                .consumerMainThread(
                        SyncKiPacket::handle
                )
                .add();

        /*
         * Flurry of Blows visuals.
         */
        instance.messageBuilder(
                        SyncFlurryVisualPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(
                        SyncFlurryVisualPacket::new
                )
                .encoder(
                        SyncFlurryVisualPacket::toBytes
                )
                .consumerMainThread(
                        SyncFlurryVisualPacket::handle
                )
                .add();

        /*
         * Stunning Strike animation.
         */
        instance.messageBuilder(
                        SyncStunningStrikeAnimationPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(
                        SyncStunningStrikeAnimationPacket::new
                )
                .encoder(
                        SyncStunningStrikeAnimationPacket::toBytes
                )
                .consumerMainThread(
                        SyncStunningStrikeAnimationPacket::handle
                )
                .add();

        /*
         * Deflect Missiles impact animation.
         *
         * The server sends:
         *
         * - the defending player's UUID
         * - whether the base or mirrored swipe was chosen
         *
         * Only empty-hand and gauntlet Deflect Missiles
         * interceptions send this packet.
         */
        instance.messageBuilder(
                        SyncDeflectMissilesAnimationPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(
                        SyncDeflectMissilesAnimationPacket::new
                )
                .encoder(
                        SyncDeflectMissilesAnimationPacket::toBytes
                )
                .consumerMainThread(
                        SyncDeflectMissilesAnimationPacket::handle
                )
                .add();
    }

    private static int nextPacketId() {
        return packetId++;
    }

    /**
     * Sends a packet only to one specific player.
     */
    public static <MSG> void sendToPlayer(
            MSG message,
            ServerPlayer player
    ) {
        if (instance == null) {
            throw new IllegalStateException(
                    "Martial Spells network has not been registered."
            );
        }

        instance.send(
                PacketDistributor.PLAYER.with(
                        () -> player
                ),
                message
        );
    }

    /**
     * Sends a packet to the target player and every client
     * currently tracking that player.
     *
     * This is used for player animations that must appear
     * consistently in multiplayer.
     */
    public static <MSG> void sendToTrackingAndSelf(
            MSG message,
            ServerPlayer player
    ) {
        if (instance == null) {
            throw new IllegalStateException(
                    "Martial Spells network has not "
                            + "been registered."
            );
        }

        instance.send(
                PacketDistributor
                        .TRACKING_ENTITY_AND_SELF
                        .with(
                                () -> player
                        ),
                message
        );
    }
}