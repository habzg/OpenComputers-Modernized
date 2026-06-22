package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.world.entity.Entity;


import java.util.Map;

public abstract class UpgradeChunkloaderBase extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("chunkloader")
            .withConnector()
            .create();
    private final Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "World stabilizer");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "Realizer9001-CL");
    }};
    protected boolean active = false;

    public UpgradeChunkloaderBase(EnvironmentHost host) {
        this.host = host;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (host.level().getGameTime() % Settings.get().tickFrequency == 0 && active) {
            if (!consumeEnergy(Settings.get().chunkloaderCost * Settings.get().tickFrequency)) {
                setActive(false);
            } else if (host instanceof Entity) {
                onChunkTicketActive();
            }
        }
    }

    protected abstract boolean consumeEnergy(double amount);

    protected abstract void onChunkTicketActive();

    protected abstract void onChunkTicketInactive();

    @Callback(doc = "function():boolean -- Gets whether the chunkloader is currently active.")
    public Object[] isActive(Context context, Arguments args) {
        return ResultWrapper.result(active);
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Enables or disables the chunkloader, returns true if active changed")
    public Object[] setActive(Context context, Arguments args) {
        return ResultWrapper.result(setActive(args.checkBoolean(0), true));
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            if (host instanceof Context && ((Context) host).isRunning()) {
                requestTicket(false);
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            setActive(false);
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("computer.stopped".equals(message.name())) {
            setActive(false);
        } else if ("computer.started".equals(message.name())) {
            setActive(true);
        }
    }

    private void setActive(boolean enabled) {
        setActive(enabled, false);
    }

    private boolean setActive(boolean enabled, boolean throwIfBlocked) {
        if (enabled && !active) {
            return requestTicket(throwIfBlocked);
        } else if (!enabled && active) {
            active = false;
            onChunkTicketInactive();
            return true;
        }
        return false;
    }

    private boolean isDimensionAllowed() {
        int id = legacyDimensionId(host.level().dimension());
        java.util.List<Integer> whitelist = Settings.get().chunkloadDimensionWhitelist;
        java.util.List<Integer> blacklist = Settings.get().chunkloadDimensionBlacklist;
        if (whitelist != null && !whitelist.isEmpty()) {
            boolean found = false;
            for (int w : whitelist) {
                if (w == id) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        if (blacklist != null) {
            for (int b : blacklist) {
                if (b == id) return false;
            }
        }
        return true;
    }

    private static int legacyDimensionId(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        if (dimension == net.minecraft.world.level.Level.OVERWORLD) return 0;
        if (dimension == net.minecraft.world.level.Level.NETHER) return -1;
        if (dimension == net.minecraft.world.level.Level.END) return 1;
        return dimension.location().hashCode();
    }

    private boolean requestTicket(boolean throwIfBlocked) {
        if (!isDimensionAllowed()) {
            if (throwIfBlocked) {
                throw new RuntimeException("this dimension is blacklisted");
            }
            return false;
        }
        active = true;
        onChunkTicketActive();
        return true;
    }
}
