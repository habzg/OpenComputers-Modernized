package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.TabletData;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;


@SuppressWarnings("unused")
public final class DriverTablet extends Item {
  @Override
  public boolean worksWith(ItemStack stack) {
    return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.Tablet));
  }

  @Override
  public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
    li.cil.oc.core.impl.util.TabletCache.get().invalidate(li.cil.oc.core.impl.util.TabletCache.getOrCreateId(stack));
    var level = host.level();
    if (level != null && level.isClientSide()) return null;
    TabletData data = new TabletData(stack);
    for (int i = 0; i < data.items.size(); i++) {
      ItemStack fs = data.items.get(i);
      if (fs != null && !fs.isEmpty()) {
        if (new DriverFileSystem().worksWith(fs)) {
          li.cil.oc.api.network.ManagedEnvironment environment = new DriverFileSystem().createEnvironment(fs, host);
          if (environment != null && environment.node() instanceof Component component) {
            component.setVisibility(Visibility.Network);
            if (level != null) {
              li.cil.oc.core.impl.common.inventory.ComponentInventory.saveComponentData(environment, fs, level.registryAccess());
              data.items.set(i, fs);
              data.save(stack, level.registryAccess());
            }
            return environment;
          }
        }
      }
    }
    return null;
  }

  @Override
  public String slot(ItemStack stack) {
    return Slot.Tablet;
  }

  public CompoundTag dataTag(ItemStack stack) {
    TabletData data = new TabletData(stack);
    for (ItemStack item : data.items) {
      if (item != null && !item.isEmpty() && new DriverFileSystem().worksWith(item)) {
        var customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
          var tag = customData.copyTag();
          if (tag.contains(OCSettings.namespace + "data")) {
            return tag.getCompound(OCSettings.namespace + "data");
          }
        }
        return new CompoundTag();
      }
    }
    return new CompoundTag();
  }
}
