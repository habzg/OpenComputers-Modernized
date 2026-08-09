package li.cil.oc.core.impl.common.item.traits;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface CPULike {
    @SuppressWarnings("unused")
    int cpuTier();

    @SuppressWarnings("unused")
    int cpuTierForComponents();

    default InteractionResultHolder<ItemStack> useCPU(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                var driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver instanceof li.cil.oc.api.driver.item.MutableProcessor mutable) {
                    var archList = mutable.allArchitectures();
                    if (!archList.isEmpty()) {
                        var architectures = java.util.List.copyOf(archList);
                        int currentIndex = architectures.indexOf(mutable.architecture(stack));
                        int newIndex = (currentIndex + 1) % architectures.size();
                        var archClass = architectures.get(newIndex);
                        String archName = li.cil.oc.api.Machine.getArchitectureName(archClass);
                        mutable.setArchitecture(stack, archClass);
                        player.sendSystemMessage(Component.translatable("tooltip.opencomputers.cpu.architecture", archName));
                    }
                    player.swing(hand);
                }
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}
