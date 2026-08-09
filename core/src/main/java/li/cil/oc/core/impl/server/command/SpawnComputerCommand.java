package li.cil.oc.core.impl.server.command;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.command.SimpleCommand;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.InventoryUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpawnComputerCommand extends SimpleCommand {
    public static final SpawnComputerCommand INSTANCE = new SpawnComputerCommand();
    public static final int MaxDistance = 16;

    private SpawnComputerCommand() {
        super("oc_spawnComputer");
        aliases.add("oc_sc");
    }

    @Override
    protected int execute(CommandSourceStack source, String[] args) {
        if (source.getEntity() != null && !(source.getEntity() instanceof Player)) {
            source.sendFailure(Component.literal("Can only be used by players."));
            return 0;
        }
        if (source.getEntity() instanceof Player player) {
            Level world = player.level();
            Vec3 origin = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
            Vec3 lookAt = player.getLookAngle().scale(MaxDistance).add(origin);
            BlockHitResult hit = world.clip(new ClipContext(origin, lookAt, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hit.getBlockPos();
                BlockPos casePos = hitPos.relative(hit.getDirection());
                BlockPos screenPos = casePos.above();
                BlockPos keyboardPos = screenPos.above();
                if (!world.isEmptyBlock(casePos) || !world.isEmptyBlock(screenPos) || !world.isEmptyBlock(keyboardPos)) {
                    source.sendFailure(Component.literal("Target position obstructed."));
                    return 0;
                }
                Direction facing = player.getDirection().getOpposite();
                world.setBlock(casePos, li.cil.oc.api.Items.get(Constants.BlockName.CaseCreative).block().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 3);
                var caseTe = world.getBlockEntity(casePos);
                if (caseTe instanceof li.cil.oc.core.impl.common.blockentity.traits.Rotatable r) {
                    r.facing(facing);
                }
                world.setBlock(screenPos, li.cil.oc.api.Items.get(Constants.BlockName.ScreenTier2).block().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, facing), 3);
                var screenTe = world.getBlockEntity(screenPos);
                if (screenTe instanceof li.cil.oc.core.impl.common.blockentity.traits.Rotatable r) {
                    r.facing(facing);
                }
                world.setBlock(keyboardPos, li.cil.oc.api.Items.get(Constants.BlockName.Keyboard).block().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.UP), 3);
                var keyboardTe = world.getBlockEntity(keyboardPos);
                if (keyboardTe instanceof li.cil.oc.core.impl.common.blockentity.traits.Rotatable r) {
                    r.facing(Direction.UP);
                }
                li.cil.oc.api.Network.joinOrCreateNetwork(world.getBlockEntity(casePos));
                ItemStack apu = li.cil.oc.api.Items.get(Constants.ItemName.APUCreative).createItemStack(1);
                LuaStateFactory.setDefaultArch(apu);
                BlockPosition caseBlockPos = BlockPosition.apply(casePos.getX(), casePos.getY(), casePos.getZ(), world);
                InventoryUtils.insertIntoInventoryAt(apu, caseBlockPos, Direction.DOWN);
                InventoryUtils.insertIntoInventoryAt(li.cil.oc.api.Items.get(Constants.ItemName.RAMTier6).createItemStack(2), caseBlockPos, Direction.DOWN);
                InventoryUtils.insertIntoInventoryAt(li.cil.oc.api.Items.get(Constants.ItemName.HDDTier3).createItemStack(1), caseBlockPos, Direction.DOWN);
                InventoryUtils.insertIntoInventoryAt(li.cil.oc.api.Items.get(Constants.ItemName.LuaBios).createItemStack(1), caseBlockPos, Direction.DOWN);
                InventoryUtils.insertIntoInventoryAt(li.cil.oc.api.Items.get(Constants.ItemName.OpenOS).createItemStack(1), caseBlockPos, Direction.DOWN);
            } else {
                source.sendFailure(Component.literal("You need to be looking at a nearby block."));
            }
        } else {
            source.sendFailure(Component.literal("Can only be used by players."));
        }
        return 0;
    }
}
