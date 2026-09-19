package li.cil.oc.fabric.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Rack extends li.cil.oc.core.impl.common.block.Rack {
  @SuppressWarnings("unused")
  public Rack(Object ignored) {
    super();
  }

  @Override
  public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
    return new li.cil.oc.fabric.common.blockentity.RackFabric(pos, state);
  }
}
