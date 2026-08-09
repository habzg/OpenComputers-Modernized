package li.cil.oc.core.impl.common.block;

import java.util.List;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.CraftHandler;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.common.item.data.RobotData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class Item extends BlockItem {
    public Item(Block block) {
        super(block, new Properties().stacksTo(64).rarity(block instanceof AbstractBlock base ? base.rarity(ItemStack.EMPTY) : net.minecraft.world.item.Rarity.COMMON));
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        CraftHandler.onItemCrafted(stack, level, player);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Block block = getBlock();
        if (block instanceof AbstractBlock base) {
            Player player = Minecraft.getInstance().player;
            base.addInformation(stack.getDamageValue(), stack, player, tooltip, flag.isAdvanced());
        }
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var item = li.cil.oc.api.Items.get(stack);
        if (item == li.cil.oc.api.Items.get(Constants.BlockName.Print) ||
                item == li.cil.oc.api.Items.get(Constants.BlockName.BeaconBasePrint)) {
            PrintData data = new PrintData(stack);
            if (data.label != null) return Component.literal(data.label);
            return Component.translatable("block.opencomputers.print");
        }
        return super.getName(stack);
    }

    @Override
    public @NotNull InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        boolean needsCopying = player != null && player.getAbilities().instabuild &&
                li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName.Robot);
        if (needsCopying) {
            ItemStack copy = new RobotData(stack).copyItemStack();
            context.getPlayer().setItemInHand(context.getHand(), copy);
            InteractionResult result = super.place(context);
            context.getPlayer().setItemInHand(context.getHand(), stack);
            return result;
        }
        return super.place(context);
    }
}
