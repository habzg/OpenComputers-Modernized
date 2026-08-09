package li.cil.oc.neoforge.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;

public class AbstractItem extends li.cil.oc.core.impl.common.item.AbstractItem {
    public AbstractItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean doesSneakBypassUse(@NotNull ItemStack stack, LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.DiskDrive) return true;
        return super.doesSneakBypassUse(stack, level, pos, player);
    }
}
