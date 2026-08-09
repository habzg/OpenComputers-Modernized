package li.cil.oc.neoforge.integration.refinedstorage2;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.exporter.AbstractExporterBlockEntity;
import com.refinedmods.refinedstorage.common.iface.InterfaceBlockEntity;
import com.refinedmods.refinedstorage.common.importer.AbstractImporterBlockEntity;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import com.refinedmods.refinedstorage.neoforge.support.resource.VariantUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ConfigHelper {
    private static final String TAG_RESOURCE_FILTER = "rf";

    private ConfigHelper() {
    }

    private static HolderLookup.Provider provider(BlockEntity tile) {
        if (tile.getLevel() != null) {
            return tile.getLevel().registryAccess();
        }
        return net.minecraft.core.RegistryAccess.EMPTY;
    }

    private static CompoundTag writeSlots(List<Optional<ResourceAmount>> slots, HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        for (int i = 0; i < slots.size(); i++) {
            var amount = slots.get(i);
            if (amount.isEmpty()) continue;
            tag.put("s" + i, ResourceCodecs.AMOUNT_CODEC.encodeStart(
                    provider.createSerializationContext(NbtOps.INSTANCE), amount.get()).getOrThrow());
        }
        return tag;
    }

    private static Object toStack(ResourceAmount amount) {
        if (amount.resource() instanceof ItemResource itemResource) {
            return itemResource.toItemStack(Math.clamp(amount.amount(), 1, Integer.MAX_VALUE));
        } else if (amount.resource() instanceof FluidResource fluidResource) {
            return VariantUtil.toFluidStack(fluidResource, Math.max(amount.amount(), 1));
        }
        return ItemStack.EMPTY;
    }

    @SuppressWarnings("SameParameterValue")
    private static int slot(li.cil.oc.api.machine.Arguments args, int arg) {
        return Math.max(0, args.optInteger(arg, 1) - 1);
    }

    public static Object[] getImportConfiguration(AbstractImporterBlockEntity tile, li.cil.oc.api.machine.Arguments args) {
        var data = tile.getMenuData().resources();
        int slot = slot(args, 0);
        if (slot < 0 || slot >= data.size()) return ResultWrapper.result(ItemStack.EMPTY);
        var amount = data.get(slot);
        return amount.map(resourceAmount -> ResultWrapper.result(toStack(resourceAmount))).orElseGet(() -> ResultWrapper.result(ItemStack.EMPTY));
    }

    public static Object[] setImportConfiguration(AbstractImporterBlockEntity tile, li.cil.oc.api.machine.Arguments args, Node node) {
        int slot;
        int valOffset;
        if (args.isInteger(0)) {
            slot = args.checkInteger(0) - 1;
            valOffset = 1;
        } else {
            slot = 0;
            valOffset = 0;
        }
        var data = tile.getMenuData().resources();
        if (slot < 0 || slot >= data.size()) throw new IllegalArgumentException("invalid slot");
        ItemStack stack;
        if (args.count() > 1) {
            stack = DatabaseAccess.getStackFromDatabase(node, args, valOffset);
        } else {
            stack = ItemStack.EMPTY;
        }
        var slots = new ArrayList<>(data);
        slots.set(slot, stack != null && !stack.isEmpty()
                ? Optional.of(new ResourceAmount(ItemResource.ofItemStack(stack), stack.getCount()))
                : Optional.empty());
        var provider = provider(tile);
        var tag = new CompoundTag();
        tag.put(TAG_RESOURCE_FILTER, writeSlots(slots, provider));
        tile.readConfiguration(tag, provider);
        return ResultWrapper.result(true);
    }

    public static Object[] getExportConfiguration(AbstractExporterBlockEntity tile, li.cil.oc.api.machine.Arguments args) {
        var data = tile.getMenuData().resourceContainerData().resources();
        int slot = slot(args, 0);
        if (slot < 0 || slot >= data.size()) return ResultWrapper.result(ItemStack.EMPTY);
        var amount = data.get(slot);
        return amount.map(resourceAmount -> ResultWrapper.result(toStack(resourceAmount))).orElseGet(() -> ResultWrapper.result(ItemStack.EMPTY));
    }

    public static Object[] setExportConfiguration(AbstractExporterBlockEntity tile, li.cil.oc.api.machine.Arguments args, Node node) {
        int slot;
        int valOffset;
        if (args.isInteger(0)) {
            slot = args.checkInteger(0) - 1;
            valOffset = 1;
        } else {
            slot = 0;
            valOffset = 0;
        }
        var data = tile.getMenuData().resourceContainerData().resources();
        if (slot < 0 || slot >= data.size()) throw new IllegalArgumentException("invalid slot");
        ItemStack stack;
        if (args.count() > 1) {
            stack = DatabaseAccess.getStackFromDatabase(node, args, valOffset);
        } else {
            stack = ItemStack.EMPTY;
        }
        var slots = new ArrayList<>(data);
        slots.set(slot, stack != null && !stack.isEmpty()
                ? Optional.of(new ResourceAmount(ItemResource.ofItemStack(stack), stack.getCount()))
                : Optional.empty());
        var provider = provider(tile);
        var tag = new CompoundTag();
        tag.put(TAG_RESOURCE_FILTER, writeSlots(slots, provider));
        tile.readConfiguration(tag, provider);
        return ResultWrapper.result(true);
    }

    public static Object[] getInterfaceConfiguration(InterfaceBlockEntity tile, li.cil.oc.api.machine.Arguments args) {
        var data = tile.getMenuData().filterContainerData().resources();
        int slot = slot(args, 0);
        if (slot < 0 || slot >= data.size()) return ResultWrapper.result(ItemStack.EMPTY);
        var amount = data.get(slot);
        return amount.map(resourceAmount -> ResultWrapper.result(toStack(resourceAmount))).orElseGet(() -> ResultWrapper.result(ItemStack.EMPTY));
    }

    public static Object[] setInterfaceConfiguration(InterfaceBlockEntity tile, li.cil.oc.api.machine.Arguments args, Node node) {
        int slot;
        int valOffset;
        if (args.isInteger(0)) {
            slot = args.checkInteger(0) - 1;
            valOffset = 1;
        } else {
            slot = 0;
            valOffset = 0;
        }
        var data = tile.getMenuData().filterContainerData().resources();
        if (slot < 0 || slot >= data.size()) throw new IllegalArgumentException("invalid slot");
        ItemStack stack;
        if (args.count() > 1) {
            stack = DatabaseAccess.getStackFromDatabase(node, args, valOffset);
        } else {
            stack = ItemStack.EMPTY;
        }
        var slots = new ArrayList<>(data);
        slots.set(slot, stack != null && !stack.isEmpty()
                ? Optional.of(new ResourceAmount(ItemResource.ofItemStack(stack), stack.getCount()))
                : Optional.empty());
        var provider = provider(tile);
        var tag = new CompoundTag();
        tag.put(TAG_RESOURCE_FILTER, writeSlots(slots, provider));
        tile.readConfiguration(tag, provider);
        return ResultWrapper.result(true);
    }
}
