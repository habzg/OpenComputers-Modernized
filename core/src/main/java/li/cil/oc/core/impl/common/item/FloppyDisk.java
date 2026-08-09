package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.FileSystemLike;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.DriveData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FloppyDisk extends DelegateItem implements FileSystemLike {
    private int kiloBytes = -1;

    private static Runnable openDriveScreen = () -> {};

    public static void setOpenDriveScreen(Runnable runnable) {
        openDriveScreen = runnable;
    }

    public FloppyDisk(Properties properties) {
        super(properties);
    }

    private void ensureInitialized() {
        if (kiloBytes == -1) {
            kiloBytes = OCSettings.get().floppySize;
        }
    }

    @Override
    public String unlocalizedName() {
        return "FloppyDisk";
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        var cd = stack.get(DataComponents.CUSTOM_DATA);
        var tag = cd != null ? cd.copyTag() : new CompoundTag();
        if (tag.contains(OCSettings.namespace + "data")) {
            var data = tag.getCompound(OCSettings.namespace + "data");
            if (data.contains(OCSettings.namespace + "fs.label")) {
                tooltip.add(Component.literal(data.getString(OCSettings.namespace + "fs.label")));
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
                tooltip.add(Component.literal("§8" + addr.substring(0, Math.min(13, addr.length())) + ".§7"));
            }
        }
    }

    @Override
    public int kiloBytes() {
        ensureInitialized();
        return kiloBytes;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            var cd = stack.get(DataComponents.CUSTOM_DATA);
            boolean isLootDisk = cd != null && !cd.isEmpty() && cd.copyTag().contains(OCSettings.namespace + "lootFactory");
            if (!isLootDisk) {
                if (world.isClientSide) {
                    openDriveScreen.run();
                }
                player.swing(hand);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

}
