package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.common.block.traits.GUI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class Printer extends SimpleBlock implements GUI, StateAware {
    public Printer() {
        super();
    }

    @Override
    public int guiType() {
        return GuiType.Printer;
    }

    @Override
    public boolean skipRendering(@NotNull BlockState state, @NotNull BlockState adjacentState, @NotNull Direction side) {
        return false;
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.Printer(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        var tileType = li.cil.oc.neoforge.common.init.TileEntities.PRINTER.get();
        return type == tileType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.tileentity.Printer) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error in printer tick", e);
            }
        } : null;
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }
}
