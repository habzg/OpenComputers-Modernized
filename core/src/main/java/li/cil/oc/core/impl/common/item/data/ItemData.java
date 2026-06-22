package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.api.Items;
import li.cil.oc.api.Persistable;
import li.cil.oc.core.impl.util.SideTracker;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;


import java.util.Optional;
import java.util.stream.Stream;

public abstract class ItemData implements Persistable {
    protected String itemName;

    public ItemData(String itemName) {
        this.itemName = itemName;
    }

    public void load(ItemStack stack, HolderLookup.Provider provider) {
        var tag = stack.get(DataComponents.CUSTOM_DATA);
        if (tag != null && !tag.isEmpty()) {
            load(tag.copyTag(), provider);
        }
    }

    public void load(ItemStack stack) {
        var server = SideTracker.getCurrentServer();
        load(stack, server != null ? server.registryAccess() : new HolderLookup.Provider() {
            @Override
            public <T> @NotNull Optional<RegistryLookup<T>> lookup(@NotNull ResourceKey<? extends Registry<? extends T>> registry) {
                return Optional.empty();
            }

            @Override
            public @NotNull Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return Stream.of();
            }
        });
    }

    public void save(ItemStack stack, HolderLookup.Provider provider) {
        var tag = stack.get(DataComponents.CUSTOM_DATA);
        var nbt = tag != null && !tag.isEmpty() ? tag.copyTag() : new CompoundTag();
        save(nbt, provider);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    public void save(ItemStack stack) {
        var server = SideTracker.getCurrentServer();
        save(stack, server != null ? server.registryAccess() : new HolderLookup.Provider() {
            @Override
            public @NotNull <T> Optional<RegistryLookup<T>> lookup(@NotNull ResourceKey<? extends Registry<? extends T>> registry) {
                return Optional.empty();
            }

            @Override
            public @NotNull Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return Stream.of();
            }
        });
    }

    public ItemStack createItemStack() {
        if (itemName == null) return null;
        ItemStack stack = Items.get(itemName).createItemStack(1);
        save(stack);
        return stack;
    }
}
