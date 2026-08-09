package li.cil.oc.api.detail;

import java.util.concurrent.Callable;
import li.cil.oc.api.FileSystem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public interface ItemAPI {
    /**
     * Get a descriptor object for the block or item with the specified name.
     * <br>
     * The names are the same as the ones used in the recipe files. An info
     * object can be used to retrieve both the block and item instance of the
     * item, if available. It can also be used to create a new item stack of
     * the item.
     *
     * @param name the name of the item to get the descriptor for.
     * @return the descriptor for the item with the specified name, or
     * <code>null</code> if there is no such item.
     */
    ItemInfo get(String name);

    /**
     * Get a descriptor object for the block or item represented by the
     * specified item stack.
     *
     * @param stack the stack to get the descriptor for.
     * @return the descriptor for the specified item stack, or <code>null</code>
     * if the stack is not a valid OpenComputers item or block.
     */
    ItemInfo get(ItemStack stack);

    /**
     * Register a single loot floppy disk.
     * <br>
     * The disk will be listed in the creative tab of OpenComputers.
     * <br>
     * The specified factory callable will be used to generate a new file
     * system when the loot disk is used as a li.cil.oc.common.component. The specified name
     * will be used as the label for the loot disk, as well as the identifier
     * to select the corresponding factory method, so choose wisely.
     * <br>
     * To use some directory in your mod JAR as the directory provided by the
     * loot disk, use {@link FileSystem#fromClass} in your callable.
     * <br>
     * If <code>doRecipeCycling</code> is <code>true</code>, the floppy disk will be
     * included in the floppy disk recipe cycle if that is enabled.
     * <br>
     * Call this in the init phase or later, <em>not</em> in pre-init.
     *
     * @param name            the label and identifier to use for the loot disk.
     * @param color           the color of the disk, as a Minecraft color.
     * @param factory         the callable to call for creating file system instances.
     * @param doRecipeCycling whether to include this floppy disk in floppy disk cycling.
     * @return an item stack representing the registered loot disk, to allow
     * adding a recipe for your loot disk, for example.
     */
    ItemStack registerFloppy(
            String name, DyeColor color, Callable<li.cil.oc.api.fs.FileSystem> factory, boolean doRecipeCycling);

    /**
     * Register a single loot floppy disk with a specified mod identifier.
     * <br>
     * This overload allows specifying the mod ID that owns the loot disk,
     * which is used for display purposes (e.g., in tooltips).
     * <br>
     * Call this in the init phase or later, <em>not</em> in pre-init.
     *
     * @param name            the label and identifier to use for the loot disk.
     * @param color           the color of the disk, as a Minecraft color.
     * @param factory         the callable to call for creating file system instances.
     * @param doRecipeCycling whether to include this floppy disk in floppy disk cycling.
     * @param modId           the mod identifier that owns this loot disk.
     * @return an item stack representing the registered loot disk, to allow
     * adding a recipe for your loot disk, for example.
     */
    ItemStack registerFloppy(
            String name, DyeColor color, Callable<li.cil.oc.api.fs.FileSystem> factory, boolean doRecipeCycling, String modId);

    /**
     * Register a single custom EEPROM.
     * <br>
     * The EEPROM will be listed in the creative tab of OpenComputers.
     * <br>
     * The EEPROM will be initialized with the specified code and data byte
     * arrays. For script code (e.g. a Lua script) use <code>String.getBytes("UTF-8")</code>.
     * You can omit any of the arguments by passing <code>null</code>.
     * <br>
     * Call this in the init phase or later, <em>not</em> in pre-init.
     *
     * @param name     the label of the EEPROM.
     * @param code     the code section of the EEPROM.
     * @param data     the data section of the EEPROM.
     * @param readonly whether the code section is read-only.
     * @return an item stack representing the registered EEPROM, to allow
     * adding a recipe for your custom BIOS, for example.
     */
    ItemStack registerEEPROM(String name, byte[] code, byte[] data, boolean readonly);
}
