package li.cil.oc.fabric.common.block;

import java.util.List;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class SimpleBlock extends li.cil.oc.core.impl.common.block.AbstractBlock {
  public SimpleBlock() {
    super();
  }

  public SimpleBlock(BlockBehaviour.Properties properties) {
    super(properties);
  }

  @Override
  protected void tooltipBody(int metadata, ItemStack stack, List<Component> tooltip, boolean advanced) {
    tooltip.addAll(Tooltip.get(getClass().getSimpleName()));
  }

  @Override
  public void tooltipTail(int metadata, ItemStack stack, List<Component> tooltip, boolean advanced) {
    if (this instanceof PowerAcceptor acceptor) {
      tooltip.addAll(Tooltip.extended("PowerAcceptor", (int) acceptor.energyThroughput()));
    }
  }
}
