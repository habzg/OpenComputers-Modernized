package li.cil.oc.core.impl.common.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.util.ContainerProviderDelegate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class Server extends DelegateItem implements ItemTier {
    public final int tier;

    public Server(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var header = li.cil.oc.core.impl.util.Tooltip.extended("server.Components");
        if (!header.isEmpty()) {
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && !customData.isEmpty()) {
                var tag = customData.copyTag();
                if (tag.contains("oc:data")) {
                    var data = tag.getCompound("oc:data");
                    var items = data.getList("oc:items", net.minecraft.nbt.Tag.TAG_COMPOUND);
                    if (!items.isEmpty()) {
                        tooltip.add(Component.literal(""));
                        tooltip.addAll(header);
                        Map<String, Integer> counts = new LinkedHashMap<>();
                        for (int i = 0; i < items.size(); i++) {
                            var itemTag = items.getCompound(i);
                            if (itemTag.contains("item")) {
                                var stackTag = itemTag.getCompound("item");
                                var registries = context.registries();
                                if (registries == null) continue;
                                var itemStack = ItemStack.parse(registries, stackTag).orElse(ItemStack.EMPTY);
                                if (!itemStack.isEmpty()) {
                                    var name = itemStack.getHoverName().getString();
                                    counts.merge(name, 1, Integer::sum);
                                }
                            }
                        }
                        for (var entry : counts.entrySet()) {
                            tooltip.add(Component.literal("§7- " + entry.getValue() + "x " + entry.getKey()));
                        }
                    }
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ContainerProviderDelegate.get().openMenu(player, GuiType.Server, level, 0, 0, 0);
            }
            player.swing(hand);
        }
        return InteractionResultHolder.success(stack);
    }
}
