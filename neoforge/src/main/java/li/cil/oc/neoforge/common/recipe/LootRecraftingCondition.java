package li.cil.oc.neoforge.common.recipe;

import com.mojang.serialization.MapCodec;
import li.cil.oc.core.impl.Settings;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.NotNull;

public final class LootRecraftingCondition implements ICondition {
    public static final LootRecraftingCondition INSTANCE = new LootRecraftingCondition();

    public static final MapCodec<LootRecraftingCondition> CODEC = MapCodec.unit(INSTANCE);

    private LootRecraftingCondition() {
    }

    @Override
    public boolean test(@NotNull IContext context) {
        return Settings.get().lootRecrafting;
    }

    @Override
    public @NotNull MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "opencomputers:loot_recrafting";
    }
}
