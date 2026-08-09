package li.cil.oc.core.impl.server.component.traits;

import com.google.common.base.Charsets;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Packet;
import li.cil.oc.core.server.component.traits.NetworkAware;
import net.minecraft.nbt.CompoundTag;


import static li.cil.oc.core.util.ResultWrapper.result;

public interface WakeMessageAware extends NetworkAware {
    String getWakeMessage();

    void setWakeMessage(String message);

    boolean isWakeMessageFuzzy();

    void setWakeMessageFuzzy(boolean fuzzy);

    @Callback(direct = true, doc = "function():string, boolean -- Get the current wake-up message.")
    default Object[] getWakeMessage(Context context, Arguments args) {
        return result(getWakeMessage(), isWakeMessageFuzzy());
    }

    @Callback(doc = "function(message:string[, fuzzy:boolean]):string -- Set the wake-up message and whether to ignore additional data/parameters.")
    default Object[] setWakeMessage(Context context, Arguments args) {
        String oldMessage = getWakeMessage();
        boolean oldFuzzy = isWakeMessageFuzzy();
        if (args.optAny(0, null) == null) setWakeMessage(null);
        else setWakeMessage(args.checkString(0));
        setWakeMessageFuzzy(args.optBoolean(1, isWakeMessageFuzzy()));
        return result(oldMessage, oldFuzzy);
    }

    default boolean isPacketAccepted(Packet packet, double distance) {
        return true;
    }

    default void receivePacket(Packet packet, double distance, EnvironmentHost host) {
        if (packet.source() != null && !packet.source().equals(node().address()) &&
                (packet.destination() == null || packet.destination().equals(node().address()))) {
            if (isPacketAccepted(packet, distance)) {
                Object[] data = new Object[4 + packet.data().length];
                data[0] = "modem_message";
                data[1] = packet.source();
                data[2] = packet.port();
                data[3] = distance;
                System.arraycopy(packet.data(), 0, data, 4, packet.data().length);
                node().sendToReachable("computer.signal", data);
            }
            boolean wakeup = false;
            String msg = getWakeMessage();
            if (msg != null) {
                Object[] pdata = packet.data();
                boolean firstMatches = false;
                if (pdata.length >= 1) {
                    if (pdata[0] instanceof byte[]) {
                        firstMatches = msg.equals(new String((byte[]) pdata[0], Charsets.UTF_8));
                    } else if (pdata[0] instanceof String) {
                        firstMatches = msg.equals(pdata[0]);
                    }
                }
                if (firstMatches && (pdata.length == 1 || isWakeMessageFuzzy())) {
                    wakeup = true;
                }
            }
            if (wakeup) {
                if (host instanceof Context) ((Context) host).start();
                else node().sendToNeighbors("computer.start");
            }
        }
    }

    default void loadWakeMessage(CompoundTag nbt) {
        if (nbt.contains("wakeMessage")) {
            setWakeMessage(nbt.getString("wakeMessage"));
        }
        setWakeMessageFuzzy(nbt.getBoolean("wakeMessageFuzzy"));
    }

    default void saveWakeMessage(CompoundTag nbt) {
        String msg = getWakeMessage();
        if (msg != null) nbt.putString("wakeMessage", msg);
        nbt.putBoolean("wakeMessageFuzzy", isWakeMessageFuzzy());
    }
}
