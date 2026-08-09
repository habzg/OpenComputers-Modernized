package li.cil.oc.core.impl.common.template;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TemplateBlacklist {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateBlacklist.class);
    private static ItemStack[] TheBlacklist = null;

    private TemplateBlacklist() {
    }

    private static void ensureInit() {
        if (TheBlacklist != null) return;
        var pattern = Pattern.compile("^([^@]+)(?:@(\\d+))?$");
        List<ItemStack> list = new ArrayList<>();
        for (var entry : OCSettings.get().assemblerBlacklist) {
            var m = pattern.matcher(entry);
            if (m.matches()) {
                var id = m.group(1);
                try {
                    var item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
                    list.add(new ItemStack(item, 1));
                } catch (NumberFormatException e) {
                    LOGGER.warn("Bad assembler blacklist entry '{}', invalid damage value.", entry);
                }
            } else {
                LOGGER.warn("Bad assembler blacklist entry '{}', invalid format (should be 'id' or 'id@damage').", entry);
            }
        }
        TheBlacklist = list.toArray(new ItemStack[0]);
    }

    public static void register() {
        li.cil.oc.core.impl.common.Registrar.registerAssemblerFilter("li.cil.oc.core.impl.common.template.TemplateBlacklist.filter");
    }

    @SuppressWarnings("unused")

    public static boolean filter(ItemStack stack) {
        ensureInit();
        for (var entry : TheBlacklist) {
            if (ItemStack.isSameItem(entry, stack)) return false;
        }
        return true;
    }
}
