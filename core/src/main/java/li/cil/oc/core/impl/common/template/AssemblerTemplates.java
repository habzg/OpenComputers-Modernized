package li.cil.oc.core.impl.common.template;

import com.google.common.base.Strings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.common.ReflectionUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class AssemblerTemplates {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssemblerTemplates.class);
    public static final Slot NoSlot = new Slot(li.cil.oc.core.common.Slot.None, li.cil.oc.core.common.Tier.None, null, null);

    private static final ArrayList<Template> templates = new ArrayList<>();
    private static final ArrayList<Method> templateFilters = new ArrayList<>();

    private AssemblerTemplates() {
    }

    public static void add(CompoundTag template) {
        var selector = ReflectionUtil.getStaticMethod(template.getString("select"), ItemStack.class);
        var validator = ReflectionUtil.getStaticMethod(template.getString("validate"), Container.class);
        var assembler = ReflectionUtil.getStaticMethod(template.getString("assemble"), Container.class);
        var hostClass = tryGetHostClass(template.getString("hostClass"));

        var containerSlots = new ArrayList<Slot>();
        if (template.contains("containerSlots")) {
            int idx = 0;
            for (var tag : parseSlots(template.getList("containerSlots", Tag.TAG_COMPOUND))) {
                if (idx >= 3) break;
                containerSlots.add(parseSlot(tag, li.cil.oc.core.common.Slot.Container, hostClass));
                idx++;
            }
        }
        while (containerSlots.size() < 3) containerSlots.add(NoSlot);

        var upgradeSlots = new ArrayList<Slot>();
        if (template.contains("upgradeSlots")) {
            int idx = 0;
            for (var tag : parseSlots(template.getList("upgradeSlots", Tag.TAG_COMPOUND))) {
                if (idx >= 9) break;
                upgradeSlots.add(parseSlot(tag, li.cil.oc.core.common.Slot.Upgrade, hostClass));
                idx++;
            }
        }
        while (upgradeSlots.size() < 9) upgradeSlots.add(NoSlot);

        var componentSlots = new ArrayList<Slot>();
        if (template.contains("componentSlots")) {
            int idx = 0;
            for (var tag : parseSlots(template.getList("componentSlots", Tag.TAG_COMPOUND))) {
                if (idx >= 9) break;
                componentSlots.add(parseSlot(tag, null, hostClass));
                idx++;
            }
        }
        while (componentSlots.size() < 9) componentSlots.add(NoSlot);

        templates.add(new Template(selector, validator, assembler,
                containerSlots.toArray(new Slot[0]),
                upgradeSlots.toArray(new Slot[0]),
                componentSlots.toArray(new Slot[0])));
    }

    private static List<CompoundTag> parseSlots(net.minecraft.nbt.ListTag list) {
        List<CompoundTag> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getCompound(i));
        }
        return result;
    }

    public static void addFilter(String method) {
        templateFilters.add(ReflectionUtil.getStaticMethod(method, ItemStack.class));
    }

    public static Template select(ItemStack stack) {
        if (stack != null) {
            for (var filter : templateFilters) {
                if (!(Boolean) ReflectionUtil.tryInvokeStatic(filter, true, stack)) return null;
            }
            for (var t : templates) {
                if (t.select(stack)) return t;
            }
        }
        return null;
    }

    private static Slot parseSlot(CompoundTag nbt, String kindOverride, Class<? extends EnvironmentHost> hostClass) {
        var kind = kindOverride != null ? kindOverride : (nbt.contains("type") ? nbt.getString("type") : li.cil.oc.core.common.Slot.None);
        var tier = nbt.contains("tier") ? nbt.getInt("tier") : li.cil.oc.core.common.Tier.Any;
        Method validator = null;
        if (nbt.contains("validate")) {
            validator = ReflectionUtil.getStaticMethod(nbt.getString("validate"), Container.class, int.class, int.class, ItemStack.class);
        }
        return new Slot(kind, tier, validator, hostClass);
    }

    private static Class<? extends EnvironmentHost> tryGetHostClass(String name) {
        if (Strings.isNullOrEmpty(name)) return null;
        try {
            return Class.forName(name).asSubclass(EnvironmentHost.class);
        } catch (Exception e) {
            return null;
        }
    }

    public record Template(Method selector, Method validator, Method assembler, Slot[] containerSlots,
                           Slot[] upgradeSlots, Slot[] componentSlots) {

        public boolean select(ItemStack stack) {
            return (Boolean) ReflectionUtil.tryInvokeStatic(selector, false, stack);
        }

        public ValidationResult validate(Container inventory) {
            var result = ReflectionUtil.tryInvokeStatic(validator, null, inventory);
            if (result instanceof Object[] arr) {
                if (arr.length >= 3 && arr[0] instanceof Boolean valid && arr[1] instanceof Component progress && arr[2] instanceof Component[] warnings) {
                    return new ValidationResult(valid, progress, warnings);
                }
                if (arr.length >= 2 && arr[0] instanceof Boolean valid && arr[1] instanceof Component progress) {
                    return new ValidationResult(valid, progress, new Component[0]);
                }
                if (arr.length >= 1 && arr[0] instanceof Boolean valid) {
                    return new ValidationResult(valid, null, new Component[0]);
                }
            }
            return new ValidationResult(false, null, new Component[0]);
        }

        public Object[] assemble(Container inventory) {
            var result = ReflectionUtil.tryInvokeStatic(assembler, null, inventory);
            if (result instanceof Object[] arr) {
                if (arr.length >= 2 && arr[0] instanceof ItemStack stack && arr[1] instanceof Number energy) {
                    return new Object[]{stack, energy.doubleValue()};
                }
                if (arr.length >= 1 && arr[0] instanceof ItemStack stack) {
                    return new Object[]{stack, 0.0};
                }
            }
            return new Object[]{null, 0.0};
        }
    }

    public record ValidationResult(boolean valid, Component value, Component[] warnings) {
    }

    public record Slot(String kind, int tier, Method validator, Class<? extends EnvironmentHost> hostClass) {

        public boolean validate(Container inventory, int slot, ItemStack stack) {
            if (validator != null) {
                return (Boolean) ReflectionUtil.tryInvokeStatic(validator, false, inventory, slot, tier, stack);
            }
            var driver = hostClass != null ? Driver.driverFor(stack, hostClass) : Driver.driverFor(stack);
            if (driver != null) {
                try {
                    return driver.slot(stack).equals(kind) && driver.tier(stack) <= tier;
                } catch (AbstractMethodError e) {
                    LOGGER.warn("Error trying to query driver '{}' for slot and/or tier information.", driver.getClass().getName());
                    return false;
                }
            }
            return false;
        }
    }
}
