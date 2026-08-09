package li.cil.oc.api.prefab;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Simple base implementation of the <code>ManagedEnvironment</code> interface, so
 * unused methods don't clutter the implementing class.
 */
public abstract class AbstractManagedEnvironment implements li.cil.oc.api.network.ManagedEnvironment {
    private volatile Node _node;
    private volatile boolean _nodeResolved = false;
    private EnvironmentHost _host;

    public AbstractManagedEnvironment() {
    }

    public AbstractManagedEnvironment(EnvironmentHost host) {
        _host = host;
    }

    public EnvironmentHost host() {
        return _host;
    }

    @Override
    public Node node() {
        if (!_nodeResolved) {
            _nodeResolved = true;
            if (_node == null) {
                try {
                    java.lang.reflect.Field f = getClass().getField("node");
                    if (Node.class.isAssignableFrom(f.getType())) {
                        _node = (Node) f.get(this);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return _node;
    }

    protected void setNode(Node value) {
        _node = value;
        _nodeResolved = true;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
    }

    @Override
    public void onConnect(final Node node) {
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public void load(final CompoundTag nbt, final HolderLookup.Provider provider) {
        if (node() != null) {
            node().load(nbt.getCompound("node"), provider);
        }
    }

    @Override
    public void save(final CompoundTag nbt, final HolderLookup.Provider provider) {
        if (node() != null) {
            if (node().address() == null) {
                li.cil.oc.api.Network.joinNewNetwork(node());
                final CompoundTag nodeTag = new CompoundTag();
                node().save(nodeTag, provider);
                nbt.put("node", nodeTag);
                node().remove();
            } else {
                final CompoundTag nodeTag = new CompoundTag();
                node().save(nodeTag, provider);
                nbt.put("node", nodeTag);
            }
        }
    }
}
