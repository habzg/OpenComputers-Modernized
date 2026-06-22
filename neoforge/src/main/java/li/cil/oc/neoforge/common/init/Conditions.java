package li.cil.oc.neoforge.common.init;

import com.mojang.serialization.MapCodec;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.recipe.LootRecraftingCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Conditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, OpenComputers.ID);

    @SuppressWarnings("unused")
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<LootRecraftingCondition>> LOOT_RECRAFTING =
            CONDITION_SERIALIZERS.register("loot_recrafting", () -> LootRecraftingCondition.CODEC);

    private Conditions() {
    }
}
