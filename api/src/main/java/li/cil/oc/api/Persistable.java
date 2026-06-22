package li.cil.oc.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * An object that can be persisted to an NBT tag and restored back from it.
 */
public interface Persistable {
    /**
     * Restores a previous state of the object from the specified NBT tag.
     */
    void load(CompoundTag nbt, HolderLookup.Provider provider);

    /**
     * Saves the current state of the object into the specified NBT tag.
     */
    void save(CompoundTag nbt, HolderLookup.Provider provider);
}
