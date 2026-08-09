package li.cil.oc.core.impl.server.component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware.RedstoneChangedEventArgs;
import li.cil.oc.core.impl.integration.util.WirelessRedstone;
import li.cil.oc.core.util.ResultWrapper;

public interface RedstoneWireless extends DeviceInfo {
    EnvironmentHost redstone();

    @Callback(doc = "function():number -- Get the wireless redstone input.")
    default Object[] getWirelessInput(Context context, Arguments args) {
        boolean input = WirelessRedstone.getInput(this);
        setWirelessInputValue(input);
        return ResultWrapper.result(input);
    }

    @Callback(direct = true, doc = "function():boolean -- Get the wireless redstone output.")
    default Object[] getWirelessOutput(Context context, Arguments args) {
        return ResultWrapper.result(getWirelessOutputValue());
    }

    boolean getWirelessOutputValue();

    void setWirelessOutputValue(boolean value);

    boolean getWirelessInputValue();

    void setWirelessInputValue(boolean value);

    int getWirelessFrequencyValue();

    void setWirelessFrequencyValue(int value);

    @Callback(doc = "function(value:boolean):boolean -- Set the wireless redstone output.")
    default Object[] setWirelessOutput(Context context, Arguments args) {
        boolean oldValue = getWirelessOutputValue();
        boolean newValue = args.checkBoolean(0);
        if (oldValue != newValue) {
            setWirelessOutputValue(newValue);
            WirelessRedstone.updateOutput(this);
            if (OCSettings.get().redstoneDelay > 0)
                context.pause(OCSettings.get().redstoneDelay);
        }
        return ResultWrapper.result(oldValue);
    }

    @Callback(direct = true, doc = "function():number -- Get the currently set wireless redstone frequency.")
    default Object[] getWirelessFrequency(Context context, Arguments args) {
        return ResultWrapper.result((double) getWirelessFrequencyValue());
    }

    @Callback(doc = "function(frequency:number):number -- Set the wireless redstone frequency to use.")
    default Object[] setWirelessFrequency(Context context, Arguments args) {
        int oldValue = getWirelessFrequencyValue();
        int newValue = args.checkInteger(0);
        if (oldValue != newValue) {
            if (WirelessRedstone.cannotHandleFrequency(newValue, getEnabledProviders())) {
                return ResultWrapper.result(null, "none of the available providers can handle the requested frequency");
            }
            setWirelessOutputValue(false);
            setWirelessInputValue(false);
            WirelessRedstone.updateOutput(this);
            WirelessRedstone.removeReceiver(this);
            WirelessRedstone.removeTransmitter(this);
            setWirelessFrequencyValue(newValue);
            WirelessRedstone.addReceiver(this);
            context.pause(0.5);
        }
        return ResultWrapper.result((double) oldValue);
    }

    @Callback(direct = true, doc = "function():table -- Get the list of enabled wireless providers.")
    default Object[] getWirelessProviders(Context context, Arguments args) {
        Set<String> enabled = getEnabledProviders();
        if (enabled == null) {
            return ResultWrapper.result((Object) WirelessRedstone.getProviderNames().toArray(new String[0]));
        }
        return ResultWrapper.result((Object) enabled.toArray(new String[0]));
    }

    @Callback(doc = "function(providers:table):table -- Set the enabled wireless providers.")
    default Object[] setWirelessProviders(Context context, Arguments args) {
        Map<?, ?> table = args.checkTable(0);
        Set<String> set = new HashSet<>();
        for (var entry : table.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof byte[] ba) {
                set.add(new String(ba));
            } else if (value != null) {
                set.add(value.toString());
            }
        }
        if (set.isEmpty()) {
            return ResultWrapper.result(null, "at least one provider must be enabled");
        }
        for (String name : set) {
            if (!WirelessRedstone.hasProvider(name)) {
                return ResultWrapper.result(null, "unknown provider: " + name);
            }
        }
        if (WirelessRedstone.cannotHandleFrequency(getWirelessFrequencyValue(), set)) {
            return ResultWrapper.result(null, "the selected providers cannot handle the current frequency");
        }
        setEnabledProviders(set);
        return ResultWrapper.result((Object) set.toArray(new String[0]));
    }

    default Set<String> getEnabledProviders() {
        if (this instanceof RedstoneSignaller rs) {
            return rs.getEnabledProviders();
        }
        return null;
    }

    default void setEnabledProviders(Set<String> providers) {
        if (this instanceof RedstoneSignaller rs) {
            rs.setEnabledProviders(providers);
        }
    }

    @SuppressWarnings({"unused", "RedundantThrows"})
    void onRedstoneChanged(RedstoneChangedEventArgs args) ;

    int getFreq();
}
