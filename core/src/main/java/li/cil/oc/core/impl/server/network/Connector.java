package li.cil.oc.core.impl.server.network;

import li.cil.oc.core.impl.Settings;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface Connector extends li.cil.oc.api.network.Connector, Node {
    double localBufferSize();

    void localBufferSize_$eq(double value);

    double localBuffer();

    void localBuffer_$eq(double value);

    Distributor distributor();

    void distributor_$eq(Distributor value);

    default double globalBuffer() {
        Distributor d = distributor();
        return d != null ? d.globalBuffer() : localBuffer();
    }

    default double globalBufferSize() {
        Distributor d = distributor();
        return d != null ? d.globalBufferSize() : localBufferSize();
    }

    default double changeBuffer(double delta) {
        if (delta == 0) return 0;
        if (Settings.get().ignorePower) return delta < 0 ? 0 : delta;
        synchronized (this) {
            Distributor d = distributor();
            if (d != null) {
                synchronized (d) {
                    return d.changeBuffer(change(delta));
                }
            }
            return change(delta);
        }
    }

    private double change(double delta) {
        if (localBufferSize() <= 0) return delta;
        double old = localBuffer();
        localBuffer_$eq(localBuffer() + delta);
        double remaining;
        if (localBuffer() < 0) {
            remaining = localBuffer();
            localBuffer_$eq(0);
        } else if (localBuffer() > localBufferSize()) {
            remaining = localBuffer() - localBufferSize();
            localBuffer_$eq(localBufferSize());
        } else remaining = 0;
        if (localBuffer() != old) {
            Distributor d = distributor();
            if (d != null) {
                d.globalBuffer_$eq(Math.clamp(d.globalBuffer() - old + localBuffer(), 0, d.globalBufferSize()));
            }
        }
        return remaining;
    }

    default boolean tryChangeBuffer(double delta) {
        if (delta == 0) return true;
        if (Settings.get().ignorePower) return delta < 0;
        synchronized (this) {
            Distributor d = distributor();
            if (d != null) {
                synchronized (d) {
                    if (localBuffer() > localBufferSize()) {
                        d.changeBuffer(localBuffer() - localBufferSize());
                        localBuffer_$eq(localBufferSize());
                    }
                    double gb = globalBuffer();
                    if ((delta > 0 || gb + delta >= 0) && (delta < 0 || gb + delta <= globalBufferSize()))
                        return d.changeBuffer(delta) == 0;
                    return false;
                }
            } else {
                double nb = localBuffer() + delta;
                if ((delta < 0 && nb < 0) || (delta > 0 && nb > localBufferSize())) return false;
                localBuffer_$eq(nb);
                return true;
            }
        }
    }

    default void setLocalBufferSize(double size) {
        double clamped = Math.max(size, 0);
        synchronized (this) {
            Distributor d = distributor();
            if (d != null) {
                synchronized (d) {
                    double oldSize = localBufferSize();
                    localBufferSize_$eq(clamped);
                    if (network() != null) {
                        if (oldSize <= 0 && clamped > 0) d.addConnector(this);
                        else if (oldSize > 0 && clamped == 0) d.removeConnector(this);
                        else d.globalBufferSize_$eq(Math.max(d.globalBufferSize() - oldSize + clamped, 0));
                    }
                    double surplus = Math.max(localBuffer() - clamped, 0);
                    changeBuffer(-surplus);
                    d.changeBuffer(surplus);
                }
            } else {
                localBufferSize_$eq(clamped);
                localBuffer_$eq(Math.min(localBuffer(), localBufferSize()));
            }
        }
    }

    default void onDisconnect(li.cil.oc.api.network.Node node) {
        Node.super.onDisconnect(node);
        if (node == this) {
            synchronized (this) {
                distributor_$eq(null);
            }
        }
    }

    default void load(CompoundTag nbt, HolderLookup.Provider provider) {
        Node.super.load(nbt, provider);
        localBuffer_$eq(nbt.getDouble("buffer"));
    }

    default void save(CompoundTag nbt, HolderLookup.Provider provider) {
        Node.super.save(nbt, provider);
        nbt.putDouble("buffer", Math.min(localBuffer(), localBufferSize()));
    }
}
