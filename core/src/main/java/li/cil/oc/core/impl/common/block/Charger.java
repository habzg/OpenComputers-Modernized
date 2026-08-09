package li.cil.oc.core.impl.common.block;

import java.util.List;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.Log;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
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

public class Charger extends RedstoneAware implements PowerAcceptor, GUI, StateAware {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static BlockEntityType<?> TYPE;

    public Charger(BlockEntityType<?> blockType) {
        super();
        TYPE = blockType;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public Charger() {
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
    public int guiType() {
        return GuiType.Charger;
    }

    @Override
    public double energyThroughput() {
        return OCSettings.get().chargerRate;
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (Wrench.holdsApplicableWrench(player, BlockPosition.apply(pos.getX(), pos.getY(), pos.getZ(), world))) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.blockentity.Charger charger) {
                if (!world.isClientSide) {
                    charger.invertSignal = !charger.invertSignal;
                    charger.chargeSpeed = 1.0 - charger.chargeSpeed;
                    PacketSender.sendChargerState(charger, charger.chargeSpeed, charger.hasPower);
                    Wrench.wrenchUsed(player, BlockPosition.apply(pos.getX(), pos.getY(), pos.getZ(), world));
                }
                return true;
            }
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Charger charger) {
            charger.onNeighborChanged();
        }
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Charger(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return type == TYPE ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.core.impl.common.blockentity.Charger) te).updateEntity();
            } catch (Exception e) {
                Log.get().warn("Error in charger tick", e);
            }
        } : null;
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        tooltip.addAll(Tooltip.get(getClass().getSimpleName(), (int) OCSettings.get().chargerRate));
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }
}
