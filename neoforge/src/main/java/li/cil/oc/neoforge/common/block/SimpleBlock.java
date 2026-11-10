package li.cil.oc.neoforge.common.block;

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
  protected void tooltipBody(int ignoredMetadata, ItemStack ignoredStack, List<Component> tooltip, boolean ignoredAdvanced) {
    tooltip.addAll(Tooltip.get(getClass().getSimpleName()));
  }

  @Override
  public void tooltipTail(int ignoredMetadata, ItemStack ignoredStack, List<Component> tooltip, boolean ignoredAdvanced) {
    if (this instanceof PowerAcceptor acceptor) {
      tooltip.addAll(Tooltip.extended("PowerAcceptor", (int) acceptor.energyThroughput()));
    }
  }
}
