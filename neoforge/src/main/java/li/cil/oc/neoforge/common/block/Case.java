package li.cil.oc.neoforge.common.block;

import java.util.List;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.block.traits.PowerAcceptor;
import li.cil.oc.core.impl.util.OCBlockStateProperties;
import li.cil.oc.core.impl.util.Tooltip;
import li.cil.oc.neoforge.common.blockentity.CaseTile;
import li.cil.oc.neoforge.common.init.BlockEntities;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

public class Case extends RedstoneAware implements PowerAcceptor, GUI, StateAware {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty RUNNING = OCBlockStateProperties.CASE_RUNNING;

    public final int tier;

    public Case(int tier) {
        super();
        this.tier = tier;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(RUNNING, false);
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        setFacing(world, pos, state.getValue(FACING));
    }

    @Override
    public int guiType() {
        return GuiType.Case;
    }

    @Override
    public double energyThroughput() {
        return OCSettings.get().caseRate[tier];
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CaseTile(pos, state, this.tier);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Case computer) {
            computer.onNeighborChanged();
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.@NotNull Level ignoredLevel, @NotNull BlockState ignoredState, @NotNull BlockEntityType<T> type) {
        var caseType = BlockEntities.CASE.get();
        return type == caseType ? (lvl, pos, st, te) -> ((li.cil.oc.core.impl.common.blockentity.Case) te).updateEntity() : null;
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        tooltip.addAll(Tooltip.get(getClass().getSimpleName(), slots()));
    }

    private String slots() {
        return switch (tier) {
            case 0 -> "2/1/1";
            case 1 -> "2/2/2";
            case 2, 3 -> "3/2/3";
            default -> "0/0/0";
        };
    }

    @Override
    public net.minecraft.world.item.Rarity rarity(ItemStack ignoredStack) {
        return li.cil.oc.core.impl.util.Rarity.byTier(tier);
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                BlockEntity te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Case computer && computer.machine() != null && !computer.machine().isRunning() && computer.isUseableByPlayer(player)) {
                    computer.machine().start();
                }
            }
            return true;
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }

    @Override
    public boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, boolean willHarvest, @NotNull FluidState fluid) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Case c) {
            if (c.isCreative() && (!player.getAbilities().instabuild || !c.canInteract(player.getGameProfile().getName())))
                return false;
            if (!c.canInteract(player.getGameProfile().getName()))
                return false;
            return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }
}
