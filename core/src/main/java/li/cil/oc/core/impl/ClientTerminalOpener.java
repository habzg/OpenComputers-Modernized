package li.cil.oc.core.impl;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ClientTerminalOpener {
  void handle(Player player, ItemStack stack, String key, String address);
}
