package li.cil.oc.core.impl.common.tileentity.traits;


public interface Inventory extends li.cil.oc.core.impl.common.inventory.Inventory {
    @SuppressWarnings("unused")
    void readFromNBTForServer(net.minecraft.nbt.CompoundTag nbt) ;

    @SuppressWarnings("unused")
    void writeToNBTForServer(net.minecraft.nbt.CompoundTag nbt);

    @SuppressWarnings("unused")
    boolean isUseableByPlayer(net.minecraft.world.entity.player.Player player);

    @SuppressWarnings("unused")
    void dropSlot(int slot);

    @SuppressWarnings("unused")
    void dropSlot(int slot, int count, net.minecraft.core.Direction direction);

    void dropAllSlots();

    @SuppressWarnings("unused")
    void spawnStackInWorld(net.minecraft.world.item.ItemStack stack);

    @SuppressWarnings("unused")
    void spawnStackInWorld(net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction direction);

    @SuppressWarnings("unused")
    int x();

    @SuppressWarnings("unused")
    int y();

    @SuppressWarnings("unused")
    int z();

    @SuppressWarnings("unused")
    net.minecraft.world.level.Level getLevel();
}
