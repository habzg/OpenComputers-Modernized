package li.cil.oc.core.impl.common.template;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class Template {
    private Template() {
    }

    public static Object[] validateComputer(Container inventory, Class<? extends EnvironmentHost> hostClass) {
        return validateComputer(inventory, hostClass, null);
    }

    public static Object[] validateComputer(Container inventory, Class<? extends EnvironmentHost> hostClass, ToIntFunction<Container> maxComplexityOverride) {
        boolean hasCase = caseTier(inventory) != Tier.None;
        boolean hasCPU = hasCPU(inventory, hostClass);
        boolean hasRAM = hasRAM(inventory, hostClass);
        boolean requiresRAM = requiresRAM(inventory, hostClass);
        int comp = complexity(inventory, hostClass);
        int maxComp = maxComplexityOverride != null ? maxComplexityOverride.applyAsInt(inventory) : maxComplexity(inventory, hostClass);
        boolean valid = hasCase && hasCPU && (hasRAM || !requiresRAM) && comp <= maxComp;

        Component progress;
        if (!hasCPU) progress = Component.translatable("gui.opencomputers.assembler.insertcpu");
        else if (!hasRAM && requiresRAM) progress = Component.translatable("gui.opencomputers.assembler.insertram");
        else {
            MutableComponent msg = Component.translatable("gui.opencomputers.assembler.complexity", String.valueOf(comp), String.valueOf(maxComp));
            progress = comp > maxComp ? Component.literal(net.minecraft.ChatFormatting.DARK_RED.toString()).append(msg) : msg;
        }

        List<Component> warnings = new ArrayList<>();
        if (!hasComponent(Constants.ItemName.EEPROM, inventory)) {
            warnings.add(Component.literal("- ").append(Component.translatable("gui.opencomputers.assembler.warning.bios")));
        }
        if (!hasComponent(Constants.BlockName.ScreenTier1, inventory)) {
            warnings.add(Component.literal("- ").append(Component.translatable("gui.opencomputers.assembler.warning.screen")));
        }
        if (!hasComponent(Constants.BlockName.Keyboard, inventory)) {
            warnings.add(Component.literal("- ").append(Component.translatable("gui.opencomputers.assembler.warning.keyboard")));
        }
        boolean hasGraphics = hasComponent(Constants.ItemName.APUCreative, inventory)
                || hasComponent(Constants.ItemName.APUTier1, inventory)
                || hasComponent(Constants.ItemName.APUTier2, inventory)
                || hasComponent(Constants.ItemName.GraphicsCardTier1, inventory)
                || hasComponent(Constants.ItemName.GraphicsCardTier2, inventory)
                || hasComponent(Constants.ItemName.GraphicsCardTier3, inventory);
        if (!hasGraphics) {
            warnings.add(Component.literal("- ").append(Component.translatable("gui.opencomputers.assembler.warning.graphicscard")));
        }
        if (!hasInventory(inventory, hostClass)) {
            warnings.add(Component.literal("- ").append(Component.translatable("gui.opencomputers.assembler.warning.inventory")));
        }
        if (!hasFileSystem(inventory, hostClass)) {
            warnings.add(Component.literal("- ").append(Component.translatable("gui.opencomputers.assembler.warning.os")));
        }
        if (!warnings.isEmpty()) {
            warnings.addFirst(Component.translatable("gui.opencomputers.assembler.warnings"));
        }

        return new Object[]{valid, progress, warnings.toArray(new Component[0])};
    }

    public static boolean exists(Container inventory, java.util.function.Predicate<ItemStack> p) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!stack.isEmpty() && p.test(stack)) return true;
        }
        return false;
    }

    public static boolean hasCPU(Container inventory, Class<? extends EnvironmentHost> hostClass) {
        return exists(inventory, stack -> li.cil.oc.api.API.driver.driverFor(stack, hostClass) instanceof li.cil.oc.api.driver.item.Processor);
    }

    public static boolean hasRAM(Container inventory, Class<? extends EnvironmentHost> hostClass) {
        return exists(inventory, stack -> li.cil.oc.api.API.driver.driverFor(stack, hostClass) instanceof li.cil.oc.api.driver.item.Memory);
    }

    public static boolean requiresRAM(Container inventory, Class<? extends EnvironmentHost> hostClass) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            var driver = li.cil.oc.api.API.driver.driverFor(stack, hostClass);
            if (driver instanceof li.cil.oc.api.driver.item.Processor processor) {
                var architecture = processor.architecture(stack);
                if (architecture != null && architecture.getAnnotation(li.cil.oc.api.machine.Architecture.NoMemoryRequirements.class) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean hasComponent(String name, Container inventory) {
        return exists(inventory, stack -> {
            var descriptor = li.cil.oc.api.Items.get(stack);
            return descriptor != null && descriptor.name().equals(name);
        });
    }

    public static boolean hasInventory(Container inventory, Class<? extends EnvironmentHost> hostClass) {
        return exists(inventory, stack -> li.cil.oc.api.API.driver.driverFor(stack, hostClass) instanceof li.cil.oc.api.driver.item.Inventory);
    }

    public static boolean hasFileSystem(Container inventory, Class<? extends EnvironmentHost> hostClass) {
        return exists(inventory, stack -> {
            var driver = li.cil.oc.api.API.driver.driverFor(stack, hostClass);
            if (driver != null) {
                var slot = driver.slot(stack);
                return Slot.Floppy.equals(slot) || Slot.HDD.equals(slot);
            }
            return false;
        });
    }

    public static int complexity(Container inventory, Class<? extends EnvironmentHost> hostClass) {
        int acc = 0;
        for (int slot = 1; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                var driver = li.cil.oc.api.API.driver.driverFor(stack, hostClass);
                //noinspection StatementWithEmptyBody
                if (driver instanceof li.cil.oc.api.driver.item.Processor) {
                    // ignored
                } else if (driver instanceof li.cil.oc.api.driver.item.Container container) {
                    acc += (1 + container.tier(stack)) * 2;
                } else if (driver != null && !Slot.EEPROM.equals(driver.slot(stack))) {
                    acc += 1 + driver.tier(stack);
                }
            }
        }
        return acc;
    }

    public static int maxComplexity(Container inventory, Class<? extends EnvironmentHost> hostClass) {
        int caseTier = caseTier(inventory);
        int cpuTier = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            var driver = li.cil.oc.api.API.driver.driverFor(stack, hostClass);
            if (driver instanceof li.cil.oc.api.driver.item.Processor processor) {
                cpuTier += processor.tier(stack);
            }
        }
        if (caseTier >= Tier.One && cpuTier >= Tier.One) {
            return Settings.deviceComplexityByTier[caseTier] - (Math.min(2, caseTier) - cpuTier) * 6;
        }
        return 0;
    }

    public static int caseTier(Container inventory) {
        return li.cil.oc.core.impl.util.ItemUtils.caseTier(inventory.getItem(0));
    }
}
