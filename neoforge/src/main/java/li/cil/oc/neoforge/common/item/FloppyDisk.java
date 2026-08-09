package li.cil.oc.neoforge.common.item;

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
    public boolean doesSneakBypassUse(@NotNull ItemStack ignoredStack, @NotNull LevelReader ignoredLevel, @NotNull BlockPos ignoredPos, @NotNull Player ignoredPlayer) {
        return true;
    }

}
