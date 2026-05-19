package io.github.blamp26.core86.foundation.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import io.github.blamp26.core86.content.reactor.ReactorConsoleBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class ReactorMapSyncPacket {
    private final BlockPos pos;
    private final byte[] mapData;
    private final float xenonLevel;

    public ReactorMapSyncPacket(BlockPos pos, byte[] mapData, float xenonLevel) {
        this.pos = pos;
        this.mapData = mapData;
        this.xenonLevel = xenonLevel;
    }

    public static void encode(ReactorMapSyncPacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.pos);
        buffer.writeByteArray(msg.mapData);
        buffer.writeFloat(msg.xenonLevel);
    }

    public static ReactorMapSyncPacket decode(FriendlyByteBuf buffer) {
        return new ReactorMapSyncPacket(buffer.readBlockPos(), buffer.readByteArray(), buffer.readFloat());
    }

    public static void handle(ReactorMapSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                BlockEntity be = Minecraft.getInstance().level.getBlockEntity(msg.pos);
                if (be instanceof ReactorConsoleBlockEntity console) {
                    console.setReactorMapFromFlat(msg.mapData);
                    console.setXenonLevel(msg.xenonLevel);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
