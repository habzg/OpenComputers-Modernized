package li.cil.oc.neoforge.integration.mekanism;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import mekanism.common.integration.computer.IComputerTile;
import mekanism.common.tile.TileEntityBoundingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DriverMekanismComputer implements DriverBlock {

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return resolveTile(world, pos) instanceof IComputerTile;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        final IComputerTile tile = (IComputerTile) resolveTile(world, pos);
        return tile == null ? null : new EnvironmentMekanismMachine(tile);
    }

    private static BlockEntity resolveTile(final Level world, final BlockPos pos) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileEntityBoundingBlock boundingBlock) {
            final BlockEntity main = boundingBlock.getMainTile(pos);
            if (main != null) {
                tile = main;
            }
        }
        return tile;
    }
}
