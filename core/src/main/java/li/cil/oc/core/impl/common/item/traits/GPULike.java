package li.cil.oc.core.impl.common.item.traits;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.impl.OCSettings;

public interface GPULike {
    int gpuTier();

    default List<Object> gpuTooltipData() {
        int tier = gpuTier();
        int w = OCSettings.screenResolutionsByTier[tier][0];
        int h = OCSettings.screenResolutionsByTier[tier][1];
        int depth = li.cil.oc.core.impl.util.PackedColor.Depth.bits(OCSettings.screenDepthsByTier[tier]);
        List<Object> data = new ArrayList<>();
        data.add(w);
        data.add(h);
        data.add(depth);
        data.add(tier == 0 ? "1/1/4/2/2" : tier == 1 ? "2/4/8/4/4" : "4/8/16/8/8");
        return data;
    }
}
