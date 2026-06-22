package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;


public class NetSplitter extends RedstoneAware {
    public NetSplitter() {
        super();
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.NetSplitter(pos, state);
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (Wrench.holdsApplicableWrench(player, BlockPosition.apply(pos.getX(), pos.getY(), pos.getZ(), world))) {
            Direction sideToToggle = player.isShiftKeyDown() ? side.getOpposite() : side;
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.tileentity.NetSplitter splitter) {
                if (!world.isClientSide) {
                    boolean oldValue = splitter.isSideOpen(sideToToggle);
                    splitter.setSideOpen(sideToToggle, !oldValue);
                }
                return true;
            }
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

}
