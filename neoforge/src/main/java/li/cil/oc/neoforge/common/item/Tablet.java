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
        currentBlockPos = new li.cil.oc.core.impl.util.BlockPosition(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ(), context.getLevel());
        currentSide = context.getClickedFace();
        currentHitX = (float) (context.getClickLocation().x - context.getClickedPos().getX());
        currentHitY = (float) (context.getClickLocation().y - context.getClickedPos().getY());
        currentHitZ = (float) (context.getClickLocation().z - context.getClickedPos().getZ());
        return InteractionResult.PASS;
    }

    @Override
    public boolean doesSneakBypassUse(@NotNull ItemStack stack, LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.DiskDrive) return true;
        return super.doesSneakBypassUse(stack, level, pos, player);
    }
}
