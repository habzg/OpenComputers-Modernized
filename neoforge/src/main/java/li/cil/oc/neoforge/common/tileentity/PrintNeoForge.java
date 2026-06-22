package li.cil.oc.neoforge.common.tileentity;

import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.common.tileentity.Print;
import li.cil.oc.core.impl.common.tileentity.traits.RedstoneAware;
import li.cil.oc.core.impl.util.ExtendedAABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PrintNeoForge extends Print {
    public static final ModelProperty<PrintData> PRINT_DATA = new ModelProperty<>();
    public static final ModelProperty<Direction> FACING = new ModelProperty<>();
    public static final ModelProperty<Boolean> STATE = new ModelProperty<>();

    public PrintNeoForge(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        requestModelDataUpdate();
    }

    @Override
    public @NotNull ModelData getModelData() {
        return ModelData.builder()
                .with(PRINT_DATA, data)
                .with(FACING, facing())
                .with(STATE, state)
                .build();
    }

    public VoxelShape shapeOff = Shapes.create(ExtendedAABB.unitBounds());
    public VoxelShape shapeOn = Shapes.create(ExtendedAABB.unitBounds());

    @Override
    public void updateBounds() {
        super.updateBounds();
        shapeOff = buildShape(data.stateOff, facing());
        shapeOn = buildShape(data.stateOn, facing());
    }

    private static VoxelShape buildShape(Set<PrintData.Shape> shapes, Direction dir) {
        if (shapes.isEmpty()) {
            return Shapes.create(ExtendedAABB.unitBounds());
        }
        VoxelShape result = Shapes.empty();
        for (var shape : shapes) {
            if (shape.texture() == null || shape.texture().isEmpty()) continue;
            var bounds = ExtendedAABB.rotateTowards(shape.bounds(), dir);
            result = Shapes.or(result, Shapes.create(bounds));
        }
        return result.optimize();
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        requestModelDataUpdate();
    }

    @Override
    public void toggleState() {
        super.toggleState();
        if (getLevel() != null && !getLevel().isClientSide) {
            setChanged();
        }
    }

    @Override
    public void onRedstoneInputChanged(RedstoneAware.RedstoneChangedEventArgs args) {
        super.onRedstoneInputChanged(args);
        if (getLevel() != null && !getLevel().isClientSide) {
            setChanged();
        }
    }
}
