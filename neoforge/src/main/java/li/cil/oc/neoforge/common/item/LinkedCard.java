package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LinkedCard extends DelegateItem implements ItemTier {
    public LinkedCard(Item.Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains(Settings.namespace + "data")) {
                CompoundTag data = tag.getCompound(Settings.namespace + "data");
                if (data.contains(Settings.namespace + "tunnel")) {
                    String channel = data.getString(Settings.namespace + "tunnel");
                    if (channel.length() > 13) {
                        tooltip.addAll(Tooltip.get(unlocalizedName() + "_Channel", channel.substring(0, 13) + "..."));
                    } else {
                        tooltip.addAll(Tooltip.get(unlocalizedName() + "_Channel", channel));
                    }
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
