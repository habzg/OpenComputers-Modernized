package li.cil.oc.api.prefab;

import li.cil.oc.api.driver.DriverBlock;import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * If you wish to create a block component for a third-party block, i.e. a block
 * for which you do not control the block entity, such as vanilla blocks, you
 * will need a block driver.
 * <br>
 * This prefab allows creating a driver that works for a specified list of item
 * stacks (to support different blocks with the same id but different metadata
 * values).
 * <br>
 * You still have to provide the implementation for creating its environment, if
 * any.
 * <br>
 * To limit sidedness, I recommend overriding {@link #worksWith(Level, BlockPos, Direction)}
 * and calling <code>super.worksWith</code> in addition to the side check.
 *
 * @see li.cil.oc.api.network.ManagedEnvironment
 */

public abstract class DriverSidedBlock implements DriverBlock {
    protected final ItemStack[] blocks;

    @SuppressWarnings("unused")
    protected DriverSidedBlock(final ItemStack... blocks) {
        this.blocks = blocks.clone();
    }

    @Override
    public boolean worksWith(final Level level, final BlockPos pos, final Direction side) {
        final BlockState state = level.getBlockState(pos);
        return worksWith(state.getBlock(), state);
    }

    @SuppressWarnings("unused")
    protected boolean worksWith(final Block referenceBlock, final BlockState referenceState) {
        for (ItemStack stack : blocks) {
            if (stack != null && stack.getItem() instanceof BlockItem item) {
                final Block supportedBlock = item.getBlock();
                if (referenceBlock == supportedBlock) {
                    return true;
                }
            }
        }
        return false;
    }
}
