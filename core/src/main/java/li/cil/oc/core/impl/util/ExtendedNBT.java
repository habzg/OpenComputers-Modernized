package li.cil.oc.core.impl.util;

import com.google.common.base.Charsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class ExtendedNBT {
    private static final int MAX_BYTE_ARRAY_SIZE = 1048576; // 1MB
    private static final int MAX_INT_ARRAY_SIZE = 262144; // 1MB / 4
    private static final int MAX_LIST_SIZE = 65536;

    public static ByteTag toNbt(boolean value) {
        return ByteTag.valueOf(value);
    }

    public static ByteTag toNbt(byte value) {
        return ByteTag.valueOf(value);
    }

    public static ShortTag toNbt(short value) {
        return ShortTag.valueOf(value);
    }

    public static IntTag toNbt(int value) {
        return IntTag.valueOf(value);
    }

    public static LongTag toNbt(long value) {
        return LongTag.valueOf(value);
    }

    public static FloatTag toNbt(float value) {
        return FloatTag.valueOf(value);
    }

    public static DoubleTag toNbt(double value) {
        return DoubleTag.valueOf(value);
    }

    public static ByteArrayTag toNbt(byte[] value) {
        return new ByteArrayTag(value);
    }

    public static IntArrayTag toNbt(int[] value) {
        return new IntArrayTag(value);
    }

    public static ByteArrayTag toNbt(boolean[] value) {
        byte[] bytes = new byte[value.length];
        for (int i = 0; i < value.length; i++) bytes[i] = (byte) (value[i] ? 1 : 0);
        return new ByteArrayTag(bytes);
    }

    public static StringTag toNbt(String value) {
        return StringTag.valueOf(value);
    }

    public static CompoundTag toNbt(ItemStack value, net.minecraft.core.HolderLookup.Provider provider) {
        if (value != null && !value.isEmpty()) {
            return (CompoundTag) value.save(provider);
        }
        return new CompoundTag();
    }

    @SuppressWarnings("unchecked")
    public static Tag typedMapToNbt(Map<?, ?> map) {
        Map<String, Object> typeAndValue = (Map<String, Object>) map;
        Number nbtType = (Number) typeAndValue.get("type");
        Object nbtValue = typeAndValue.get("value");

        if (nbtType == null) throw new IllegalArgumentException("Missing NBT type.");

        byte tagId;
        tagId = nbtType.byteValue();

        return switch (tagId) {
            case Tag.TAG_BYTE -> {
                if (nbtValue instanceof Number v) yield ByteTag.valueOf(v.byteValue());
                throw new IllegalArgumentException("Illegal or missing value.");
            }
            case Tag.TAG_SHORT -> {
                if (nbtValue instanceof Number v) yield ShortTag.valueOf(v.shortValue());
                throw new IllegalArgumentException("Illegal or missing value.");
            }
            case Tag.TAG_INT -> {
                if (nbtValue instanceof Number v) yield IntTag.valueOf(v.intValue());
                throw new IllegalArgumentException("Illegal or missing value.");
            }
            case Tag.TAG_LONG -> {
                if (nbtValue instanceof Number v) yield LongTag.valueOf(v.longValue());
                throw new IllegalArgumentException("Illegal or missing value.");
            }
            case Tag.TAG_FLOAT -> {
                if (nbtValue instanceof Number v) yield FloatTag.valueOf(v.floatValue());
                throw new IllegalArgumentException("Illegal or missing value.");
            }
            case Tag.TAG_DOUBLE -> {
                if (nbtValue instanceof Number v) yield DoubleTag.valueOf(v.doubleValue());
                throw new IllegalArgumentException("Illegal or missing value.");
            }
            case Tag.TAG_BYTE_ARRAY -> {
                List<?> list = asList(nbtValue);
                if (list.size() > MAX_BYTE_ARRAY_SIZE) throw new IllegalArgumentException("Byte array too large: " + list.size() + " (max " + MAX_BYTE_ARRAY_SIZE + ")");
                byte[] bytes = new byte[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Number n) bytes[i] = n.byteValue();
                    else throw new IllegalArgumentException("Illegal value.");
                }
                yield new ByteArrayTag(bytes);
            }
            case Tag.TAG_STRING -> {
                if (nbtValue instanceof String s) yield StringTag.valueOf(s);
                if (nbtValue instanceof byte[] b) yield StringTag.valueOf(new String(b, Charsets.UTF_8));
                throw new IllegalArgumentException("Illegal or missing value.");
            }
            case Tag.TAG_LIST -> {
                List<?> listValues = asList(nbtValue);
                if (listValues.size() > MAX_LIST_SIZE) throw new IllegalArgumentException("List too large: " + listValues.size() + " (max " + MAX_LIST_SIZE + ")");
                ListTag list = new ListTag();
                for (Object v : listValues) {

                    Map<String, Object> m = (Map<String, Object>) v;
                    list.add(typedMapToNbt(m));
                }
                yield list;
            }
            case Tag.TAG_COMPOUND -> {
                CompoundTag nbt = new CompoundTag();

                Map<String, Object> values = (Map<String, Object>) nbtValue;
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    try {

                        Map<String, Object> entryMap = (Map<String, Object>) entry.getValue();
                        nbt.put(entry.getKey(), typedMapToNbt(entryMap));
                    } catch (Throwable t) {
                        throw new IllegalArgumentException("Error converting entry '" + entry.getKey() + "': " + t.getMessage());
                    }
                }
                yield nbt;
            }
            case Tag.TAG_INT_ARRAY -> {
                List<?> intList = asList(nbtValue);
                if (intList.size() > MAX_INT_ARRAY_SIZE) throw new IllegalArgumentException("Int array too large: " + intList.size() + " (max " + MAX_INT_ARRAY_SIZE + ")");
                int[] ints = new int[intList.size()];
                for (int i = 0; i < intList.size(); i++) {
                    if (intList.get(i) instanceof Number n) ints[i] = n.intValue();
                    else throw new IllegalArgumentException();
                }
                yield new IntArrayTag(ints);
            }
            default -> throw new IllegalArgumentException("Unsupported NBT type '" + tagId + "'.");
        };
    }

    private static List<?> asList(Object value) {
        if (value instanceof Object[] arr) return Arrays.asList(arr);
        if (value instanceof List<?> l) return l;
        if (value instanceof Collection<?> c) return new ArrayList<>(c);
        if (value instanceof String s) {
            byte[] bytes = s.getBytes(Charsets.UTF_8);
            List<Byte> byteList = new ArrayList<>();
            for (byte b : bytes) byteList.add(b);
            return byteList;
        }
        throw new IllegalArgumentException("Illegal or missing value.");
    }

    public static Map<String, Object> toTypedMap(Tag nbt) {
        Object value;
        switch (nbt) {
            case ByteTag tag -> value = tag.getAsByte();
            case ShortTag tag -> value = tag.getAsShort();
            case IntTag tag -> value = tag.getAsInt();
            case LongTag tag -> value = tag.getAsLong();
            case FloatTag tag -> value = tag.getAsFloat();
            case DoubleTag tag -> value = tag.getAsDouble();
            case ByteArrayTag tag -> value = tag.getAsByteArray();
            case StringTag tag -> value = tag.getAsString();
            case ListTag tag -> {
                List<Map<String, Object>> list = new ArrayList<>();
                for (Tag entry : tag) list.add(toTypedMap(entry));
                value = list;
            }
            case CompoundTag tag -> {
                Map<String, Object> map = new LinkedHashMap<>();
                for (String key : tag.getAllKeys()) map.put(key, toTypedMap(tag.get(key)));
                value = map;
            }
            case IntArrayTag tag -> value = tag.getAsIntArray();
            case null, default -> throw new IllegalArgumentException();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", (int) nbt.getId());
        result.put("value", value);
        return result;
    }

    public static void setNewCompoundTag(CompoundTag nbt, String name, java.util.function.Consumer<CompoundTag> f) {
        CompoundTag t = new CompoundTag();
        f.accept(t);
        nbt.put(name, t);
    }

    public static void setNewTagList(CompoundTag nbt, String name, Iterable<Tag> values) {
        ListTag t = new ListTag();
        for (Tag v : values) t.add(v);
        nbt.put(name, t);
    }

    public static Direction getDirection(CompoundTag nbt, String name) {
        byte id = nbt.getByte(name);
        if (id < 0) return null;
        return Direction.from3DDataValue(id);
    }

    public static void setDirection(CompoundTag nbt, String name, Direction d) {
        if (d != null) nbt.putByte(name, (byte) d.get3DDataValue());
        else nbt.putByte(name, (byte) -1);
    }

}
