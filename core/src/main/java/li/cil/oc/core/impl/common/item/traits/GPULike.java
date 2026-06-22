package li.cil.oc.core.impl.common.item.traits;

import li.cil.oc.core.impl.Settings;

import java.util.ArrayList;
import java.util.List;

public interface GPULike {
    int gpuTier();

    default List<Object> gpuTooltipData() {
        int tier = gpuTier();
        int w = Settings.screenResolutionsByTier[tier][0];
        int h = Settings.screenResolutionsByTier[tier][1];
        int depth = li.cil.oc.core.impl.util.PackedColor.Depth.bits(Settings.screenDepthsByTier[tier]);
        List<Object> data = new ArrayList<>();
        data.add(w);
        data.add(h);
        data.add(depth);
        data.add(tier == 0 ? "1/1/4/2/2" : tier == 1 ? "2/4/8/4/4" : "4/8/16/8/8");
        return data;
    }
}
