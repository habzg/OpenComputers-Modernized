package li.cil.oc.core.impl.common.template;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.MicrocontrollerData;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MicrocontrollerTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrocontrollerTemplate.class);
    private static final Class<? extends EnvironmentHost> hostClass = li.cil.oc.api.internal.Microcontroller.class;

    private MicrocontrollerTemplate() {
    }

    @SuppressWarnings("unused")
    public static boolean selectTier1(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseTier1);
    }

    @SuppressWarnings("unused")
    public static boolean selectTier2(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseTier2);
    }

    @SuppressWarnings("unused")
    public static boolean selectTierCreative(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseCreative);
    }

    @SuppressWarnings("unused")
    public static Object[] validate(Container inventory) {
        return Template.validateComputer(inventory, hostClass, MicrocontrollerTemplate::maxComplexity);
    }

    @SuppressWarnings("unused")
    public static int maxComplexity(Container inventory) {
        int tier = caseTier(inventory);
        if (tier == Tier.Two) return 5;
        if (tier == Tier.Four) return 9001;
        return 4;
    }

    @SuppressWarnings("unused")
    public static Object[] assemble(Container inventory) {
        var data = new MicrocontrollerData();
        data.tier = caseTier(inventory);
        java.util.List<ItemStack> components = new java.util.ArrayList<>();
        for (int i = 1; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty()) components.add(stack);
        }
        data.components = components;
        data.storedEnergy = (int) OCSettings.get().bufferMicrocontroller;
        var stack = data.createItemStack();
        double energy = OCSettings.get().microcontrollerBaseCost + Template.complexity(inventory, hostClass) * OCSettings.get().microcontrollerComplexityCost;
        return new Object[]{stack, energy};
    }

    @SuppressWarnings("unused")
    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName.Microcontroller);
    }

    @SuppressWarnings("unused")
    public static Object[] disassemble(ItemStack stack, ItemStack[] ignoredIngredients) {
        var info = new MicrocontrollerData(stack);
        var itemName = Constants.ItemName.MicrocontrollerCase(info.tier);
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        var caseStackHolder = li.cil.oc.api.Items.get(itemName);
        if (caseStackHolder != null) {
            result.add(caseStackHolder.createItemStack(1));
        } else {
            LOGGER.warn("Unknown microcontroller case item: {} (tier={})", itemName, info.tier);
        }
        result.addAll(info.components);
        return result.toArray();
    }

    public static void register() {
        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Microcontroller (Tier 1)",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.selectTier1",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.validate",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.assemble",
                hostClass, null,
                new int[]{Tier.Two},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.One),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.One),
                        null,
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.One),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.One),
                        null,
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any)
                ));

        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Microcontroller (Tier 2)",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.selectTier2",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.validate",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.assemble",
                hostClass, null,
                new int[]{Tier.Three},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.One),
                        null,
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.One),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.One),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.One),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any)
                ));

        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Microcontroller (Creative)",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.selectTierCreative",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.validate",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.assemble",
                hostClass, null,
                new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any)
                ));

        li.cil.oc.core.impl.common.Registrar.registerDisassemblerTemplate("Microcontroller",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.selectDisassembler",
                "li.cil.oc.core.impl.common.template.MicrocontrollerTemplate.disassemble");
    }

    public static int caseTier(Container inventory) {
        return li.cil.oc.core.impl.util.ItemUtils.caseTier(inventory.getItem(0));
    }
}
