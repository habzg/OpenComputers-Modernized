package li.cil.oc.core.impl.server.driver;

import com.google.common.hash.Hashing;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.core.server.machine.EnvironmentHost;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompoundBlockEnvironment implements ManagedEnvironment, EnvironmentHost {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompoundBlockEnvironment.class);
    public final Node node;

    @Override
    public Node node() {
        return node;
    }

    private final String[] driverNames;
    private final ManagedEnvironment[] environments;
    private final List<ManagedEnvironment> updatingEnvironments = new ArrayList<>();

    public ManagedEnvironment[] environments() {
        return environments;
    }

    public CompoundBlockEnvironment(String name, String[] driverNames, ManagedEnvironment[] environments) {
        this.driverNames = driverNames;
        this.environments = environments;
        Visibility maxVis = Visibility.None;
        for (ManagedEnvironment env : environments) {
            if (env.node() != null && env.node().reachability().ordinal() > maxVis.ordinal()) {
                maxVis = env.node().reachability();
            }
        }
        this.node = li.cil.oc.api.Network.newNode(this, maxVis).withComponent(name).create();
        for (ManagedEnvironment env : environments) {
            if (env.canUpdate()) updatingEnvironments.add(env);
        }
        for (ManagedEnvironment env : environments) {
            if (env.node() instanceof Component component) {
                component.setVisibility(Visibility.Neighbors);
            }
        }
    }

    @Override
    public boolean canUpdate() {
        for (ManagedEnvironment env : environments) {
            if (env.canUpdate()) return true;
        }
        return false;
    }

    @Override
    public void update() {
        for (ManagedEnvironment env : updatingEnvironments) {
            env.update();
        }
    }

    @Override
    public void onMessage(Message message) {
    }

    @Override
    public void onConnect(Node node) {
        if (node == this.node) {
            for (ManagedEnvironment env : environments) {
                if (env.node() != null) {
                    node.connect(env.node());
                }
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == this.node) {
            for (ManagedEnvironment env : environments) {
                if (env.node() != null) {
                    env.node().remove();
                }
            }
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.contains("typeHash") && nbt.getLong("typeHash") != typeHash()) return;
        node.load(nbt, provider);
        for (int i = 0; i < driverNames.length; i++) {
            if (nbt.contains(driverNames[i])) {
                try {
                    environments[i].load(nbt.getCompound(driverNames[i]), provider);
                } catch (Throwable e) {
                    LOGGER.warn("A block component of type '{}' (provided by driver '{}') threw an error while loading.", environments[i].getClass().getName(), driverNames[i], e);
                }
            }
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        nbt.putLong("typeHash", typeHash());
        node.save(nbt, provider);
        for (int i = 0; i < driverNames.length; i++) {
            int idx = i;
            try {
                ExtendedNBT.setNewCompoundTag(nbt, driverNames[i], tag -> environments[idx].save(tag, provider));
            } catch (Throwable e) {
                LOGGER.warn("A block component of type '{}' (provided by driver '{}') threw an error while saving.", environments[i].getClass().getName(), driverNames[i], e);
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private long typeHash() {
        com.google.common.hash.Hasher hash = Hashing.sha256().newHasher();
        for (ManagedEnvironment env : environments) {
            hash.putString(env.getClass().getName(), Charset.defaultCharset());
        }
        return hash.hash().asLong();
    }
}
