package li.cil.oc.fabric.common.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Tablet extends li.cil.oc.core.impl.common.item.Tablet {
  public Tablet(Properties properties) {
    super(properties);
  }

  @Override
  public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
    return ItemStack.matches(oldStack, newStack);
  }
}
