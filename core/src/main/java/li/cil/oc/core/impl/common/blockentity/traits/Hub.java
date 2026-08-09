package li.cil.oc.core.impl.common.blockentity.traits;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;


public interface Hub extends Environment, SidedEnvironment {
    Node node();

    boolean isConnected();

    @SuppressWarnings("unused")
    Direction[] validDirections();

    Node sidedNode(Direction side);

    boolean canConnect(Direction side);

    @SuppressWarnings("unused")
    void enqueuePacket(Direction sourceSide, Packet packet);

    @SuppressWarnings("unused")
    boolean tryEnqueuePacket(Direction sourceSide, Packet packet);

    void readFromNBTForServer(CompoundTag nbt) ;

    void writeToNBTForServer(CompoundTag nbt);

    @SuppressWarnings("unused")
    void updateEntity() ;

    void dispose();

    void initialize();

    @SuppressWarnings({"unused", "SameReturnValue"})
    Plug createPlug(Direction side);

    @SuppressWarnings("unused")
    void onPlugConnect(Plug plug, Node node) ;

    @SuppressWarnings("unused")
    void onPlugDisconnect(Plug plug, Node node);

    @SuppressWarnings("unused")
    void onPlugMessage(Plug plug, Message message) ;

    @SuppressWarnings("unused")
    Node createNode(Plug plug);

    interface Plug extends Environment {
        Direction side();

        Node node();

        boolean isPrimary();

        @SuppressWarnings("unused")
        java.util.List<Plug> plugsInOtherNetworks();
    }
}
