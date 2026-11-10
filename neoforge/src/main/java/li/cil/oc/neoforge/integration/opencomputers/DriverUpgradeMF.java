package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;


@SuppressWarnings("unused")
public final class DriverUpgradeMF extends Item implements HostAware {
  @Override
  public boolean worksWith(ItemStack stack) {
    return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.MFU));
  }

  @Override
  public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
    return INSTANCE.worksWith(stack) && isAdapter(host);
  }

  @Override
  public String slot(ItemStack stack) {
    return Slot.Upgrade;
  }

  @Override
  public int tier(ItemStack stack) {
    return Tier.Three;
  }

  @Override
  public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
    if (host.level() != null && !host.level().isClientSide()) {
      var customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData != null && !customData.isEmpty()) {
        var tag = customData.copyTag();
        if (tag.contains(li.cil.oc.core.impl.OCSettings.namespace + "coord", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
          var coords = tag.getIntArray(li.cil.oc.core.impl.OCSettings.namespace + "coord");
          if (coords.length == 4) {
            var dimension = net.minecraft.resources.ResourceLocation.tryParse(tag.getString(li.cil.oc.core.impl.OCSettings.namespace + "dimension"));
            if (dimension != null) {
              var server = host.level().getServer();
              if (server != null) {
                var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
                if (level != null) {
                  return new li.cil.oc.neoforge.server.component.UpgradeMF(
                    host, BlockPosition.apply(coords[0], coords[1], coords[2], level), Direction.from3DDataValue(coords[3]));
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static final DriverUpgradeMF INSTANCE = new DriverUpgradeMF();

  public static final class Provider implements EnvironmentProvider {
    @Override
    public Class<?> getEnvironment(ItemStack stack) {
      if (INSTANCE.worksWith(stack)) {
        return li.cil.oc.neoforge.server.component.UpgradeMF.class;
      }
      return null;
    }
  }
}
