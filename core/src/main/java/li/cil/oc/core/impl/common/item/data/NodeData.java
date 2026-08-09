package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class NodeData extends ItemData {
    public String address;
    public Double buffer;
    public Visibility visibility;

    public NodeData() {
        super(null);
    }

    public NodeData(ItemStack stack) {
        this();
        load(stack);
    }

    @Override
    public void load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        var nodeNbt = nbt.getCompound(OCSettings.namespace + "data").getCompound("node");
        if (nodeNbt.contains("address")) {
            address = nodeNbt.getString("address");
        }
        if (nodeNbt.contains("buffer")) {
            buffer = nodeNbt.getDouble("buffer");
        }
        if (nodeNbt.contains("visibility")) {
            visibility = Visibility.values()[nodeNbt.getInt("visibility")];
        }
    }

    @Override
    public void save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        if (!nbt.contains(OCSettings.namespace + "data")) {
            nbt.put(OCSettings.namespace + "data", new CompoundTag());
        }
        var dataNbt = nbt.getCompound(OCSettings.namespace + "data");
        if (!dataNbt.contains("node")) {
            dataNbt.put("node", new CompoundTag());
        }
        var nodeNbt = dataNbt.getCompound("node");
        if (address != null) nodeNbt.putString("address", address);
        if (buffer != null) nodeNbt.putDouble("buffer", buffer);
        if (visibility != null) nodeNbt.putInt("visibility", visibility.ordinal());
    }
}
