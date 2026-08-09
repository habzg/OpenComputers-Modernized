package li.cil.oc.core.impl.common.block;

import java.util.List;
import java.util.function.Consumer;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.core.impl.util.Tooltip;
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

public class Screen extends RedstoneAware {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public final int tier;

    public static BlockEntityType<?> TYPE;
    private static Consumer<BlockPos> openGui = pos -> {};

    public static void setOpenGui(Consumer<BlockPos> callback) {
        openGui = callback;
    }

    public Screen(int tier, BlockEntityType<?> blockType) {
        super();
        this.tier = tier;
        TYPE = blockType;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(AbstractBlock.LIGHT_LEVEL, 5));
    }

    public Screen(int tier) {
        super();
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(AbstractBlock.LIGHT_LEVEL, 5));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
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
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Screen screen) {
            screen.setFromEntityPitchAndYaw(placer);
            screen.invertRotation();
            screen.delayUntilCheckForMultiBlock = 0;
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return type == TYPE ? (lvl, pos, st, te) -> ((li.cil.oc.core.impl.common.blockentity.Screen) te).updateEntity() : null;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.core.impl.common.blockentity.Screen(pos, state, this.tier);
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        int w = OCSettings.screenResolutionsByTier[tier][0];
        int h = OCSettings.screenResolutionsByTier[tier][1];
        int depth = PackedColor.Depth.bits(OCSettings.screenDepthsByTier[tier]);
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
        if (te instanceof li.cil.oc.core.impl.common.blockentity.Screen screen) {
            if (screen.hasKeyboard() && (force || player.isShiftKeyDown() == screen.origin.invertTouchMode)) {
                if (world.isClientSide) {
                    openGui.accept(pos);
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
            if (te instanceof li.cil.oc.core.impl.common.blockentity.Screen screen && screen.tier > 0 && screen.facing() == Direction.UP) {
                screen.walk(entity);
            }
        }
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (entity instanceof AbstractArrow arrow && world.isClientSide) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof li.cil.oc.core.impl.common.blockentity.Screen screen && screen.tier > 0) {
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
