package li.cil.oc.core.impl.common.asm.template;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.nbt.CompoundTag;

public interface SimpleComponentImpl extends Environment, SimpleComponent {
    @SuppressWarnings("EmptyMethod")
    void validate_OpenComputers();

    @SuppressWarnings("EmptyMethod")
    void invalidate_OpenComputers();

    @SuppressWarnings("EmptyMethod")
    void onChunkUnload_OpenComputers();

    void readFromNBT_OpenComputers(CompoundTag nbt);

    void writeToNBT_OpenComputers(CompoundTag nbt);
}
