package li.cil.oc.core.impl.server.component;


import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;


import java.util.Map;

public abstract class UpgradeTankBase extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost owner;
    public final int capacity;

    public final Node node = Network.newNode(this, Visibility.None).create();
    private final Map<String, String> deviceInfo;

    public UpgradeTankBase(EnvironmentHost owner, int capacity) {
        this.owner = owner;
        this.capacity = capacity;
        deviceInfo = new java.util.HashMap<>();
        deviceInfo.put(DeviceAttribute.Class, DeviceClass.Generic);
        deviceInfo.put(DeviceAttribute.Description, "Tank upgrade");
        deviceInfo.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        deviceInfo.put(DeviceAttribute.Product, "Superblubb V10");
        deviceInfo.put(DeviceAttribute.Capacity, String.valueOf(capacity));
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    protected abstract void loadTankNbt(CompoundTag nbt, HolderLookup.Provider provider);

    protected abstract void saveTankNbt(CompoundTag nbt, HolderLookup.Provider provider);

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (nbt.contains("fluid")) {
            loadTankNbt(nbt.getCompound("fluid"), provider);
        } else if (nbt.contains("FluidName")) {
            CompoundTag translated = new CompoundTag();
            translated.putString("id", nbt.getString("FluidName"));
            if (nbt.contains("Amount")) {
                translated.putInt("Amount", nbt.getInt("Amount"));
            }
            loadTankNbt(translated, provider);
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        CompoundTag fluidTag = new CompoundTag();
        saveTankNbt(fluidTag, provider);
        if (!fluidTag.isEmpty()) {
            nbt.put("fluid", fluidTag);
        }
    }

    protected int tankIndex() {
        if (owner instanceof li.cil.oc.api.internal.Agent agent && agent.tank() != null) {
            int count = agent.tank().tankCount();
            for (int i = 0; i < count; i++) {
                if (agent.tank().getFluidTank(i) == this) {
                    return i + 1;
                }
            }
        }
        return 1;
    }
}
