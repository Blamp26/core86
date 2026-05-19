package io.github.blamp26.core86.content.turbine;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Паровая турбина — блок как {@code DieselEngineBlock} (Create Diesel Generators).
 * Тик обрабатывает SmartBlockEntity через IBE, без отдельного BlockEntityTicker.
 */
public class SteamTurbineBlock extends DirectionalKineticBlock implements IBE<SteamTurbineBlockEntity> {

    public SteamTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SteamTurbineBlockEntity> getBlockEntityClass() {
        return SteamTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamTurbineBlockEntity> getBlockEntityType() {
        return CoreBlockEntities.STEAM_TURBINE.get();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    public boolean isGenerator() {
        return true;
    }
}
