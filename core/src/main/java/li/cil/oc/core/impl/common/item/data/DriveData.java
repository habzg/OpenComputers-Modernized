package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.core.impl.OCSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DriveData extends ItemData {
    private static final String UnmanagedKey = OCSettings.namespace + "unmanaged";
    private static final String LockKey = OCSettings.namespace + "lock";
    public boolean isUnmanaged = false;
    public String lockInfo = "";

    public DriveData() {
        super(null);
    }

    public DriveData(ItemStack stack) {
        this();
        load(stack);
    }

    public static void lock(ItemStack stack, Player player) {
        var key = player.getDisplayName().getString();
        var data = new DriveData(stack);
        if (!data.isLocked()) {
            data.lockInfo = !key.isEmpty() ? key : "notch";
            data.save(stack);
        }
    }

    public static void setUnmanaged(ItemStack stack, boolean unmanaged) {
        var data = new DriveData(stack);
        if (data.isUnmanaged != unmanaged) {
            li.cil.oc.core.impl.server.fs.FileSystem.removeAddress(stack);
            data.lockInfo = "";
        }
        data.isUnmanaged = unmanaged;
        data.save(stack);
    }

    public boolean isLocked() {
        return lockInfo != null && !lockInfo.isEmpty();
    }

    @Override
    public void load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        isUnmanaged = nbt.getBoolean(UnmanagedKey);
        lockInfo = nbt.contains(LockKey) ? nbt.getString(LockKey) : "";
    }

    @Override
    public void save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        nbt.putBoolean(UnmanagedKey, isUnmanaged);
        nbt.putString(LockKey, lockInfo);
    }
}
