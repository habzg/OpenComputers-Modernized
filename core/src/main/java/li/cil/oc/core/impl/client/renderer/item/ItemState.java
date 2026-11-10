package li.cil.oc.core.impl.client.renderer.item;

import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public enum ItemState {
  DEFAULT,
  ON,
  OFF;

  public static ItemState tablet(ItemStack stack) {
    CompoundTag tag = customData(stack);
    if (tag == null) return DEFAULT;
    String key = OCSettings.namespace + "isRunning";
    if (!tag.contains(key)) return DEFAULT;
    return tag.getBoolean(key) ? ON : OFF;
  }

  public static ItemState terminal(ItemStack stack) {
    CompoundTag tag = customData(stack);
    if (tag != null && tag.contains(OCSettings.namespace + "server")) return ON;
    return OFF;
  }

  private static CompoundTag customData(ItemStack stack) {
    var data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null || data.isEmpty() ? null : data.copyTag();
  }
}
