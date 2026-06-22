package li.cil.oc.core.impl.common.tileentity.traits;

import net.minecraft.core.Direction;

import java.util.Map;

public interface BundledRedstoneAware extends RedstoneAware {

    default int[][] bundledInput() {
        return new int[6][16];
    }

    default int[][] rednetInput() {
        return new int[6][16];
    }

    default int[][] bundledOutput() {
        return new int[6][16];
    }

    default int[] getBundledInput(Direction side) {
        int[][] input = bundledInput();
        int[][] rednet = rednetInput();
        int sideIndex = side.get3DDataValue();
        int[] result = new int[16];
        for (int color = 0; color < 16; color++) {
            result[color] = Math.max(Math.max(input[sideIndex][color], rednet[sideIndex][color]), 0);
        }
        return result;
    }

    default int getBundledInput(Direction side, int color) {
        int[][] input = bundledInput();
        int[][] rednet = rednetInput();
        return Math.max(Math.max(input[side.get3DDataValue()][color], rednet[side.get3DDataValue()][color]), 0);
    }

    default void setBundledInput(Direction side, int color, int value) {
        updateInput(bundledInput(), side, color, value);
    }

    default void setBundledInput(Direction side, int[] values) {
        for (int color = 0; color < 16; color++) {
            int newValue = (values == null || color >= values.length) ? 0 : values[color];
            setBundledInput(side, color, newValue);
        }
    }

    private void updateInput(int[][] inputs, Direction side, int color, int newValue) {
        int sideIndex = side.get3DDataValue();
        int oldValue = inputs[sideIndex][color];
        if (oldValue != newValue) {
            inputs[sideIndex][color] = newValue;
            if (oldValue != -1) {
                onRedstoneInputChanged(side.get3DDataValue(), oldValue, newValue, color);
            }
        }
    }

    default int[] getBundledOutput(Direction side) {
        return bundledOutput()[toLocal(side).get3DDataValue()];
    }

    default int getBundledOutput(Direction side, int color) {
        return bundledOutput()[toLocal(side).get3DDataValue()][color];
    }

    default int[][] getBundledOutput() {
        return bundledInput();
    }

    default void setBundledOutput(Direction side, int color, int value) {
        int[][] output = bundledOutput();
        int sideIndex = toLocal(side).get3DDataValue();
        if (output[sideIndex][color] != value) {
            output[sideIndex][color] = value;
            onRedstoneOutputChanged(side);
        }
    }

    default void setBundledOutput(Direction side, Map<?, ?> values) {
        int[][] output = bundledOutput();
        int sideIndex = toLocal(side).get3DDataValue();
        boolean changed = false;
        for (int color = 0; color < 16; color++) {
            Object obj = values.get(color);
            if (obj instanceof Number n) {
                int newValue = n.intValue();
                if (output[sideIndex][color] != newValue) {
                    output[sideIndex][color] = newValue;
                    changed = true;
                }
            }
        }
        if (changed) {
            onRedstoneOutputChanged(side);
        }
    }

    default void setBundledOutput(Map<?, ?> values) {
        boolean changed = false;
        for (Direction side : Direction.values()) {
            Object obj = values.get(side.get3DDataValue());
            if (obj instanceof Map<?, ?> child) {
                int[][] output = bundledOutput();
                int sideIndex = side.get3DDataValue();
                for (int color = 0; color < 16; color++) {
                    Object val = child.get(color);
                    if (val instanceof Number n) {
                        int newValue = n.intValue();
                        if (output[sideIndex][color] != newValue) {
                            output[sideIndex][color] = newValue;
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    onRedstoneOutputChanged(side);
                }
            }
        }
    }

}
