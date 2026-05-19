package io.github.blamp26.core86.content.reactor;

import com.mojang.brigadier.CommandDispatcher;
import io.github.blamp26.core86.Core86;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = Core86.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RadiationCommands {
    private RadiationCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("core86")
                .then(Commands.literal("radiation")
                        .then(Commands.literal("info")
                                .executes(context -> showRadiationInfo(context.getSource())))));
    }

    private static int showRadiationInfo(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        long gameTime = level.getGameTime();
        RadiationSavedData data = RadiationSavedData.get(level);
        double backgroundRadPerSec = data.getIntensityRadPerSec(pos, gameTime);
        double millisvPerHour = IrradiationTicker.radPerSecToMillisievertPerHour(backgroundRadPerSec);
        double doseSv = IrradiationTicker.getPlayerDoseSv(player);
        double doseDeltaSvPerSec = IrradiationTicker.doseNetDeltaSvPerSecond(backgroundRadPerSec);
        int zoneCount = data.getActiveZoneCount(gameTime);
        RadiationSavedData.RadiationZone nearest = data.getNearestZone(pos, gameTime);

        source.sendSuccess(() -> Component.literal("[CORE86 Radiation]").withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Background: %.4f gameRad/s (%.2f mSv/h)", backgroundRadPerSec, millisvPerHour)), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Dose: %.4f Sv", doseSv)), false);
        String trend = doseDeltaSvPerSec > 1.0E-9D ? "growing" : (doseDeltaSvPerSec < -1.0E-9D ? "decaying" : "stable");
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Dose delta: %+.6f Sv/s (%s)", doseDeltaSvPerSec, trend)), false);
        source.sendSuccess(() -> Component.literal("Active zones in dimension: " + zoneCount), false);

        if (nearest == null) {
            source.sendSuccess(() -> Component.literal("Nearest zone: none"), false);
        } else {
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "Nearest zone: %d %d %d, distance %.1f, center %.2f gameRad/s",
                    nearest.center().getX(),
                    nearest.center().getY(),
                    nearest.center().getZ(),
                    nearest.distanceTo(pos),
                    nearest.centerIntensity(gameTime))), false);
        }
        return 1;
    }
}
