package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;


public abstract class AgentBase extends ManagedEnvironment implements
        li.cil.oc.core.impl.server.component.traits.WorldControl,
        li.cil.oc.core.impl.server.component.traits.InventoryControl,
        li.cil.oc.core.impl.server.component.traits.InventoryWorldControl,
        li.cil.oc.core.impl.server.component.traits.TankAware,
        li.cil.oc.core.impl.server.component.traits.TankControl,
        li.cil.oc.core.impl.server.component.traits.TankWorldControl {

    public abstract li.cil.oc.api.internal.Agent agent();

    @Override
    public abstract Direction checkSideForAction(Arguments args, int n);

    @Override
    public BlockPosition position() {
        return BlockPosition.apply(agent());
    }

    @Override
    public Player fakePlayer() {
        return agent().player();
    }

    protected abstract Player createRotatedPlayer(Direction facing, Direction side);

    protected Player rotatedPlayer(Direction facing, Direction side) {
        return createRotatedPlayer(facing, side);
    }

    @Override
    public Container inventory() {
        return agent().mainInventory();
    }

    @Override
    public int selectedSlot() {
        return agent().selectedSlot();
    }

    @Override
    public void selectedSlot_$eq(int value) {
        agent().setSelectedSlot(value);
    }

    @Override
    public MultiTank tank() {
        return agent().tank();
    }

    public int selectedTank() {
        return agent().selectedTank();
    }

    @Override
    public void selectedTank_$eq(int value) {
        agent().setSelectedTank(value);
    }

    public boolean canPlaceInAir() {
        return postRobotPlaceInAirEvent();
    }

    protected abstract boolean postRobotPlaceInAirEvent();

    public void onWorldInteraction(Context context, double duration) {
        context.pause(duration);
    }

    @Callback(doc = "function():string -- Get the name of the agent.")
    public Object[] name(Context context, Arguments args) {
        return ResultWrapper.result(agent().name());
    }

    protected abstract void setPlayerSneaking(Player player, boolean sneaky);

    protected abstract double playerBreakBlock(Player player, int x, int y, int z, int dir);

    protected abstract void playerAttackEntity(Player player, Entity target);

    protected abstract Entity playerClosestEntity(Player player, Direction facing, Class<? extends Entity> cls);

    protected abstract HitResult playerPick(Player player, double range);

    protected abstract void beginConsumeDrops(Entity entity);

    protected abstract void endConsumeDrops(Player player, Entity entity);

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean, string -- Perform a 'left click'.")
    public Object[] swing(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        Direction[] sides;
        if (args.isInteger(1)) {
            sides = new Direction[]{checkSideForFace(args, facing)};
        } else {
            sides = new Direction[Direction.values().length + 1];
            sides[0] = facing;
            int i = 1;
            for (Direction side : Direction.values()) {
                if (side != facing && side != facing.getOpposite()) {
                    sides[i++] = side;
                }
            }
        }
        boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);

        for (Direction side : sides) {
            if (side == null) continue;
            Player player = rotatedPlayer(facing, side);
            setPlayerSneaking(player, sneaky);

            HitResult hit = playerPick(player, Settings.get().swingRange);
            Object[] result;

            if (hit.getType() == HitResult.Type.ENTITY) {
                beginConsumeDrops(((EntityHitResult) hit).getEntity());
                playerAttackEntity(player, ((EntityHitResult) hit).getEntity());
                if (((EntityHitResult) hit).getEntity() instanceof Minecart) {
                    for (int i = 0; i < 10 && !((EntityHitResult) hit).getEntity().isRemoved(); i++) {
                        playerAttackEntity(player, ((EntityHitResult) hit).getEntity());
                    }
                }
                endConsumeDrops(player, ((EntityHitResult) hit).getEntity());
                onWorldInteraction(context, Settings.get().swingDelay);
                result = ResultWrapper.result(true, "entity");
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                double breakTime = playerBreakBlock(player, ((BlockHitResult) hit).getBlockPos().getX(), ((BlockHitResult) hit).getBlockPos().getY(), ((BlockHitResult) hit).getBlockPos().getZ(), ((BlockHitResult) hit).getDirection().ordinal());
                boolean broke = breakTime > 0;
                if (broke) {
                    onWorldInteraction(context, breakTime);
                }
                result = ResultWrapper.result(broke, "block");
                } else {
                    Entity closest = playerClosestEntity(player, facing, LivingEntity.class);
                    if (closest != null) {
                        beginConsumeDrops(closest);
                        playerAttackEntity(player, closest);
                        endConsumeDrops(player, closest);
                        onWorldInteraction(context, Settings.get().swingDelay);
                        result = ResultWrapper.result(true, "entity");
                    } else {
                        var blockPos = position().offset(facing);
                        var level = agent().level();
                        BlockPos pos = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
                        if (level != null && level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FIRE)) {
                            level.destroyBlock(pos, false);
                            onWorldInteraction(context, Settings.get().swingDelay);
                            result = ResultWrapper.result(true, "fire");
                        } else {
                            result = ResultWrapper.result(false, "air");
                        }
                    }
                }

            setPlayerSneaking(player, false);
            if (result.length > 0 && result[0] instanceof Boolean && (Boolean) result[0]) {
                return result;
            }
        }

        Object[] bc = blockContent(facing);
        if ((Boolean) bc[0]) {
            BlockPosition blockPos = position().offset(facing);
            Player player = rotatedPlayer(facing, facing);
            setPlayerSneaking(player, sneaky);
            double breakTime = playerBreakBlock(player, blockPos.x(), blockPos.y(), blockPos.z(), facing.ordinal());
            boolean ok = breakTime > 0;
            setPlayerSneaking(player, false);
            return ResultWrapper.result(ok, "block");
        }

        return ResultWrapper.result(false);
    }

    protected Direction checkSideForFace(Arguments args, Direction facing) {
        Direction excluded = agent().toLocal(facing).getOpposite();
        var allowed = java.util.Arrays.stream(Direction.values())
                .filter(d -> d != excluded)
                .toArray(Direction[]::new);
        return agent().toGlobal(li.cil.oc.core.impl.util.ExtendedArguments.checkSide(args, 1, allowed));
    }

    protected abstract boolean playerPlaceBlock(Player player, int slot, int x, int y, int z, int side, float hitX, float hitY, float hitZ);

    protected abstract int playerActivateBlockOrUseItem(Player player, int x, int y, int z, int side, float hitX, float hitY, float hitZ, double duration);

    protected abstract boolean playerUseEquippedItem(Player player, double duration);

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean -- Place a block towards the specified side.")
    public Object[] place(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        Direction[] sides;
        if (args.isInteger(1)) {
            sides = new Direction[]{checkSideForFace(args, facing)};
        } else {
            sides = new Direction[Direction.values().length + 1];
            sides[0] = facing;
            int i = 1;
            for (Direction side : Direction.values()) {
                if (side != facing && side != facing.getOpposite()) {
                    sides[i++] = side;
                }
            }
        }
        boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);

        ItemStack stack = agent().mainInventory().getItem(agent().selectedSlot());
        if (stack.isEmpty()) {
            return ResultWrapper.result(null, "nothing selected");
        }

        for (Direction side : sides) {
            if (side == null) continue;
            Player player = rotatedPlayer(facing, side);
            setPlayerSneaking(player, sneaky);

            HitResult hit = playerPick(player, Settings.get().useAndPlaceRange);
            boolean success;

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                success = playerPlaceBlock(player, agent().selectedSlot(),
                        blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ(),
                        blockHit.getDirection().ordinal(),
                        (float) (blockHit.getLocation().x - blockHit.getBlockPos().getX()),
                        (float) (blockHit.getLocation().y - blockHit.getBlockPos().getY()),
                        (float) (blockHit.getLocation().z - blockHit.getBlockPos().getZ()));
            } else {
                BlockPosition bp = position().offset(facing);
                var level = agent().level();
                if (level != null) {
                    var targetState = level.getBlockState(new BlockPos(bp.x(), bp.y(), bp.z()));
                    if (targetState.canBeReplaced()) {
                        Entity closest = playerClosestEntity(player, facing, Entity.class);
                        if (canPlaceInAir() && closest == null) {
                            success = playerPlaceBlock(player, agent().selectedSlot(),
                                    bp.x(), bp.y(), bp.z(), Direction.UP.ordinal(),
                                    0.5f, 1.0f, 0.5f);
                        } else {
                            success = false;
                        }
                    } else {
                        success = false;
                    }
                } else {
                    success = false;
                }
            }

            setPlayerSneaking(player, false);
            if (success) {
                onWorldInteraction(context, Settings.get().placeDelay);
                return ResultWrapper.result(true);
            }
        }

        return ResultWrapper.result(false);
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false[, duration:number=0]]]):boolean, string -- Perform a 'right click' towards the specified side.")
    public Object[] use(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        Direction[] sides;
        if (args.isInteger(1)) {
            sides = new Direction[]{checkSideForFace(args, facing)};
        } else {
            sides = new Direction[Direction.values().length + 1];
            sides[0] = facing;
            int i = 1;
            for (Direction side : Direction.values()) {
                if (side != facing && side != facing.getOpposite()) {
                    sides[i++] = side;
                }
            }
        }
        boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);
        double duration = args.isDouble(3) ? args.checkDouble(3) : 0.0;

        for (Direction side : sides) {
            if (side == null) continue;
            Player player = rotatedPlayer(facing, side);
            setPlayerSneaking(player, sneaky);

            boolean success = false;
            String what = "";

            HitResult hit = playerPick(player, Settings.get().useAndPlaceRange);

            if (hit.getType() == HitResult.Type.ENTITY) {
                Entity entity = ((EntityHitResult) hit).getEntity();
                beginConsumeDrops(entity);
                try {
                    if (entity.interact(player, InteractionHand.MAIN_HAND).consumesAction()) {
                        success = true;
                        what = "item_interacted";
                    }
                } finally {
                    endConsumeDrops(player, entity);
                }
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                int activationType = playerActivateBlockOrUseItem(player,
                        blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ(),
                        blockHit.getDirection().ordinal(),
                        (float) (blockHit.getLocation().x - blockHit.getBlockPos().getX()),
                        (float) (blockHit.getLocation().y - blockHit.getBlockPos().getY()),
                        (float) (blockHit.getLocation().z - blockHit.getBlockPos().getZ()),
                        duration);
                if (activationType == li.cil.oc.core.server.agent.ActivationType.BlockActivated) {
                    success = true;
                    what = "block_activated";
                } else if (activationType == li.cil.oc.core.server.agent.ActivationType.ItemPlaced) {
                    success = true;
                    what = "item_placed";
                } else if (activationType == li.cil.oc.core.server.agent.ActivationType.ItemUsed) {
                    success = true;
                    what = "item_used";
                }
            } else {
                BlockPosition bp = position().offset(facing);
                var level = agent().level();
                if (level != null) {
                    var targetState = level.getBlockState(new BlockPos(bp.x(), bp.y(), bp.z()));
                    if (targetState.isAir()) {
                        Entity closest = playerClosestEntity(player, facing, Entity.class);
                        if (canPlaceInAir() && closest == null) {
                            if (playerPlaceBlock(player, 0, bp.x(), bp.y(), bp.z(), facing.ordinal(), 0.5f, 1.0f, 0.5f)) {
                                success = true;
                                what = "item_placed";
                            }
                        }
                    }
                }
                if (!success) {
                    BlockPosition usePos = position().offset(facing);
                    int activationType = playerActivateBlockOrUseItem(player,
                            usePos.x(), usePos.y(), usePos.z(),
                            side.ordinal(), 0.5f, 0.5f, 0.5f,
                            duration);
                    if (activationType == li.cil.oc.core.server.agent.ActivationType.BlockActivated) {
                        success = true;
                        what = "block_activated";
                    } else if (activationType == li.cil.oc.core.server.agent.ActivationType.ItemPlaced) {
                        success = true;
                        what = "item_placed";
                    } else if (activationType == li.cil.oc.core.server.agent.ActivationType.ItemUsed) {
                        success = true;
                        what = "item_used";
                    }
                }
                if (!success && playerUseEquippedItem(player, duration)) {
                    success = true;
                    what = "item_used";
                }
                if (!success) {
                    what = "air";
                }
            }

            setPlayerSneaking(player, false);
            if (success) {
                onWorldInteraction(context, Settings.get().useDelay);
                return ResultWrapper.result(true, what);
            }
        }

        Object[] bc = blockContent(facing);
        if ((Boolean) bc[0]) {
            BlockPosition blockPos = position().offset(facing);
            Player player = rotatedPlayer(facing, facing);
            setPlayerSneaking(player, sneaky);
            int activationType = playerActivateBlockOrUseItem(player,
                    blockPos.x(), blockPos.y(), blockPos.z(),
                    facing.ordinal(), 0.5f, 0.5f, 0.5f,
                    duration);
            boolean ok = activationType != li.cil.oc.core.server.agent.ActivationType.None;
            setPlayerSneaking(player, false);
            if (ok) {
                String resultWhat;
                if (activationType == li.cil.oc.core.server.agent.ActivationType.BlockActivated)
                    resultWhat = "block_activated";
                else if (activationType == li.cil.oc.core.server.agent.ActivationType.ItemPlaced)
                    resultWhat = "item_placed";
                else resultWhat = "item_used";
                onWorldInteraction(context, Settings.get().useDelay);
                return ResultWrapper.result(true, resultWhat);
            }
        }

        return ResultWrapper.result(false);
    }
}
