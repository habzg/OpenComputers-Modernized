package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.traits.SimpleItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;

public class EEPROM extends SimpleItem {
    public EEPROM() {
        super();
    }

    @Override
    public boolean doesSneakBypassUse(@NotNull ItemStack stack, LevelReader level, net.minecraft.core.@NotNull BlockPos pos, net.minecraft.world.entity.player.@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull String getDescriptionId(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains(Settings.namespace + "data")) {
                CompoundTag data = tag.getCompound(Settings.namespace + "data");
                if (data.contains(Settings.namespace + "label")) {
                    return data.getString(Settings.namespace + "label");
                }
            }
        }
        return super.getDescriptionId(stack);
    }
}
