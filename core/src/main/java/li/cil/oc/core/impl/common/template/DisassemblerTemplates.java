package li.cil.oc.core.impl.common.template;

import li.cil.oc.core.impl.common.ReflectionUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;

public final class DisassemblerTemplates {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisassemblerTemplates.class);
    private static final ArrayList<Template> templates = new ArrayList<>();

    private DisassemblerTemplates() {
    }

    public static void add(CompoundTag template) {
        try {
            var selector = ReflectionUtil.getStaticMethod(template.getString("select"), ItemStack.class);
            var disassembler = ReflectionUtil.getStaticMethod(template.getString("disassemble"), ItemStack.class, ItemStack[].class);
            if (selector != null && disassembler != null) {
                templates.add(new Template(selector, disassembler));
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed registering disassembler template.", t);
        }
    }

    public static Template select(ItemStack stack) {
        for (var t : templates) {
            if (t.select(stack)) return t;
        }
        return null;
    }

    public record Template(Method selector, Method disassembler) {

        public boolean select(ItemStack stack) {
            return (Boolean) ReflectionUtil.tryInvokeStatic(selector, false, stack);
        }

        public Object[] disassemble(ItemStack stack, ItemStack[] ingredients) {
            var result = ReflectionUtil.tryInvokeStatic(disassembler, null, stack, ingredients);
            if (result instanceof Object[] arr) {
                if (arr.length >= 2 && arr[0] instanceof ItemStack[] stacks && arr[1] instanceof ItemStack[] drops) {
                    return new Object[]{stacks, drops};
                }
                if (arr.length >= 2 && arr[0] instanceof ItemStack s && arr[1] instanceof ItemStack[] drops) {
                    return new Object[]{new ItemStack[]{s}, drops};
                }
                if (arr.length >= 2 && arr[0] instanceof ItemStack[] stacks && arr[1] instanceof ItemStack drop) {
                    return new Object[]{stacks, new ItemStack[]{drop}};
                }
                if (arr.length >= 1 && arr[0] instanceof ItemStack[] stacks) {
                    return new Object[]{stacks, null};
                }
                if (arr.length >= 1) {
                    ItemStack[] stacks = new ItemStack[arr.length];
                    for (int i = 0; i < arr.length; i++) stacks[i] = (ItemStack) arr[i];
                    return new Object[]{stacks, null};
                }
            }
            return new Object[]{null, null};
        }
    }
}
