package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.common.block.traits.CustomDrops;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.item.data.RaidData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Raid extends SimpleBlock implements GUI, CustomDrops<li.cil.oc.core.impl.common.tileentity.Raid> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public Raid() {
        super();
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public int guiType() {
        return GuiType.Raid;
    }

    @Override
    public Class<li.cil.oc.core.impl.common.tileentity.Raid> getTileClass() {
        return li.cil.oc.core.impl.common.tileentity.Raid.class;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.Raid(pos, state);
    }

    @Override
    public void doCustomInit(li.cil.oc.core.impl.common.tileentity.Raid tileEntity, LivingEntity player, ItemStack stack) {
        var level = tileEntity.getLevel();
        if (level == null) return;
        if (!level.isClientSide) {
            var data = new RaidData(stack);
            int count = Math.min(data.disks.size(), tileEntity.getContainerSize());
            for (int i = 0; i < count; i++) {
                tileEntity.updateItems(i, data.disks.get(i));
            }
            if (data.label != null) {
                tileEntity.label.setLabel(data.label);
            }
            if (!data.filesystem.isEmpty()) {
                tileEntity.tryCreateRaid(data.filesystem.getCompound("node").getString("address"));
                if (tileEntity.filesystem != null) {
                    var fs = tileEntity.filesystem;
                    fs.load(data.filesystem, level.registryAccess());
                }
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, Level world, @NotNull BlockPos pos) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.Raid raid) {
            int occupied = 0;
            var items = raid.items();
            for (int i = 0; i < items.length && i < raid.getContainerSize(); i++) {
                if (items[i] != null && !items[i].isEmpty()) occupied++;
            }
            if (occupied >= raid.getContainerSize()) return 15;
            return 0;
        }
        return 0;
    }

    @Override
    public void doCustomDrops(li.cil.oc.core.impl.common.tileentity.Raid tileEntity, Player player, boolean willHarvest) {
        var stack = createItemStack();
        boolean hasItems = false;
        for (var item : tileEntity.items()) {
            if (item != null && !item.isEmpty()) {
                hasItems = true;
                break;
            }
        }
        var level = tileEntity.getLevel();
        if (level == null) return;
        if (hasItems) {
            var data = new RaidData();
            data.disks.addAll(Arrays.asList(tileEntity.items()));
            if (tileEntity.filesystem != null) {
                var fs = tileEntity.filesystem;
                fs.save(data.filesystem, level.registryAccess());
            }
            data.label = tileEntity.label.getLabel();
            data.save(stack, level.registryAccess());
        }
        Block.popResource(level, tileEntity.getBlockPos(), stack);
    }
}
