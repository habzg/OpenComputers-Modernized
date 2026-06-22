package li.cil.oc.neoforge.common.item;

import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;


public class TexturePicker extends DelegateItem {

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
                var mc = Minecraft.getInstance();
                var model = mc.getBlockRenderer().getBlockModel(state);
                var sprite = model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
                var player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(Component.literal(sprite.contents().name().toString()), true);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
