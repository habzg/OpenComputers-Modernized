package li.cil.oc.neoforge.common.asm.template;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.common.asm.template.SimpleComponentImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings({"DataFlowIssue", "unused"})
public abstract class SimpleEnvironment extends BlockEntity implements SimpleComponentImpl {
    @SuppressWarnings("unused")
    public SimpleEnvironment(BlockPos pos, BlockState state) {
        super(null, pos, state);
    }

    @Override
    public Node node() {
        return StaticSimpleEnvironment.node(this);
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(Message message) {
    }

    @SuppressWarnings("unused")
    public void validate() {
        StaticSimpleEnvironment.validate(this);
    }

    @SuppressWarnings("unused")
    public void invalidate() {
        StaticSimpleEnvironment.invalidate(this);
    }

    @SuppressWarnings("unused")
    public void onChunkUnload() {
        StaticSimpleEnvironment.onChunkUnload(this);
    }

    @SuppressWarnings("unused")
    public void readFromNBT(CompoundTag nbt) {
        StaticSimpleEnvironment.readFromNBT(this, nbt);
    }

    @SuppressWarnings("unused")
    public void writeToNBT(CompoundTag nbt) {
        StaticSimpleEnvironment.writeToNBT(this, nbt);
    }

    public void validate_OpenComputers() {
    }

    public void invalidate_OpenComputers() {
    }

    public void onChunkUnload_OpenComputers() {
    }

    public void readFromNBT_OpenComputers(CompoundTag nbt) {
        super.loadAdditional(nbt, null);
    }

    public void writeToNBT_OpenComputers(CompoundTag nbt) {
        super.saveAdditional(nbt, null);
    }
}
