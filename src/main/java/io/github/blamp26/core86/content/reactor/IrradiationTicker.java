package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.foundation.config.CommonConfig;
import io.github.blamp26.core86.foundation.registry.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Core86.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IrradiationTicker {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> PENDING_COUNTDOWNS = new HashMap<>();
    private static final String DOSE_TAG = "core86RadiationDoseSv";

    public static void scheduleNuclearCountdown(ServerLevel level, BlockPos pos, int ticks) {
        PENDING_COUNTDOWNS.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(pos.immutable(), ticks);
    }

    public static boolean hasPendingCountdown(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Integer> pending = PENDING_COUNTDOWNS.get(level.dimension());
        return pending != null && pending.containsKey(pos);
    }

    static void cancelScheduledMeltdown(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Integer> pending = PENDING_COUNTDOWNS.get(level.dimension());
        if (pending != null) {
            pending.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        Map<BlockPos, Integer> pending = PENDING_COUNTDOWNS.get(serverLevel.dimension());
        if (pending != null && !pending.isEmpty()) {
            Iterator<Map.Entry<BlockPos, Integer>> iterator = pending.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = iterator.next();
                int countdown = entry.getValue() - 1;
                if (countdown <= 0) {
                    iterator.remove();
                    tryTriggerNuclearExplosion(serverLevel, entry.getKey());
                } else {
                    entry.setValue(countdown);
                }
            }
        }

        if (serverLevel.getGameTime() % Math.max(1, CommonConfig.radiationTickInterval()) != 0) {
            return;
        }

        RadiationSavedData radiation = RadiationSavedData.get(serverLevel);
        long gameTime = serverLevel.getGameTime();

        for (ServerPlayer player : serverLevel.players()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            BlockPos playerPos = player.blockPosition();
            double backgroundRadPerSec = radiation.getIntensityRadPerSec(playerPos, gameTime);
            double doseSv = updateDose(player, backgroundRadPerSec);
            applyRadiationEffects(player, backgroundRadPerSec, doseSv);

            if (hasDosimeterInHotbar(player)) {
                player.displayClientMessage(formatDosimeterMessage(backgroundRadPerSec, doseSv), true);
            }
        }
    }

    public static boolean tryTriggerNuclearExplosion(ServerLevel level, BlockPos position) {
        RadiationSavedData radiation = RadiationSavedData.get(level);
        if (radiation.hasExplosionTriggered(position)) {
            return false;
        }
        radiation.markExplosionTriggered(position);

        level.explode(null,
                position.getX() + 0.5D,
                position.getY() + 0.5D,
                position.getZ() + 0.5D,
                CoreReactorConstants.MELTDOWN_EXPLOSION_RADIUS_NUCLEAR,
                Level.ExplosionInteraction.BLOCK);

        radiation.addZone(
                position,
                CommonConfig.radiationZoneRadius(),
                CommonConfig.radiationZoneInitialIntensityRadPerSec(),
                level.getGameTime(),
                CommonConfig.radiationZoneHalfLifeTicks());
        return true;
    }

    public static double getPlayerDoseSv(Player player) {
        return player.getPersistentData().getDouble(DOSE_TAG);
    }

    public static double radPerSecToMillisievertPerHour(double radPerSec) {
        return radPerSec * CommonConfig.gameRadPerSecToMillisievertPerHour();
    }

    public static double doseGainSvPerSecond(double backgroundRadPerSec) {
        if (backgroundRadPerSec <= 0.0D) {
            return 0.0D;
        }
        double millisvPerHour = radPerSecToMillisievertPerHour(backgroundRadPerSec);
        return millisvPerHour / 3_600_000.0D;
    }

    public static double doseNetDeltaSvPerSecond(double backgroundRadPerSec) {
        return doseGainSvPerSecond(backgroundRadPerSec) - CommonConfig.radiationDoseDecayPerSecond();
    }

    private static double updateDose(ServerPlayer player, double backgroundRadPerSec) {
        double dose = getPlayerDoseSv(player);
        double intervalScale = CommonConfig.radiationTickInterval() / 20.0D;
        dose += doseNetDeltaSvPerSecond(backgroundRadPerSec) * intervalScale;

        dose = Math.max(0.0D, Math.min(CommonConfig.maxRadiationDoseSv(), dose));
        player.getPersistentData().putDouble(DOSE_TAG, dose);
        return dose;
    }

    private static void applyRadiationEffects(ServerPlayer player, double backgroundRadPerSec, double doseSv) {
        double millisvPerHour = radPerSecToMillisievertPerHour(backgroundRadPerSec);
        int duration = Math.max(60, CommonConfig.radiationTickInterval() + 40);

        if (doseSv >= 4.0D || millisvPerHour >= 500.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 1, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1, false, true, true));
        } else if (doseSv >= 1.0D || millisvPerHour >= 100.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0, false, true, true));
        } else if (doseSv >= 0.25D || millisvPerHour >= 25.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0, false, true, true));
        }
    }

    private static boolean hasDosimeterInHotbar(ServerPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(CoreItems.DOSIMETER.get())) {
                return true;
            }
        }
        return player.getOffhandItem().is(CoreItems.DOSIMETER.get());
    }

    private static MutableComponent formatDosimeterMessage(double backgroundRadPerSec, double doseSv) {
        double millisvPerHour = radPerSecToMillisievertPerHour(backgroundRadPerSec);
        String radiationText = millisvPerHour < 1.0D
                ? String.format(Locale.ROOT, "Radiation: %.2f uSv/h", millisvPerHour * 1000.0D)
                : String.format(Locale.ROOT, "Radiation: %.2f mSv/h", millisvPerHour);

        ChatFormatting color = ChatFormatting.GREEN;
        if (millisvPerHour >= 500.0D || doseSv >= 4.0D) {
            color = ChatFormatting.DARK_RED;
        } else if (millisvPerHour >= 100.0D || doseSv >= 1.0D) {
            color = ChatFormatting.RED;
        } else if (millisvPerHour >= 25.0D || doseSv >= 0.25D) {
            color = ChatFormatting.YELLOW;
        }

        return Component.literal(radiationText + String.format(Locale.ROOT, " | Dose: %.2f Sv", doseSv)).withStyle(color);
    }
}
