package li.cil.oc.neoforge.integration.cbmultipart;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.part.NormalOcclusionPart;
import codechicken.multipart.api.part.SlottedPart;
import codechicken.multipart.api.part.redstone.RedstonePart;
import codechicken.multipart.util.PartMap;
import codechicken.multipart.util.PartRayTraceResult;
import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedAABB;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.neoforge.common.block.Print;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class PrintPart extends SimpleBlockPart
        implements NormalOcclusionPart, SlottedPart, RedstonePart {

    private static final float q0 = 0 / 16f;
    private static final float q1 = 4 / 16f;
    private static final float q2 = 12 / 16f;
    private static final float q3 = 16 / 16f;

    private static final AABB[] SLOT_BOUNDS = new AABB[]{
            new AABB(q1, q0, q1, q2, q1, q2), new AABB(q1, q2, q1, q2, q3, q2),
            new AABB(q1, q1, q0, q2, q2, q1), new AABB(q1, q1, q2, q2, q2, q3),
            new AABB(q0, q1, q1, q1, q2, q2), new AABB(q2, q1, q1, q3, q2, q2),
            new AABB(q1, q1, q1, q2, q2, q2),
            new AABB(q0, q0, q0, q1, q1, q1), new AABB(q0, q2, q0, q1, q3, q1),
            new AABB(q0, q0, q2, q1, q1, q3), new AABB(q0, q2, q2, q1, q3, q3),
            new AABB(q2, q0, q0, q3, q1, q1), new AABB(q2, q2, q0, q3, q3, q1),
            new AABB(q2, q0, q2, q3, q1, q3), new AABB(q2, q2, q2, q3, q3, q3),
            new AABB(q0, q1, q0, q1, q2, q1), new AABB(q0, q1, q2, q1, q2, q3),
            new AABB(q2, q1, q0, q3, q2, q1), new AABB(q2, q1, q2, q3, q2, q3),
            new AABB(q0, q0, q1, q1, q1, q2), new AABB(q2, q0, q1, q3, q1, q2),
            new AABB(q0, q2, q1, q1, q3, q2), new AABB(q2, q2, q1, q3, q3, q2),
            new AABB(q1, q0, q0, q2, q1, q1), new AABB(q1, q2, q0, q2, q3, q1),
            new AABB(q1, q0, q2, q2, q1, q3), new AABB(q1, q2, q2, q2, q3, q3),
    };

    public Direction facing = Direction.SOUTH;
    public PrintData data = new PrintData();
    public AABB boundsOff = ExtendedAABB.unitBounds();
    public AABB boundsOn = ExtendedAABB.unitBounds();
    public boolean state = false;
    private boolean toggling = false;

    public PrintPart() {
        this(null);
    }

    public PrintPart(li.cil.oc.core.impl.common.blockentity.Print original) {
        if (original != null) {
            facing = original.facing();
            data = original.data;
            boundsOff = original.boundsOff;
            boundsOn = original.boundsOn;
            state = original.state;
        }
    }

    @Override
    public @NotNull MultipartType<?> getType() {
        return MultipartRegistrations.PRINT_TYPE.get();
    }

    @Override
    public Print simpleBlock() {
        return (Print) Items.get(Constants.BlockName.Print).block();
    }

    @Override
    public int getLightEmission() {
        return data.lightLevel;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull CollisionContext context) {
        return Shapes.create(state ? boundsOn : boundsOff);
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape() {
        var shapes = state ? data.stateOn : data.stateOff;
        var result = Shapes.empty();
        for (PrintData.Shape shape : shapes) {
            AABB rotated = ExtendedAABB.rotateTowards(shape.bounds(), facing);
            result = Shapes.joinUnoptimized(result, Shapes.create(rotated), net.minecraft.world.phys.shapes.BooleanOp.OR);
        }
        return result;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull CollisionContext context) {
        return getOcclusionShape();
    }

    @Override
    public int getSlotMask() {
        int mask = 0;
        VoxelShape occlusion = getOcclusionShape();
        for (PartMap slot : PartMap.values()) {
            AABB slotBound = SLOT_BOUNDS[slot.i];
            VoxelShape slotShape = Shapes.create(slotBound);
            if (Shapes.joinIsNotEmpty(occlusion, slotShape, net.minecraft.world.phys.shapes.BooleanOp.AND)) {
                mask |= slot.mask;
            }
        }
        return mask;
    }

    @Override
    public boolean canConnectRedstone(int side) {
        return true;
    }

    @Override
    public int strongPowerLevel(int side) {
        return weakPowerLevel(side);
    }

    @Override
    public int weakPowerLevel(int side) {
        return data.emitRedstone(state) ? data.redstoneLevel : 0;
    }

    @Override
    public net.minecraft.world.@NotNull InteractionResult useWithoutItem(@NotNull Player player, @NotNull PartRayTraceResult hit) {
        if (data.hasActiveState()) {
            if (!state || !data.isButtonMode) {
                toggleState();
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    public void toggleState() {
        if (canToggle()) {
            toggling = true;
            state = !state;
            BlockPos pos = pos();
            level().playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON,
                    SoundSource.BLOCKS, 0.3F, state ? 0.6F : 0.5F);
            tile().notifyPartChange(this);
            sendUpdate(this::writeDesc);
            if (state && data.isButtonMode) {
                scheduleTick(20);
            }
            toggling = false;
        }
    }

    public boolean canToggle() {
        if (toggling || !hasLevel() || level().isClientSide) return false;
        PrintPart toggled = new PrintPart();
        toggled.facing = this.facing;
        toggled.data = this.data;
        toggled.state = !this.state;
        toggled.boundsOff = this.boundsOff;
        toggled.boundsOn = this.boundsOn;
        return tile().canReplacePart(this, toggled);
    }

    @Override
    public void scheduledTick() {
        if (state) toggleState();
    }

    @Override
    public @NotNull ItemStack getCloneStack(@NotNull PartRayTraceResult hit, @NotNull Player player) {
        return data.createItemStack();
    }

    @Override
    public @NotNull Iterable<ItemStack> getDrops() {
        return java.util.Collections.singletonList(data.createItemStack());
    }

    @Override
    public void onPartChanged(codechicken.multipart.api.part.MultiPart part) {
        super.onPartChanged(part);
        checkRedstone();
    }

    @Override
    public void onNeighborBlockChanged(@NotNull BlockPos from) {
        super.onNeighborBlockChanged(from);
        checkRedstone();
    }

    private void checkRedstone() {
        int newMaxValue = computeInput();
        boolean newState = newMaxValue > 1;
        if (!data.emitRedstone() && data.hasActiveState() && state != newState) {
            toggleState();
        }
    }

    private int computeInput() {
        int inner = 0;
        for (var part : tile().getPartList()) {
            if (part instanceof PrintPart print) {
                if (print.data.emitRedstone(print.state)) {
                    inner = Math.max(inner, print.data.redstoneLevel);
                }
            }
        }
        int maxBundled = 0;
        BlockPos pos = pos();
        for (Direction dir : Direction.values()) {
            maxBundled = Math.max(maxBundled,
                    BundledRedstone.computeInput(new BlockPosition(pos.getX(), pos.getY(), pos.getZ(), level()), dir));
        }
        return Math.max(inner, maxBundled);
    }

    @Override
    public void save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.save(nbt, registries);
        ExtendedNBT.setDirection(nbt, "facing", facing);
        var dataNbt = new CompoundTag();
        data.save(dataNbt, registries);
        nbt.put("data", dataNbt);
        nbt.putBoolean("state", state);
    }

    @Override
    public void load(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.load(nbt, registries);
        Direction dir = ExtendedNBT.getDirection(nbt, "facing");
        if (dir != null) facing = dir;
        if (nbt.contains("data")) {
            data.load(nbt.getCompound("data"), registries);
        }
        state = nbt.getBoolean("state");
        updateBounds();
    }

    @Override
    public void writeDesc(@NotNull MCDataOutput packet) {
        super.writeDesc(packet);
        packet.writeByte((byte) facing.get3DDataValue());
        var nbt = new CompoundTag();
        data.save(nbt, level().registryAccess());
        packet.writeCompoundNBT(nbt);
        packet.writeBoolean(state);
    }

    @Override
    public void readDesc(@NotNull MCDataInput packet) {
        super.readDesc(packet);
        facing = Direction.from3DDataValue(packet.readUByte());
        data.load(packet.readCompoundNBT(), level().registryAccess());
        state = packet.readBoolean();
        updateBounds();
    }

    public void updateBounds() {
        boundsOff = computeBounds(data.stateOff);
        boundsOn = computeBounds(data.stateOn);
    }

    private AABB computeBounds(java.util.Set<PrintData.Shape> shapes) {
        if (shapes.isEmpty()) return ExtendedAABB.unitBounds();
        AABB result = ExtendedAABB.unitBounds();
        boolean first = true;
        for (PrintData.Shape shape : shapes) {
            if (first) {
                result = shape.bounds();
                first = false;
            } else {
                result = result.minmax(shape.bounds());
            }
        }
        if (ExtendedAABB.volume(result) == 0) return ExtendedAABB.unitBounds();
        return ExtendedAABB.rotateTowards(result, facing);
    }
}
