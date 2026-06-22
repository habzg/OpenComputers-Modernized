package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.impl.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AbstractItem extends Item {
    public AbstractItem(Properties properties) {
        super(properties);
    }

    public ItemStack createItemStack(int amount) {
        return new ItemStack(this, amount);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return false;
    }

    @Override
    public boolean doesSneakBypassUse(@NotNull ItemStack stack, LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.DiskDrive) return true;
        return super.doesSneakBypassUse(stack, level, pos, player);
    }

    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
    }

    public int tierFromDriver(ItemStack stack) {
        var driver = li.cil.oc.api.API.driver.driverFor(stack);
        if (driver instanceof li.cil.oc.api.driver.Item itemDriver) return itemDriver.tier(stack);
        return 0;
    }

    @SuppressWarnings("unused")
    public Rarity getRarity(ItemStack stack) {
        return li.cil.oc.core.impl.util.Rarity.byTier(tierFromDriver(stack));
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
        if (tag.contains(Settings.namespace + "data")) {
            var data = tag.getCompound(Settings.namespace + "data");
            if (data.contains("node") && data.getCompound("node").contains("address")) {
                String addr = data.getCompound("node").getString("address");
                tooltip.add(Component.literal("§8" + addr.substring(0, Math.min(13, addr.length())) + "...§7"));
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
