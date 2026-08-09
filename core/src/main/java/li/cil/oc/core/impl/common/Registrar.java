package li.cil.oc.core.impl.common;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.common.template.AssemblerTemplates;
import li.cil.oc.core.impl.common.template.DisassemblerTemplates;
import li.cil.oc.core.impl.integration.util.ItemCharge;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.server.driver.Registry;
import li.cil.oc.core.server.machine.ProgramLocations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Registrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(Registrar.class);
    private static boolean locked = false;

    private Registrar() {
    }

    public static void lock() {
        locked = true;
    }

    private static void checkLocked() {
        if (locked) throw new IllegalStateException("Registration is locked: OC initialization has completed");
    }

    public static void registerAssemblerFilter(final String callback) {
        checkLocked();
        AssemblerTemplates.addFilter(callback);
    }

    public static void registerAssemblerTemplate(
            final String name,
            final String select,
            final String validate,
            final String assemble,
            final Class<?> host,
            final int[] containerTiers,
            final int[] upgradeTiers,
            final Iterable<Pair<String, Integer>> componentSlots) {
        checkLocked();
        final CompoundTag nbt = new CompoundTag();
        if (name != null) {
            nbt.putString("name", name);
        }
        nbt.putString("select", select);
        nbt.putString("validate", validate);
        nbt.putString("assemble", assemble);
        if (host != null) {
            nbt.putString("hostClass", host.getName());
        }

        final ListTag containersNbt = new ListTag();
        if (containerTiers != null) {
            for (int tier : containerTiers) {
                final CompoundTag slotNbt = new CompoundTag();
                slotNbt.putInt("tier", tier);
                containersNbt.add(slotNbt);
            }
        }
        if (!containersNbt.isEmpty()) {
            nbt.put("containerSlots", containersNbt);
        }

        final ListTag upgradesNbt = new ListTag();
        if (upgradeTiers != null) {
            for (int tier : upgradeTiers) {
                final CompoundTag slotNbt = new CompoundTag();
                slotNbt.putInt("tier", tier);
                upgradesNbt.add(slotNbt);
            }
        }
        if (!upgradesNbt.isEmpty()) {
            nbt.put("upgradeSlots", upgradesNbt);
        }

        final ListTag componentsNbt = new ListTag();
        if (componentSlots != null) {
            for (Pair<String, Integer> slot : componentSlots) {
                if (slot == null) {
                    componentsNbt.add(new CompoundTag());
                } else {
                    final CompoundTag slotNbt = new CompoundTag();
                    slotNbt.putString("type", slot.getLeft());
                    slotNbt.putInt("tier", slot.getRight());
                    componentsNbt.add(slotNbt);
                }
            }
        }
        if (!componentsNbt.isEmpty()) {
            nbt.put("componentSlots", componentsNbt);
        }

        AssemblerTemplates.add(nbt);
    }

    public static void registerDisassemblerTemplate(final String name, final String select, final String disassemble) {
        checkLocked();
        final CompoundTag nbt = new CompoundTag();
        if (name != null) {
            nbt.putString("name", name);
        }
        nbt.putString("select", select);
        nbt.putString("disassemble", disassemble);
        DisassemblerTemplates.add(nbt);
    }

    @SuppressWarnings("unused")
    public static void registerToolDurabilityProvider(final String callback) {
        checkLocked();
        ToolDurabilityProviders.add(ReflectionUtil.getStaticMethod(callback, net.minecraft.world.item.ItemStack.class));
    }

    public static void registerWrenchTool(final String callback) {
        checkLocked();
        Wrench.addUsage(ReflectionUtil.getStaticMethod(callback, net.minecraft.world.entity.player.Player.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE));
    }

    public static void registerWrenchToolCheck(final String callback) {
        checkLocked();
        Wrench.addCheck(ReflectionUtil.getStaticMethod(callback, net.minecraft.world.item.ItemStack.class));
    }

    public static void registerItemCharge(final String ignoredName, final String canCharge, final String charge) {
        checkLocked();
        ItemCharge.add(
                ReflectionUtil.getStaticMethod(canCharge, net.minecraft.world.item.ItemStack.class),
                ReflectionUtil.getStaticMethod(charge, net.minecraft.world.item.ItemStack.class, Double.TYPE, Boolean.TYPE)
        );
    }

    public static void registerInkProvider(final String callback) {
        checkLocked();
        PrintData.addInkProvider(ReflectionUtil.getStaticMethod(callback, net.minecraft.world.item.ItemStack.class));
    }

    @SuppressWarnings("unused")
    public static void blacklistPeripheral(final Class<?> peripheral) {
        checkLocked();
        OCSettings.get().peripheralBlacklist.add(peripheral.getName());
    }

    public static void blacklistHost(final String ignoredName, final Class<?> host, final ItemStack stack) {
        checkLocked();
        Registry.INSTANCE.blacklistHost(stack, host);
    }

    @SuppressWarnings("unused")
    public static void registerCustomPowerSystem() {
        checkLocked();
        LOGGER.debug("Custom power system registered.");
    }

    public static void registerProgramDiskLabel(
            final String programName, final String diskLabel, final String... architectures) {
        checkLocked();
        ProgramLocations.addMapping(programName, diskLabel, architectures);
    }
}
