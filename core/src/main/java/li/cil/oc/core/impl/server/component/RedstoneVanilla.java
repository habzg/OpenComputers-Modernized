package li.cil.oc.core.impl.server.component;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware.RedstoneChangedEventArgs;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class RedstoneVanilla extends RedstoneSignaller implements DeviceInfo {
    protected static final Direction[] SIDE_RANGE = Direction.values();
    private final Map<String, String> deviceInfo;

    public RedstoneVanilla() {
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Communication, DeviceAttribute.Description, "Redstone controller", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Rs100-V", DeviceAttribute.Capacity, "16", DeviceAttribute.Width, "1");
    }

    public abstract EnvironmentHost redstone();

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Get the redstone input (all sides, or optionally on the specified side)")
    public Object[] getInput(Context context, Arguments args) {
        Direction side = getOptionalSide(args);
        if (side != null)
            return ResultWrapper.result((double) ((RedstoneAware) redstone()).getInput(side));
        return ResultWrapper.result(valuesToMap(((RedstoneAware) redstone()).input()));
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Get the redstone output (all sides, or optionally on the specified side)")
    public Object[] getOutput(Context context, Arguments args) {
        Direction side = getOptionalSide(args);
        if (side != null)
            return ResultWrapper.result((double) ((RedstoneAware) redstone()).getOutput(side));
        return ResultWrapper.result(valuesToMap(((RedstoneAware) redstone()).output()));
    }

    @SuppressWarnings("rawtypes")
    @Callback(doc = "function([side:number, ]value:number or table):number or table --  Set the redstone output (all sides, or optionally on the specified side). Returns previous values")
    public Object[] setOutput(Context context, Arguments args) {
        Object ret;
        if (args.count() == 2) {
            Direction side = checkSide(args);
            int value = args.checkInteger(1);
            ret = ((RedstoneAware) redstone()).getOutput(side);
            ((RedstoneAware) redstone()).setOutput(side, value);
        } else if (args.count() == 1) {
            Map table = args.checkTable(0);
            ret = valuesToMap(((RedstoneAware) redstone()).output());
            ((RedstoneAware) redstone()).setOutput(table);
        } else {
            throw new RuntimeException("invalid number of arguments, expected 1 or 2");
        }
        if (OCSettings.get().redstoneDelay > 0)
            context.pause(OCSettings.get().redstoneDelay);
        return ResultWrapper.result(ret);
    }

    @Callback(direct = true, doc = "function(side:number):number -- Get the comparator input on the specified side.")
    public Object[] getComparatorInput(Context context, Arguments args) {
        Direction side = checkSide(args);
        BlockPosition blockPos = BlockPosition.apply(redstone()).offset(side);
        var level = redstone().level();
        if (level.isLoaded(blockPos.toBlockPos())) {
            BlockState state = level.getBlockState(blockPos.toBlockPos());
            if (state.hasAnalogOutputSignal()) {
                return ResultWrapper.result((double) state.getSignal(level, blockPos.toBlockPos(), side.getOpposite()));
            }
        }
        return ResultWrapper.result(0);
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("redstone.changed".equals(message.name()) && message.data().length > 0 && message.data()[0] instanceof RedstoneChangedEventArgs) {
            onRedstoneChanged((RedstoneChangedEventArgs) message.data()[0]);
        }
    }

    private @Nullable Direction getOptionalSide(Arguments args) {
        if (args.count() == 1)
            return checkSide(args);
        return null;
    }

    protected Direction checkSide(Arguments args) {
        int side = args.checkInteger(0);
        if (side < 0 || side > 5)
            throw new IllegalArgumentException("invalid side");
        return ((RedstoneAware) redstone()).toGlobal(Direction.from3DDataValue(side));
    }

    private Map<Integer, Integer> valuesToMap(int[] ar) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Direction side : SIDE_RANGE) {
            int ord = side.ordinal();
            if (ord < ar.length)
                map.put(ord, ar[ord]);
        }
        return map;
    }
}
