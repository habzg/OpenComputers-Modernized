package li.cil.oc.core.impl.util;

import net.minecraft.core.Direction;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RotationHelper {

    private static final Map<Direction, Map<Direction, Direction[]>> translationCache = new LinkedHashMap<>();
    private static final Map<Direction, Map<Direction, Direction[]>> inverseTranslationCache = new LinkedHashMap<>();
    private static final Direction[][][] translations = {
            // Pitch = Down (0)
            {
                    // Yaw = North (2->0)
                    {D.SOUTH, D.NORTH, D.UP, D.DOWN, D.EAST, D.WEST},
                    // Yaw = South (3->1)
                    {D.SOUTH, D.NORTH, D.DOWN, D.UP, D.WEST, D.EAST},
                    // Yaw = West (4->2)
                    {D.SOUTH, D.NORTH, D.WEST, D.EAST, D.UP, D.DOWN},
                    // Yaw = East (5->3)
                    {D.SOUTH, D.NORTH, D.EAST, D.WEST, D.DOWN, D.UP}
            },
            // Pitch = Up (1)
            {
                    {D.NORTH, D.SOUTH, D.DOWN, D.UP, D.EAST, D.WEST},
                    {D.NORTH, D.SOUTH, D.UP, D.DOWN, D.WEST, D.EAST},
                    {D.NORTH, D.SOUTH, D.WEST, D.EAST, D.DOWN, D.UP},
                    {D.NORTH, D.SOUTH, D.EAST, D.WEST, D.UP, D.DOWN}
            },
            // Pitch = North (2)
            {
                    {D.DOWN, D.UP, D.SOUTH, D.NORTH, D.EAST, D.WEST},
                    {D.DOWN, D.UP, D.NORTH, D.SOUTH, D.WEST, D.EAST},
                    {D.DOWN, D.UP, D.WEST, D.EAST, D.SOUTH, D.NORTH},
                    {D.DOWN, D.UP, D.EAST, D.WEST, D.NORTH, D.SOUTH}
            },
            // Pitch = South (3)
            {
                    {D.DOWN, D.UP, D.SOUTH, D.NORTH, D.EAST, D.WEST},
                    {D.DOWN, D.UP, D.NORTH, D.SOUTH, D.WEST, D.EAST},
                    {D.DOWN, D.UP, D.WEST, D.EAST, D.SOUTH, D.NORTH},
                    {D.DOWN, D.UP, D.EAST, D.WEST, D.NORTH, D.SOUTH}
            },
            // Pitch = West (4)
            {
                    {D.DOWN, D.UP, D.SOUTH, D.NORTH, D.EAST, D.WEST},
                    {D.DOWN, D.UP, D.NORTH, D.SOUTH, D.WEST, D.EAST},
                    {D.DOWN, D.UP, D.WEST, D.EAST, D.SOUTH, D.NORTH},
                    {D.DOWN, D.UP, D.EAST, D.WEST, D.NORTH, D.SOUTH}
            },
            // Pitch = East (5)
            {
                    {D.DOWN, D.UP, D.SOUTH, D.NORTH, D.EAST, D.WEST},
                    {D.DOWN, D.UP, D.NORTH, D.SOUTH, D.WEST, D.EAST},
                    {D.DOWN, D.UP, D.WEST, D.EAST, D.SOUTH, D.NORTH},
                    {D.DOWN, D.UP, D.EAST, D.WEST, D.NORTH, D.SOUTH},
            }
    };

    public static Direction fromYaw(float yaw) {
        return switch (Math.round(yaw / 360 * 4) & 3) {
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            default -> Direction.SOUTH;
        };
    }

    public static Direction toLocal(Direction pitch, Direction yaw, Direction value) {
        return translationFor(pitch, yaw)[value.get3DDataValue()];
    }

    public static Direction toGlobal(Direction pitch, Direction yaw, Direction value) {
        return inverseTranslationFor(pitch, yaw)[value.get3DDataValue()];
    }

    public static Direction[] translationFor(Direction pitch, Direction yaw) {
        return translationCache.computeIfAbsent(pitch, k -> new LinkedHashMap<>())
                .computeIfAbsent(yaw, k -> translations[pitch.get3DDataValue()][yaw.get3DDataValue() - 2]);
    }

    public static Direction[] inverseTranslationFor(Direction pitch, Direction yaw) {
        return inverseTranslationCache.computeIfAbsent(pitch, k -> new LinkedHashMap<>())
                .computeIfAbsent(yaw, k -> {
                    Direction[] t = translationFor(pitch, yaw);
                    Direction[] result = new Direction[t.length];
                    for (int i = 0; i < t.length; i++) {
                        for (int j = 0; j < t.length; j++) {
                            if (t[j].get3DDataValue() == i) {
                                result[i] = Direction.from3DDataValue(j);
                                break;
                            }
                        }
                    }
                    return result;
                });
    }

    private static class D {
        static final Direction DOWN = Direction.DOWN;
        static final Direction UP = Direction.UP;
        static final Direction NORTH = Direction.NORTH;
        static final Direction SOUTH = Direction.SOUTH;
        static final Direction WEST = Direction.WEST;
        static final Direction EAST = Direction.EAST;
    }
}
