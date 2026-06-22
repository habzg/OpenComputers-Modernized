package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.item.traits.FileSystemLike;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.data.DriveData;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HardDiskDrive extends DelegateItem implements ItemTier, FileSystemLike {
    private final int tier;
    private int kiloBytes = -1;
    private int platterCount = -1;

    public HardDiskDrive(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    private void ensureInitialized() {
        if (kiloBytes == -1) {
            kiloBytes = Settings.get().hddSizes[tier];
            platterCount = Settings.get().hddPlatterCounts[tier];
        }
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public int kiloBytes() {
        ensureInitialized();
        return kiloBytes;
    }

    public int platterCount() {
        ensureInitialized();
        return platterCount;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        ensureInitialized();
        String name = super.getName(stack).getString();
        int kb = kiloBytes;
        String suffix = kb >= 1024 ? " (" + (kb / 1024) + "MB)" : " (" + kb + "KB)";
        return Component.literal(name + suffix);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        var cd = stack.get(DataComponents.CUSTOM_DATA);
        var tag = cd != null ? cd.copyTag() : new CompoundTag();
        if (tag.contains(Settings.namespace + "data")) {
            var data = tag.getCompound(Settings.namespace + "data");
            if (data.contains(Settings.namespace + "fs.label")) {
                tooltip.add(Component.literal(data.getString(Settings.namespace + "fs.label")));
            }
            if (flag.isAdvanced() && data.contains("fs")) {
                var fsNbt = data.getCompound("fs");
                if (fsNbt.contains("capacity.used")) {
                    long used = fsNbt.getLong("capacity.used");
                    tooltip.add(Component.translatable("tooltip.opencomputers.diskusage", used, kiloBytes() * 1024));
                }
            }
        }
        var driveData = new DriveData(stack);
        tooltip.add(Component.translatable(driveData.isUnmanaged ? "tooltip.opencomputers.diskmodeunmanaged" : "tooltip.opencomputers.diskmodemanaged"));
        if (driveData.isLocked()) {
            tooltip.add(Component.translatable("tooltip.opencomputers.disklocked", driveData.lockInfo));
        }
        if (tag.contains("oc:data")) {
            var data = tag.getCompound("oc:data");
            if (data.contains("node") && data.getCompound("node").contains("address")) {
                String addr = data.getCompound("node").getString("address");
                tooltip.add(Component.literal("§8" + addr.substring(0, Math.min(13, addr.length())) + "...§7"));
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (world.isClientSide) {
                li.cil.oc.neoforge.client.GuiHandler.openScreen(GuiType.Drive, 0, 0, 0);
            }
            player.swing(hand);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
}
