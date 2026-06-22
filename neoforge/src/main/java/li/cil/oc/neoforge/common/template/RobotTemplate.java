package li.cil.oc.neoforge.common.template;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.template.Template;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

public final class RobotTemplate {
    private static final Class<? extends EnvironmentHost> hostClass = li.cil.oc.api.internal.Robot.class;

    private RobotTemplate() {
    }

    @SuppressWarnings("unused")

    public static boolean selectTier1(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier1);
    }

    @SuppressWarnings("unused")

    public static boolean selectTier2(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier2);
    }

    @SuppressWarnings("unused")

    public static boolean selectTier3(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier3);
    }

    @SuppressWarnings("unused")

    public static boolean selectCreative(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName.CaseCreative);
    }

    @SuppressWarnings("unused")

    public static Object[] validate(Container inventory) {
        return Template.validateComputer(inventory, hostClass);
    }

    @SuppressWarnings("unused")

    public static Object[] assemble(Container inventory) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 1; i < inventory.getContainerSize(); i++) {
            items.add(inventory.getItem(i));
        }
        var data = new RobotData();
        data.tier = caseTier(inventory);
        data.name = RobotData.randomName();
        data.robotEnergy = (int) Settings.get().bufferRobot;
        data.totalEnergy = data.robotEnergy;
        data.containers = items.subList(0, Math.min(3, items.size())).stream().filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
        data.components = items.subList(Math.min(3, items.size()), items.size()).stream().filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
        var stack = data.createItemStack();
        double energy = Settings.get().robotBaseCost + Template.complexity(inventory, hostClass) * Settings.get().robotComplexityCost;
        return new Object[]{stack, energy};
    }

    @SuppressWarnings("unused")

    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName.Robot);
    }

    @SuppressWarnings("unused")

    public static Object[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        var info = new RobotData(stack);
        var itemName = Constants.BlockName.Case(info.tier);
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        result.add(li.cil.oc.api.Items.get(itemName).createItemStack(1));
        result.addAll(info.containers);
        result.addAll(info.components);
        return result.toArray();
    }

    public static void register() {
        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Robot (Tier 1)",
                "li.cil.oc.neoforge.common.template.RobotTemplate.selectTier1",
                "li.cil.oc.neoforge.common.template.RobotTemplate.validate",
                "li.cil.oc.neoforge.common.template.RobotTemplate.assemble",
                hostClass,
                new int[]{Tier.Two, Tier.One, Tier.One},
                new int[]{Tier.One, Tier.One, Tier.One},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.One), null, null,
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.One),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.One),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.One),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.One)
                ));

        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Robot (Tier 2)",
                "li.cil.oc.neoforge.common.template.RobotTemplate.selectTier2",
                "li.cil.oc.neoforge.common.template.RobotTemplate.validate",
                "li.cil.oc.neoforge.common.template.RobotTemplate.assemble",
                hostClass,
                new int[]{Tier.Three, Tier.Two, Tier.One},
                new int[]{Tier.Two, Tier.Two, Tier.Two, Tier.One, Tier.One, Tier.One},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.One), null,
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.Two)
                ));

        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Robot (Tier 3)",
                "li.cil.oc.neoforge.common.template.RobotTemplate.selectTier3",
                "li.cil.oc.neoforge.common.template.RobotTemplate.validate",
                "li.cil.oc.neoforge.common.template.RobotTemplate.assemble",
                hostClass,
                new int[]{Tier.Three, Tier.Two, Tier.Two},
                new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Two, Tier.Two, Tier.Two, Tier.One, Tier.One, Tier.One},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Two),
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.Two)
                ));

        li.cil.oc.core.impl.common.Registrar.registerAssemblerTemplate("Robot (Creative)",
                "li.cil.oc.neoforge.common.template.RobotTemplate.selectCreative",
                "li.cil.oc.neoforge.common.template.RobotTemplate.validate",
                "li.cil.oc.neoforge.common.template.RobotTemplate.assemble",
                hostClass,
                new int[]{Tier.Three, Tier.Three, Tier.Three},
                new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three, Tier.Three},
                java.util.Arrays.asList(
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Card, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.CPU, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.Memory, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.EEPROM, Tier.Any),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.Three),
                        Pair.of(li.cil.oc.core.common.Slot.HDD, Tier.Three)
                ));

        li.cil.oc.core.impl.common.Registrar.registerDisassemblerTemplate("Robot",
                "li.cil.oc.neoforge.common.template.RobotTemplate.selectDisassembler",
                "li.cil.oc.neoforge.common.template.RobotTemplate.disassemble");
    }

    public static int caseTier(Container inventory) {
        return li.cil.oc.core.impl.util.ItemUtils.caseTier(inventory.getItem(0));
    }
}
