package li.cil.oc.core.impl.common.block;

import java.util.List;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.blockentity.RobotBase;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class RobotProxy extends RedstoneAware implements GUI, StateAware {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final VoxelShape SHAPE = box(2, 2, 2, 14, 14, 14);

    protected RobotProxy(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    protected abstract RobotBase findRobotBase(BlockGetter world, BlockPos pos);

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public net.minecraft.world.item.Rarity rarity(ItemStack stack) {
        var data = new RobotData(stack);
        return li.cil.oc.core.impl.util.Rarity.byTier(data.tier);
    }

    @Override
    protected void tooltipHead(int metadata, ItemStack stack, Player player, List<net.minecraft.network.chat.Component> tooltip, boolean advanced) {
        super.tooltipHead(metadata, stack, player, tooltip, advanced);
        var data = new RobotData(stack);
        if (data.totalEnergy > 0) {
            tooltip.addAll(Tooltip.get("robot_storedenergy", data.totalEnergy));
        }
        for (var component : data.components) {
            if (!component.isEmpty()) {
                var tag = component.get(DataComponents.CUSTOM_DATA);
                if (tag != null && !tag.isEmpty()) {
                    var nbt = tag.copyTag();
                    if (nbt.contains(OCSettings.namespace + "xp", Tag.TAG_DOUBLE)) {
                        double xp = nbt.getDouble(OCSettings.namespace + "xp");
                        int level = Math.min((int) (Math.pow(xp - OCSettings.get().baseXpToLevel, 1.0 / OCSettings.get().exponentialXpGrowth) / OCSettings.get().constantXpGrowth), 30);
                        if (level > 0) {
                            tooltip.addAll(Tooltip.get("robot_level", level));
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<net.minecraft.network.chat.Component> tooltip, boolean advanced) {
        tooltip.addAll(Tooltip.get("robot"));
    }

    @Override
    public void tooltipTail(int metadata, ItemStack stack, Player player, List<net.minecraft.network.chat.Component> tooltip, boolean advanced) {
        super.tooltipTail(metadata, stack, player, tooltip, advanced);
        var data = new RobotData(stack);
        var components = new java.util.ArrayList<ItemStack>();
        components.addAll(data.containers);
        components.addAll(data.components);
        if (!components.isEmpty()) {
            var header = Tooltip.extended("server.Components");
            if (!header.isEmpty()) {
                tooltip.addAll(header);
                for (var component : components) {
                    if (!component.isEmpty()) {
                        tooltip.add(net.minecraft.network.chat.Component.literal("- ").append(component.getHoverName()));
                    }
                }
            }
        }
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        var robot = findRobotBase(world, pos);
        if (robot != null && robot.isAnimatingMove()) {
            var remaining = robot.animationTicksTotal > 0 ? (double) robot.animationTicksLeft / (double) robot.animationTicksTotal : 0.0;
            var dx = (robot.moveFromX - robot.getBlockPos().getX()) * remaining;
            var dy = (robot.moveFromY - robot.getBlockPos().getY()) * remaining;
            var dz = (robot.moveFromZ - robot.getBlockPos().getZ()) * remaining;
            return SHAPE.move(dx, dy, dz);
        }
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        var robot = findRobotBase(world, pos);
        if (robot != null && robot.animationTicksLeft <= 0) {
            return Shapes.empty();
        }
        return SHAPE;
    }

    @Override
    public net.minecraft.world.level.block.@NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Override
    public void onPlace(@NotNull BlockState newState, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(newState, world, pos, oldState, isMoving);
        if (oldState.getBlock() == newState.getBlock() && oldState != newState) {
            var robot = findRobotBase(world, pos);
            if (robot != null) {
                robot.syncFromBlockState(newState);
            }
        }
    }

    @Override
    public int guiType() {
        return GuiType.Robot;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader world, @NotNull BlockPos pos, @NotNull BlockState state) {
        var robot = findRobotBase(world, pos);
        if (robot != null) {
            return robot.info.copyItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void onDropInventory(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }
}
