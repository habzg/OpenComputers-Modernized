package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.server.driver.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public abstract class Item implements li.cil.oc.api.driver.Item {
    public static CompoundTag getDataTag(ItemStack stack) {
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        CompoundTag nbt;
        if (customData != null && !customData.isEmpty()) {
            nbt = customData.copyTag();
        } else {
            nbt = new CompoundTag();
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt));
        }
        if (!nbt.contains(Settings.namespace + "data")) {
            nbt.put(Settings.namespace + "data", new CompoundTag());
        }
        return nbt.getCompound(Settings.namespace + "data");
    }

    public static String address(ItemStack stack) {
        String addressKey = "address";
        CompoundTag tag = getTag(stack, new String[]{Settings.namespace + "data", "node"});
        if (tag != null && tag.contains(addressKey)) {
            return tag.getString(addressKey);
        }
        return null;
    }

    private static CompoundTag getTag(CompoundTag tagCompound, String[] keys) {
        if (keys.length == 0) {
            return tagCompound;
        } else if (!tagCompound.contains(keys[0])) {
            return null;
        } else {
            String[] remaining = new String[keys.length - 1];
            System.arraycopy(keys, 1, remaining, 0, keys.length - 1);
            return getTag(tagCompound.getCompound(keys[0]), remaining);
        }
    }

    private static CompoundTag getTag(ItemStack stack, String[] keys) {
        if (stack == null || stack.getCount() == 0) {
            return null;
        }
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return null;
        }
        return getTag(customData.copyTag(), keys);
    }

    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return worksWith(stack) && !Registry.INSTANCE.isBlacklisted(stack, host);
    }

    @Override
    public CompoundTag dataTag(ItemStack stack) {
        return li.cil.oc.neoforge.integration.opencomputers.Item.getDataTag(stack);
    }

    public int tier(ItemStack stack) {
        return Tier.One;
    }

    protected boolean isOneOf(ItemStack stack, li.cil.oc.api.detail.ItemInfo... items) {
        for (li.cil.oc.api.detail.ItemInfo item : items) {
            if (item != null && li.cil.oc.api.Items.get(stack) == item) {
                return true;
            }
        }
        return false;
    }

    protected boolean isAdapter(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Adapter.class.isAssignableFrom(host);
    }

    protected boolean isComputer(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Case.class.isAssignableFrom(host);
    }

    protected boolean isRobot(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Robot.class.isAssignableFrom(host);
    }

    protected boolean isRotatable(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Rotatable.class.isAssignableFrom(host);
    }

    protected boolean isServer(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Server.class.isAssignableFrom(host);
    }

    protected boolean isTablet(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Tablet.class.isAssignableFrom(host);
    }

    protected boolean isMicrocontroller(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Microcontroller.class.isAssignableFrom(host);
    }

    protected boolean isDrone(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Drone.class.isAssignableFrom(host);
    }
}
