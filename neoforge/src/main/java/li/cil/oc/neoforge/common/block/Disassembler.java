package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Disassembler extends SimpleBlock implements PowerAcceptor, GUI, StateAware {
    public Disassembler() {
        super();
    }

    @Override
    public int guiType() {
        return GuiType.Disassembler;
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        tooltip.addAll(Tooltip.get(getClass().getSimpleName(), (int) (Settings.get().disassemblerBreakChance * 100)));
    }

    @Override
    public double energyThroughput() {
        return Settings.get().disassemblerRate;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.Disassembler(pos, state);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.Disassembler disassembler) {
            disassembler.onNeighborChanged();
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        var tileType = li.cil.oc.neoforge.common.init.TileEntities.DISASSEMBLER.get();
        return type == tileType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.tileentity.Disassembler) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error in disassembler tick", e);
            }
        } : null;
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, Level world, @NotNull BlockPos pos) {
        return 0;
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }
}
