package li.cil.oc.fabric.common.block;

import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.RobotBase;
import li.cil.oc.core.impl.util.ContainerProviderDelegate;
import li.cil.oc.fabric.common.init.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RobotProxy extends li.cil.oc.core.impl.common.block.RobotProxy {
    public RobotProxy() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 10f).sound(SoundType.METAL));
    }

    @Override
    protected RobotBase findRobotBase(BlockGetter world, BlockPos pos) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy) {
            return proxy.robot;
        }
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        var r = RobotBase.movingRobot.get();
        if (r instanceof li.cil.oc.fabric.common.blockentity.Robot robot) {
            return new li.cil.oc.fabric.common.blockentity.RobotProxy(robot, pos, state);
        }
        return new li.cil.oc.fabric.common.blockentity.RobotProxy(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        var tileType = BlockEntities.ROBOT;
        return type == tileType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.fabric.common.blockentity.RobotProxy) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.fabric.OpenComputers.log().warn("Error in robot tick", e);
            }
        } : null;
    }

    @SuppressWarnings("unused")
    public boolean canConnectRedstone(@NotNull BlockState state, BlockGetter world, @NotNull BlockPos pos, @Nullable Direction side) {
        var te = world.getBlockEntity(pos);
        return te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy && proxy.robot.isOutputEnabled();
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (!player.isCrouching()) {
            if (!world.isClientSide) {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy && proxy.robot.node != null && proxy.robot.node.network() != null) {
                    PacketSender.sendRobotSelectedSlotChange(proxy, proxy.robot.selectedSlot());
                    int gt = guiType();
                    ContainerProviderDelegate.get().openMenu(player, gt, world, pos.getX(), pos.getY(), pos.getZ());
                }
            }
            return true;
        }
        if (player.getMainHandItem().isEmpty()) {
            if (!world.isClientSide) {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy) {
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
        if (te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy) {
            var robot = proxy.robot;
            if (!world.isClientSide) {
                if (placer instanceof li.cil.oc.fabric.server.agent.Player ap) {
                    robot.ownerName = ap.agent.ownerName();
                    robot.ownerUUID = li.cil.oc.fabric.server.agent.Player.determineUUID(ap.agent.ownerUUID());
                } else if (placer instanceof Player p) {
                    robot.ownerName = p.getGameProfile().getName();
                    robot.ownerUUID = li.cil.oc.fabric.server.agent.Player.determineUUID(p.getGameProfile().getId());
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
    public @NotNull BlockState playerWillDestroy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy) {
            var robot = proxy.robot;
            if (robot.isCreative() && (!player.getAbilities().instabuild || !robot.canInteract(player.getGameProfile().getName()))) {
                return state;
            }
            if (!world.isClientSide) {
                if (robot.player() != player) {
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
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
