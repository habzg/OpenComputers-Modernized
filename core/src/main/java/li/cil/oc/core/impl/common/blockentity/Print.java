package li.cil.oc.core.impl.common.blockentity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.util.ExtendedAABB;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.core.impl.util.RotationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Print extends BlockEntity implements RedstoneAware, Rotatable, Nameable {
    public static BlockEntityType<Print> TYPE;
    public final PrintData data = new PrintData();
    public AABB boundsOff = ExtendedAABB.unitBounds();
    public AABB boundsOn = ExtendedAABB.unitBounds();
    public VoxelShape shapeOff = Shapes.create(ExtendedAABB.unitBounds());
    public VoxelShape shapeOn = Shapes.create(ExtendedAABB.unitBounds());
    public boolean state = false;
    private boolean _isOutputEnabled;
    private Direction _facing = Direction.SOUTH;
    private Direction _pitch = Direction.NORTH;
    private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};
    private final int[] _output = new int[6];

    public Print(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        _isOutputEnabled = true;
    }

    @Override
    public void initialize() {
        super.initialize();
        syncFromBlockState(getBlockState());
        if (isServer()) {
            li.cil.oc.core.impl.util.EventHandlerDelegate.get().scheduleServer(this::checkRedstoneInputChanged);
        }
    }

    @Override
    public @NotNull Component getName() {
        return hasCustomName() ? Component.literal(data.label) : Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public boolean hasCustomName() {
        return data.label != null && !data.label.isEmpty();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return hasCustomName() ? Component.literal(data.label) : null;
    }

    @Override
    public Direction facing() {
        var state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            var val = state.getValue(BlockStateProperties.FACING);
            if (val.getAxis().isVertical()) return val;
            return val;
        }
        return _pitch == Direction.DOWN || _pitch == Direction.UP ? _pitch : _facing;
    }

    @Override
    public void facing(Direction value) {
        if (value == Direction.DOWN || value == Direction.UP) {
            _pitch = value;
        } else {
            _pitch = Direction.NORTH;
            _facing = value;
        }
    }

    public Direction pitch() {
        return _pitch;
    }

    public void pitch(Direction value) {
        _pitch = value;
    }

    public Direction yaw() {
        return _facing;
    }

    public void yaw(Direction value) {
        _facing = value;
    }

    @Override
    public Direction toLocal(Direction side) {
        return RotationHelper.toLocal(_pitch, _facing, side);
    }

    @Override
    public Direction toGlobal(Direction side) {
        return RotationHelper.toGlobal(_pitch, _facing, side);
    }

    @Override
    public void setFromFacing(Direction value) {
        if (value == Direction.DOWN || value == Direction.UP) {
            _pitch = value;
        } else {
            _pitch = Direction.NORTH;
            _facing = value;
        }
        onRotationChanged();
    }

    @Override
    public void setFromEntityPitchAndYaw(net.minecraft.world.entity.Entity entity) {
        Direction[] pitch2Direction = {Direction.UP, Direction.NORTH, Direction.DOWN};
        Direction[] yaw2Direction = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        Direction newPitch = pitch2Direction[(int) Math.round(entity.getXRot() / 90.0) + 1];
        Direction newYaw = yaw2Direction[Math.round(entity.getYRot() / 360 * 4) & 3];
        _pitch = newPitch;
        _facing = newYaw;
        onRotationChanged();
    }

    @Override
    public void invertRotation() {
        var newPitch = (_pitch == Direction.DOWN || _pitch == Direction.UP) ? _pitch.getOpposite() : Direction.NORTH;
        var newYaw = _facing.getOpposite();
        _pitch = newPitch;
        _facing = newYaw;
        onRotationChanged();
    }

    public void syncFromBlockState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            _facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            var bf = state.getValue(BlockStateProperties.FACING);
            if (bf.getAxis().isVertical()) {
                _pitch = bf;
            } else {
                _facing = bf;
            }
        }
    }

    private void syncFacingToBlockState() {
        var level = getLevel();
        if (level == null) return;
        var state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            var current = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (current != _facing && _facing.getAxis().isHorizontal()) {
                level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.HORIZONTAL_FACING, _facing), 3);
            }
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            var desired = _pitch.getAxis().isVertical() ? _pitch : _facing;
            var current = state.getValue(BlockStateProperties.FACING);
            if (current != desired) {
                level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.FACING, desired), 3);
            }
        }
    }

    @Override
    public boolean isOutputEnabled() {
        return _isOutputEnabled;
    }

    @Override
    public void setOutputEnabled(boolean value) {
        if (value != _isOutputEnabled) {
            _isOutputEnabled = value;
            if (!value) {
                java.util.Arrays.fill(_output, 0);
            }
            onRedstoneOutputEnabledChanged();
        }
    }

    public boolean activate() {
        if (data.hasActiveState()) {
            if (!state || !data.isButtonMode) {
                toggleState();
                return true;
            }
        }
        return false;
    }

    private Map<Object, Object> buildValueSet(int value) {
        var map = new HashMap<>();
        for (int i = 0; i < 6; i++) map.put(i, value);
        return map;
    }

    public void toggleState() {
        state = !state;
        var level = getLevel();
        if (level == null) return;
        level.playSeededSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS,
                0.3F, state ? 0.6F : 0.5F, level.random.nextLong());
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        updateRedstone();
        if (state && data.isButtonMode) {
            level.scheduleTick(worldPosition, block(), 20);
        }
        if (!level.isClientSide) {
            setChanged();
        }
    }

    @Override
    public int[] output() {
        return _output;
    }

    @Override
    public int getOutput(Direction side) {
        return _output[toLocal(side).ordinal()];
    }

    @Override
    public void setOutput(Direction side, int value) {
        var ord = toLocal(side).ordinal();
        if (_output[ord] != value) {
            _output[ord] = value;
            onRedstoneOutputChanged(side);
        }
    }

    @Override
    public void setOutput(Map<?, ?> values) {
        for (var side : Direction.values()) {
            var ord = toLocal(side).ordinal();
            var key = Integer.valueOf(ord);
            if (values.containsKey(key)) {
                var raw = values.get(key);
                if (raw instanceof Number num) {
                    setOutput(side, num.intValue());
                }
            }
        }
    }

    @Override
    public void onRedstoneOutputChanged(Direction side) {
        var level = getLevel();
        if (level == null) return;
        var neighborPos = getBlockPos().relative(side);
        level.neighborChanged(neighborPos, getBlockState().getBlock(), neighborPos);
        level.updateNeighborsAt(neighborPos, getBlockState().getBlock());
        syncRedstoneState();
    }

    @Override
    public int[] input() {
        return _input;
    }

    @Override
    public void setInput(Direction side, int value) {
        int oldInput = _input[side.ordinal()];
        _input[side.ordinal()] = value;
        if (oldInput >= 0 && oldInput != value) {
            onRedstoneInputChanged(new RedstoneChangedEventArgs(side, oldInput, value, -1));
        }
    }

    @Override
    public void setInput(int[] values) {
        for (int i = 0; i < values.length && i < _input.length; i++) {
            _input[i] = values[i];
        }
    }

    @Override
    public void updateRedstoneInput(Direction side) {
        int oldValue = _input[side.ordinal()];
        int newValue = li.cil.oc.core.impl.integration.util.BundledRedstone.computeInput(position(), side);
        if (oldValue != newValue) {
            _input[side.ordinal()] = newValue;
            onRedstoneInputChanged(new RedstoneChangedEventArgs(side, oldValue, newValue, -1));
        }
    }

    @Override
    public void checkRedstoneInputChanged() {
        if (getLevel() != null && !getLevel().isClientSide) {
            for (Direction side : Direction.values()) {
                updateRedstoneInput(side);
            }
        }
    }

    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        if (!data.emitRedstone() && data.hasActiveState()) {
            state = args.newValue() > 0;
            var level = getLevel();
            if (level == null) return;
            level.playSeededSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS,
                    0.3F, state ? 0.6F : 0.5F, level.random.nextLong());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (state && data.isButtonMode) {
                level.scheduleTick(worldPosition, block(), 20);
            }
            if (!level.isClientSide) {
                setChanged();
            }
        }
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        var dataKey = nbt.contains(OCSettings.namespace + "data") ? OCSettings.namespace + "data" : "data";
        data.load(nbt.getCompound(dataKey), getEffectiveProvider());
        state = nbt.contains(OCSettings.namespace + "state") ? nbt.getBoolean(OCSettings.namespace + "state") : nbt.getBoolean("state");
        if (nbt.contains(OCSettings.namespace + "yaw")) {
            _facing = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "yaw"));
        }
        if (nbt.contains(OCSettings.namespace + "pitch")) {
            _pitch = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "pitch"));
        }
        var outputTag = nbt.getIntArray(OCSettings.namespace + "rs.output");
        if (outputTag.length > 0) {
            System.arraycopy(outputTag, 0, _output, 0, Math.min(outputTag.length, _output.length));
        }
        updateBounds();
        updateRedstone();
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "data", t -> data.save(t, getEffectiveProvider()));
        nbt.putBoolean(OCSettings.namespace + "state", state);
        nbt.putInt(OCSettings.namespace + "yaw", _facing.get3DDataValue());
        nbt.putInt(OCSettings.namespace + "pitch", _pitch.get3DDataValue());
        nbt.putIntArray(OCSettings.namespace + "rs.output", _output);
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        super.readFromNBTForClient(nbt);
        data.load(nbt.getCompound(OCSettings.namespace + "data"), getEffectiveProvider());
        state = nbt.getBoolean(OCSettings.namespace + "state");
        if (nbt.contains(OCSettings.namespace + "yaw")) {
            _facing = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "yaw"));
        }
        if (nbt.contains(OCSettings.namespace + "pitch")) {
            _pitch = Direction.from3DDataValue(nbt.getInt(OCSettings.namespace + "pitch"));
        }
        var outputTag = nbt.getIntArray(OCSettings.namespace + "rs.output");
        if (outputTag.length > 0) {
            System.arraycopy(outputTag, 0, _output, 0, Math.min(outputTag.length, _output.length));
        }
        updateBounds();
        var level = getLevel();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        super.writeToNBTForClient(nbt);
        var level = getLevel();
        if (level != null)
            ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "data", t -> data.save(t, level.registryAccess()));
        nbt.putBoolean(OCSettings.namespace + "state", state);
        nbt.putInt(OCSettings.namespace + "yaw", _facing.get3DDataValue());
        nbt.putInt(OCSettings.namespace + "pitch", _pitch.get3DDataValue());
        nbt.putIntArray(OCSettings.namespace + "rs.output", _output);
    }

    public void updateBounds() {
        var offList = new java.util.ArrayList<>(data.stateOff);
        var onList = new java.util.ArrayList<>(data.stateOn);
        boundsOff = offList.isEmpty() ? ExtendedAABB.unitBounds() : offList.getFirst().bounds();
        for (int i = 1; i < offList.size(); i++) {
            boundsOff = boundsOff.minmax(offList.get(i).bounds());
        }
        if (ExtendedAABB.volume(boundsOff) == 0) boundsOff = ExtendedAABB.unitBounds();
        else boundsOff = ExtendedAABB.rotateTowards(boundsOff, facing());

        boundsOn = onList.isEmpty() ? ExtendedAABB.unitBounds() : onList.getFirst().bounds();
        for (int i = 1; i < onList.size(); i++) {
            boundsOn = boundsOn.minmax(onList.get(i).bounds());
        }
        if (ExtendedAABB.volume(boundsOn) == 0) boundsOn = ExtendedAABB.unitBounds();
        else boundsOn = ExtendedAABB.rotateTowards(boundsOn, facing());

        shapeOff = buildShape(data.stateOff, facing());
        shapeOn = buildShape(data.stateOn, facing());
    }

    private static VoxelShape buildShape(Set<PrintData.Shape> shapes, Direction dir) {
        if (shapes.isEmpty()) {
            return Shapes.create(ExtendedAABB.unitBounds());
        }
        VoxelShape result = Shapes.empty();
        for (var shape : shapes) {
            if (shape.texture() == null || shape.texture().isEmpty()) continue;
            var bounds = ExtendedAABB.rotateTowards(shape.bounds(), dir);
            result = Shapes.or(result, Shapes.create(bounds));
        }
        return result.optimize();
    }

    public void updateRedstone() {
        if (data.emitRedstone()) {
            setOutput(buildValueSet(data.emitRedstone(state) ? data.redstoneLevel : 0));
        }
    }

    public void onRotationChanged() {
        syncFacingToBlockState();
        updateBounds();
    }
}
