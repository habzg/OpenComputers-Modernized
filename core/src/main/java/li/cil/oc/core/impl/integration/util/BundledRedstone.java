package li.cil.oc.core.impl.integration.util;

import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class BundledRedstone {
    private static final List<RedstoneProvider> providers = new ArrayList<>();
    private static BooleanSupplier availableCheck = () -> false;

    private BundledRedstone() {
    }

    public static void setAvailableCheck(BooleanSupplier check) {
        availableCheck = check;
    }

    public static void addProvider(RedstoneProvider provider) {
        providers.add(provider);
    }

    public static boolean isAvailable() {
        return availableCheck.getAsBoolean() || !providers.isEmpty();
    }

    public static int computeInput(BlockPosition pos, Direction side) {
        var brPos = pos.offset(side).toBlockPos();
        if (pos.level().hasChunk(brPos.getX() >> 4, brPos.getZ() >> 4)) {
            int max = 0;
            for (RedstoneProvider provider : providers) {
                max = Math.max(max, provider.computeInput(pos, side));
            }
            return max;
        }
        return 0;
    }

    public static int[] computeBundledInput(BlockPosition pos, Direction side) {
        var bundPos = pos.offset(side).toBlockPos();
        if (pos.level().hasChunk(bundPos.getX() >> 4, bundPos.getZ() >> 4)) {
            int[] result = null;
            for (RedstoneProvider provider : providers) {
                int[] input = provider.computeBundledInput(pos, side);
                if (input != null) {
                    if (result == null) {
                        result = input.clone();
                    } else {
                        for (int i = 0; i < Math.min(result.length, input.length); i++) {
                            result[i] = Math.max(result[i], input[i]);
                        }
                    }
                }
            }
            return result;
        }
        return null;
    }

    public interface RedstoneProvider {
        int computeInput(BlockPosition pos, Direction side);

        int[] computeBundledInput(BlockPosition pos, Direction side);
    }
}
