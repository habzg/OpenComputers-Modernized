package li.cil.oc.core.impl.common.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.DroneData;
import li.cil.oc.core.impl.common.item.data.MicrocontrollerData;
import li.cil.oc.core.impl.common.item.data.RobotData;
import li.cil.oc.core.impl.common.item.data.TabletData;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.core.impl.util.SideTracker;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    private static PrintHandler printHandler = null;
    private static BooleanSupplier ae2Check = () -> false;

    private ExtendedRecipe() {
    }

    @FunctionalInterface
    public interface PrintHandler {
        @Nullable
        ItemStack handlePrintCraft(ItemStack ignoredCraftedStack, CraftingInput ignoredInventory);
    }

    public static void setPrintHandler(@Nullable PrintHandler handler) {
        printHandler = handler;
    }

    public static void setAe2Check(BooleanSupplier check) {
        ae2Check = check;
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
                        ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "map", t -> t.merge((CompoundTag) stack.save(provider)));
                    }
                }
            }
        }

        if (craftedItemName == linkedCard) {
            if (ae2Check.getAsBoolean()) return disabled.copy();
            if (SideTracker.isServer()) {
                var driver = li.cil.oc.api.API.driver.driverFor(craftedStack);
                if (driver != null) {
                    var nbt = driver.dataTag(craftedStack);
                    nbt.putString(OCSettings.namespace + "tunnel", UUID.randomUUID().toString());
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
                String colorKey = OCSettings.namespace + "color";
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

        if (printHandler != null) {
            var result = printHandler.handlePrintCraft(craftedStack, inventory);
            if (result != null) {
                return result;
            }
        }

        if (!(recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe) ||
            getItems(inventory).length != 2) return craftedStack;

        if (craftedItemName == eeprom && craftedStack.getCount() == 2) {
            for (var stack : getItems(inventory)) {
                var stackCustomData = stack.get(DataComponents.CUSTOM_DATA);
                if (li.cil.oc.api.Items.get(stack) == eeprom && stackCustomData != null && !stackCustomData.isEmpty()) {
                    var copy = stackCustomData.copyTag();
                    if (copy.getCompound(OCSettings.namespace + "data").getCompound("node").contains("address")) {
                        copy.getCompound(OCSettings.namespace + "data").getCompound("node").remove("address");
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

    public static NonNullList<ItemStack> getRecraftRemainingItems(CraftingInput inventory, NonNullList<ItemStack> defaultRemaining) {
        ensureInit();
        var items = getItems(inventory);
        if (items.length != 2) return defaultRemaining;

        ItemStack oldDevice = null;
        ItemInfo deviceInfo = null;
        boolean hasEeprom = false;
        for (var stack : items) {
            var info = li.cil.oc.api.Items.get(stack);
            if (info == eeprom) hasEeprom = true;
            else if (info == mcu || info == drone || info == robot || info == tablet) {
                oldDevice = stack;
                deviceInfo = info;
            }
        }

        if (oldDevice != null && hasEeprom && deviceInfo != null) {
            var data = createWrapper(deviceInfo, oldDevice);
            if (data != null) {
                for (var comp : data.components()) {
                    if (li.cil.oc.api.Items.get(comp) == eeprom && !comp.isEmpty()) {
                        for (int i = 0; i < inventory.size(); i++) {
                            if (inventory.getItem(i) == oldDevice) {
                                defaultRemaining.set(i, comp);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            return defaultRemaining;
        }

        ItemStack oldNavi = null;
        boolean hasMap = false;
        for (var stack : items) {
            if (li.cil.oc.api.Items.get(stack) == navigationUpgrade) oldNavi = stack;
            if (stack.getItem() == net.minecraft.world.item.Items.FILLED_MAP) hasMap = true;
        }

        if (oldNavi != null && hasMap) {
            var driver = li.cil.oc.api.API.driver.driverFor(oldNavi);
            if (driver != null) {
                var tag = driver.dataTag(oldNavi);
                if (tag != null && tag.contains(OCSettings.namespace + "map")) {
                    var server = SideTracker.getCurrentServer();
                    if (server != null) {
                        var oldMap = ItemStack.parseOptional(server.registryAccess(), tag.getCompound(OCSettings.namespace + "map"));
                        if (!oldMap.isEmpty()) {
                            for (int i = 0; i < inventory.size(); i++) {
                                if (inventory.getItem(i) == oldNavi) {
                                    defaultRemaining.set(i, oldMap);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return defaultRemaining;
        }

        return defaultRemaining;
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

    private static ItemDataWrapper createWrapper(ItemInfo descriptor, ItemStack stack) {
        if (descriptor == mcu) return new MCUDataWrapper(stack);
        if (descriptor == drone) return new DroneDataWrapper(stack);
        if (descriptor == robot) return new RobotDataWrapper(stack);
        if (descriptor == tablet) return new TabletDataWrapper(stack);
        return null;
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
