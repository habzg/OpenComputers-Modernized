package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;


public abstract class AgentBase extends AbstractManagedEnvironment implements
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

    private java.util.List<ItemEntity> capturedDropsBefore;

    protected void beginConsumeDrops(Entity entity) {
        capturedDropsBefore = new java.util.ArrayList<>(
                entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2, 2, 2)));
    }

    protected void endConsumeDrops(Player player, Entity entity) {
        if (capturedDropsBefore != null) {
            var itemsAfter = entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2, 2, 2));
            for (ItemEntity drop : itemsAfter) {
                if (!capturedDropsBefore.contains(drop) && !drop.isRemoved()) {
                    ItemStack stack = drop.getItem();
                    li.cil.oc.core.impl.util.InventoryUtils.addToPlayerInventory(stack, player, false);
                    drop.discard();
                }
            }
            capturedDropsBefore = null;
        }
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean, string -- Perform a 'left click' towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
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

        String reason = null;
        for (Direction side : sides) {
            if (side == null) continue;
            Player player = rotatedPlayer(facing, side);
            setPlayerSneaking(player, sneaky);

            HitResult hit = playerPick(player, OCSettings.get().swingRange);
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
                onWorldInteraction(context, OCSettings.get().swingDelay);
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
                        onWorldInteraction(context, OCSettings.get().swingDelay);
                        result = ResultWrapper.result(true, "entity");
                    } else {
                        var level = agent().level();
                        if (level != null && extinguishFire(level, position(), facing)) {
                            onWorldInteraction(context, OCSettings.get().swingDelay);
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
            if (reason == null && result.length > 1 && result[1] instanceof String) {
                reason = (String) result[1];
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

        return ResultWrapper.result(false, reason);
    }

    protected Direction checkSideForFace(Arguments args, Direction facing) {
        Direction excluded = agent().toLocal(facing).getOpposite();
        var allowed = java.util.Arrays.stream(Direction.values())
                .filter(d -> d != excluded)
                .toArray(Direction[]::new);
        return agent().toGlobal(li.cil.oc.core.impl.util.ExtendedArguments.checkSide(args, 1, allowed));
    }

    private static boolean extinguishFire(net.minecraft.world.level.Level level, BlockPosition base, Direction facing) {
        BlockPos center = new BlockPos(base.x(), base.y(), base.z()).relative(facing);
        if (tryExtinguishFire(level, center)) return true;
        for (Direction d : Direction.values()) {
            if (tryExtinguishFire(level, center.relative(d))) return true;
        }
        return false;
    }

    private static boolean tryExtinguishFire(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FIRE)) {
            level.destroyBlock(pos, false);
            return true;
        }
        return false;
    }

    protected abstract boolean playerPlaceBlock(Player player, int slot, int x, int y, int z, int side, float hitX, float hitY, float hitZ);

    protected abstract int playerActivateBlockOrUseItem(Player player, int x, int y, int z, int side, float hitX, float hitY, float hitZ, double duration);

    protected abstract boolean playerUseEquippedItem(Player player, double duration);

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean -- Place a block towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
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

            HitResult hit = playerPick(player, OCSettings.get().useAndPlaceRange);
            boolean success = false;

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                success = playerPlaceBlock(player, agent().selectedSlot(),
                        blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ(),
                        blockHit.getDirection().ordinal(),
                        (float) (blockHit.getLocation().x - blockHit.getBlockPos().getX()),
                        (float) (blockHit.getLocation().y - blockHit.getBlockPos().getY()),
                        (float) (blockHit.getLocation().z - blockHit.getBlockPos().getZ()));
            } else if (hit.getType() == HitResult.Type.MISS && canPlaceInAir()) {
                Entity closest = playerClosestEntity(player, facing, Entity.class);
                if (closest == null) {
                    BlockPosition bp = position().offset(facing);
                    var level = agent().level();
                    if (level != null) {
                        var targetState = level.getBlockState(new BlockPos(bp.x(), bp.y(), bp.z()));
                        if (targetState.canBeReplaced()) {
                            success = playerPlaceBlock(player, agent().selectedSlot(),
                                    bp.x(), bp.y(), bp.z(), facing.ordinal(),
                                    0.5f + facing.getStepX() * 0.5f,
                                    0.5f + facing.getStepY() * 0.5f,
                                    0.5f + facing.getStepZ() * 0.5f);
                        }
                    }
                }
            }

            setPlayerSneaking(player, false);
            if (success) {
                onWorldInteraction(context, OCSettings.get().placeDelay);
                return ResultWrapper.result(true);
            }
        }

        return ResultWrapper.result(false);
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false[, duration:number=0]]]):boolean, string -- Perform a 'right click' towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
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

            HitResult hit = playerPick(player, OCSettings.get().useAndPlaceRange);

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
            }

            if (!success && hit.getType() == HitResult.Type.BLOCK) {
              assert hit instanceof BlockHitResult;
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
            }

            if (!success && hit.getType() != HitResult.Type.BLOCK) {
                int airActivation = li.cil.oc.core.server.agent.ActivationType.None;
                if (canPlaceInAir()) {
                    BlockPosition placePos = position();
                    if (playerPlaceBlock(player, 0, placePos.x(), placePos.y(), placePos.z(), facing.ordinal(),
                            0.5f + facing.getStepX() * 0.5f, 0.5f + facing.getStepY() * 0.5f, 0.5f + facing.getStepZ() * 0.5f)) {
                        airActivation = li.cil.oc.core.server.agent.ActivationType.ItemPlaced;
                    } else {
                        BlockPosition usePos = position().offset(facing).offset(side);
                        airActivation = playerActivateBlockOrUseItem(player,
                                usePos.x(), usePos.y(), usePos.z(),
                                side.getOpposite().ordinal(),
                                0.5f - side.getStepX() * 0.5f, 0.5f - side.getStepY() * 0.5f, 0.5f - side.getStepZ() * 0.5f,
                                duration);
                    }
                }
                if (airActivation == li.cil.oc.core.server.agent.ActivationType.None) {
                    if (playerUseEquippedItem(player, duration)) {
                        success = true;
                        what = "item_used";
                    } else {
                        what = "air";
                    }
                } else {
                    success = true;
                    if (airActivation == li.cil.oc.core.server.agent.ActivationType.BlockActivated) {
                        what = "block_activated";
                    } else if (airActivation == li.cil.oc.core.server.agent.ActivationType.ItemPlaced) {
                        what = "item_placed";
                    } else {
                        what = "item_used";
                    }
                }
            }

            setPlayerSneaking(player, false);
            if (success) {
                onWorldInteraction(context, OCSettings.get().useDelay);
                return ResultWrapper.result(true, what);
            }
        }
        return ResultWrapper.result(false);
    }
}
