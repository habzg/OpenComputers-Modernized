package li.cil.oc.core.impl.common.blockentity.traits;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.traits.power.AppliedEnergistics2;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockEntity.class);
    public static boolean savingForClients = false;
    protected HolderLookup.Provider loadProvider;

    private boolean initialized;

    public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setLevel(net.minecraft.world.level.@NotNull Level level) {
        super.setLevel(level);
        if (!initialized && !level.isClientSide) {
            initialized = true;
            EventHandlerDelegate.get().scheduleServer(this::initialize);
        }
    }

    public boolean isClient() {
        return !isServer();
    }

    public boolean isServer() {
        var level = getLevel();
        if (level != null) return !level.isClientSide;
        return true;
    }

    public Block block() {
        return getBlockState().getBlock();
    }

    public BlockPosition position() {
        return BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        dispose();
    }

    public void initialize() {
        if (this instanceof AppliedEnergistics2 ae2) {
            ae2.ae2Validate();
        }
    }

    public void dispose() {
        if (this instanceof AppliedEnergistics2 ae2) {
            ae2.ae2Invalidate();
        }
    }

    public void updateEntity() {
        if (this instanceof AppliedEnergistics2 ae2) {
            ae2.ae2UpdateEntity();
        }
        var level = getLevel();
        if (level != null && OCSettings.get().periodicallyForceLightUpdate && level.getGameTime() % 40 == 0 && getBlockState().getLightEmission() > 0) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void readFromNBTForServer(CompoundTag nbt) {
        if (this instanceof AppliedEnergistics2 ae2) {
            ae2.ae2ReadFromNBT(nbt);
        }
        if (this instanceof RedstoneAware ra) {
            ra.readRedstoneFromNBT(nbt);
        }
    }

    public void writeToNBTForServer(CompoundTag nbt) {
        if (this instanceof AppliedEnergistics2 ae2) {
            ae2.ae2WriteToNBT(nbt);
        }
        if (this instanceof RedstoneAware ra) {
            ra.writeRedstoneToNBT(nbt);
        }
    }

    public void readFromNBTForClient(CompoundTag nbt) {
    }

    public void writeToNBTForClient(CompoundTag nbt) {
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        loadProvider = provider;
        super.loadAdditional(nbt, provider);
        if (isServer()) {
            readFromNBTForServer(nbt);
        } else {
            readFromNBTForClient(nbt);
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        loadProvider = provider;
        super.saveAdditional(nbt, provider);
        if (isServer()) {
            writeToNBTForServer(nbt);
        }
    }

    public HolderLookup.Provider getEffectiveProvider() {
        if (loadProvider != null) return loadProvider;
        var level = getLevel();
        if (level != null) return level.registryAccess();
        return null;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        var nbt = new CompoundTag();
        savingForClients = true;
        try {
            try {
                writeToNBTForClient(nbt);
            } catch (Throwable e) {
                LOGGER.warn("Problem writing BlockEntity description packet", e);
            }
            if (nbt.isEmpty()) return null;
            return ClientboundBlockEntityDataPacket.create(this, (be, registry) -> nbt);
        } finally {
            savingForClients = false;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider provider) {
        if (getLevel() == null) return new CompoundTag();
        savingForClients = true;
        var nbt = super.getUpdateTag(provider);
        try {
            writeToNBTForClient(nbt);
        } catch (Throwable ignored) {
        }
        savingForClients = false;
        return nbt;
    }


}
