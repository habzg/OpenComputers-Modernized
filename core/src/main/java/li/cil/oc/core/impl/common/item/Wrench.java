package li.cil.oc.core.impl.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class Wrench extends DelegateItem implements li.cil.oc.api.internal.Wrench {

    public Wrench(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean useWrenchOnBlock(Player player, Level world, BlockPos pos, boolean simulate) {
        if (!simulate) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }
}
