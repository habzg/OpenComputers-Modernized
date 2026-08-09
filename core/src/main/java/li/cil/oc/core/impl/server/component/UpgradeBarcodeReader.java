package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

public class UpgradeBarcodeReader extends AbstractManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    @SuppressWarnings("unused")
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("barcode_reader")
            .withConnector()
            .create();
    private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Barcode reader upgrade");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "Readerizer Deluxe");
    }};

    public UpgradeBarcodeReader(EnvironmentHost host) {
        this.host = host;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("tablet.use".equals(message.name()) && message.source().host() instanceof li.cil.oc.api.machine.Machine machine) {
            if (machine.host() instanceof li.cil.oc.api.internal.Tablet && message.data().length >= 8) {
                Object[] data = message.data();
                CompoundTag nbt = (CompoundTag) data[0];
                BlockPosition blockPos = (BlockPosition) data[3];
                Direction side = (Direction) data[4];
                float hitX = (Float) data[5];
                float hitY = (Float) data[6];
                float hitZ = (Float) data[7];

                Object te = host.level().getBlockEntity(blockPos.toBlockPos());
                if (te instanceof Analyzable) {
                    processNodes(((Analyzable) te).onAnalyze((Player) data[2], side, hitX, hitY, hitZ), nbt);
                } else if (te instanceof SidedEnvironment) {
                    processNodes(new Node[]{((SidedEnvironment) te).sidedNode(side)}, nbt);
                } else if (te instanceof Environment) {
                    processNodes(new Node[]{((Environment) te).node()}, nbt);
                }
            }
        }
    }

    private void processNodes(Node[] nodes, CompoundTag nbt) {
        ListTag readerNBT = new ListTag();
        for (Node node : nodes) {
            if (node != null) {
                CompoundTag nodeNBT = new CompoundTag();
                if (node instanceof Component) {
                    nodeNBT.putString("type", ((Component) node).name());
                }
                String address = node.address();
                if (address != null && !address.isEmpty()) {
                    nodeNBT.putString("address", address);
                }
                readerNBT.add(nodeNBT);
            }
        }
        nbt.put("analyzed", readerNBT);
    }
}
