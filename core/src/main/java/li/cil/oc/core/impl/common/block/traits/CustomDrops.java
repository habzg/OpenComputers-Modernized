package li.cil.oc.core.impl.common.block.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface CustomDrops<T extends BlockEntity> {

    @SuppressWarnings("unused")
    default void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te != null && getBlockClass().isInstance(te)) {
            doCustomInit(getBlockClass().cast(te), placer, stack);
        }
    }

    Class<T> getBlockClass();

    @SuppressWarnings("unused")
    default void doCustomInit(T blockEntity, LivingEntity player, ItemStack stack) {
    }

    @SuppressWarnings("unused")
    default void doCustomDrops(T blockEntity, Player player, boolean willHarvest) {
    }
}
