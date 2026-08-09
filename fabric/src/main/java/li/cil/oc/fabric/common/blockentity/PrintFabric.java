package li.cil.oc.fabric.common.blockentity;

import java.util.List;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.common.blockentity.Print;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class PrintFabric extends Print implements RenderDataBlockEntity {

    public record PrintRenderData(List<PrintData.Shape> shapes, Direction facing) {
    }

    public PrintFabric(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void updateBounds() {
        super.updateBounds();
        var level = getLevel();
        if (level != null) {
            var state = getBlockState();
            int clamped = Mth.clamp(data.lightLevel, 0, 15);
            if (state.hasProperty(li.cil.oc.core.impl.common.block.AbstractBlock.LIGHT_LEVEL) && state.getValue(li.cil.oc.core.impl.common.block.AbstractBlock.LIGHT_LEVEL) != clamped) {
                level.setBlock(worldPosition, state.setValue(li.cil.oc.core.impl.common.block.AbstractBlock.LIGHT_LEVEL, clamped), 2);
            }
        }
    }

    @Override
    public Object getRenderData() {
        var shapes = this.state ? data.stateOn : data.stateOff;
        return new PrintRenderData(List.copyOf(shapes), facing());
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        if (getLevel() != null) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
