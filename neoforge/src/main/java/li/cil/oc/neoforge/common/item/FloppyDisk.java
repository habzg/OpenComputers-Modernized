package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.common.item.SneakBypass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;

public class FloppyDisk extends li.cil.oc.core.impl.common.item.FloppyDisk {
  public FloppyDisk(Properties properties) {
    super(properties);
  }

  @Override
  public boolean doesSneakBypassUse(@NotNull ItemStack stack, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
    return SneakBypass.bypasses(stack, level, pos);
  }

}
