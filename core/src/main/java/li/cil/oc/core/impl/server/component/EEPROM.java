package li.cil.oc.core.impl.server.component;

import com.google.common.hash.Hashing;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class EEPROM extends AbstractManagedEnvironment implements DeviceInfo {
    public final Node node = Network.newNode(this, Visibility.Neighbors)
            .withComponent("eeprom", Visibility.Neighbors)
            .withConnector()
            .create();
    private final Map<String, String> deviceInfo;
    public byte[] codeData = new byte[0];
    public byte[] volatileData = new byte[0];
    public boolean readonly = false;
    public String label = "EEPROM";

    public EEPROM() {
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Memory, DeviceAttribute.Description, "EEPROM", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "FlashStick2k", DeviceAttribute.Capacity, String.valueOf(OCSettings.get().eepromSize), DeviceAttribute.Size, String.valueOf(OCSettings.get().eepromSize));
    }

    public String checksum() {
        return Hashing.crc32().hashBytes(codeData).toString();
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(direct = true, doc = "function():string -- Get the currently stored byte array.")
    public Object[] get(Context context, Arguments args) {
        return ResultWrapper.result((Object) codeData);
    }

    @Callback(doc = "function(data:string) -- Overwrite the currently stored byte array.")
    public Object @Nullable [] set(Context context, Arguments args) {
        if (readonly) {
            return ResultWrapper.result(null, "storage is readonly");
        }
        if (!((Connector) node).tryChangeBuffer(-OCSettings.get().eepromWriteCost)) {
            return ResultWrapper.result(null, "not enough energy");
        }
        byte[] newData = args.optByteArray(0, new byte[0]);
        if (newData.length > OCSettings.get().eepromSize)
            throw new IllegalArgumentException("not enough space");
        codeData = newData;
        context.pause(2);
        return null;
    }

    @Callback(direct = true, doc = "function():string -- Get the label of the EEPROM.")
    public Object[] getLabel(Context context, Arguments args) {
        return ResultWrapper.result(label);
    }

    @Callback(doc = "function(data:string):string -- Set the label of the EEPROM.")
    public Object[] setLabel(Context context, Arguments args) {
        if (readonly) {
            return ResultWrapper.result(null, "storage is readonly");
        }
        label = args.optString(0, "EEPROM").trim();
        if (label.length() > 24) label = label.substring(0, 24);
        if (label.isEmpty()) label = "EEPROM";
        return ResultWrapper.result(label);
    }

    @Callback(direct = true, doc = "function():number -- Get the storage capacity of this EEPROM.")
    public Object[] getSize(Context context, Arguments args) {
        return ResultWrapper.result((double) OCSettings.get().eepromSize);
    }

    @Callback(direct = true, doc = "function():string -- Get the checksum of the data on this EEPROM.")
    public Object[] getChecksum(Context context, Arguments args) {
        return ResultWrapper.result(checksum());
    }

    @Callback(direct = true, doc = "function(checksum:string):boolean -- Make this EEPROM readonly if it isn't already. This process cannot be reversed!")
    public Object[] makeReadonly(Context context, Arguments args) {
        if (args.checkString(0).equals(checksum())) {
            readonly = true;
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(null, "incorrect checksum");
    }

    @Callback(direct = true, doc = "function():number -- Get the storage capacity of this EEPROM.")
    public Object[] getDataSize(Context context, Arguments args) {
        return ResultWrapper.result((double) OCSettings.get().eepromDataSize);
    }

    @Callback(direct = true, doc = "function():string -- Get the currently stored byte array.")
    public Object[] getData(Context context, Arguments args) {
        return ResultWrapper.result((Object) volatileData);
    }

    @Callback(doc = "function(data:string) -- Overwrite the currently stored byte array.")
    public Object @Nullable [] setData(Context context, Arguments args) {
        if (!((Connector) node).tryChangeBuffer(-OCSettings.get().eepromWriteCost)) {
            return ResultWrapper.result(null, "not enough energy");
        }
        byte[] newData = args.optByteArray(0, new byte[0]);
        if (newData.length > OCSettings.get().eepromDataSize)
            throw new IllegalArgumentException("not enough space");
        volatileData = newData;
        context.pause(1);
        return null;
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        codeData = nbt.getByteArray(OCSettings.namespace + "eeprom");
        if (nbt.contains(OCSettings.namespace + "label")) {
            label = nbt.getString(OCSettings.namespace + "label");
        }
        readonly = nbt.getBoolean(OCSettings.namespace + "readonly");
        volatileData = nbt.getByteArray(OCSettings.namespace + "userdata");
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        nbt.putByteArray(OCSettings.namespace + "eeprom", codeData);
        nbt.putString(OCSettings.namespace + "label", label);
        nbt.putBoolean(OCSettings.namespace + "readonly", readonly);
        nbt.putByteArray(OCSettings.namespace + "userdata", volatileData);
    }
}
