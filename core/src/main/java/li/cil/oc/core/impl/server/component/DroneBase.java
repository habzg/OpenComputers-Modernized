package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;

public abstract class DroneBase extends AgentBase implements DeviceInfo {
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("drone")
            .withConnector(OCSettings.get().bufferDrone)
            .create();
    private final Map<String, String> deviceInfo;

    protected DroneBase(int inventorySize) {
        deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.System,
                DeviceAttribute.Description, "Drone",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Overwatcher",
                DeviceAttribute.Capacity, String.valueOf(inventorySize));
        setNode(this.node);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    protected abstract String statusTextImpl();

    protected abstract void setStatusTextImpl(String value);

    protected abstract int lightColorImpl();

    protected abstract void setLightColorImpl(int value);

    protected abstract float targetXImpl();

    protected abstract void setTargetXImpl(float value);

    protected abstract float targetYImpl();

    protected abstract void setTargetYImpl(float value);

    protected abstract float targetZImpl();

    protected abstract void setTargetZImpl(float value);

    protected abstract double distanceToTargetSqr();

    protected abstract double motionX();

    protected abstract double motionY();

    protected abstract double motionZ();

    protected abstract float maxVelocity();

    protected abstract float targetAccelerationImpl();

    protected abstract void setTargetAccelerationImpl(float value);

    protected abstract void playPickupSound();

    @Override
    public void onSuckCollect(ItemEntity entity) {
        var inv = inventory();
        if (InventoryUtils.insertIntoInventory(entity.getItem(), inv, null, 64)) {
            playPickupSound();
        }
    }

    @Override
    public int suckFromItems(Direction facing) {
        int extracted = super.suckFromItems(facing);
        if (extracted <= 0) {
            extracted = collectFromItems(entitiesInBlock(position(), ItemEntity.class));
        }
        return extracted;
    }

    @Override
    public void onWorldInteraction(Context context, double duration) {
        super.onWorldInteraction(context, duration * 2);
    }

    @Callback(doc = "function():string -- Get the status text currently being displayed in the GUI.")
    public Object[] getStatusText(Context context, Arguments args) {
        return ResultWrapper.result(statusTextImpl());
    }

    @Callback(doc = "function(value:string):string -- Set the status text to display in the GUI, returns new value.")
    public Object[] setStatusText(Context context, Arguments args) {
        setStatusTextImpl(args.checkString(0));
        context.pause(0.1);
        return ResultWrapper.result(statusTextImpl());
    }

    @Callback(doc = "function():number -- Get the current color of the flap lights as an integer encoded RGB value (0xRRGGBB).")
    public Object[] getLightColor(Context context, Arguments args) {
        return ResultWrapper.result(lightColorImpl());
    }

    @Callback(doc = "function(value:number):number -- Set the color of the flap lights to the specified integer encoded RGB value (0xRRGGBB).")
    public Object[] setLightColor(Context context, Arguments args) {
        setLightColorImpl(args.checkInteger(0));
        context.pause(0.1);
        return ResultWrapper.result(lightColorImpl());
    }

    @Callback(doc = "function(dx:number, dy:number, dz:number) -- Change the target position by the specified offset.")
    @SuppressWarnings("SameReturnValue")
    public Object @Nullable [] move(Context context, Arguments args) {
        setTargetXImpl(targetXImpl() + (float) args.checkDouble(0));
        setTargetYImpl(targetYImpl() + (float) args.checkDouble(1));
        setTargetZImpl(targetZImpl() + (float) args.checkDouble(2));
        return null;
    }

    @Callback(doc = "function():number -- Get the current distance to the target position.")
    public Object[] getOffset(Context context, Arguments args) {
        return ResultWrapper.result(Math.sqrt(distanceToTargetSqr()));
    }

    @Callback(doc = "function():number -- Get the current velocity in m/s.")
    public Object[] getVelocity(Context context, Arguments args) {
        return ResultWrapper.result(Math.sqrt(motionX() * motionX() + motionY() * motionY() + motionZ() * motionZ()) * 20);
    }

    @Callback(doc = "function():number -- Get the maximum velocity, in m/s.")
    public Object[] getMaxVelocity(Context context, Arguments args) {
        return ResultWrapper.result(maxVelocity() * 20);
    }

    @Callback(doc = "function():number -- Get the currently set acceleration.")
    public Object[] getAcceleration(Context context, Arguments args) {
        return ResultWrapper.result(targetAccelerationImpl() * 20);
    }

    @Callback(doc = "function(value:number):number -- Try to set the acceleration.")
    public Object[] setAcceleration(Context context, Arguments args) {
        setTargetAccelerationImpl((float) (args.checkDouble(0) / 20.0));
        return ResultWrapper.result(targetAccelerationImpl() * 20);
    }
}
