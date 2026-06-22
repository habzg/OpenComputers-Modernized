package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.neoforge.integration.util.MapUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class ConverterItemStack implements Converter {
    public static ItemStack parse(Map<?, ?> args) {
        Integer id = MapUtils.getInt(args, "id");
        String name = MapUtils.getString(args, "name");
        var item = id != null ? BuiltInRegistries.ITEM.byId(id) :
                name != null ? BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(name)) :
                null;
        if (item == null) throw new IllegalArgumentException("item id or name expected");
        Integer size = MapUtils.getInt(args, "size");
        int amount = size != null ? size : 1;
        return new ItemStack(item, amount);
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack stack) {
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            boolean hasTag = customData != null && !customData.isEmpty();
            if (Settings.get().insertIdsInConverters) {
                output.put("id", BuiltInRegistries.ITEM.getId(stack.getItem()));
                output.put("oreNames", stack.getTags().map(t -> t.location().toString()).collect(Collectors.toList()));
            }
            output.put("damage", stack.getDamageValue());
            output.put("maxDamage", stack.getMaxDamage());
            output.put("size", stack.getCount());
            output.put("maxSize", stack.getMaxStackSize());
            output.put("hasTag", hasTag);
            output.put("name", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            output.put("label", stack.getDisplayName().getString());

            if (hasTag) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("display", 10) && tag.getCompound("display").contains("Lore", 9)) {
                    var loreTag = tag.getCompound("display").getList("Lore", 8);
                    StringBuilder lore = new StringBuilder();
                    for (int i = 0; i < loreTag.size(); i++) {
                        if (i > 0) lore.append("\n");
                        lore.append(loreTag.getString(i));
                    }
                    output.put("lore", lore.toString());
                }
            }

            var enchantments = new ArrayList<Map<String, Object>>();
            for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
                enchantments.add(buildEnchantmentMap(entry.getKey(), entry.getIntValue()));
            }
            if (!enchantments.isEmpty()) {
                output.put("enchantments", enchantments);
            }

            if (Settings.get().allowItemStackNBTTags) {
                if (customData != null && !customData.isEmpty()) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        var server = SideTracker.getCurrentServer();
                        if (server != null) {
                            var fullTag = stack.save(server.registryAccess(), new CompoundTag());
                            NbtIo.writeCompressed((CompoundTag) fullTag, baos);
                        } else {
                            NbtIo.writeCompressed(customData.copyTag(), baos);
                        }
                        output.put("tag", baos.toByteArray());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private static Map<String, Object> buildEnchantmentMap(Holder<Enchantment> enchantment, int level) {
        var map = new HashMap<String, Object>();
        var ench = enchantment.value();
        var key = enchantment.getKey();
        map.put("name", key != null ? key.location().toString() : ench.description().getString());
        map.put("label", ench.description().getString());
        map.put("level", level);
        if (Settings.get().insertIdsInConverters) {
            if (key != null) {
                map.put("id", key.location().toString());
            }
        }
        return map;
    }
}
