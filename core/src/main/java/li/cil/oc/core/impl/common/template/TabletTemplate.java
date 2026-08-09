package li.cil.oc.core.impl.common.template;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.TabletData;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TabletTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(TabletTemplate.class);
    private static final Class<? extends EnvironmentHost> hostClass = li.cil.oc.api.internal.Tablet.class;

    private TabletTemplate() {
    }

    @SuppressWarnings("unused")

    public static boolean selectTier1(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseTier1);
    }

    @SuppressWarnings("unused")

    public static boolean selectTier2(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseTier2);
    }

    @SuppressWarnings("unused")

    public static boolean selectCreative(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseCreative);
    }

    @SuppressWarnings("unused")

    public static Object[] validate(Container inventory) {
        return Template.validateComputer(inventory, hostClass, TabletTemplate::maxComplexity);
    }

    @SuppressWarnings("unused")
    public static int maxComplexity(Container inventory) {
        return Template.maxComplexity(inventory, hostClass) / 2 + 5;
    }

    @SuppressWarnings("unused")
    public static Object[] assemble(Container inventory) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 1; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty()) items.add(stack);
        }
        var data = new TabletData();
        data.tier = li.cil.oc.core.impl.util.ItemUtils.caseTier(inventory.getItem(0));
        data.container = items.isEmpty() ? null : items.getFirst();
        java.util.List<ItemStack> itemList = new java.util.ArrayList<>();
        itemList.add(li.cil.oc.api.Items.get(Constants.BlockName.ScreenTier1).createItemStack(1));
        for (int i = (data.tier == Tier.One ? 0 : 1); i < items.size(); i++) {
            itemList.add(items.get(i));
        }
        data.items = itemList;
        data.energy = OCSettings.get().bufferTablet;
        data.maxEnergy = data.energy;
        var stack = li.cil.oc.api.Items.get(Constants.ItemName.Tablet).createItemStack(1);
        data.save(stack);
        double energy = OCSettings.get().tabletBaseCost + Template.complexity(inventory, hostClass) * OCSettings.get().tabletComplexityCost;
        return new Object[]{stack, energy};
    }

    @SuppressWarnings("unused")

    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.Tablet);
    }

    @SuppressWarnings("unused")

    public static Object[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        var info = new TabletData(stack);
        var itemName = Constants.ItemName.TabletCase(info.tier);
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        var caseStackHolder = li.cil.oc.api.Items.get(itemName);
        if (caseStackHolder != null) {
            result.add(caseStackHolder.createItemStack(1));
        } else {
            LOGGER.warn("Unknown tablet case item: {} (tier={})", itemName, info.tier);
        }
        if (info.container != null) result.add(info.container);
        boolean first = true;
        for (var item : info.items) {
            if (first) {
                first = false;
                continue;
            }
            if (item != null) result.add(item);
        }
        result.removeIf(s -> s == null || s.isEmpty());
        return result.toArray();
    }

    public static void register() {
        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Tablet (Tier 1)",
                "li.cil.oc.core.impl.common.template.TabletTemplate.selectTier1",
                "li.cil.oc.core.impl.common.template.TabletTemplate.validate",
                "li.cil.oc.core.impl.common.template.TabletTemplate.assemble",
                hostClass, null,
                new int[]{Tier.Three, Tier.Two, Tier.One},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Two), null,
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.Two)
                ));

        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Tablet (Tier 2)",
                "li.cil.oc.core.impl.common.template.TabletTemplate.selectTier2",
                "li.cil.oc.core.impl.common.template.TabletTemplate.validate",
                "li.cil.oc.core.impl.common.template.TabletTemplate.assemble",
                hostClass, new int[]{Tier.Two},
                new int[]{Tier.Three, Tier.Two, Tier.Two},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Two), null,
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.Two)
                ));

        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Tablet (Creative)",
                "li.cil.oc.core.impl.common.template.TabletTemplate.selectCreative",
                "li.cil.oc.core.impl.common.template.TabletTemplate.validate",
                "li.cil.oc.core.impl.common.template.TabletTemplate.assemble",
                hostClass, new int[]{Tier.Three},
                new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.Three)
                ));

        li.cil.oc.core.impl.common.Registrar.registerDisassemblerTemplate("Tablet",
                "li.cil.oc.core.impl.common.template.TabletTemplate.selectDisassembler",
                "li.cil.oc.core.impl.common.template.TabletTemplate.disassemble");
    }
}
