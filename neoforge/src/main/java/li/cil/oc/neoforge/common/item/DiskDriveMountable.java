package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class DiskDriveMountable extends DelegateItem {
    public DiskDriveMountable(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.openMenu(OpenComputers.getContainerProvider(GuiType.DiskDriveMountable, world, 0, 0, 0),
                (net.minecraft.network.RegistryFriendlyByteBuf buf) -> {
                    buf.writeInt(GuiType.DiskDriveMountable);
                    buf.writeInt(0);
                    buf.writeInt(0);
                    buf.writeInt(0);
                });
        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }
}
