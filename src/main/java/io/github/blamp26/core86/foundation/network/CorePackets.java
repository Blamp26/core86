package io.github.blamp26.core86.foundation.network;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.foundation.network.packets.ReactorControlPacket;
import io.github.blamp26.core86.foundation.network.packets.ReactorMapSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CorePackets {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Core86.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        CHANNEL.messageBuilder(ReactorControlPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReactorControlPacket::encode)
                .decoder(ReactorControlPacket::decode)
                .consumerMainThread(ReactorControlPacket::handle)
                .add();

        CHANNEL.messageBuilder(ReactorMapSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ReactorMapSyncPacket::encode)
                .decoder(ReactorMapSyncPacket::decode)
                .consumerMainThread(ReactorMapSyncPacket::handle)
                .add();
    }

    private CorePackets() {}
}
