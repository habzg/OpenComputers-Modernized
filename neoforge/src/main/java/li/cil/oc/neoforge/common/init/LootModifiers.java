package li.cil.oc.neoforge.common.init;

import com.mojang.serialization.MapCodec;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.loot.OCLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class LootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, OpenComputers.ID);

    @SuppressWarnings("unused")
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<OCLootModifier>> OC_LOOT =
            GLM_CODECS.register("oc_loot", () -> OCLootModifier.CODEC);

    private LootModifiers() {
    }
}
