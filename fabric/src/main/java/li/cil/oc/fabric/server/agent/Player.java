package li.cil.oc.fabric.server.agent;

import com.mojang.authlib.GameProfile;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.network.Connector;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.server.agent.ActivationType;
import li.cil.oc.fabric.common.EventHandler;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class Player extends FakePlayer implements li.cil.oc.core.impl.server.agent.AgentPlayer {
    public final li.cil.oc.api.internal.Agent agent;
    public Direction facing = Direction.SOUTH;
    public Direction side = Direction.SOUTH;

    public Player(ServerLevel world, li.cil.oc.api.internal.Agent agent) {
        super(world, profileFor(agent));
        this.agent = agent;
        getAbilities().mayfly = true;
        getAbilities().flying = true;
        setOnGround(true);
    }

    public Direction facing() {
        return facing;
    }

    public Direction side() {
        return side;
    }

    @Override
    public HitResult pick(double range) {
        Vec3 origin = new Vec3(getX() + facing().getStepX() * 0.5, getY() + facing().getStepY() * 0.5, getZ() + facing().getStepZ() * 0.5);
        Vec3 blockCenter = origin.add(facing().getStepX() * 0.51, facing().getStepY() * 0.51, facing().getStepZ() * 0.51);
        Vec3 target = blockCenter.add(side().getStepX() * range, side().getStepY() * range, side().getStepZ() * range);
        HitResult hit = level().clip(new net.minecraft.world.level.ClipContext(origin, target, net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        Entity closest = closestEntity(facing(), Entity.class);
        if ((closest instanceof LivingEntity || closest instanceof net.minecraft.world.entity.vehicle.Minecart || closest instanceof li.cil.oc.core.impl.common.entity.Drone)) {
            if (new Vec3(getX(), getY(), getZ()).distanceTo(hit.getLocation()) > distanceTo(closest)) {
                return new EntityHitResult(closest);
            }
        }
        return hit;
    }

    public static GameProfile profileFor(li.cil.oc.api.internal.Agent agent) {
        UUID uuid = agent.ownerUUID();
        var level = agent.level();
        int randomId;
        if (level != null) {
            randomId = level.random.nextInt(0xFFFFFF) + 1;
        } else {
            randomId = new java.util.Random().nextInt(0xFFFFFF) + 1;
        }
        String name = OCSettings.get().nameFormat
                .replace("$player$", agent.ownerName())
                .replace("$random$", Integer.toString(randomId));
        return new GameProfile(uuid, name);
    }

    public static UUID determineUUID(UUID playerUUID) {
        String format = OCSettings.get().uuidFormat;
        UUID randomUUID = UUID.randomUUID();
        try {
            return UUID.fromString(format
                    .replaceAll("\\$random\\$", randomUUID.toString())
                    .replaceAll("\\$player\\$", playerUUID != null ? playerUUID.toString() : randomUUID.toString()));
        } catch (Throwable t) {
            li.cil.oc.fabric.OpenComputers.log().warn("Failed determining robot UUID, check your config's `uuidFormat` entry!", t);
            return randomUUID;
        }
    }

    public static void updatePositionAndRotation(Player player, Direction facing, Direction side) {
        player.facing = facing;
        player.side = side;
        Vec3 direction = new Vec3(
                facing.getStepX() + side.getStepX(),
                facing.getStepY() + side.getStepY(),
                facing.getStepZ() + side.getStepZ()).normalize();
        float yaw = (float) Math.toDegrees(-Math.atan2(direction.x, direction.z));
        float pitch = (float) (Math.toDegrees(-Math.atan2(direction.y, Math.sqrt((direction.x * direction.x) + (direction.z * direction.z)))) * 0.99);
        player.moveTo(player.agent.xPosition(), player.agent.yPosition(), player.agent.zPosition(), yaw, pitch);
        player.xRotO = player.getXRot();
        player.yRotO = player.getYRot();
    }

    public static void setInventoryPlayerItems(Player player) {
        li.cil.oc.api.internal.Agent agent = player.agent;
        int armorSize = Math.min(4, agent.equipmentInventory().getContainerSize());
        for (int i = 0; i < armorSize; i++) {
            ItemStack item = agent.equipmentInventory().getItem(i);
            player.getInventory().armor.set(i, item.isEmpty() ? ItemStack.EMPTY : item.copy());
        }
        int size = Math.min(player.getInventory().items.size(), agent.mainInventory().getContainerSize());
        for (int i = 0; i < size; i++) {
            ItemStack item = agent.mainInventory().getItem(i);
            player.getInventory().items.set(i, item.copy());
        }
    }

    public static void detectInventoryPlayerChanges(Player player) {
        li.cil.oc.api.internal.Agent agent = player.agent;
        int armorSize = Math.min(4, agent.equipmentInventory().getContainerSize());
        for (int i = 0; i < armorSize; i++) {
            ItemStack item = player.getInventory().armor.get(i);
            agent.equipmentInventory().setItem(i, item.isEmpty() ? ItemStack.EMPTY : item.copy());
        }
        int size = Math.min(player.getInventory().items.size(), agent.mainInventory().getContainerSize());
        for (int i = 0; i < size; i++) {
            ItemStack item = player.getInventory().items.get(i);
            agent.mainInventory().setItem(i, item.isEmpty() ? ItemStack.EMPTY : item.copy());
        }
    }

    public @NotNull Component getDisplayName() {
        return Component.literal(agent != null ? agent.name() : "Robot");
    }

    public <T extends Entity> T closestEntity(Direction side, Class<T> type) {
        BlockPosition blockPos = BlockPosition.apply(agent).offset(side);
        return level().getEntitiesOfClass(type, blockPos.bounds()).stream().findFirst().orElse(null);
    }

    private List<ItemEntity> adjacentItems() {
        return level().getEntitiesOfClass(ItemEntity.class, BlockPosition.apply(agent).bounds().inflate(2, 2, 2));
    }

    @Override
    public void attackTargetEntityWithCurrentItem(Entity entity) {
        callUsingItemInSlot(agent.equipmentInventory(), stack -> {
            if (entity instanceof Player && !OCSettings.get().canAttackPlayers) {
                return null;
            }
            RobotAttackEntityEvent.Pre event = new RobotAttackEntityEvent.Pre(agent, entity);
            RobotAttackEntityEvent.Pre.EVENT.invoker().onRobotAttackEntity(event);
            if (!event.isCanceled()) {
                super.attack(entity);
                RobotAttackEntityEvent.Post.EVENT.invoker().onRobotAttackEntity(new RobotAttackEntityEvent.Post(agent, entity));
            }
            return null;
        });
    }

    public int activateBlockOrUseItem(int x, int y, int z, int side, float hitX, float hitY, float hitZ, double duration) {
        return callUsingItemInSlot(agent.equipmentInventory(), stack -> {
            BlockPos pos = new BlockPos(x, y, z);
            Direction dir = Direction.from3DDataValue(side);
            var vetoHit = new BlockHitResult(new Vec3(x + 0.5, y + 0.5, z + 0.5), dir, pos, false);
            if (UseBlockCallback.EVENT.invoker().interact(this, level(), InteractionHand.OFF_HAND, vetoHit) == InteractionResult.FAIL) {
                return ActivationType.None;
            }

            BlockHitResult blockHit = new BlockHitResult(new Vec3(x + hitX, y + hitY, z + hitZ), dir, pos, false);

            if (!stack.isEmpty()) {
                var context = new UseOnContext(level(), this, InteractionHand.OFF_HAND, stack, blockHit);
                if (stack.useOn(context).consumesAction()) {
                    return ActivationType.ItemUsed;
                }
            }

            BlockState state = level().getBlockState(pos);
            if (!state.isAir() && OCSettings.get().allowActivateBlocks) {
                boolean shouldActivate = !isShiftKeyDown() || stack.isEmpty();
                if (shouldActivate) {
                    if (state.useWithoutItem(level(), this, blockHit).consumesAction()) {
                        return ActivationType.BlockActivated;
                    }
                }
            }

            if (duration <= Double.MIN_VALUE && isItemUseAllowed(stack) && tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, dir, hitX, hitY, hitZ)) {
                return ActivationType.ItemPlaced;
            }

            if (useEquippedItem(duration, stack)) {
                return ActivationType.ItemUsed;
            }

            return ActivationType.None;
        });
    }

    public boolean useEquippedItem(double duration) {
        return useEquippedItem(duration, null);
    }

    private boolean useEquippedItem(double duration, ItemStack stack) {
        if (stack == null) {
            return callUsingItemInSlot(agent.equipmentInventory(), s -> useEquippedItem(duration, s));
        }

        if (UseItemCallback.EVENT.invoker().interact(this, level(), InteractionHand.OFF_HAND).getResult() == InteractionResult.FAIL) {
            return false;
        }

        double oldX = getX();
        double oldZ = getZ();
        setPos(getX() + facing.getStepX() / 2.0, getY(), getZ() + facing.getStepZ() / 2.0);
        try {
            return useItemWithHand(duration, stack);
        } finally {
            setPos(oldX, getY(), oldZ);
        }
    }

    @Override
    public boolean placeBlock(int slot, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return callUsingItemInSlot(agent.mainInventory(), slot, stack -> {
            BlockPos pos = new BlockPos(x, y, z);
            Direction dir = Direction.from3DDataValue(side);
            var vetoHit = new BlockHitResult(new Vec3(x + 0.5, y + 0.5, z + 0.5), dir, pos, false);
            if (UseBlockCallback.EVENT.invoker().interact(this, level(), InteractionHand.OFF_HAND, vetoHit) == InteractionResult.FAIL) {
                return false;
            }
            return tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, dir, hitX, hitY, hitZ);
        }, false);
    }

    public double clickBlock(int x, int y, int z, int side, boolean immediate) {
        return callUsingItemInSlot(agent.equipmentInventory(), stack -> {
            BlockPos pos = new BlockPos(x, y, z);
            Direction dir = Direction.from3DDataValue(side);
            BlockState state = level().getBlockState(pos);

            if (!hasCorrectToolForDrops(state)) return 0.0;

            // Query Fabric interaction listeners (e.g. protection mods) so they can veto
            // the robot's synthetic left-click, mirroring NeoForge's PlayerInteractEvent.LeftClickBlock.
            if (AttackBlockCallback.EVENT.invoker().interact(this, level(), InteractionHand.OFF_HAND, pos, dir) == InteractionResult.FAIL) {
                return 0.0;
            }

            float hardness = state.getDestroySpeed(level(), pos);
            if (hardness < 0) return 0.0;

            boolean cobwebOverride = state.is(Blocks.COBWEB) && OCSettings.get().screwCobwebs;
            float strength = getDigSpeed(state, pos);
            double breakTime = cobwebOverride ? OCSettings.get().swingDelay : hardness * 1.5 / strength;

            if (Double.isInfinite(breakTime)) return 0.0;

            var preEvent = new li.cil.oc.api.event.RobotBreakBlockEvent.Pre(agent, level(), pos, breakTime * OCSettings.get().harvestRatio);
            li.cil.oc.api.event.RobotBreakBlockEvent.Pre.EVENT.invoker().onRobotBreakBlock(preEvent);
            if (preEvent.isCanceled()) return 0.0;
            double adjustedBreakTime = Math.max(0.05, preEvent.getBreakTime());

            if (!PlayerInteractionManagerHelper.onBlockClicked(this, pos, dir)) {
                if (level().isEmptyBlock(pos)) return 1.0 / 20.0;
                return 0.0;
            }

            if (!immediate) {
                EventHandler.scheduleServer(() ->
                        new DamageOverTime(this, x, y, z, side, (int) (adjustedBreakTime * 20)).tick());
            }

            return adjustedBreakTime;
        });
    }

    public double clickBlock(int x, int y, int z, int side) {
        return clickBlock(x, y, z, side, false);
    }

    private boolean tryPlaceBlockWhileHandlingFunnySpecialCases(ItemStack stack, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        if (stack.isEmpty() || stack.getCount() <= 0) return false;
        var preEvent = new li.cil.oc.api.event.RobotPlaceBlockEvent.Pre(agent, stack, level(), pos);
        li.cil.oc.api.event.RobotPlaceBlockEvent.Pre.EVENT.invoker().onRobotPlaceBlock(preEvent);
        if (preEvent.isCanceled()) return false;

        double fakeEyeHeight = 0;
        if (getXRot() < 0 && isSomeKindOfPiston(stack)) {
            fakeEyeHeight = 1.82;
            setPos(getX(), getY() - fakeEyeHeight, getZ());
        }
        try {
            setInventoryPlayerItems(this);
            var blockHit = new net.minecraft.world.phys.BlockHitResult(
                    new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ),
                    side, pos, false);
            var context = new net.minecraft.world.item.context.UseOnContext(level(), this, InteractionHand.OFF_HAND, stack, blockHit);
            var didPlace = stack.useOn(context);
            detectInventoryPlayerChanges(this);

            if (didPlace == InteractionResult.SUCCESS) {
                li.cil.oc.api.event.RobotPlaceBlockEvent.Post.EVENT.invoker().onRobotPlaceBlock(new li.cil.oc.api.event.RobotPlaceBlockEvent.Post(agent, stack, level(), pos));
                return true;
            }
            if (didPlace == InteractionResult.CONSUME) {
                li.cil.oc.api.event.RobotPlaceBlockEvent.Post.EVENT.invoker().onRobotPlaceBlock(new li.cil.oc.api.event.RobotPlaceBlockEvent.Post(agent, stack, level(), pos));
                return true;
            }
            return false;
        } finally {
            if (fakeEyeHeight != 0) {
                setPos(getX(), getY() + fakeEyeHeight, getZ());
            }
        }
    }

    private boolean isSomeKindOfPiston(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            var block = blockItem.getBlock();
            return block instanceof PistonBaseBlock;
        }
        return false;
    }

    private boolean useItemWithHand(double duration, ItemStack stack) {
        if (!trySetActiveHand(duration)) {
            if (duration > 0) return false;
        }

        ItemStack oldStack = stack.copy();
        if (!isItemUseAllowed(stack)) return false;

        int maxDuration = stack.getUseDuration(this);
        int heldTicks = Math.clamp((int) (duration * 20), 0, maxDuration);
        agent.machine().pause(heldTicks / 20.0);

        InteractionResultHolder<ItemStack> useResult = stack.use(level(), this, InteractionHand.OFF_HAND);
        stopUsingItem();

        if (!useResult.getResult().consumesAction()) return false;

        ItemStack newStack = useResult.getObject();
        boolean stackChanged = !ItemStack.isSameItemSameComponents(oldStack, newStack) || !ItemStack.isSameItemSameComponents(oldStack, stack);

        if (stackChanged) {
            getInventory().offhand.set(0, newStack);
        }
        return stackChanged;
    }

    private boolean trySetActiveHand(double duration) {
        stopUsingItem();
        startUsingItem(InteractionHand.OFF_HAND);
        useItemRemaining = (int) duration;
        return isUsingItem();
    }

    private boolean isItemUseAllowed(ItemStack stack) {
        return stack == null || (OCSettings.get().allowUseItemsWithDuration || stack.getMaxStackSize() <= 0) &&
                !stack.is(Items.LEAD);
    }

    @Override
    public ItemEntity drop(@NotNull ItemStack stack, boolean inPlace) {
        return InventoryUtils.spawnStackInWorld(BlockPosition.apply(agent), stack, inPlace ? null : facing, null);
    }

    @Override
    public void causeFoodExhaustion(float amount) {
        if (OCSettings.get().robotExhaustionCost > 0) {
            if (agent.machine().node() instanceof Connector connector) {
                connector.changeBuffer(-OCSettings.get().robotExhaustionCost * amount);
            }
        }
        RobotExhaustionEvent.EVENT.invoker().onRobotExhaustion(new RobotExhaustionEvent(agent, amount));
    }

    @Override
    public boolean canEat(boolean value) {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity entity) {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public void heal(float amount) {
    }

    @Override
    public void setHealth(float value) {
    }

    @Override
    public void aiStep() {
    }

    @Override
    public void onItemPickup(@NotNull ItemEntity itemEntity) {
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND && agent.equipmentInventory().getContainerSize() > 0) {
            agent.equipmentInventory().setItem(0, stack);
        }
    }

    @Override
    public void setLastHurtMob(@NotNull Entity entity) {
    }

    @Override
    public void setLastHurtByMob(LivingEntity entity) {
    }

    @Override
    public void startSleeping(@NotNull BlockPos pos) {
    }

    @Override
    public void displayClientMessage(@NotNull Component message, boolean actionBar) {
    }

    @Override
    public java.util.@NotNull OptionalInt openMenu(net.minecraft.world.MenuProvider provider) {
        return java.util.OptionalInt.empty();
    }

    @Override
    public void sendSystemMessage(@NotNull Component component) {
    }

    private <T> T callUsingItemInSlot(Container inventory, Function<ItemStack, T> f) {
        return callUsingItemInSlot(inventory, 0, f, true);
    }

    private <T> T callUsingItemInSlot(Container inventory, int slot, java.util.function.Function<ItemStack, T> f, boolean repair) {
        List<ItemEntity> itemsBefore = adjacentItems();
        int oldSelected = getInventory().selected;
        ItemStack oldOffhand = getInventory().offhand.getFirst().copy();
        ItemStack stack = inventory.getItem(slot);
        ItemStack oldStack = stack.copy();
        ItemStack oldSlot0 = ItemStack.EMPTY;
        if (inventory == agent.mainInventory()) {
            getInventory().selected = slot;
        } else {
            oldSlot0 = getInventory().items.getFirst().copy();
            getInventory().items.set(0, inventory.getItem(slot));
            getInventory().selected = 0;
        }
        getInventory().offhand.set(0, inventory.getItem(slot));
        try {
            return f.apply(stack);
        } finally {
            getInventory().selected = oldSelected;
            if (inventory != agent.mainInventory()) {
                getInventory().items.set(0, oldSlot0);
            }
            inventory.setItem(slot, getInventory().offhand.getFirst());
            getInventory().offhand.set(0, oldOffhand);
            ItemStack newStack = inventory.getItem(slot);
            if (repair && !newStack.isEmpty()) {
                tryRepair(newStack, oldStack);
            }
            List<ItemEntity> itemsAfter = adjacentItems();
            for (ItemEntity item : itemsAfter) {
                if (!itemsBefore.contains(item)) {
                    item.setPickUpDelay(0);
                    ItemStack itemStack = item.getItem();
                    if (!itemStack.isEmpty()) {
                        int before = itemStack.getCount();
                        ItemStack remainder = addToAgentInventory(itemStack.copy());
                        int pickedUp = before - remainder.getCount();
                        if (pickedUp > 0) {
                            itemStack.shrink(pickedUp);
                        }
                        if (itemStack.isEmpty()) {
                            item.discard();
                        }
                    }
                }
            }
            Collections.fill(getInventory().items, ItemStack.EMPTY);
            if (!getInventory().offhand.getFirst().isEmpty()) {
                getInventory().offhand.set(0, ItemStack.EMPTY);
            }
        }
    }

    private void tryRepair(ItemStack stack, ItemStack oldStack) {
        if (!stack.isEmpty() && !oldStack.isEmpty() && stack.getItem() == oldStack.getItem()) {
            RobotUsedToolEvent.ComputeDamageRate damageRate = new RobotUsedToolEvent.ComputeDamageRate(agent, oldStack, stack, OCSettings.get().itemDamageRate);
            RobotUsedToolEvent.ComputeDamageRate.EVENT.invoker().onRobotComputeDamageRate(damageRate);
            if (damageRate.getDamageRate() < 1) {
                RobotUsedToolEvent.ApplyDamageRate.EVENT.invoker().onRobotApplyDamageRate(new RobotUsedToolEvent.ApplyDamageRate(agent, oldStack, stack, damageRate.getDamageRate()));
            }
        }
    }

    private ItemStack addToAgentInventory(ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int j = 0; j < agent.mainInventory().getContainerSize() && !remainder.isEmpty(); j++) {
            ItemStack slot = agent.mainInventory().getItem(j);
            if (slot.isEmpty()) {
                agent.mainInventory().setItem(j, remainder.copy());
                remainder.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(slot, remainder) && slot.getCount() < slot.getMaxStackSize()) {
                int transfer = Math.min(remainder.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(transfer);
                agent.mainInventory().setItem(j, slot);
                remainder.shrink(transfer);
            }
        }
        return remainder;
    }

    @Override
    public @NotNull ItemStack getMainHandItem() {
        if (agent != null && agent.equipmentInventory() != null && agent.equipmentInventory().getContainerSize() > 0) {
            var tool = agent.equipmentInventory().getItem(0);
            if (!tool.isEmpty()) return tool;
        }
        if (getInventory().selected >= 0 && getInventory().selected < getInventory().items.size()) {
            return getInventory().getSelected();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack getOffhandItem() {
        return getInventory().offhand.getFirst();
    }

    @Override
    public boolean addItem(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack remainder = addToAgentInventory(stack);
        return remainder.getCount() < stack.getCount();
    }

    public float getDigSpeed(@NotNull BlockState state, BlockPos ignoredPos) {
        int oldSelected = getInventory().selected;
        int slot = Math.max(oldSelected, 0);
        ItemStack oldItem = getInventory().items.get(slot).copy();
        try {
            getInventory().selected = slot;
            getInventory().items.set(slot, getMainHandItem());
            return getDestroySpeed(state);
        } finally {
            getInventory().items.set(slot, oldItem);
            getInventory().selected = oldSelected;
        }
    }

    @Override
    public boolean hasCorrectToolForDrops(@NotNull BlockState state) {
        return !state.requiresCorrectToolForDrops() || getMainHandItem().isCorrectToolForDrops(state);
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return getMainHandItem();
        }
        if (slot == EquipmentSlot.OFFHAND) {
            return getOffhandItem();
        }
        return super.getItemBySlot(slot);
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    public static class DamageOverTime {
        public final Player player;
        @SuppressWarnings("unused")
        public final int x, y, z, side;
        public final int ticksTotal;
        public final Level world;
        public int ticks = 0;
        public int lastDamageSent = 0;

        public DamageOverTime(Player player, int x, int y, int z, int side, int ticksTotal) {
            this.player = player;
            this.x = x;
            this.y = y;
            this.z = z;
            this.side = side;
            this.ticksTotal = ticksTotal;
            this.world = player.level();
        }

        public void tick() {
            if (world != player.level() || !world.isLoaded(new BlockPos(x, y, z)) || world.isEmptyBlock(new BlockPos(x, y, z)) || !player.agent.machine().isRunning()) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.destroyBlockProgress(player.getId(), new BlockPos(x, y, z), -1);
                }
                return;
            }
            int damage = 10 * ticks / Math.max(ticksTotal, 1);
            if (damage >= 10) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.destroyBlockProgress(player.getId(), new BlockPos(x, y, z), -1);
                }
                player.callUsingItemInSlot(player.agent.equipmentInventory(), stack -> {
                    int expGained = PlayerInteractionManagerHelper.blockRemoving(player, new BlockPos(x, y, z));
                    if (expGained >= 0) {
                        li.cil.oc.api.event.RobotBreakBlockEvent.Post.EVENT.invoker().onRobotBreakBlock(new li.cil.oc.api.event.RobotBreakBlockEvent.Post(player.agent, expGained));
                    }
                    return null;
                });
            } else {
                ticks++;
                if (damage != lastDamageSent) {
                    lastDamageSent = damage;
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.destroyBlockProgress(player.getId(), new BlockPos(x, y, z), damage);
                    }
                }
                EventHandler.scheduleServer(this::tick);
            }
        }
    }
}
