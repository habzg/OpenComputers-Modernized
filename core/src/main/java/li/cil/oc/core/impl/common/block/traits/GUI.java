package li.cil.oc.core.impl.common.block.traits;

import li.cil.oc.core.impl.util.ContainerProviderDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface GUI {
    int guiType();

    default boolean openGuiFor(Level world, BlockPos pos, Player player, Direction ignoredSide, float ignoredHitX, float ignoredHitY, float ignoredHitZ) {
        if (!player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                int gt = guiType();
                player.openMenu(ContainerProviderDelegate.get().getContainerProvider(gt, world, pos.getX(), pos.getY(), pos.getZ()), (net.minecraft.network.RegistryFriendlyByteBuf buf) -> {
                    buf.writeInt(gt);
                    buf.writeInt(pos.getX());
                    buf.writeInt(pos.getY());
                    buf.writeInt(pos.getZ());
                });
            }
            return true;
        }
        return false;
    }
}
