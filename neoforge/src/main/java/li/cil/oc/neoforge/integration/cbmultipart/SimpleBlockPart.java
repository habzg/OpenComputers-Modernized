package li.cil.oc.neoforge.integration.cbmultipart;

import codechicken.multipart.api.part.BaseMultipart;
import codechicken.multipart.api.part.ModelRenderPart;
import codechicken.multipart.util.PartRayTraceResult;
import li.cil.oc.neoforge.common.block.SimpleBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public abstract class SimpleBlockPart extends BaseMultipart implements ModelRenderPart {
    public abstract SimpleBlock simpleBlock();

    @Override
    public @NotNull BlockState getCurrentState() {
        return simpleBlock().defaultBlockState();
    }

    @Override
    public @NotNull ItemStack getCloneStack(@NotNull PartRayTraceResult hit, @NotNull Player player) {
        return new ItemStack(simpleBlock());
    }

    @Override
    public @NotNull Iterable<ItemStack> getDrops() {
        return java.util.Collections.singletonList(new ItemStack(simpleBlock()));
    }

    @Override
    public float getExplosionResistance(net.minecraft.world.level.@NotNull Explosion explosion) {
        return simpleBlock().getExplosionResistance(getCurrentState(), level(), pos(), explosion);
    }
}
