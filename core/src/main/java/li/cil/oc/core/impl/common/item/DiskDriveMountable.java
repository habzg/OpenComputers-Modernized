package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.util.ContainerProviderDelegate;
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
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ContainerProviderDelegate.get().openMenu(player, GuiType.DiskDriveMountable, world, 0, 0, 0);
        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }
}
