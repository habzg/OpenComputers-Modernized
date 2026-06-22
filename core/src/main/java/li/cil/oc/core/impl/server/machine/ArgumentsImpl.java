package li.cil.oc.core.impl.server.machine;

import li.cil.oc.api.machine.Arguments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;

public class ArgumentsImpl implements Arguments {
    private final Object[] args;

    public ArgumentsImpl(Object[] args) {
        this.args = args != null ? args : new Object[0];
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof byte[]) {
                result[i] = new String((byte[]) args[i], java.nio.charset.StandardCharsets.UTF_8);
            } else {
                result[i] = args[i];
            }
        }
        return result;
    }

    @Override
    public @NotNull Iterator<Object> iterator() {
        return java.util.Arrays.asList(args).iterator();
    }

    @Override
    public int count() {
        return args.length;
    }

    @Override
    public Object checkAny(int index) {
        if (index < 0 || index >= args.length) throw new IllegalArgumentException("not enough arguments");
        return args[index];
    }

    @Override
    public boolean checkBoolean(int index) {
        Object value = checkAny(index);
        if (value instanceof Boolean) return (Boolean) value;
        throw new IllegalArgumentException("expected boolean, got " + (value != null ? value.getClass().getSimpleName() : "nil"));
    }

    @Override
    public double checkDouble(int index) {
        Object value = checkAny(index);
        if (value instanceof Number) return ((Number) value).doubleValue();
        throw new IllegalArgumentException("expected number, got " + (value != null ? value.getClass().getSimpleName() : "nil"));
    }

    @Override
    public int checkInteger(int index) {
        double value = checkDouble(index);
        if (Double.isNaN(value)) throw new IllegalArgumentException("expected integer, got NaN");
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }

    @Override
    public String checkString(int index) {
        Object value = checkAny(index);
        if (value instanceof String) return (String) value;
        if (value instanceof byte[]) return new String((byte[]) value);
        throw new IllegalArgumentException("expected string, got " + (value != null ? value.getClass().getSimpleName() : "nil"));
    }

    @Override
    public byte[] checkByteArray(int index) {
        Object value = checkAny(index);
        if (value instanceof byte[]) return (byte[]) value;
        if (value instanceof String) return ((String) value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        throw new IllegalArgumentException("expected byte array, got " + (value != null ? value.getClass().getSimpleName() : "nil"));
    }

    @Override
    public Object optAny(int index, Object defaultVal) {
        if (index < 0 || index >= count()) return defaultVal;
        return args[index];
    }

    @Override
    public boolean optBoolean(int index, boolean defaultVal) {
        if (index < 0 || index >= count()) return defaultVal;
        Object value = args[index];
        if (value instanceof Boolean) return (Boolean) value;
        return defaultVal;
    }

    @Override
    public double optDouble(int index, double defaultVal) {
        if (index < 0 || index >= count()) return defaultVal;
        Object value = args[index];
        if (value instanceof Number) return ((Number) value).doubleValue();
        return defaultVal;
    }

    @Override
    public int optInteger(int index, int defaultVal) {
        double value = optDouble(index, defaultVal);
        if (Double.isNaN(value)) return defaultVal;
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }

    @Override
    public String optString(int index, String defaultVal) {
        if (index < 0 || index >= count()) return defaultVal;
        Object value = args[index];
        if (value instanceof String) return (String) value;
        if (value instanceof byte[]) return new String((byte[]) value);
        return defaultVal;
    }

    @Override
    public byte[] optByteArray(int index, byte[] defaultVal) {
        if (index < 0 || index >= count()) return defaultVal;
        Object value = args[index];
        if (value instanceof byte[]) return (byte[]) value;
        if (value instanceof String) return ((String) value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return defaultVal;
    }

    @Override
    public boolean isString(int index) {
        if (index < 0 || index >= count()) return false;
        return args[index] instanceof String || args[index] instanceof byte[];
    }

    @Override
    public long checkLong(int index) {
        double value = checkDouble(index);
        if (Double.isNaN(value)) throw new IllegalArgumentException("expected long, got NaN");
        if (value > Long.MAX_VALUE) return Long.MAX_VALUE;
        if (value < Long.MIN_VALUE) return Long.MIN_VALUE;
        return (long) value;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map checkTable(int index) {
        Object value = checkAny(index);
        if (value instanceof Map) return (Map) value;
        throw new IllegalArgumentException("expected table, got " + (value != null ? value.getClass().getSimpleName() : "nil"));
    }

    @Override
    public ItemStack checkItemStack(int index) {
        Map<?, ?> map = checkTable(index);
        Object nameObj = map.get("name");
        if (!(nameObj instanceof String name)) {
            throw new IllegalArgumentException("invalid item stack");
        }
        int damage = 0;
        Object damageObj = map.get("damage");
        if (damageObj instanceof Number number) {
            damage = number.intValue();
        }
        CompoundTag tag = null;
        Object tagObj = map.get("tag");
        try {
            if (tagObj instanceof byte[] ba) {
                tag = NbtIo.readCompressed(new java.io.ByteArrayInputStream(ba), NbtAccounter.unlimitedHeap());
            } else if (tagObj instanceof String s) {
                tag = NbtIo.readCompressed(new java.io.ByteArrayInputStream(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)), NbtAccounter.unlimitedHeap());
            }
        } catch (java.io.IOException ignored) {
        }
        ResourceLocation location;
        try {
            location = ResourceLocation.parse(name);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid item stack");
        }
        Item item = BuiltInRegistries.ITEM.get(location);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("invalid item stack");
        }
        ItemStack stack = new ItemStack(item, 1);
        if (damage > 0) {
            stack.setDamageValue(damage);
        }
        if (tag != null && !tag.isEmpty()) {
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        }
        return stack;
    }

    @Override
    public long optLong(int index, long def) {
        double value = optDouble(index, def);
        if (Double.isNaN(value)) return def;
        if (value > Long.MAX_VALUE) return Long.MAX_VALUE;
        if (value < Long.MIN_VALUE) return Long.MIN_VALUE;
        return (long) value;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map optTable(int index, Map def) {
        if (index < 0 || index >= count()) return def;
        Object value = args[index];
        if (value instanceof Map) return (Map) value;
        return def;
    }

    @Override
    public ItemStack optItemStack(int index, ItemStack def) {
        if (index < 0 || index >= count()) return def;
        try {
            return checkItemStack(index);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    @Override
    public boolean isBoolean(int index) {
        if (index < 0 || index >= count()) return false;
        return args[index] instanceof Boolean;
    }

    @Override
    public boolean isInteger(int index) {
        if (index < 0 || index >= count()) return false;
        var val = args[index];
        if (val instanceof Double d) return !d.isNaN();
        if (val instanceof Float f) return !f.isNaN();
        return val instanceof Number;
    }

    @Override
    public boolean isLong(int index) {
        return isInteger(index);
    }

    @Override
    public boolean isDouble(int index) {
        if (index < 0 || index >= count()) return false;
        return args[index] instanceof Number;
    }

    @Override
    public boolean isByteArray(int index) {
        if (index < 0 || index >= count()) return false;
        return args[index] instanceof byte[] || args[index] instanceof String;
    }

    @Override
    public boolean isTable(int index) {
        if (index < 0 || index >= count()) return false;
        return args[index] instanceof Map;
    }

    @Override
    public boolean isItemStack(int index) {
        if (index < 0 || index >= count()) return false;
        if (!(args[index] instanceof Map<?, ?> map)) return false;
        Object name = map.get("name");
        return name instanceof String || name instanceof byte[];
    }
}
