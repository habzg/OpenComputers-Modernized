package li.cil.oc.neoforge.common.recipe;

import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.data.DroneData;
import li.cil.oc.core.impl.common.item.data.MicrocontrollerData;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.item.data.TabletData;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ExtendedRecipe {
    private static ItemInfo drone = null;
    private static ItemInfo eeprom = null;
    private static ItemInfo mcu = null;
    private static ItemInfo navigationUpgrade = null;
    private static ItemInfo linkedCard = null;
    private static ItemInfo floppy = null;
    private static ItemInfo[] hdds = null;
    private static ItemInfo[] cpus = null;
    private static ItemInfo robot = null;
    private static ItemInfo tablet = null;
    private static ItemStack disabled = null;

    private ExtendedRecipe() {
    }

    @SuppressWarnings("unused")
    private static void ensureInit() {
        if (drone == null) {
            drone = li.cil.oc.api.Items.get(Constants.ItemName.Drone);
            eeprom = li.cil.oc.api.Items.get(Constants.ItemName.EEPROM);
            ItemInfo luaBios = li.cil.oc.api.Items.get(Constants.ItemName.LuaBios);
            mcu = li.cil.oc.api.Items.get(Constants.BlockName.Microcontroller);
            navigationUpgrade = li.cil.oc.api.Items.get(Constants.ItemName.NavigationUpgrade);
            linkedCard = li.cil.oc.api.Items.get(Constants.ItemName.LinkedCard);
            floppy = li.cil.oc.api.Items.get(Constants.ItemName.Floppy);
            hdds = new ItemInfo[]{
                    li.cil.oc.api.Items.get(Constants.ItemName.HDDTier1),
                    li.cil.oc.api.Items.get(Constants.ItemName.HDDTier2),
                    li.cil.oc.api.Items.get(Constants.ItemName.HDDTier3)
            };
            cpus = new ItemInfo[]{
                    li.cil.oc.api.Items.get(Constants.ItemName.CPUTier1),
                    li.cil.oc.api.Items.get(Constants.ItemName.CPUTier2),
                    li.cil.oc.api.Items.get(Constants.ItemName.CPUTier3),
                    li.cil.oc.api.Items.get(Constants.ItemName.APUTier1),
                    li.cil.oc.api.Items.get(Constants.ItemName.APUTier2)
            };
            robot = li.cil.oc.api.Items.get(Constants.BlockName.Robot);
            tablet = li.cil.oc.api.Items.get(Constants.ItemName.Tablet);
            var stack = new ItemStack(Blocks.DIRT);
            var tag = new CompoundTag();
            ExtendedNBT.setNewCompoundTag(tag, "display", t ->
                    ExtendedNBT.setNewTagList(t, "Lore", Collections.singletonList(
                            net.minecraft.nbt.StringTag.valueOf("Autocrafting of this item is disabled to avoid exploits.")
                    ))
            );
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            disabled = stack;
        }
    }

    @SuppressWarnings("unused")
    public static ItemStack addNBTToResult(Recipe<?> recipe, ItemStack craftedStack, CraftingInput inventory, net.minecraft.core.HolderLookup.Provider provider) {
        ensureInit();
        var craftedItemName = li.cil.oc.api.Items.get(craftedStack);

        if (craftedItemName == navigationUpgrade) {
            var driver = li.cil.oc.api.API.driver.driverFor(craftedStack);
            if (driver != null) {
                for (var stack : getItems(inventory)) {
                    if (stack.getItem() == net.minecraft.world.item.Items.FILLED_MAP) {
                        var nbt = driver.dataTag(craftedStack);
                        ExtendedNBT.setNewCompoundTag(nbt, Settings.namespace + "map", t -> stack.save(provider, t));
                    }
                }
            }
        }

        if (craftedItemName == linkedCard) {
            if (weAreBeingCalledFromAppliedEnergistics2()) return disabled.copy();
            if (SideTracker.isServer()) {
                var driver = li.cil.oc.api.API.driver.driverFor(craftedStack);
                if (driver != null) {
                    var nbt = driver.dataTag(craftedStack);
                    nbt.putString(Settings.namespace + "tunnel", UUID.randomUUID().toString());
                }
            }
        }

        if (contains(cpus, craftedItemName)) {
            LuaStateFactory.setDefaultArch(craftedStack);
        }

        if (craftedItemName == floppy || contains(hdds, craftedItemName)) {
            var craftedCustomData = craftedStack.get(DataComponents.CUSTOM_DATA);
            if (craftedCustomData == null || craftedCustomData.isEmpty()) {
                craftedStack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
                craftedCustomData = craftedStack.get(DataComponents.CUSTOM_DATA);
            }
            CompoundTag nbt = craftedCustomData != null ? craftedCustomData.copyTag() : new CompoundTag();
            if (craftedStack.getCount() == 1) {
                String colorKey = Settings.namespace + "color";
                for (var stack : getItems(inventory)) {
                    var info = li.cil.oc.api.Items.get(stack);
                    var stackCustomData = stack.get(DataComponents.CUSTOM_DATA);
                    if (info != null && (info == floppy || info == li.cil.oc.api.Items.get(Constants.ItemName.LootDisk)) && stackCustomData != null && !stackCustomData.isEmpty()) {
                        var oldData = stackCustomData.copyTag();
                        var colorTag = oldData.get(colorKey);
                        if (colorTag != null && oldData.getInt(colorKey) != java.util.Arrays.asList(Color.dyes).indexOf("lightGray")) {
                            nbt.put(colorKey, colorTag.copy());
                        }
                    }
                }
                if (nbt.isEmpty()) {
                    craftedStack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                } else {
                    craftedStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                }
            } else if (allFloppies(inventory)) {
                for (var stack : getItems(inventory)) {
                    var stackCustomData = stack.get(DataComponents.CUSTOM_DATA);
                    if (li.cil.oc.api.Items.get(stack) == floppy && stackCustomData != null && !stackCustomData.isEmpty()) {
                        var oldData = stackCustomData.copyTag();
                        for (var oldTagName : oldData.getAllKeys()) {
                            var tag = oldData.get(oldTagName);
                            if (tag != null) nbt.put(oldTagName, tag.copy());
                        }
                    }
                }
                craftedStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
            }
        }

        if (craftedStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() instanceof li.cil.oc.neoforge.common.block.Print) {
            var data = new PrintData(craftedStack);
            var inputs = getItems(inventory);
            boolean isBeaconBaseInput = false;

            for (var stack : inputs) {
                if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof li.cil.oc.neoforge.common.block.Print) {
                    data.load(stack);
                    if (stack.is(li.cil.oc.neoforge.common.init.Items.BEACON_BASE_PRINT.get())) {
                        isBeaconBaseInput = true;
                    }
                }
            }

            var glowstoneDust = new ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST);
            var glowstone = new ItemStack(net.minecraft.world.level.block.Blocks.GLOWSTONE);

            for (var stack : inputs) {
                if (ItemStack.isSameItem(glowstoneDust, stack)) {
                    if (data.lightLevel >= 15) return null;
                    data.lightLevel = Math.min(15, data.lightLevel + 1);
                }
                if (ItemStack.isSameItem(glowstone, stack)) {
                    if (data.lightLevel >= 15) return null;
                    data.lightLevel = Math.min(15, data.lightLevel + 4);
                }
            }

            if (isBeaconBaseInput && !craftedStack.is(li.cil.oc.neoforge.common.init.Items.BEACON_BASE_PRINT.get())) {
                var correctStack = new ItemStack(li.cil.oc.neoforge.common.init.Items.BEACON_BASE_PRINT.get(), craftedStack.getCount());
                data.save(correctStack);
                return correctStack;
            }

            data.save(craftedStack);
        }

        if (!(recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe) ||
            getItems(inventory).length != 2) return craftedStack;

        if (craftedItemName == eeprom && craftedStack.getCount() == 2) {
            for (var stack : getItems(inventory)) {
                var stackCustomData = stack.get(DataComponents.CUSTOM_DATA);
                if (li.cil.oc.api.Items.get(stack) == eeprom && stackCustomData != null && !stackCustomData.isEmpty()) {
                    var copy = stackCustomData.copyTag();
                    if (copy.getCompound(Settings.namespace + "data").getCompound("node").contains("address")) {
                        copy.getCompound(Settings.namespace + "data").getCompound("node").remove("address");
                    }
                    craftedStack.set(DataComponents.CUSTOM_DATA, CustomData.of(copy));
                    break;
                }
            }
        }

        recraft(craftedStack, inventory, mcu, MCUDataWrapper::new);
        recraft(craftedStack, inventory, drone, DroneDataWrapper::new);
        recraft(craftedStack, inventory, robot, RobotDataWrapper::new);
        recraft(craftedStack, inventory, tablet, TabletDataWrapper::new);

        return craftedStack;
    }

    private static ItemStack[] getItems(CraftingInput inventory) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty()) list.add(stack);
        }
        return list.toArray(new ItemStack[0]);
    }

    private static boolean contains(ItemInfo[] arr, ItemInfo item) {
        for (var i : arr) if (i == item) return true;
        return false;
    }

    private static boolean allFloppies(CraftingInput inventory) {
        for (var stack : getItems(inventory)) {
            if (li.cil.oc.api.Items.get(stack) != floppy) return false;
        }
        return true;
    }

    private static void recraft(ItemStack craftedStack, CraftingInput inventory, ItemInfo descriptor, java.util.function.Function<ItemStack, ItemDataWrapper> dataFactory) {
        if (li.cil.oc.api.Items.get(craftedStack) == descriptor) {
            ItemStack oldMcu = null;
            for (var stack : getItems(inventory)) {
                if (li.cil.oc.api.Items.get(stack) == descriptor) {
                    oldMcu = stack;
                    break;
                }
            }
            if (oldMcu != null) {
                var data = dataFactory.apply(oldMcu);
                var oldRom = new ArrayList<ItemStack>();
                for (var comp : data.components()) {
                    if (li.cil.oc.api.Items.get(comp) == eeprom) oldRom.add(comp);
                }
                data.components().removeAll(oldRom);

                for (var stack : getItems(inventory)) {
                    if (li.cil.oc.api.Items.get(stack) == eeprom) {
                        data.components().add(stack.copy().split(1));
                    }
                }
                data.save(craftedStack);
            }
        }
    }

    private static boolean weAreBeingCalledFromAppliedEnergistics2() {
        return Mods.AppliedEnergistics2.isAvailable() && Thread.currentThread().getStackTrace().length > 0;
    }

    private interface ItemDataWrapper {
        List<ItemStack> components();

        void save(ItemStack stack);
    }

    private record MCUDataWrapper(MicrocontrollerData data) implements ItemDataWrapper {
        MCUDataWrapper(ItemStack stack) {
            this(new MicrocontrollerData(stack));
        }

        @Override
        public List<ItemStack> components() {
            return data.components;
        }

        @Override
        public void save(ItemStack stack) {
            data.save(stack);
        }
    }

    private record DroneDataWrapper(DroneData data) implements ItemDataWrapper {
        DroneDataWrapper(ItemStack stack) {
            this(new DroneData(stack));
        }

        @Override
        public List<ItemStack> components() {
            return data.components;
        }

        @Override
        public void save(ItemStack stack) {
            data.save(stack);
        }
    }

    private record RobotDataWrapper(RobotData data) implements ItemDataWrapper {
        RobotDataWrapper(ItemStack stack) {
            this(new RobotData(stack));
        }

        @Override
        public List<ItemStack> components() {
            return data.components;
        }

        @Override
        public void save(ItemStack stack) {
            data.save(stack);
        }
    }

    private static class TabletDataWrapper implements ItemDataWrapper {
        final TabletData data;
        final List<ItemStack> components;

        TabletDataWrapper(ItemStack stack) {
            data = new TabletData(stack);
            this.components = new ArrayList<>();
            for (var opt : data.items) {
                if (opt != null) this.components.add(opt);
            }
        }

        @Override
        public List<ItemStack> components() {
            return components;
        }

        @Override
        public void save(ItemStack stack) {
            data.items = new ArrayList<>(components);
            data.save(stack);
        }
    }
}
