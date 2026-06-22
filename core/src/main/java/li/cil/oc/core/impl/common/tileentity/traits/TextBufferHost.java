package li.cil.oc.core.impl.common.tileentity.traits;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;


public interface TextBufferHost extends Environment {
    @SuppressWarnings("unused")
    int tier();

    @SuppressWarnings("unused")
    TextBuffer buffer();

    Node node();

    @SuppressWarnings("unused")
    void updateEntity() ;

    void readFromNBTForServer(CompoundTag nbt) ;

    void writeToNBTForServer(CompoundTag nbt);

    void readFromNBTForClient(CompoundTag nbt);

    void writeToNBTForClient(CompoundTag nbt);
}
