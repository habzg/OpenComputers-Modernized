package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.neoforge.integration.Mods;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;

public class DiskDrive extends SimpleBlock implements li.cil.oc.core.impl.common.block.traits.GUI {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DiskDrive() {
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public int guiType() {
        return GuiType.DiskDrive;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.DiskDrive(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, Level world, @NotNull BlockPos pos) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.DiskDrive drive && !drive.getItem(0).isEmpty()) {
            return 15;
        }
        return 0;
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            var te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.tileentity.DiskDrive drive) {
                boolean isDiskInDrive = !drive.getItem(0).isEmpty();
                var heldItem = player.getMainHandItem();
                boolean isHoldingDisk = drive.canPlaceItem(0, heldItem);
                if (isDiskInDrive) {
                    if (!world.isClientSide) {
                        drive.dropSlot(0, 1, drive.facing());
                    }
                }
                if (isHoldingDisk) {
                    drive.setItem(0, heldItem.split(1));
                }
                return isDiskInDrive || isHoldingDisk;
            }
            return false;
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public void tooltipTail(int metadata, ItemStack stack, Player player, java.util.List<net.minecraft.network.chat.Component> tooltip, boolean advanced) {
        super.tooltipTail(metadata, stack, player, tooltip, advanced);
        if (Mods.ComputerCraft.isAvailable()) {
            tooltip.addAll(Tooltip.get(getClass().getSimpleName() + ".CC"));
        }
    }
}
