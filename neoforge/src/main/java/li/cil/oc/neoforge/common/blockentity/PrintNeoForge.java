package li.cil.oc.neoforge.common.blockentity;

import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.common.blockentity.Print;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        requestModelDataUpdate();
    }
}
