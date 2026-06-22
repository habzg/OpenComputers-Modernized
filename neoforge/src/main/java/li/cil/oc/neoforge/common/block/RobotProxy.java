package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.block.traits.GUI;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.tileentity.RobotBase;
import li.cil.oc.neoforge.common.tileentity.Robot;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RobotProxy extends RedstoneAware implements GUI, StateAware {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final VoxelShape SHAPE = box(2, 2, 2, 14, 14, 14);

    public RobotProxy() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 10f).sound(SoundType.METAL));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

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
        var data = new li.cil.oc.core.impl.common.item.data.RobotData(stack);
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
                    if (nbt.contains(Settings.namespace + "xp", Tag.TAG_DOUBLE)) {
                        double xp = nbt.getDouble(Settings.namespace + "xp");
                        int level = Math.min((int) (Math.pow(xp - Settings.get().baseXpToLevel, 1.0 / Settings.get().exponentialXpGrowth) / Settings.get().constantXpGrowth), 30);
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
        var data = new li.cil.oc.core.impl.common.item.data.RobotData(stack);
        var components = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        components.addAll(data.containers);
        components.addAll(data.components);
        if (!components.isEmpty()) {
            var header = li.cil.oc.neoforge.util.Tooltip.extended("server.Components");
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
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        var r = RobotBase.movingRobot.get();
        if (r instanceof Robot robot) {
            return new li.cil.oc.neoforge.common.tileentity.RobotProxy(robot, pos, state);
        }
        return new li.cil.oc.neoforge.common.tileentity.RobotProxy(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        var tileType = li.cil.oc.neoforge.common.init.TileEntities.ROBOT.get();
        return type == tileType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.neoforge.common.tileentity.RobotProxy) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error in robot tick", e);
            }
        } : null;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
            var robot = proxy.robot;
            if (robot.isAnimatingMove()) {
                var remaining = robot.animationTicksTotal > 0 ? (double) robot.animationTicksLeft / (double) robot.animationTicksTotal : 0.0;
                var dx = (robot.moveFromX - robot.getBlockPos().getX()) * remaining;
                var dy = (robot.moveFromY - robot.getBlockPos().getY()) * remaining;
                var dz = (robot.moveFromZ - robot.getBlockPos().getZ()) * remaining;
                return SHAPE.move(dx, dy, dz);
            }
        }
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
            if (proxy.robot.animationTicksLeft <= 0) {
                return net.minecraft.world.phys.shapes.Shapes.empty();
            }
        }
        return SHAPE;
    }

    @Override
    public net.minecraft.world.level.block.@NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level world, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
            if (!world.isClientSide) proxy.robot.checkRedstoneInputChanged();
        }
    }

    @Override
    public boolean canConnectRedstone(@NotNull BlockState state, BlockGetter world, @NotNull BlockPos pos, Direction side) {
        var te = world.getBlockEntity(pos);
        return te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy && proxy.robot.isOutputEnabled();
    }

    @Override
    public int getSignal(@NotNull BlockState state, BlockGetter world, @NotNull BlockPos pos, @NotNull Direction side) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
            return Math.max(0, proxy.robot.getOutput(side.getOpposite()));
        }
        return 0;
    }

    @Override
    public int getDirectSignal(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull Direction side) {
        return getSignal(state, world, pos, side);
    }

    @Override
    public void onBlockStateChange(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState oldState, @NotNull BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);
        var te = level.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
            proxy.robot.syncFromBlockState(newState);
        }
    }

    @Override
    public int guiType() {
        return GuiType.Robot;
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (!player.isCrouching()) {
            if (!world.isClientSide) {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy && proxy.robot.node != null && proxy.robot.node.network() != null) {
                    li.cil.oc.core.impl.common.PacketSender.sendRobotSelectedSlotChange(proxy, proxy.robot.selectedSlot());
                    int gt = guiType();
                    player.openMenu(li.cil.oc.core.impl.util.ContainerProviderDelegate.get().getContainerProvider(gt, world, pos.getX(), pos.getY(), pos.getZ()), (net.minecraft.network.RegistryFriendlyByteBuf buf) -> {
                        buf.writeInt(gt);
                        buf.writeInt(pos.getX());
                        buf.writeInt(pos.getY());
                        buf.writeInt(pos.getZ());
                    });
                }
            }
            return true;
        }
        if (player.getMainHandItem().isEmpty()) {
            if (!world.isClientSide) {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
                    var machine = proxy.machine();
                    if (!machine.isRunning() && proxy.isUseableByPlayer(player)) {
                        try {
                            machine.start();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
            var robot = proxy.robot;
            if (!world.isClientSide) {
                if (placer instanceof li.cil.oc.neoforge.server.agent.Player ap) {
                    robot.ownerName = ap.agent.ownerName();
                    robot.ownerUUID = li.cil.oc.neoforge.server.agent.Player.determineUUID(ap.agent.ownerUUID());
                } else if (placer instanceof Player p) {
                    robot.ownerName = p.getGameProfile().getName();
                    robot.ownerUUID = li.cil.oc.neoforge.server.agent.Player.determineUUID(p.getGameProfile().getId());
                }
            }
            robot.setFromEntityPitchAndYaw(placer);
            robot.syncFacingToBlockState();
            if (robot.getLevel() == null) {
                robot.setLevel(world);
                robot.setBlockPos(pos);
                robot.proxy = proxy;
            }
            robot.info.load(stack, world.registryAccess());
            robot.updateInventorySize();
            if (!world.isClientSide) {
                robot._components(null);
                robot.connectComponents();
                robot.setChanged();
                var botNode = robot.bot != null ? ((li.cil.oc.core.impl.server.component.Robot) robot.bot).node() : null;
                if (botNode instanceof li.cil.oc.api.network.Connector connector) {
                    connector.changeBuffer(robot.info.robotEnergy - connector.localBuffer());
                }
            }
        }
    }

    @Override
    public boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, boolean willHarvest, net.minecraft.world.level.material.@NotNull FluidState fluid) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
            var robot = proxy.robot;
            if (robot.isCreative() && (!player.getAbilities().instabuild || !robot.canInteract(player.getGameProfile().getName())))
                return false;
            if (!world.isClientSide) {
                if (robot.player() == player) return false;
                if (robot.node != null) robot.node.remove();
                robot.saveComponents(world.registryAccess());
                Block.popResource(world, pos, robot.info.createItemStack());
                robot.dropInventorySlots();
                if (robot.isAnimatingMove()) {
                    var fromPos = new BlockPos(robot.moveFromX, robot.moveFromY, robot.moveFromZ);
                    if (world.getBlockState(fromPos).getBlock() instanceof RobotAfterimage) {
                        world.setBlock(fromPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 1);
                    }
                }
            }
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, LevelReader world, @NotNull BlockPos pos, @NotNull Player player) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
            return proxy.robot.info.copyItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void onDropInventory(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public java.util.Set<li.cil.oc.api.util.StateAware.State> getCurrentState() {
        return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    }
}
