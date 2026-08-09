package li.cil.oc.core.impl.server.component;

import java.util.HashSet;
import java.util.Set;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware.RedstoneChangedEventArgs;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public abstract class RedstoneSignaller extends AbstractManagedEnvironment {
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("redstone", Visibility.Neighbors)
            .create();

    public int wakeThreshold = 0;
    public boolean wakeNeighborsOnly = true;

    @Nullable
    private Set<String> enabledProviders = null;

    @Nullable
    public Set<String> getEnabledProviders() {
        return enabledProviders;
    }

    public void setEnabledProviders(@Nullable Set<String> providers) {
        this.enabledProviders = providers != null && providers.isEmpty() ? null : providers;
    }

    @Callback(direct = true, doc = "function():number -- Get the current wake-up threshold.")
    public Object[] getWakeThreshold(Context context, Arguments args) {
        return ResultWrapper.result((double) wakeThreshold);
    }

    @Callback(doc = "function(threshold:number):number -- Set the wake-up threshold.")
    public Object[] setWakeThreshold(Context context, Arguments args) {
        int oldThreshold = wakeThreshold;
        wakeThreshold = args.checkInteger(0);
        return ResultWrapper.result((double) oldThreshold);
    }

    public void onRedstoneChanged(RedstoneChangedEventArgs args) {
        Object side = args.side() == null ? "wireless" : args.side().ordinal();
        if (args.color() >= 0) {
            node.sendToReachable("computer.signal", "redstone_changed", side, args.oldValue(), args.newValue(), args.color());
        } else {
            node.sendToReachable("computer.signal", "redstone_changed", side, args.oldValue(), args.newValue());
        }
        if (args.oldValue() < wakeThreshold && args.newValue() >= wakeThreshold) {
            if (wakeNeighborsOnly)
                node.sendToNeighbors("computer.start");
            else
                node.sendToReachable("computer.start");
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        wakeThreshold = nbt.getInt("wakeThreshold");
        if (nbt.contains("enabledProviders")) {
            ListTag list = nbt.getList("enabledProviders", Tag.TAG_STRING);
            Set<String> set = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                set.add(list.getString(i));
            }
            enabledProviders = set;
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        nbt.putInt("wakeThreshold", wakeThreshold);
        if (enabledProviders != null) {
            ListTag list = new ListTag();
            for (String name : enabledProviders) {
                list.add(StringTag.valueOf(name));
            }
            nbt.put("enabledProviders", list);
        }
    }
}
