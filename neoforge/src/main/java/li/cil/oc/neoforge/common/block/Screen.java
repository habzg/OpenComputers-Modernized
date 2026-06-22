package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
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

import java.util.List;

public class Screen extends RedstoneAware {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public final int tier;

    public Screen(int tier) {
        super();
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.Screen screen) {
            screen.setFromEntityPitchAndYaw(placer);
            screen.invertRotation();
            screen.delayUntilCheckForMultiBlock = 0;
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        var screenType = li.cil.oc.neoforge.common.init.TileEntities.SCREEN.get();
        return type == screenType ? (lvl, pos, st, te) -> ((li.cil.oc.core.impl.common.tileentity.Screen) te).updateEntity() : null;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.tileentity.Screen(pos, state, this.tier);
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        int w = Settings.screenResolutionsByTier[tier][0];
        int h = Settings.screenResolutionsByTier[tier][1];
        int depth = PackedColor.Depth.bits(Settings.screenDepthsByTier[tier]);
        tooltip.addAll(Tooltip.get(getClass().getSimpleName(), w, h, depth));
    }

    @Override
    public net.minecraft.world.item.Rarity rarity(ItemStack stack) {
        return li.cil.oc.core.impl.util.Rarity.byTier(tier);
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        return rightClick(world, pos, player, side, hitX, hitY, hitZ, false);
    }

    public boolean rightClick(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, boolean force) {
        if (Wrench.holdsApplicableWrench(player, BlockPosition.apply(pos.getX(), pos.getY(), pos.getZ(), world)) && !force)
            return false;
        if (li.cil.oc.api.Items.get(player.getMainHandItem()) == li.cil.oc.api.Items.get(Constants.ItemName.Analyzer))
            return false;
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.Screen screen) {
            if (screen.hasKeyboard() && (force || player.isShiftKeyDown() == screen.origin.invertTouchMode)) {
                if (world.isClientSide) {
                    li.cil.oc.neoforge.client.GuiHandler.openScreen(li.cil.oc.core.common.GuiType.Screen, pos.getX(), pos.getY(), pos.getZ());
                }
                return true;
            }
            if (screen.tier > 0 && side == screen.facing()) {
                if (world.isClientSide) {
                    screen.click(hitX, hitY, hitZ);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void stepOn(Level world, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Entity entity) {
        if (!world.isClientSide) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.tileentity.Screen screen && screen.tier > 0 && screen.facing() == Direction.UP) {
                screen.walk(entity);
            }
        }
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (entity instanceof AbstractArrow arrow && world.isClientSide) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.tileentity.Screen screen && screen.tier > 0) {
                Direction side = getHitSide(entity, pos);
                if (side == screen.facing()) {
                    screen.shot(arrow);
                }
            }
        }
    }

    private static Direction getHitSide(Entity entity, BlockPos pos) {
        double hitX = Math.clamp(entity.getX() - pos.getX(), 0, 1);
        double hitY = Math.clamp(entity.getY() - pos.getY(), 0, 1);
        double hitZ = Math.clamp(entity.getZ() - pos.getZ(), 0, 1);
        double absX = Math.abs(hitX - 0.5);
        double absY = Math.abs(hitY - 0.5);
        double absZ = Math.abs(hitZ - 0.5);
        if (absX > absY && absX > absZ) {
            return hitX < 0.5 ? Direction.WEST : Direction.EAST;
        } else if (absY > absZ) {
            return hitY < 0.5 ? Direction.DOWN : Direction.UP;
        } else {
            return hitZ < 0.5 ? Direction.NORTH : Direction.SOUTH;
        }
    }
}
