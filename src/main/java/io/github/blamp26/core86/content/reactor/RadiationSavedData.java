package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.foundation.config.CommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RadiationSavedData extends SavedData {
    private static final String DATA_NAME = Core86.MODID + "_radiation";
    private final List<RadiationZone> zones = new ArrayList<>();
    private final Set<Long> explodedReactors = new HashSet<>();

    public static RadiationSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RadiationSavedData::load, RadiationSavedData::new, DATA_NAME);
    }

    public static RadiationSavedData load(CompoundTag tag) {
        RadiationSavedData data = new RadiationSavedData();
        ListTag zoneTags = tag.getList("Zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < zoneTags.size(); i++) {
            CompoundTag zoneTag = zoneTags.getCompound(i);
            data.zones.add(new RadiationZone(
                    new BlockPos(zoneTag.getInt("X"), zoneTag.getInt("Y"), zoneTag.getInt("Z")),
                    zoneTag.getInt("Radius"),
                    zoneTag.getFloat("InitialIntensityRadPerSec"),
                    zoneTag.getLong("CreatedGameTime"),
                    zoneTag.getLong("HalfLifeTicks")));
        }
        ListTag explodedTags = tag.getList("ExplodedReactors", Tag.TAG_LONG);
        for (int i = 0; i < explodedTags.size(); i++) {
            Tag longTag = explodedTags.get(i);
            if (longTag instanceof LongTag value) {
                data.explodedReactors.add(value.getAsLong());
            }
        }
        return data;
    }

    public void addZone(BlockPos center, int radius, float initialIntensityRadPerSec, long createdGameTime, long halfLifeTicks) {
        zones.add(new RadiationZone(center.immutable(), radius, initialIntensityRadPerSec, createdGameTime, halfLifeTicks));
        setDirty();
    }

    public double getIntensityRadPerSec(BlockPos pos, long gameTime) {
        double total = 0.0D;
        boolean removed = false;
        Iterator<RadiationZone> iterator = zones.iterator();
        while (iterator.hasNext()) {
            RadiationZone zone = iterator.next();
            double centerIntensity = zone.centerIntensity(gameTime);
            if (centerIntensity < CommonConfig.radiationMinIntensityRadPerSec()) {
                iterator.remove();
                removed = true;
                continue;
            }
            total += zone.intensityAt(pos, gameTime);
        }
        if (removed) {
            setDirty();
        }
        return total;
    }

    public int getActiveZoneCount(long gameTime) {
        pruneExpired(gameTime);
        return zones.size();
    }

    public boolean hasExplosionTriggered(BlockPos pos) {
        return explodedReactors.contains(pos.asLong());
    }

    public boolean markExplosionTriggered(BlockPos pos) {
        long key = pos.asLong();
        if (explodedReactors.add(key)) {
            setDirty();
            return true;
        }
        return false;
    }

    public RadiationZone getNearestZone(BlockPos pos, long gameTime) {
        pruneExpired(gameTime);
        RadiationZone nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (RadiationZone zone : zones) {
            double distSq = zone.center.distSqr(pos);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = zone;
            }
        }
        return nearest;
    }

    private void pruneExpired(long gameTime) {
        boolean removed = zones.removeIf(zone -> zone.centerIntensity(gameTime) < CommonConfig.radiationMinIntensityRadPerSec());
        if (removed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag zoneTags = new ListTag();
        for (RadiationZone zone : zones) {
            CompoundTag zoneTag = new CompoundTag();
            zoneTag.putInt("X", zone.center.getX());
            zoneTag.putInt("Y", zone.center.getY());
            zoneTag.putInt("Z", zone.center.getZ());
            zoneTag.putInt("Radius", zone.radius);
            zoneTag.putFloat("InitialIntensityRadPerSec", zone.initialIntensityRadPerSec);
            zoneTag.putLong("CreatedGameTime", zone.createdGameTime);
            zoneTag.putLong("HalfLifeTicks", zone.halfLifeTicks);
            zoneTags.add(zoneTag);
        }
        tag.put("Zones", zoneTags);
        ListTag explodedTags = new ListTag();
        for (Long posLong : explodedReactors) {
            explodedTags.add(net.minecraft.nbt.LongTag.valueOf(posLong));
        }
        tag.put("ExplodedReactors", explodedTags);
        return tag;
    }

    public record RadiationZone(BlockPos center, int radius, float initialIntensityRadPerSec, long createdGameTime, long halfLifeTicks) {
        public double intensityAt(BlockPos pos, long gameTime) {
            double distance = Math.sqrt(center.distSqr(pos));
            if (distance >= radius) {
                return 0.0D;
            }

            double normalizedDistance = Math.max(0.0D, 1.0D - distance / Math.max(1.0D, radius));
            double distanceFalloff = normalizedDistance * normalizedDistance;
            return initialIntensityRadPerSec * distanceFalloff * timeDecay(gameTime);
        }

        public double centerIntensity(long gameTime) {
            return initialIntensityRadPerSec * timeDecay(gameTime);
        }

        public double distanceTo(BlockPos pos) {
            return Math.sqrt(center.distSqr(pos));
        }

        private double timeDecay(long gameTime) {
            long ageTicks = Math.max(0L, gameTime - createdGameTime);
            return Math.pow(0.5D, (double) ageTicks / Math.max(1L, halfLifeTicks));
        }
    }
}
