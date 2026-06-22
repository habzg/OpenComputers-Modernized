package li.cil.oc.api.prefab;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * To limit sidedness, I recommend overriding {@link #worksWith(Level, int, int, int, Direction)}
 * and calling <code>super.worksWith</code> in addition to the side check.
 */
@SuppressWarnings("unused")
public abstract class DriverSidedTileEntity implements li.cil.oc.api.driver.SidedBlock {
    public abstract Class<?> getTileEntityClass();

    @Override
    public boolean worksWith(final Level world, final int x, final int y, final int z, final Direction side) {
        final Class<?> filter = getTileEntityClass();
        if (filter == null) {
            return false;
        }
        final BlockEntity blockEntity = world.getBlockEntity(new net.minecraft.core.BlockPos(x, y, z));
        return filter.isInstance(blockEntity);
    }
}
