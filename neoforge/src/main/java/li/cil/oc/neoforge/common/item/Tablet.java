package li.cil.oc.neoforge.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;

public class Tablet extends li.cil.oc.core.impl.common.item.Tablet {
  public Tablet(Properties properties) {
    super(properties);
  }

  @Override
  public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack ignoredStack, @NotNull UseOnContext context) {
    return useFirst(context);
  }

  @Override
  public boolean doesSneakBypassUse(@NotNull ItemStack stack, LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
    if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.DiskDrive) return true;
    return super.doesSneakBypassUse(stack, level, pos, player);
  }

  @Override
  public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
    return slotChanged || oldStack.getItem() != newStack.getItem();
  }
}
