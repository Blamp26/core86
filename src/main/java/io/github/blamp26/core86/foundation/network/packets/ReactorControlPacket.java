package io.github.blamp26.core86.foundation.network.packets;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.reactor.ReactorConsoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReactorControlPacket {
    private final BlockPos pos;
    private final int action; // 0: setAll, 1: cycleAll, 2: cycleIndividual
    private final int value;

    public ReactorControlPacket(BlockPos pos, int action, int value) {
        this.pos = pos;
        this.action = action;
        this.value = value;
    }

    public static void encode(ReactorControlPacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.pos);
        buffer.writeInt(msg.action);
        buffer.writeInt(msg.value);
    }

    public static ReactorControlPacket decode(FriendlyByteBuf buffer) {
        return new ReactorControlPacket(buffer.readBlockPos(), buffer.readInt(), buffer.readInt());
    }

    public static void handle(ReactorControlPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            Core86.LOGGER.info("Received ReactorControlPacket on server from {}: action={}, value={} at {}", 
                    player != null ? player.getName().getString() : "unknown", msg.action, msg.value, msg.pos);
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(msg.pos);
                if (be instanceof ReactorConsoleBlockEntity console) {
                    if (console.isScramInProgress()) {
                        boolean blocked = false;
                        if (msg.action == 0 && msg.value < 100) {
                            blocked = true;
                        } else if (msg.action == 2) {
                            int insertion = msg.value & 0xFFFF;
                            blocked = insertion < 100;
                        } else if (msg.action == 4) {
                            int insertion = msg.value & 0xFFFF;
                            blocked = insertion < 100;
                        }
                        if (blocked) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[SCRAM] Control rods are moving to safe position."));
                            return;
                        }
                    }
                    if (msg.action == 0) { // Set All
                        console.setTargetInsertion(msg.value);
                    } else if (msg.action == 1) { // Cycle
                        console.cycleTargetInsertion();
                    } else if (msg.action == 2) { // Set Individual Precise
                        // msg.value is packed as (index << 16) | insertion
                        int index = msg.value >> 16;
                        int insertion = msg.value & 0xFFFF;
                        console.setChannelInsertion(index, insertion);
                    } else if (msg.action == 5) { // Cycle Individual (Old logic)
                        int index = msg.value;
                        int current = console.getChannelInsertion(index);
                        int next = (current + 25) > 100 ? 0 : (current + 25);
                        console.setChannelInsertion(index, next);
                    } else if (msg.action == 3) { // Set Channel Color
                        // value: (index << 16) | colorId
                        int index = msg.value >> 16;
                        int colorId = msg.value & 0xFFFF;
                        console.setChannelColor(index, colorId);
                    } else if (msg.action == 4) { // Set Group Insertion
                        // value: (colorId << 16) | insertion
                        int colorId = msg.value >> 16;
                        int insertion = msg.value & 0xFFFF;
                        console.setGroupInsertion(colorId, insertion);
                    } else if (msg.action == 6) { // Toggle experiment mode (SIUR only)
                        console.toggleExperimentMode();
                    }
                    console.updateReactorState();
                    console.setChanged();
                } else {
                    Core86.LOGGER.warn("ReactorControlPacket target at {} is not a ReactorConsoleBlockEntity! It is: {}", 
                            msg.pos, be != null ? be.getClass().getSimpleName() : "null");
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
