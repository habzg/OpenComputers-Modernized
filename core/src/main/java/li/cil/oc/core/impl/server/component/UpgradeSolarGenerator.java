package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Map;

public class UpgradeSolarGenerator extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withConnector()
            .create();
    private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Power);
        put(DeviceAttribute.Description, "Solar panel");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "Enligh10");
    }};
    private int ticksUntilCheck = 0;

    private boolean isSunShining = false;

    public UpgradeSolarGenerator(EnvironmentHost host) {
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
        ticksUntilCheck -= 1;
        if (ticksUntilCheck <= 0) {
            ticksUntilCheck = 100;
            isSunShining = isSunVisible();
        }
        if (isSunShining && node != null) {
            ((li.cil.oc.api.network.Connector) node).changeBuffer(Settings.get().solarGeneratorEfficiency);
        }
    }

    private boolean isSunVisible() {
        BlockPosition blockPos = BlockPosition.apply(host).offset(Direction.UP);
        var bp = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
        return host.level().isDay() &&
                host.level().dimensionType().hasSkyLight() &&
                host.level().canSeeSky(bp) &&
                (!host.level().getBiome(bp).value().hasPrecipitation() ||
                        (!host.level().isRaining() && !host.level().isThundering()));
    }
}
