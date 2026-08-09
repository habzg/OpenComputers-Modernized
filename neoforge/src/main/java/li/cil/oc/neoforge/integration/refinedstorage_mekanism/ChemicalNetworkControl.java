package li.cil.oc.neoforge.integration.refinedstorage_mekanism;

import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.mekanism.ChemicalResource;
import java.util.ArrayList;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.neoforge.integration.refinedstorage2.RS2Util;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ChemicalNetworkControl {
    BlockEntity tile();

    @Callback(doc = "function():table -- Get a list of the stored chemicals in the network.")
    default Object[] getChemicalsInNetwork(Context context, Arguments args) {
        var result = new ArrayList<>();
        var network = RS2Util.networkOf(tile());
        if (network == null) return ResultWrapper.result((Object) result.toArray());
        var storage = network.getComponent(StorageNetworkComponent.class);
        for (var resourceAmount : storage.getAll()) {
            if (resourceAmount.resource() instanceof ChemicalResource(mekanism.api.chemical.Chemical chemical)) {
                result.add(new ChemicalStack(
                        MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical),
                        resourceAmount.amount()));
            }
        }
        return ResultWrapper.result((Object) result.toArray());
    }
}
