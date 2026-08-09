package li.cil.oc.fabric.common.recipe;

import com.mojang.serialization.MapCodec;
import li.cil.oc.core.impl.OCSettings;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class LootRecraftingCondition implements ResourceCondition {
    public static final LootRecraftingCondition INSTANCE = new LootRecraftingCondition();

    public static final ResourceConditionType<LootRecraftingCondition> TYPE =
            ResourceConditionType.create(
                    ResourceLocation.fromNamespaceAndPath("opencomputers", "loot_recrafting"),
                    MapCodec.unit(INSTANCE)
            );

    private LootRecraftingCondition() {
    }

    public static void init() {
        ResourceConditions.register(TYPE);
    }

    @Override
    public boolean test(net.minecraft.core.HolderLookup.Provider registryLookup) {
        var settings = OCSettings.get();
        return settings == null || settings.lootRecrafting;
    }

    @Override
    public @NotNull ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "opencomputers:loot_recrafting";
    }
}
