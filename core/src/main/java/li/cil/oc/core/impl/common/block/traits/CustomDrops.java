package li.cil.oc.core.impl.common.block.traits;

import li.cil.oc.core.impl.common.block.AbstractBlock;
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
    default boolean removedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest, AbstractBlock self) {
        if (!world.isClientSide) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te != null && getTileClass().isInstance(te)) {
                doCustomDrops(getTileClass().cast(te), player, willHarvest);
            }
        }
        return self.onDestroyedByPlayer(state, world, pos, player, willHarvest, world.getFluidState(pos));
    }


    @SuppressWarnings("unused")
    default void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack, AbstractBlock self) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te != null && getTileClass().isInstance(te)) {
            doCustomInit(getTileClass().cast(te), placer, stack);
        }
    }

    Class<T> getTileClass();

    @SuppressWarnings("unused")
    default void doCustomInit(T tileEntity, LivingEntity player, ItemStack stack) {
    }

    @SuppressWarnings("unused")
    default void doCustomDrops(T tileEntity, Player player, boolean willHarvest) {
    }
}
