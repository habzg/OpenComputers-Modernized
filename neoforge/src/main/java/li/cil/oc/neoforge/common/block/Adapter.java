package li.cil.oc.neoforge.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Adapter extends li.cil.oc.core.impl.common.block.Adapter {
    public Adapter(net.minecraft.world.level.block.entity.BlockEntityType<?> tileType) {
        super(tileType);
    }

    public Adapter() {
        super();
    }

    @Override
    public void neighborChanged(@NotNull BlockState ignoredState, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block ignoredBlock, @NotNull BlockPos fromPos, boolean ignoredIsMoving) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Adapter adapter) {
            Direction side;
            if (fromPos.equals(pos.below())) side = Direction.DOWN;
            else if (fromPos.equals(pos.above())) side = Direction.UP;
            else if (fromPos.equals(pos.north())) side = Direction.NORTH;
            else if (fromPos.equals(pos.south())) side = Direction.SOUTH;
            else if (fromPos.equals(pos.west())) side = Direction.WEST;
            else if (fromPos.equals(pos.east())) side = Direction.EAST;
            else return;
            adapter.neighborChanged(side);
        }
    }
}
