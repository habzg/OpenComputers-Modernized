package li.cil.oc.fabric.common.loot;

import java.util.Set;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.LootManager;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class OCLootModifier {
    private static final Set<ResourceLocation> INJECT_TABLES = Set.of(
            ResourceLocation.withDefaultNamespace("chests/simple_dungeon"),
            ResourceLocation.withDefaultNamespace("chests/desert_pyramid"),
            ResourceLocation.withDefaultNamespace("chests/jungle_temple"),
            ResourceLocation.withDefaultNamespace("chests/stronghold_library")
    );

    private OCLootModifier() {
    }

    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (source.isBuiltin() && INJECT_TABLES.contains(key.location()) && !LootManager.disksForSampling.isEmpty()) {
                var poolBuilder = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0.0f, OCSettings.get().lootProbability / 100.0f));
                for (var disk : LootManager.disksForSampling) {
                    poolBuilder.add(LootItem.lootTableItem(disk.getItem())
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                            .setWeight(1));
                }
                tableBuilder.pool(poolBuilder.build());
            }
        });
    }
}
