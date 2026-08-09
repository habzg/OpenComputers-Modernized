package li.cil.oc.core.impl.server.component;

import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public class UpgradeDatabase extends AbstractManagedEnvironment implements li.cil.oc.api.internal.Database, DeviceInfo {
    public final Container data;

    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("database")
            .create();
    private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Object catalogue");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "iCatalogue (patent pending)");
        put(DeviceAttribute.Capacity, String.valueOf(size()));
    }};

    public UpgradeDatabase(Container data) {
        this.data = data;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public Container data() {
        return data;
    }

    @Override
    public int size() {
        return data.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack stack = data.getItem(slot);
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        data.setItem(slot, stack);
    }

    @Override
    public int findStackWithHash(String needle) {
        return indexOf(needle, 0);
    }

    @Callback(doc = "function(slot:number):table -- Get the representation of the item stack stored in the specified slot.")
    public Object[] get(Context context, Arguments args) {
        return ResultWrapper.result(data.getItem(ExtendedArguments.checkSlot(args, data, 0)));
    }

    @Callback(doc = "function(slot:number):string -- Computes a hash value for the item stack in the specified slot.")
    public Object @Nullable [] computeHash(Context context, Arguments args) {
        ItemStack stack = data.getItem(ExtendedArguments.checkSlot(args, data, 0));
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            CompoundTag serialized = new CompoundTag();
            stack.save(SideTracker.getCurrentServer().registryAccess(), serialized);
            NbtIo.writeCompressed(serialized, baos);
            String hash = Hashing.sha256().hashBytes(baos.toByteArray()).toString();
            return ResultWrapper.result(hash);
        } catch (IOException e) {
            return null;
        }
    }

    @Callback(doc = "function(hash:string):number -- Get the index of an item stack with the specified hash. Returns a negative value if no such stack was found.")
    public Object[] indexOf(Context context, Arguments args) {
        return ResultWrapper.result(indexOf(args.checkString(0), 1));
    }

    @Callback(doc = "function(slot:number):boolean -- Clears the specified slot. Returns true if there was something in the slot before.")
    public Object[] clear(Context context, Arguments args) {
        int slot = ExtendedArguments.checkSlot(args, data, 0);
        boolean nonEmpty = !data.getItem(slot).isEmpty();
        data.setItem(slot, ItemStack.EMPTY);
        return ResultWrapper.result(nonEmpty);
    }

    @Callback(doc = "function(fromSlot:number, toSlot:number[, address:string]):boolean -- Copies an entry to another slot, optionally to another database. Returns true if something was overwritten.")
    public Object[] copy(Context context, Arguments args) {
        final int fromSlot = ExtendedArguments.checkSlot(args, data, 0);
        final ItemStack entry = data.getItem(fromSlot);
        if (args.count() > 2) {
            return DatabaseAccess.withDatabase(node, args.checkString(2), database -> {
                int toSlot = ExtendedArguments.checkSlot(args, database.data(), 1);
                boolean nonEmpty = !database.data().getItem(toSlot).isEmpty();
                database.data().setItem(toSlot, entry.copy());
                return ResultWrapper.result(nonEmpty);
            });
        } else {
            int toSlot = ExtendedArguments.checkSlot(args, data, 1);
            boolean nonEmpty = !data.getItem(toSlot).isEmpty();
            data.setItem(toSlot, entry.copy());
            return ResultWrapper.result(nonEmpty);
        }
    }

    @Callback(doc = "function(address:string):number -- Copies the data stored in this database to another database with the specified address.")
    public Object[] clone(Context context, Arguments args) {
        return DatabaseAccess.withDatabase(node, args.checkString(0), database -> {
            int numberToCopy = Math.min(data.getContainerSize(), database.data().getContainerSize());
            for (int slot = 0; slot < numberToCopy; slot++) {
                ItemStack stack = data.getItem(slot);
                database.data().setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            context.pause(0.25);
            return ResultWrapper.result(numberToCopy);
        });
    }

    @SuppressWarnings("unused")
    @Callback(doc = "function(slot:number, id:string, damage:number, nbt:string):boolean -- Sets an item into the specified database slot. The NBT tag is expected in string (SNBT) format.")
    public Object[] set(Context context, Arguments args) {
        int slot = ExtendedArguments.checkSlot(args, data, 0);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(args.checkString(1)));
        int damage = args.checkInteger(2);
        String tagJson = args.optString(3, "");
        CompoundTag tag = null;
        if (!Strings.isNullOrEmpty(tagJson)) {
            try {
                tag = TagParser.parseTag(tagJson);
            } catch (Exception e) {
                return ResultWrapper.result(false, "invalid nbt tag");
            }
        }
        ItemStack stack = new ItemStack(item, 1);
        if (stack.isDamageableItem()) {
            stack.setDamageValue(damage);
        }
        if (tag != null) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        data.setItem(slot, stack);
        return ResultWrapper.result(true);
    }

    private int indexOf(String needle, int offset) {
        for (int slot = 0; slot < data.getContainerSize(); slot++) {
            ItemStack stack = data.getItem(slot);
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                CompoundTag serialized = new CompoundTag();
                stack.save(SideTracker.getCurrentServer().registryAccess(), serialized);
                NbtIo.writeCompressed(serialized, baos);
                String hash = Hashing.sha256().hashBytes(baos.toByteArray()).toString();
                if (hash.equals(needle)) return slot + offset;
            } catch (IOException e) {
                // ignore
            }
        }
        return -1;
    }
}
