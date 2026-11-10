package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class EEPROM extends SimpleItem {
  public EEPROM() {
    super();
  }

  @Override
  public @NotNull Component getName(@NotNull ItemStack stack) {
    var label = label(stack);
    return label != null ? Component.literal(label) : super.getName(stack);
  }

  @Override
  public @NotNull String getDescriptionId(@NotNull ItemStack stack) {
    var label = label(stack);
    return label != null ? label : super.getDescriptionId(stack);
  }

  private static @Nullable String label(ItemStack stack) {
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
    return null;
  }
}
