package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.tileentity.traits.BundledRedstoneAware;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;

public interface RedstoneBundled extends DeviceInfo {
    int[] COLOR_RANGE = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    BundledRedstoneAware bundledRedstone();

    default Map<String, String> bundledDeviceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put(DeviceAttribute.Class, DeviceClass.Communication);
        info.put(DeviceAttribute.Description, "Advanced redstone controller");
        info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        info.put(DeviceAttribute.Product, "Rb800-M");
        info.put(DeviceAttribute.Capacity, "65536");
        info.put(DeviceAttribute.Width, "16");
        return info;
    }

    @Override
    default Map<String, String> getDeviceInfo() {
        return bundledDeviceInfo();
    }

    default Object[] getBundleKey(Arguments args) {
        int count = args.count();
        if (count == 2) return new Object[]{checkSide(args, 0), checkColor(args, 1)};
        if (count == 1) return new Object[]{checkSide(args, 0), null};
        if (count == 0) return new Object[]{null, null};
        throw new RuntimeException("too many arguments, expected 0, 1, or 2");
    }

    default Direction checkSide(Arguments args, int index) {
        int side = args.checkInteger(index);
        if (side < 0 || side > 5) throw new IllegalArgumentException("invalid side");
        return bundledRedstone().toGlobal(Direction.from3DDataValue(side));
    }

    default int checkColor(Arguments args, int index) {
        int color = args.checkInteger(index);
        if (color < 0 || color > 15) throw new IllegalArgumentException("invalid color");
        return color;
    }

    default Map<Integer, Integer> colorsToMap(int[] ar) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int color : COLOR_RANGE) {
            if (color < ar.length) map.put(color, ar[color]);
        }
        return map;
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Fewer params returns set of inputs.")
    default Object[] getBundledInput(Context context, Arguments args) {
        Object[] key = getBundleKey(args);
        Direction side = (Direction) key[0];
        Integer color = (Integer) key[1];
        if (color != null) return ResultWrapper.result((double) bundledRedstone().getBundledInput(side, color));
        if (side != null)
            return ResultWrapper.result(colorsToMap(bundledRedstone().getBundledInput(side)));
        return ResultWrapper.result(sidesToMap(bundledRedstone().bundledInput()));
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Fewer params returns set of outputs.")
    default Object[] getBundledOutput(Context context, Arguments args) {
        Object[] key = getBundleKey(args);
        Direction side = (Direction) key[0];
        Integer color = (Integer) key[1];
        if (color != null) return ResultWrapper.result((double) bundledRedstone().getBundledOutput(side, color));
        if (side != null)
            return ResultWrapper.result(colorsToMap(bundledRedstone().bundledOutput()[side.get3DDataValue()]));
        return ResultWrapper.result(sidesToMap(bundledRedstone().getBundledOutput()));
    }

    @SuppressWarnings("rawtypes")
    @Callback(doc = "function([side:number[, color:number,]] value:number or table):number or table -- Fewer params to assign set of outputs. Returns previous values.")
    default Object[] setBundledOutput(Context context, Arguments args) {
        Object ret;
        int count = args.count();
        if (count == 3) {
            Direction side = checkSide(args, 0);
            int color = checkColor(args, 1);
            int value = args.checkInteger(2);
            ret = bundledRedstone().getBundledOutput(side, color);
            bundledRedstone().setBundledOutput(side, color, value);
        } else if (count == 2) {
            Direction side = checkSide(args, 0);
            Map value = args.checkTable(1);
            ret = colorsToMap(bundledRedstone().getBundledOutput(side));
            bundledRedstone().setBundledOutput(side, value);
        } else if (count == 1) {
            Map value = args.checkTable(0);
            ret = sidesToMap(bundledRedstone().getBundledOutput());
            bundledRedstone().setBundledOutput(value);
        } else {
            throw new RuntimeException("invalid number of arguments, expected 1, 2, or 3");
        }
        if (Settings.get().redstoneDelay > 0) context.pause(Settings.get().redstoneDelay);
        return ResultWrapper.result(ret);
    }

    default Map<Integer, Map<Integer, Integer>> sidesToMap(int[][] ar) {
        Map<Integer, Map<Integer, Integer>> map = new HashMap<>();
        for (Direction side : Direction.values()) {
            int ord = side.ordinal();
            if (ord < ar.length && ar[ord].length > 0) {
                map.put(ord, colorsToMap(ar[ord]));
            }
        }
        return map;
    }
}
