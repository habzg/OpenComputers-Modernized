package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.driver.Converter;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class ConverterNBT implements Converter {
    private static Object convert(Tag nbt) {
        return switch (nbt) {
            case ByteTag tag -> tag.getAsByte();
            case ShortTag tag -> tag.getAsShort();
            case IntTag tag -> tag.getAsInt();
            case LongTag tag -> tag.getAsLong();
            case FloatTag tag -> tag.getAsFloat();
            case DoubleTag tag -> tag.getAsDouble();
            case ByteArrayTag tag -> tag.getAsByteArray();
            case StringTag tag -> tag.getAsString();
            case ListTag tag -> {
                var copy = tag.copy();
                var list = new ArrayList<>();
                while (!copy.isEmpty()) {
                    list.add(convert(copy.removeFirst()));
                }
                yield list.toArray();
            }
            case CompoundTag tag -> {
                var map = new HashMap<>();
                for (String key : tag.getAllKeys()) {
                    map.put(key, convert(tag.get(key)));
                }
                yield map;
            }
            case IntArrayTag tag -> tag.getAsIntArray();
            case null, default -> null;
        };
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof CompoundTag nbt) {
            output.put("oc:flatten", convert(nbt));
        }
    }
}
