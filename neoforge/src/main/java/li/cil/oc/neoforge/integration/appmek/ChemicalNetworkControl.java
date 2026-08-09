package li.cil.oc.neoforge.integration.appmek;

import appeng.api.networking.security.IActionHost;
import java.util.ArrayList;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.neoforge.integration.appeng.AEUtil;
import me.ramidzkh.mekae2.ae2.MekanismKey;

public interface ChemicalNetworkControl {
    IActionHost tile();

    @Callback(doc = "function():table -- Get a list of the stored chemicals in the network.")
    default Object[] getChemicalsInNetwork(Context context, Arguments args) {
        var node = tile().getActionableNode();
        if (node == null) return ResultWrapper.result((Object) new Object[0]);
        var grid = node.getGrid();
        if (grid == null) return ResultWrapper.result((Object) new Object[0]);
        var storage = AEUtil.getGridStorage(grid);
        var all = new appeng.api.stacks.KeyCounter();
        storage.getAvailableStacks(all);
        var result = new ArrayList<>();
        for (var entry : all) {
            var key = entry.getKey();
            if (key instanceof MekanismKey chemicalKey) {
                result.add(chemicalKey.getStack().copyWithAmount(entry.getLongValue()));
            }
        }
        return ResultWrapper.result((Object) result.toArray());
    }
}
