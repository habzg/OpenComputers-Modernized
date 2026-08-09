package li.cil.oc.neoforge.common.block;

import li.cil.oc.core.impl.common.blockentity.RobotBase;
import li.cil.oc.neoforge.common.blockentity.Robot;
import li.cil.oc.neoforge.common.init.BlockEntities;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public class RobotProxy extends li.cil.oc.core.impl.common.block.RobotProxy {
    public RobotProxy() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 10f).sound(SoundType.METAL));
    }

    @Override
    protected RobotBase findRobotBase(BlockGetter world, BlockPos pos) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy proxy) {
            return proxy.robot;
        }
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        var r = RobotBase.movingRobot.get();
        if (r instanceof Robot robot) {
            return new li.cil.oc.neoforge.common.blockentity.RobotProxy(robot, pos, state);
        }
        return new li.cil.oc.neoforge.common.blockentity.RobotProxy(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level ignoredLevel, @NotNull BlockState ignoredState, @NotNull BlockEntityType<T> type) {
        var tileType = BlockEntities.ROBOT.get();
        return type == tileType ? (lvl, pos, st, te) -> {
            try {
                ((li.cil.oc.neoforge.common.blockentity.RobotProxy) te).updateEntity();
            } catch (Exception e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error in robot tick", e);
            }
        } : null;
    }

    @Override
    public boolean canConnectRedstone(@NotNull BlockState ignoredState, BlockGetter world, @NotNull BlockPos pos, Direction ignoredSide) {
        var te = world.getBlockEntity(pos);
        return te instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy proxy && proxy.robot.isOutputEnabled();
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction ignoredSide, float ignoredHitX, float ignoredHitY, float ignoredHitZ, InteractionHand ignoredHand) {
        if (!player.isCrouching()) {
            if (!world.isClientSide) {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy proxy && proxy.robot.node != null && proxy.robot.node.network() != null) {
                    li.cil.oc.core.impl.common.PacketSender.sendRobotSelectedSlotChange(proxy, proxy.robot.selectedSlot());
                    int gt = guiType();
                    li.cil.oc.core.impl.util.ContainerProviderDelegate.get().openMenu(player, gt, world, pos.getX(), pos.getY(), pos.getZ());
                }
            }
            return true;
        }
        if (player.getMainHandItem().isEmpty()) {
            if (!world.isClientSide) {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy proxy) {
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
        if (te instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy proxy) {
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
    public boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, boolean willHarvest, @NotNull FluidState fluid) {
        var te = world.getBlockEntity(pos);
        if (te instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy proxy) {
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
                    if (world.getBlockState(fromPos).getBlock() instanceof li.cil.oc.neoforge.common.block.RobotAfterimage) {
                        world.setBlock(fromPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 1);
                    }
                }
            }
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }
}
