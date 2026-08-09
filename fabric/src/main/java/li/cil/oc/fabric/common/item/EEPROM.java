package li.cil.oc.fabric.common.item;

import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

public class EEPROM extends SimpleItem {
    @SuppressWarnings("unused")
    public EEPROM() {
        super();
    }

    @Override
    public @NotNull String getDescriptionId(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains(OCSettings.namespace + "data")) {
                CompoundTag data = tag.getCompound(OCSettings.namespace + "data");
                if (data.contains(OCSettings.namespace + "label")) {
                    return data.getString(OCSettings.namespace + "label");
                }
            }
        }
        return super.getDescriptionId(stack);
    }
}
