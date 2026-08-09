package li.cil.oc.core.impl.common.blockentity.traits;

import java.util.Map;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface RedstoneAware extends RotationAware {
    default int[] input() {
        return new int[]{-1, -1, -1, -1, -1, -1};
    }

    default int[] output() {
        return new int[6];
    }

    default boolean isOutputEnabled() {
        return false;
    }

    default void setOutputEnabled(boolean value) {
    }

    @SuppressWarnings("unused")
    default boolean shouldUpdateInput() {
        return false;
    }

    @SuppressWarnings("unused")
    default void shouldUpdateInput(boolean value) {
    }

    default int getInput(Direction side) {
        return 0;
    }

    @SuppressWarnings("unused")
    default void setInput(Direction side, int value) {
    }

    @SuppressWarnings("unused")
    default void setInput(int[] values) {
    }

    default int maxInput() {
        return 0;
    }

    default int getOutput(Direction side) {
        return 0;
    }

    default void setOutput(Direction side, int value) {
    }

    default void setOutput(Map<?, ?> values) {
    }

    default void checkRedstoneInputChanged() {
    }

    @SuppressWarnings("unused")
    default void updateRedstoneInput(Direction side) {
    }

    default void updateEntity() {
    }

    @SuppressWarnings("unused")
    default void validate() {
    }

    @SuppressWarnings("unused")
    default void readFromNBTForServer(CompoundTag nbt) {
    }

    @SuppressWarnings("unused")
    default void writeToNBTForServer(CompoundTag nbt) {
    }

    @SuppressWarnings("unused")
    default void readFromNBTForClient(CompoundTag nbt) {
    }

    @SuppressWarnings("unused")
    default void writeToNBTForClient(CompoundTag nbt) {
    }

    @SuppressWarnings("EmptyMethod")
    default void onRedstoneInputChanged(int ignoredSide, int ignoredOldValue, int ignoredNewValue) {
    }

    @SuppressWarnings("EmptyMethod")
    default void onRedstoneInputChanged(int ignoredSide, int ignoredOldValue, int ignoredNewValue, int ignoredColor) {
    }

    @SuppressWarnings("EmptyMethod")
    default void onRedstoneOutputEnabledChanged() {
        if (this instanceof BlockEntity be && be.getLevel() != null) {
            be.getLevel().updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
            syncRedstoneState();
        }
    }

    default void onRedstoneOutputChanged(Direction side) {
        if (this instanceof BlockEntity be && be.getLevel() != null) {
            Level level = be.getLevel();
            BlockPos outPos = be.getBlockPos().relative(side);
            level.neighborChanged(outPos, be.getBlockState().getBlock(), be.getBlockPos());
            level.updateNeighborsAtExceptFromFacing(outPos, level.getBlockState(outPos).getBlock(), side.getOpposite());
        }
        syncRedstoneState();
    }

    default void syncRedstoneState() {
        if (this instanceof BlockEntity be && be.getLevel() != null && !be.getLevel().isClientSide()) {
            PacketSender.sendRedstoneState(be, isOutputEnabled(), output());
        }
    }

    record RedstoneChangedEventArgs(Direction side, int oldValue, int newValue, int color) {
    }

    default void readRedstoneFromNBT(CompoundTag nbt) {
        String ns = OCSettings.namespace;
        if (nbt.contains(ns + "rs.output")) {
            int[] saved = nbt.getIntArray(ns + "rs.output");
            int[] current = output();
            System.arraycopy(saved, 0, current, 0, Math.min(saved.length, current.length));
        }
        if (nbt.contains(ns + "rs.input")) {
            int[] saved = nbt.getIntArray(ns + "rs.input");
            int[] current = input();
            System.arraycopy(saved, 0, current, 0, Math.min(saved.length, current.length));
        }
    }

    default void writeRedstoneToNBT(CompoundTag nbt) {
        nbt.putIntArray(OCSettings.namespace + "rs.output", output());
        nbt.putIntArray(OCSettings.namespace + "rs.input", input());
    }
}
