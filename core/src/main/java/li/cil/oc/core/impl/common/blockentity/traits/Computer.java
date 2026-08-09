package li.cil.oc.core.impl.common.blockentity.traits;

import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;


public interface Computer extends Environment, ComponentInventory, Rotatable, BundledRedstoneAware, Analyzable, MachineHost, li.cil.oc.api.util.StateAware {
    li.cil.oc.api.machine.Machine machine();

    Node node();

    @SuppressWarnings("unused")
    boolean isRunning();

    void setRunning(boolean value);

    @SuppressWarnings("unused")
    boolean hasErrored();

    void hasErrored(boolean value);

    @SuppressWarnings("unused")
    boolean canInteract(String player);

    void setUsers(Iterable<String> list);

    @SuppressWarnings("unused")
    boolean hasRedstoneCard();

    @SuppressWarnings("unused")
    Object[] getInterfaces(int side);

    @SuppressWarnings("unused")
    java.util.Set<String> users();

    @SuppressWarnings("unused")
    int getSizeInventory();

    @SuppressWarnings("unused")
    net.minecraft.world.item.ItemStack getStackInSlot(int slot);

    boolean isComponentSlot(int slot, net.minecraft.world.item.ItemStack stack);

    void onMachineConnect(Node node);

    void onMachineDisconnect(Node node);

    java.lang.Iterable<net.minecraft.world.item.ItemStack> internalComponents();

    @SuppressWarnings("unused")
    Iterable<ManagedEnvironment> installedComponents();

    void readFromNBTForServer(net.minecraft.nbt.CompoundTag nbt) ;

    void writeToNBTForServer(net.minecraft.nbt.CompoundTag nbt);

    void readFromNBTForClient(net.minecraft.nbt.CompoundTag nbt);

    void writeToNBTForClient(net.minecraft.nbt.CompoundTag nbt);

    void updateEntity();

    void dispose();

    @SuppressWarnings("unused")
    void markDirty();

    boolean isUseableByPlayer(net.minecraft.world.entity.player.Player player);

    Node[] onAnalyze(net.minecraft.world.entity.player.Player player, net.minecraft.core.Direction side, float hitX, float hitY, float hitZ);

    void onRotationChanged();

    void checkRedstoneInputChanged();

    java.util.EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState();

    default String runSound() {
        return "computer_running";
    }
}
