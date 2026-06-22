package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.ToolDurabilityProviders;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;


import java.util.Map;

public abstract class RobotBase extends AgentBase implements DeviceInfo {
    public final Node node;
    private final Map<String, String> deviceInfo;
    private final ManagedEnvironment romRobot;

    protected abstract Direction toGlobal(Direction value);

    protected abstract void animateSwing(double duration);

    protected abstract int getLightColor();

    protected abstract void setLightColor(int value);

    @SuppressWarnings("SameParameterValue")
    protected abstract ItemStack getEquipmentInSlot(int slot);

    protected abstract boolean isAnimatingMove();

    protected abstract boolean tryMove(Direction direction);

    protected abstract void rotateProxy(Direction axis);

    protected abstract void animateTurn(boolean clockwise, double duration);

    @SuppressWarnings("SameParameterValue")
    protected abstract void sendParticleEffect(BlockPosition pos, String name, int count, double speed, Direction dir);

    @SuppressWarnings("SameParameterValue")
    protected abstract void sendToReachable(String message, Object data);

    protected abstract Node getAgentNode();

    public RobotBase(String capacity) {
        this.node = Network.newNode(this, Visibility.Network)
                .withComponent("robot")
                .withConnector(Settings.get().bufferRobot)
                .create();
        this.deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.System,
                DeviceAttribute.Description, "Robot",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Caterpillar",
                DeviceAttribute.Capacity, capacity
        );
        this.romRobot = li.cil.oc.api.FileSystem.asManagedEnvironment(
                li.cil.oc.api.FileSystem.fromClass(Settings.class, Settings.resourceDomain, "lua/component/robot"),
                "robot");
        setNode(this.node);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public Direction checkSideForAction(Arguments args, int n) {
        return toGlobal(ExtendedArguments.checkSideForAction(args, n));
    }

    @Override
    public void onWorldInteraction(Context context, double duration) {
        super.onWorldInteraction(context, duration);
        animateSwing(duration);
    }

    @Callback(doc = "function():number -- Get the current color of the activity light.")
    public Object[] getLightColor(Context context, Arguments args) {
        return ResultWrapper.result((double) getLightColor());
    }

    @Callback(doc = "function(value:number):number -- Set the color of the activity light.")
    public Object[] setLightColor(Context context, Arguments args) {
        setLightColor(args.checkInteger(0));
        context.pause(0.1);
        return ResultWrapper.result((double) getLightColor());
    }

    @Callback(doc = "function():number -- Get the durability of the currently equipped tool.")
    public Object[] durability(Context context, Arguments args) {
        ItemStack stack = getEquipmentInSlot(0);
        if (!stack.isEmpty()) {
            Double durability = ToolDurabilityProviders.getDurability(stack);
            if (durability != null) return ResultWrapper.result(durability);
            return ResultWrapper.result(null, "tool cannot be damaged");
        }
        return ResultWrapper.result(null, "no tool equipped");
    }

    @Callback(doc = "function(direction:number):boolean -- Move in the specified direction.")
    public Object[] move(Context context, Arguments args) {
        Direction direction = toGlobal(ExtendedArguments.checkSideForMovement(args, 0));
        if (isAnimatingMove()) {
            return ResultWrapper.result(null, "already moving");
        }
        Object[] bc = blockContent(direction);
        if ((Boolean) bc[0]) {
            context.pause(0.4);
            sendParticleEffect(BlockPosition.apply(agent()), "crit", 8, 0.25, direction);
            return ResultWrapper.result(null, bc[1]);
        }
        if (!((Connector) node).tryChangeBuffer(-Settings.get().robotMoveCost)) {
            return ResultWrapper.result(null, "not enough energy");
        }
        if (tryMove(direction)) {
            context.pause(Settings.get().moveDelay);
            return ResultWrapper.result(true);
        }
        ((Connector) node).changeBuffer(Settings.get().robotMoveCost);
        context.pause(0.4);
        sendParticleEffect(BlockPosition.apply(agent()), "crit", 8, 0.25, direction);
        return ResultWrapper.result(null, "impossible move");
    }

    @Callback(doc = "function(clockwise:boolean):boolean -- Rotate in the specified direction.")
    public Object[] turn(Context context, Arguments args) {
        boolean clockwise = args.checkBoolean(0);
        if (((Connector) node).tryChangeBuffer(-Settings.get().robotTurnCost)) {
            if (clockwise) rotateProxy(Direction.UP);
            else rotateProxy(Direction.DOWN);
            animateTurn(clockwise, Settings.get().turnDelay);
            context.pause(Settings.get().turnDelay);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(null, "not enough energy");
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node && romRobot != null) {
            ((Component) romRobot.node()).setVisibility(Visibility.Network);
            node.connect(romRobot.node());
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("network.message".equals(message.name()) && message.source() != getAgentNode()) {
            if (message.data().length > 0 && message.data()[0] instanceof Packet) {
                sendToReachable("network.message", message.data()[0]);
            }
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (romRobot != null) {
            romRobot.load(nbt.getCompound("romRobot"), provider);
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (romRobot != null) {
            CompoundTag fsNbt = new CompoundTag();
            romRobot.save(fsNbt, provider);
            nbt.put("romRobot", fsNbt);
        }
    }
}
