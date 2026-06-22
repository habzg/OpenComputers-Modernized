package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class Assembler extends SimpleBlock implements PowerAcceptor, GUI, StateAware {
    public Assembler() {
        super();
    }

    @Override
    public int guiType() {
        return GuiType.Assembler;
    }

    @Override
    public double energyThroughput() {
        return Settings.get().assemblerRate;
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
        return new li.cil.oc.core.impl.common.tileentity.Assembler(pos, state);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.Assembler assembler) {
            assembler.onNeighborChanged();
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        var assemblerType = li.cil.oc.neoforge.common.init.TileEntities.ASSEMBLER.get();
        return type == assemblerType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.tileentity.Assembler) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error in assembler tick", e);
            }
        } : null;
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }
}
