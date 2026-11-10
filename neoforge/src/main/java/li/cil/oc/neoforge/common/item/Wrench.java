package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.common.item.SneakBypass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;

public class Wrench extends li.cil.oc.core.impl.common.item.Wrench {
  public Wrench(Properties properties) {
    super(properties);
  }

  @Override
  public boolean doesSneakBypassUse(@NotNull ItemStack stack, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
    return SneakBypass.bypasses(stack, level, pos);
  }

  @Override
  public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack ignoredStack, @NotNull UseOnContext context) {
    return useFirst(context);
  }
}
