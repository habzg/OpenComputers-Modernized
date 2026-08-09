package li.cil.oc.core.impl.server.component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.component.traits.WorldAware;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.phys.Vec3;

public abstract class UpgradeTradingBase extends AbstractManagedEnvironment implements WorldAware, DeviceInfo {
    public final EnvironmentHost host;

    @SuppressWarnings("unused")
    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withComponent("trading")
            .create();
    private final Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Trading upgrade");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "Capitalism H.O. 1200T");
    }};

    public UpgradeTradingBase(EnvironmentHost host) {
        this.host = host;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public BlockPosition position() {
        return BlockPosition.apply(host);
    }

    public double maxRange() {
        return OCSettings.get().tradingRange;
    }

    public boolean isInRange(Entity entity) {
        return new Vec3(entity.getX(), entity.getY(), entity.getZ()).distanceTo(position().toVec3()) <= maxRange();
    }

    protected abstract Object createTradeObject(Merchant merchant, int recipeID, int merchantID);

    @Callback(doc = "function():table -- Returns a table of trades in range as userdata objects.")
    public Object[] getTrades(Context context, Arguments args) {
        List<Merchant> merchants = new ArrayList<>();
        for (Entity entity : entitiesInBounds(position().bounds().inflate(maxRange(), maxRange(), maxRange()), Entity.class)) {
            if (entity instanceof Merchant && isInRange(entity)) {
                merchants.add((Merchant) entity);
            }
        }
        merchants.sort(Comparator.comparing(m -> ((Entity) m).getUUID()));
        Map<UUID, Integer> idMap = new HashMap<>();
        int nextId = 1;
        for (Merchant merchant : merchants) {
            idMap.put(((Entity) merchant).getUUID(), nextId);
            nextId++;
        }
        List<Object> result = new ArrayList<>();
        for (Merchant merchant : merchants) {
            for (int index = 0; index < merchant.getOffers().size(); index++) {
                result.add(createTradeObject(merchant, index, idMap.get(((Entity) merchant).getUUID())));
            }
        }
        return ResultWrapper.result(result.toArray());
    }
}
