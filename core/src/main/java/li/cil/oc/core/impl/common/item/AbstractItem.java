package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.api.driver.DriverItem;import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.CraftHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class AbstractItem extends Item {
    public AbstractItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        CraftHandler.onItemCrafted(stack, level, player);
    }

    public ItemStack createItemStack(int amount) {
        return new ItemStack(this, amount);
    }

    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
    }

    public int tierFromDriver(ItemStack stack) {
        var driver = li.cil.oc.api.API.driver.driverFor(stack);
        if (driver instanceof DriverItem itemDriver) return itemDriver.tier(stack);
        return 0;
    }

    @SuppressWarnings("unused")
    public void tooltipBody(ItemStack stack, List<Component> tooltip) {
    }

    @SuppressWarnings("unused")
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltipBody(stack, tooltip);
        var cd = stack.get(DataComponents.CUSTOM_DATA);
        var tag = cd != null ? cd.copyTag() : new CompoundTag();
        if (tag.contains(OCSettings.namespace + "data")) {
            var data = tag.getCompound(OCSettings.namespace + "data");
            if (data.contains("node") && data.getCompound("node").contains("address")) {
                String addr = data.getCompound("node").getString("address");
                tooltip.add(Component.literal("§8" + addr.substring(0, Math.min(13, addr.length())) + ".§7"));
            }
        }
        tooltipExtended(stack, tooltip);
        if (this instanceof li.cil.oc.core.common.item.traits.ItemTier tiered) {
            if (flag.isAdvanced()) {
                tooltip.add(Component.literal(Component.translatable("tooltip.opencomputers.tier", String.valueOf(tierFromDriver(stack) + 1)).getString().replaceAll("\\[nl]", "\n").trim()));
            }
        }
    }
}
