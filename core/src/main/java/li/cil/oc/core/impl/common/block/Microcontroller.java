package li.cil.oc.core.impl.common.block;

import java.util.List;
import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.block.traits.CustomDrops;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.impl.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;

public class Microcontroller extends SimpleBlock implements PowerAcceptor, StateAware, CustomDrops<li.cil.oc.core.impl.common.blockentity.Microcontroller> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static BlockEntityType<?> TYPE;

    public Microcontroller(BlockEntityType<?> blockType) {
        super();
        TYPE = blockType;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public Microcontroller() {
        super();
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public double energyThroughput() {
        return OCSettings.get().caseRate[Tier.One];
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Microcontroller(pos, state);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Microcontroller mcu) {
            mcu.onNeighborChanged();
        }
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (placer != null) {
            var te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.blockentity.Microcontroller mcu) {
                mcu.facing(state.getValue(FACING));
            }
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == TYPE ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.blockentity.Microcontroller) te).updateEntity();
            } catch (Exception e) {
                Log.get().warn("Error in microcontroller tick", e);
            }
        } : null;
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (Wrench.holdsApplicableWrench(player, BlockPosition.apply(pos.getX(), pos.getY(), pos.getZ(), world)))
            return false;
        if (!player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Microcontroller mcu) {
                    if (mcu.machine() != null) {
                        if (mcu.machine().isRunning()) mcu.machine().stop();
                        else mcu.machine().start();
                    }
                }
            }
            return true;
        } else if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            var heldInfo = Items.get(player.getItemInHand(InteractionHand.MAIN_HAND));
            if (heldInfo != null && Constants.ItemName.EEPROM.equals(heldInfo.name())) {
                if (!world.isClientSide) {
                    var te = world.getBlockEntity(pos);
                    if (te instanceof li.cil.oc.core.impl.common.blockentity.Microcontroller mcu) {
                        var held = player.getItemInHand(InteractionHand.MAIN_HAND);
                        var newEeprom = held.split(1);
                        var oldEeprom = mcu.changeEEPROM(newEeprom);
                        if (oldEeprom != null && !oldEeprom.isEmpty()) {
                            InventoryUtils.addToPlayerInventory(oldEeprom, player);
                        }
                    }
                }
                return true;
            }
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public void tooltipTail(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        super.tooltipTail(metadata, stack, player, tooltip, advanced);
        var info = new li.cil.oc.core.impl.common.item.data.MicrocontrollerData(stack);
        if (!info.components.isEmpty()) {
            var header = li.cil.oc.core.impl.util.Tooltip.extended("server.Components");
            if (!header.isEmpty()) {
                tooltip.addAll(header);
                for (var component : info.components) {
                    if (component != null && !component.isEmpty()) {
                        tooltip.add(Component.literal("- ").append(component.getHoverName()));
                    }
                }
            }
        }
    }

    @Override
    public Class<li.cil.oc.core.impl.common.blockentity.Microcontroller> getBlockClass() {
        return li.cil.oc.core.impl.common.blockentity.Microcontroller.class;
    }

    @Override
    public void doCustomInit(li.cil.oc.core.impl.common.blockentity.Microcontroller blockEntity, LivingEntity player, ItemStack stack) {
        var level = blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            blockEntity.info.load(stack, level.registryAccess());
            ((li.cil.oc.api.network.Connector) blockEntity.snooperNode).changeBuffer(
                    blockEntity.info.storedEnergy - ((li.cil.oc.api.network.Connector) blockEntity.snooperNode).localBuffer());
        }
    }

    @Override
    public void doCustomDrops(li.cil.oc.core.impl.common.blockentity.Microcontroller blockEntity, Player player, boolean willHarvest) {
        blockEntity.info.storedEnergy = (int) ((li.cil.oc.api.network.Connector) blockEntity.snooperNode).localBuffer();
        var level = blockEntity.getLevel();
        if (level != null) {
            Block.popResource(level, blockEntity.getBlockPos(), blockEntity.info.createItemStack());
        }
    }
}
