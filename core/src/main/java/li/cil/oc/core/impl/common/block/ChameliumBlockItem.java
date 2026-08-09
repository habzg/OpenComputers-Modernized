package li.cil.oc.core.impl.common.block;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ChameliumBlockItem extends BlockItem {
    @SuppressWarnings("unused")
    public ChameliumBlockItem(Block block) {
        super(block, new Properties().stacksTo(64));
    }

    @Override
    public @NotNull String getDescriptionId() {
        return getBlock().getDescriptionId();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var block = getBlock();
        if (block instanceof AbstractBlock base) {
            base.addInformation(stack.getDamageValue(), stack, null, tooltip, flag.isAdvanced());
        }
    }
}
