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
    private static final String PROTOCOL_VERSION = "1";

    private static SimpleChannel instance;
    private static int packetId;

    private MartialNetwork() {
    }

    public static void register() {
        instance = NetworkRegistry.ChannelBuilder
                .named(
                        ResourceLocation.fromNamespaceAndPath(
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

        instance.messageBuilder(
                        SyncKiPacket.class,
                        nextPacketId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(SyncKiPacket::new)
                .encoder(SyncKiPacket::toBytes)
                .consumerMainThread(SyncKiPacket::handle)
                .add();
    }

    private static int nextPacketId() {
        return packetId++;
    }

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
}