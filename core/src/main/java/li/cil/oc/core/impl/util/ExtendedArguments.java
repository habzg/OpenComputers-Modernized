package li.cil.oc.core.impl.util;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;

public final class ExtendedArguments {

    public static int optItemCount(Arguments args, int index, int defaultVal) {
        if (isUndefined(args, index) || isMissing(args, index)) return defaultVal;
        return Math.clamp(args.checkInteger(index), 0, 64);
    }

    public static int optFluidCount(Arguments args, int index, int defaultVal) {
        if (isUndefined(args, index) || isMissing(args, index)) return defaultVal;
        return Math.max(0, args.checkInteger(index));
    }

    public static int checkSlot(Arguments args, Container inventory, int n) {
        int slot = args.checkInteger(n) - 1;
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            throw new IllegalArgumentException("invalid slot");
        }
        return slot;
    }

    public static int optSlot(Arguments args, Container inventory, int index, int defaultVal) {
        if (isUndefined(args, index)) return defaultVal;
        return checkSlot(args, inventory, index);
    }

    public static int checkTank(Arguments args, MultiTank multi, int n) {
        int tank = args.checkInteger(n) - 1;
        if (tank < 0 || tank >= multi.tankCount()) {
            throw new IllegalArgumentException("invalid tank index");
        }
        return tank;
    }

    public static Direction checkSideAny(Arguments args, int index) {
        return checkSide(args, index, Direction.values());
    }

    public static Direction optSideAny(Arguments args, int index, Direction defaultVal) {
        return optSide(args, index, defaultVal);
    }

    public static Direction checkSideForAction(Arguments args, int index) {
        return checkSide(args, index, Direction.SOUTH, Direction.UP, Direction.DOWN);
    }

    public static Direction checkSideForMovement(Arguments args, int index) {
        return checkSide(args, index, Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN);
    }

    public static Direction optSide(Arguments args, int index, Direction defaultVal) {
        if (isUndefined(args, index)) return defaultVal;
        return checkSideAny(args, index);
    }

    public static Direction checkSide(Arguments args, int index, Direction... allowed) {
        int sideIndex = args.checkInteger(index);
        if (sideIndex < 0 || sideIndex > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        Direction side = Direction.from3DDataValue(sideIndex);
        for (Direction allowedSide : allowed) {
            if (side == allowedSide) return side;
        }
        throw new IllegalArgumentException("unsupported side");
    }

    public static boolean isUndefined(Arguments args, int index) {
        return index < 0 || index >= args.count();
    }

    public static boolean isMissing(Arguments args, int index) {
        return index < 0 || index >= args.count() || args.checkAny(index) == null;
    }
}
