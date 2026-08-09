package li.cil.oc.core.impl.common.item;

import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class TexturePicker extends DelegateItem {
    private static Function<BlockState, String> particleIconProvider = (state) -> "";

    public static void setParticleIconProvider(Function<BlockState, String> provider) {
        particleIconProvider = provider;
    }

    @SuppressWarnings("unused")
    public TexturePicker(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        if (!state.isAir()) {
            if (world.isClientSide) {
                var player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(Component.literal(particleIconProvider.apply(state)), true);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
