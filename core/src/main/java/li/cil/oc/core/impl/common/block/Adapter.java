package li.cil.oc.core.impl.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Adapter extends SimpleBlock implements GUI {
    public static BlockEntityType<?> TYPE;

    public Adapter(BlockEntityType<?> blockType) {
        super();
        TYPE = blockType;
    }

    public Adapter() {
        super();
    }

    @Override
    public int guiType() {
        return GuiType.Adapter;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Adapter(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == TYPE ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.blockentity.Adapter) te).updateEntity();
            } catch (Exception e) {
                Log.get().warn("Error in adapter tick", e);
            }
        } : null;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Adapter adapter) {
            adapter.neighborChanged();
        }
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (Wrench.holdsApplicableWrench(player, BlockPosition.apply(pos.getX(), pos.getY(), pos.getZ(), world))) {
            Direction sideToToggle = player.isShiftKeyDown() ? side.getOpposite() : side;
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.blockentity.Adapter adapter) {
                if (!world.isClientSide) {
                    boolean oldValue = adapter.openSides()[sideToToggle.ordinal()];
                    adapter.setSideOpen(sideToToggle, !oldValue);
                }
                return true;
            }
            return false;
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }
}
