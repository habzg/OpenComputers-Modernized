package li.cil.oc.neoforge.common.tileentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import li.cil.oc.core.impl.common.tileentity.Case;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CaseGoggleTileEntity extends Case implements IHaveGoggleInformation {
    public CaseGoggleTileEntity(BlockPos pos, BlockState state, int tier) {
        super(pos, state, tier);
    }

    @Override
    public String inventoryName() {
        return Case.class.getSimpleName();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return GoggleDisplayHelper.appendWirelessInfo(tooltip, this);
    }
}
