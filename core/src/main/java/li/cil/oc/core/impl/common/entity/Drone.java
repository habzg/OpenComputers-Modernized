package li.cil.oc.core.impl.common.entity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.function.Function;
import li.cil.oc.api.Items;
import li.cil.oc.api.Machine;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.inventory.ComponentInventory;
import li.cil.oc.core.impl.common.inventory.Inventory;
import li.cil.oc.core.impl.common.item.data.DroneData;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.server.component.AgentBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.DroneHelper;
import li.cil.oc.core.impl.util.DroneMenuDelegate;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.FluidTankHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class Drone extends Entity implements MachineHost, li.cil.oc.api.internal.Drone, li.cil.oc.api.internal.Rotatable, li.cil.oc.api.network.Analyzable, Context {
    private static final EntityDataAccessor<Byte> DATA_RUNNING = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_TARGET_X = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_ACCELERATION = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_SELECTED_SLOT = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_GLOBAL_BUFFER = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GLOBAL_BUFFER_SIZE = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_STATUS_TEXT = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_INVENTORY_SIZE = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_LIGHT_COLOR = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_TIER = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> DATA_NAME = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.STRING);

    private static EntityType<? extends Drone> TYPE;
    private static Function<Drone, AgentBase> controlFactory;
    private static DroneMenuDelegate menuOpener;

    public static void setEntityType(EntityType<? extends Drone> type) {
        TYPE = type;
    }

    public static EntityType<? extends Drone> getEntityType() {
        return TYPE;
    }

    public static void setControlFactory(Function<Drone, AgentBase> factory) {
        controlFactory = factory;
    }

    public static void setMenuOpener(DroneMenuDelegate opener) {
        menuOpener = opener;
    }

    public final float gravity = 0.05f;
    public final float drag = 0.8f;
    public final float maxAcceleration = 0.1f;
    public final float maxVelocity = 0.4f;
    public final int maxInventorySize = 8;

    public final float[][] targetFlapAngles = new float[4][2];
    public final float[][] flapAngles = new float[4][2];
    public final DroneData info = new DroneData();
    public final li.cil.oc.api.machine.Machine machine;
    public final AgentBase control;
    public final ComponentInventory components;
    public final Inventory equipmentInventory;
    public final Inventory mainInventory;
    public final MultiTank tank;
    public int nextFlapChange = 0;
    public float bodyAngle = (float) (Math.random() * 90);
    public float angularVelocity = 0f;
    public int nextAngularVelocityChange = 0;
    public int lastEnergyUpdate = 0;
    public int selectedTank = 0;

    public String ownerName = OCSettings.get().fakePlayerName;
    public UUID ownerUUID = OCSettings.get().fakePlayerProfile.getId();

    private Player player_;
    private boolean isChangingDimension = false;

    public Drone(EntityType<? extends Drone> type, Level level) {
        super(type, level);

        setInvulnerable(true);
        this.noCulling = true;

        if (!level.isClientSide) {
            machine = Machine.create(this);
            ((Connector) machine.node()).setLocalBufferSize(0);
        } else {
            machine = null;
        }
        control = !level.isClientSide && controlFactory != null ? controlFactory.apply(this) : null;

        components = new ComponentInventory() {
            private ManagedEnvironment[] cachedComponents;
            private final ArrayList<ManagedEnvironment> updatingComponents = new ArrayList<>();

            @Override
            public EnvironmentHost host() {
                return Drone.this;
            }

            @Override
            public ItemStack[] items() {
                return info.components.toArray(new ItemStack[0]);
            }

            @Override
            public int getContainerSize() {
                return info.components.size();
            }

            @Override
            public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
                return true;
            }

            @Override
            public Node node() {
                return machine != null ? machine.node() : null;
            }

            @Override
            public void onConnect(Node ignoredNode) {
            }

            @Override
            public void onDisconnect(Node ignoredNode) {
            }

            @Override
            public void onMessage(Message ignoredMessage) {
            }

            @Override
            public ManagedEnvironment[] _components() {
                return cachedComponents;
            }

            @Override
            public void _components(ManagedEnvironment[] value) {
                cachedComponents = value;
            }

            @Override
            public boolean isSizeInventoryReady() {
                return true;
            }

            @Override
            public ArrayList<ManagedEnvironment> updatingComponents() {
                return updatingComponents;
            }

            @Override
            public void updateItems(int slot, ItemStack stack) {
            }

            @Override
            public void clearContent() {
            }
        };

        equipmentInventory = new Inventory() {
            private final ItemStack[] items = new ItemStack[0];

            @Override
            public ItemStack[] items() {
                return items;
            }

            @Override
            public int getContainerSize() {
                return 0;
            }

            @Override
            public int getMaxStackSize() {
                return 0;
            }

            @Override
            public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void updateItems(int slot, ItemStack stack) {
            }

            @Override
            public void clearContent() {
            }

            @Override
            public boolean stillValid(@NotNull Player player) {
                return false;
            }
        };

        mainInventory = new Inventory() {
            private final ItemStack[] items = new ItemStack[8];

            @Override
            public ItemStack[] items() {
                return items;
            }

            @Override
            public int getContainerSize() {
                return inventorySize();
            }

            @Override
            public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
                return slot >= 0 && slot < getContainerSize();
            }

            @Override
            public void updateItems(int slot, ItemStack stack) {
                items[slot] = stack;
            }

            @Override
            public void clearContent() {
            }

            @Override
            public boolean stillValid(@NotNull Player player) {
                return player.distanceToSqr(Drone.this) < 64;
            }
        };

        tank = FluidTankHelper.get() != null ? (MultiTank) FluidTankHelper.get().createMultiTank(components) : null;
    }

    @Override
    public li.cil.oc.api.machine.Machine machine() {
        return machine;
    }

    @Override
    public void setSelectedSlot(int index) {
        selectedSlot(index);
    }

    @Override
    public void setSelectedTank(int index) {
        selectedTank = index;
    }

    @Override
    public int selectedTank() {
        return selectedTank;
    }

    @Override
    public Container equipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public Container mainInventory() {
        return mainInventory;
    }

    @Override
    public MultiTank tank() {
        return tank;
    }

    @Override
    public String ownerName() {
        return ownerName;
    }

    @Override
    public UUID ownerUUID() {
        return ownerUUID;
    }

    @Override
    public int tier() {
        return level().isClientSide ? entityData.get(DATA_TIER) : info.tier;
    }

    @Override
    public Player player() {
        if (player_ == null && DroneHelper.get() != null) player_ = DroneHelper.get().createPlayer(this);
        if (player_ != null && DroneHelper.get() != null) {
            DroneHelper.get().updatePlayerPosition(player_, facing(), facing());
            DroneHelper.get().setPlayerInventoryItems(player_);
        }
        return player_;
    }

    @Override
    public String name() {
        return level().isClientSide ? entityData.get(DATA_NAME) : info.name;
    }

    @Override
    public void setName(String name) {
        info.name = name;
        entityData.set(DATA_NAME, name);
    }

    @Override
    public Node node() {
        return machine != null ? machine.node() : null;
    }

    @Override
    public boolean canInteract(String player) {
        return machine != null && machine.canInteract(player);
    }

    @Override
    public boolean isPaused() {
        return machine != null && machine.isPaused();
    }

    @Override
    public boolean start() {
        if (level().isClientSide || (machine != null && machine.isRunning())) return false;
        preparePowerUp();
        return machine != null && machine.start();
    }

    @Override
    public boolean pause(double seconds) {
        return machine != null && machine.pause(seconds);
    }

    @Override
    public boolean stop() {
        return machine != null && machine.stop();
    }

    @Override
    public void consumeCallBudget(double callCost) {
        if (machine != null) machine.consumeCallBudget(callCost);
    }

    @Override
    public boolean signal(String name, Object... args) {
        return machine != null && machine.signal(name, args);
    }

    @Override
    public Vec3 getTarget() {
        return new Vec3(targetX(), targetY(), targetZ());
    }

    @Override
    public void setTarget(Vec3 value) {
        targetX((float) value.x);
        targetY((float) value.y);
        targetZ((float) value.z);
    }

    @Override
    public Vec3 getVelocity() {
        return new Vec3(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.fixed(12 / 16f, 6 / 16f);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(0.15, 0.1, 0.15);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public double xPosition() {
        return getX();
    }

    @Override
    public double yPosition() {
        return getY();
    }

    @Override
    public double zPosition() {
        return getZ();
    }

    @Override
    public void markChanged() {
    }

    @Override
    public Direction facing() {
        return Direction.SOUTH;
    }

    @Override
    public Direction toLocal(Direction value) {
        return value;
    }

    @Override
    public Direction toGlobal(Direction value) {
        return value;
    }

    @Override
    public Node[] onAnalyze(Player ignoredPlayer, Direction ignoredSide, float ignoredHitX, float ignoredHitY, float ignoredHitZ) {
        return new Node[]{machine != null ? machine.node() : null};
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        return info.components;
    }

    @Override
    public int componentSlot(String address) {
        int i = 0;
        for (var env : components.componentEnvironments()) {
            if (env != null && env.node() != null && address.equals(env.node().address())) return i;
            i++;
        }
        return -1;
    }

    @Override
    public void onMachineConnect(Node node) {
    }

    @Override
    public void onMachineDisconnect(Node node) {
    }

    public int computeInventorySize() {
        return Math.min(maxInventorySize, info.components.stream().mapToInt(component -> {
            if (component == null || component.isEmpty()) return 0;
            var driver = li.cil.oc.api.API.driver.driverFor(component, getClass());
            if (driver instanceof li.cil.oc.api.driver.item.Inventory invDriver) {
                return Math.max(1, invDriver.inventoryCapacity(component) / 4);
            }
            return 0;
        }).sum());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RUNNING, (byte) 0);
        builder.define(DATA_TARGET_X, 0f);
        builder.define(DATA_TARGET_Y, 0f);
        builder.define(DATA_TARGET_Z, 0f);
        builder.define(DATA_TARGET_ACCELERATION, 0f);
        builder.define(DATA_SELECTED_SLOT, (byte) 0);
        builder.define(DATA_GLOBAL_BUFFER, 0);
        builder.define(DATA_GLOBAL_BUFFER_SIZE, 100);
        builder.define(DATA_STATUS_TEXT, "");
        builder.define(DATA_INVENTORY_SIZE, (byte) 0);
        builder.define(DATA_LIGHT_COLOR, 0x66DD55);
        builder.define(DATA_TIER, (byte) 0);
        builder.define(DATA_NAME, "");
    }

    @SuppressWarnings("unused")
    public void initializeAfterPlacement(ItemStack stack, Player player, Vec3 position) {
        info.load(stack, level().registryAccess());
        syncInfo();
        ((Connector) control.node()).changeBuffer(info.storedEnergy - ((Connector) control.node()).localBuffer());
        wireThingsTogether();
        inventorySize(computeInventorySize());
        setPos(position.x, position.y + getDimensions(Pose.STANDING).height() / 2, position.z);
    }

    public void preparePowerUp() {
        targetX((float) Math.floor(getX()) + 0.5f);
        targetY((float) Math.round(getY()) + 0.5f);
        targetZ((float) Math.floor(getZ()) + 0.5f);
        targetAcceleration(maxAcceleration);
        wireThingsTogether();
    }

    private void wireThingsTogether() {
        li.cil.oc.api.Network.joinNewNetwork(machine.node());
        machine.node().connect(control.node());
        machine.setCostPerTick(OCSettings.get().droneCost);
        components.connectComponents();
    }

    public boolean isRunning() {
        return entityData.get(DATA_RUNNING) != 0;
    }

    public void setRunning(boolean value) {
        entityData.set(DATA_RUNNING, (byte) (value ? 1 : 0));
    }

    public float targetX() {
        return entityData.get(DATA_TARGET_X);
    }

    public float targetY() {
        return entityData.get(DATA_TARGET_Y);
    }

    public float targetZ() {
        return entityData.get(DATA_TARGET_Z);
    }

    public float targetAcceleration() {
        return entityData.get(DATA_TARGET_ACCELERATION);
    }

    public int selectedSlot() {
        return entityData.get(DATA_SELECTED_SLOT) & 0xFF;
    }

    public int globalBuffer() {
        return entityData.get(DATA_GLOBAL_BUFFER);
    }

    public int globalBufferSize() {
        return entityData.get(DATA_GLOBAL_BUFFER_SIZE);
    }

    public String statusText() {
        return entityData.get(DATA_STATUS_TEXT);
    }

    public int inventorySize() {
        return entityData.get(DATA_INVENTORY_SIZE) & 0xFF;
    }

    public int lightColor() {
        return entityData.get(DATA_LIGHT_COLOR);
    }

    public void targetX(float value) {
        entityData.set(DATA_TARGET_X, Math.round(value * 4) / 4f);
    }

    public void targetY(float value) {
        entityData.set(DATA_TARGET_Y, Math.round(value * 4) / 4f);
    }

    public void targetZ(float value) {
        entityData.set(DATA_TARGET_Z, Math.round(value * 4) / 4f);
    }

    public void targetAcceleration(float value) {
        entityData.set(DATA_TARGET_ACCELERATION, Math.clamp(value, 0, maxAcceleration));
    }

    public void selectedSlot(int value) {
        entityData.set(DATA_SELECTED_SLOT, (byte) value);
    }

    public void globalBuffer(int value) {
        entityData.set(DATA_GLOBAL_BUFFER, value);
    }

    public void globalBufferSize(int value) {
        entityData.set(DATA_GLOBAL_BUFFER_SIZE, value);
    }

    public void statusText(String value) {
        if (value == null) value = "";
        var lines = value.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, lines.length); i++) {
            if (i > 0) sb.append("\n");
            String line = lines[i];
            sb.append(line.length() > 10 ? line.substring(0, 10) : line);
        }
        entityData.set(DATA_STATUS_TEXT, sb.toString());
    }

    public void inventorySize(int value) {
        entityData.set(DATA_INVENTORY_SIZE, (byte) value);
    }

    public void lightColor(int value) {
        entityData.set(DATA_LIGHT_COLOR, value);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
        if (!isRunning() || distanceToSqr(x, y, z) > 1) {
            super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements);
        } else {
            targetX((float) x);
            targetY((float) y);
            targetZ((float) z);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (isInWater() || isInLava()) {
                if (machine != null) machine.stop();
            }
            if (machine != null) machine.update();
            components.updateComponents();
            setRunning(machine != null && machine.isRunning());

            if (machine != null) {
                int buffer = (int) Math.round(((Connector) machine.node()).globalBuffer());
                if (Math.abs(lastEnergyUpdate - buffer) > 1 || level().getGameTime() % 200 == 0) {
                    lastEnergyUpdate = buffer;
                    globalBuffer(buffer);
                    globalBufferSize((int) ((Connector) machine.node()).globalBufferSize());
                }
            }
        } else {
            if (isRunning()) {
                var rng = level().random;
                nextFlapChange--;
                nextAngularVelocityChange--;

                if (nextFlapChange < 0) {
                    nextFlapChange = 5 + rng.nextInt(10);
                    for (int i = 0; i < 2; i++) {
                        int flap = rng.nextInt(targetFlapAngles.length);
                        targetFlapAngles[flap][0] = (float) Math.toRadians(rng.nextFloat() * 4 - 2);
                        targetFlapAngles[flap][1] = (float) Math.toRadians(rng.nextFloat() * 4 - 2);
                    }
                }

                if (nextAngularVelocityChange < 0) {
                    if (angularVelocity != 0) {
                        angularVelocity = 0;
                        nextAngularVelocityChange = 20;
                    } else {
                        angularVelocity = rng.nextBoolean() ? 0.1f : -0.1f;
                        nextAngularVelocityChange = 100;
                    }
                }

                for (int i = 0; i < flapAngles.length; i++) {
                    flapAngles[i][0] = flapAngles[i][0] * 0.7f + targetFlapAngles[i][0] * 0.3f;
                    flapAngles[i][1] = flapAngles[i][1] * 0.7f + targetFlapAngles[i][1] * 0.3f;
                }

                bodyAngle += angularVelocity;
            }
        }

        setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z);
        noPhysics = pushOutOfBlocks(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2, getZ());

        if (isRunning()) {
            Vec3 toTarget = new Vec3(targetX() - getX(), targetY() - getY(), targetZ() - getZ());
            double distance = toTarget.length();
            Vec3 velocity = getDeltaMovement();
            if (distance > 0 && (distance > 0.005f || velocity.dot(velocity) > 0.005f)) {
                double acceleration = Math.min(targetAcceleration(), distance) / distance;
                double vx = velocity.x + toTarget.x * acceleration;
                double vy = velocity.y + toTarget.y * acceleration;
                double vz = velocity.z + toTarget.z * acceleration;
                setDeltaMovement(
                        Math.clamp(vx, -maxVelocity, maxVelocity),
                        Math.clamp(vy, -maxVelocity, maxVelocity),
                        Math.clamp(vz, -maxVelocity, maxVelocity)
                );
            } else {
                setDeltaMovement(0, 0, 0);
                setPos(targetX(), targetY(), targetZ());
            }
        } else {
            setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y - gravity, getDeltaMovement().z);
        }

        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

        if (isRunning()) {
            setDeltaMovement(getDeltaMovement().x * drag, getDeltaMovement().y * drag, getDeltaMovement().z * drag);
        } else {
            BlockPos below = BlockPos.containing(position()).below();
            float groundDrag = level().getBlockState(below).getBlock().getFriction() * drag;
            setDeltaMovement(getDeltaMovement().x * groundDrag, getDeltaMovement().y * drag, getDeltaMovement().z * groundDrag);
            if (onGround()) {
                setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y * -0.5, getDeltaMovement().z);
            }
        }
    }

    private boolean pushOutOfBlocks(double x, double y, double z) {
        BlockPos blockPos = BlockPos.containing(x, y, z);
        if (!level().isLoaded(blockPos)) return false;
        var box = getBoundingBox();
        if (level().getBlockCollisions(this, box).iterator().hasNext()) {
            double height = getDimensions(Pose.STANDING).height();
            for (double dy = 0.1; dy <= height + 0.5; dy += 0.1) {
                setPos(getX(), getY() + dy, getZ());
                if (level().noCollision(this, getBoundingBox())) return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(@NotNull Entity entity) {
        if (isRunning()) {
            Vec3 direction = new Vec3(
                    entity.getX() - getX(),
                    entity.getY() + entity.getEyeHeight() - getY(),
                    entity.getZ() - getZ()
            ).normalize();
            if (!level().isClientSide && machine != null) {
                if (OCSettings.get().inputUsername) {
                    machine.signal("hit", direction.x, direction.z, direction.y, entity.getName().getString());
                } else {
                    machine.signal("hit", direction.x, direction.z, direction.y);
                }
            }
            setDeltaMovement(
                    (getDeltaMovement().x - direction.x) * 0.5f,
                    (getDeltaMovement().y - direction.y) * 0.5f,
                    (getDeltaMovement().z - direction.z) * 0.5f
            );
        }
        return super.skipAttackInteraction(entity);
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (isRemoved()) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            if (Wrench.isWrench(player.getItemInHand(hand))) {
                if (!level().isClientSide) {
                    kill();
                }
            } else if (!level().isClientSide && machine != null && !machine.isRunning()) {
                start();
            }
        } else if (!level().isClientSide) {
            if (menuOpener != null) {
                menuOpener.openMenu(player, this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void playStepSound(@NotNull BlockPos pos, net.minecraft.world.level.block.state.@NotNull BlockState state) {
        var now = Calendar.getInstance();
        if (now.get(Calendar.MONTH) == Calendar.APRIL && now.get(Calendar.DAY_OF_MONTH) == 1) {
            super.playStepSound(pos, state);
        }
    }

    @Override
    public Entity changeDimension(@NotNull DimensionTransition transition) {
        targetX((targetX() - (float) getX()));
        targetY((targetY() - (float) getY()));
        targetZ((targetZ() - (float) getZ()));
        try {
            isChangingDimension = true;
            return super.changeDimension(transition);
        } finally {
            isChangingDimension = false;
            remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    private void syncInfo() {
        entityData.set(DATA_TIER, (byte) info.tier);
        entityData.set(DATA_NAME, info.name == null ? "" : info.name);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        info.load(nbt.getCompound("info"), level().registryAccess());
        syncInfo();
        inventorySize(computeInventorySize());
        if (!level().isClientSide) {
            if (machine != null) {
                machine.load(nbt.getCompound("machine"), level().registryAccess());
            }
            control.load(nbt.getCompound("control"), level().registryAccess());
            components.load(nbt.getCompound("components"), level().registryAccess());
            mainInventory.load(nbt.getCompound("inventory"), level().registryAccess());
            wireThingsTogether();
        }
        targetX(nbt.getFloat("targetX"));
        targetY(nbt.getFloat("targetY"));
        targetZ(nbt.getFloat("targetZ"));
        targetAcceleration(nbt.getFloat("targetAcceleration"));
        selectedSlot(nbt.getByte("selectedSlot") & 0xFF);
        setSelectedTank(nbt.getByte("selectedTank") & 0xFF);
        statusText(nbt.getString("statusText"));
        lightColor(nbt.getInt("lightColor"));
        if (nbt.contains("owner")) {
            ownerName = nbt.getString("owner");
        }
        if (nbt.contains("ownerUuid")) {
            ownerUUID = UUID.fromString(nbt.getString("ownerUuid"));
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        if (level().isClientSide) return;
        components.saveComponents(level().registryAccess());
        info.storedEnergy = (int) ((Connector) control.node()).localBuffer();
        ExtendedNBT.setNewCompoundTag(nbt, "info", tag -> info.save(tag, level().registryAccess()));
        if (!level().isClientSide) {
            if (machine != null)
                ExtendedNBT.setNewCompoundTag(nbt, "machine", tag -> machine.save(tag, level().registryAccess()));
            ExtendedNBT.setNewCompoundTag(nbt, "control", tag -> control.save(tag, level().registryAccess()));
            ExtendedNBT.setNewCompoundTag(nbt, "components", tag -> components.save(tag, level().registryAccess()));
            ExtendedNBT.setNewCompoundTag(nbt, "inventory", tag -> mainInventory.save(tag, level().registryAccess()));
        }
        nbt.putFloat("targetX", targetX());
        nbt.putFloat("targetY", targetY());
        nbt.putFloat("targetZ", targetZ());
        nbt.putFloat("targetAcceleration", targetAcceleration());
        nbt.putByte("selectedSlot", (byte) selectedSlot());
        nbt.putByte("selectedTank", (byte) selectedTank);
        nbt.putString("statusText", statusText());
        nbt.putInt("lightColor", lightColor());
        nbt.putString("owner", ownerName);
        nbt.putString("ownerUuid", ownerUUID.toString());
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && !isChangingDimension) {
            if (machine != null) machine.stop();
            if (machine != null) machine.node().remove();
            components.disconnectComponents();
            components.saveComponents(level().registryAccess());
        }
    }

    @Override
    public void kill() {
        if (isRemoved()) return;
        super.kill();
        if (!level().isClientSide) {
            var stack = Items.get(Constants.ItemName.Drone).createItemStack(1);
            info.storedEnergy = (int) ((Connector) control.node()).localBuffer();
            info.save(stack, level().registryAccess());
            var entity = new net.minecraft.world.entity.item.ItemEntity(level(), getX(), getY(), getZ(), stack);
            entity.setPickUpDelay(15);
            level().addFreshEntity(entity);
            InventoryUtils.dropAllSlots(BlockPosition.apply((Entity) this), mainInventory);
        }
    }

    @Override
    public net.minecraft.network.chat.@NotNull Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("entity.opencomputers.drone");
    }

    @Override
    public boolean isInWater() {
        return level().isWaterAt(BlockPos.containing(position()));
    }

    @Override
    public boolean isInLava() {
        return level().getFluidState(BlockPos.containing(position())).is(FluidTags.LAVA);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.@NotNull ServerEntity entity) {
        return super.getAddEntityPacket(entity);
    }

}
