package li.cil.oc.api;

import java.util.Collection;
import java.util.Set;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * This API allows registering new drivers with the mod.
 * <br>
 * Drivers are used to make items and third-party blocks available in the mod's
 * component network, and optionally to user programs. If you implement a new
 * block that should interact with the mod's component network it is enough to
 * have it implement {@link li.cil.oc.api.network.Environment} - no driver is
 * needed in that case.
 * <br>
 * Note that these methods should <em>not</em> be called in the pre-init phase,
 * since the {@link li.cil.oc.api.API#driver} may not have been initialized
 * at that time. Only start calling these methods in the init phase or later.
 *
 * @see Network
 * @see DriverBlock
 * @see DriverItem
 */
public final class Driver {
    private Driver() {
    }

    /**
     * Registers a new side-aware block driver.
     * <br>
     * Whenever the neighboring blocks of an Adapter block change, it checks if
     * there exists a driver for the changed block, and if it is configured to
     * interface that block type connects it to the component network.
     * <br>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver to register.
     */
    @SuppressWarnings("unused")
    public static void add(final DriverBlock driver) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        API.driver.add(driver);
    }

    /**
     * Registers a new item driver.
     * <br>
     * Item components can inserted into a computers component slots. They have
     * to specify their type, to determine into which slots they can fit.
     * <br>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver to register.
     */
    public static void add(final DriverItem driver) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        API.driver.add(driver);
    }

    /**
     * Registers a new type converter.
     * <br>
     * Type converters are used to automatically convert values returned from
     * callbacks to a "simple" format that can be pushed to any architecture.
     * <br>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param converter the converter to register.
     */
    public static void add(final Converter converter) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        API.driver.add(converter);
    }

    /**
     * Register a new environment provider.
     * <br>
     * Environment providers are used for mapping item stacks to the type of
     * environment that will be created by the stack, either by it being
     * placed in the Level and acting as a block component, or by being
     * placed in an component inventory and created by the item's driver.
     *
     * @param provider the provider to register.
     */
    public static void add(final EnvironmentProvider provider) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        API.driver.add(provider);
    }

    /**
     * Register a new inventory provider.
     * <br>
     * Inventory providers are used for accessing item inventories using
     * the inventory controller upgrade, for example.
     *
     * @param provider the provider to register.
     */
    public static void add(final InventoryProvider provider) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        API.driver.add(provider);
    }

    /**
     * Looks up a driver for the block at the specified position in the
     * specified Level.
     * <br>
     * Note that several drivers for a single block can exist. Because of this
     * block drivers are always encapsulated in a 'compound' driver, which is
     * what will be returned here. In other words, you should will <em>not</em>
     * get actual instances of drivers registered via {@link #add(DriverBlock)}.
     *
     * @param level the Level containing the block.
     * @param pos   the position of the block.
     * @param side  the side of the block.
     * @return a driver for the block, or <code>null</code> if there is none.
     */
    public static DriverBlock driverFor(Level level, BlockPos pos, Direction side) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.driver.driverFor(level, pos, side);
    }

    /**
     * Looks up a driver for the specified item stack.
     * <br>
     * Note that unlike for blocks, there can always only be one item driver
     * per item. If there are multiple ones, the first one that was registered
     * will be used.
     *
     * @param stack the item stack to get a driver for.
     * @param host  the type that will host the environment created by returned driver.
     * @return a driver for the item, or <code>null</code> if there is none.
     */
    public static DriverItem driverFor(ItemStack stack, Class<? extends EnvironmentHost> host) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.driver.driverFor(stack, host);
    }

    /**
     * Looks up a driver for the specified item stack.
     * <br>
     * Note that unlike for blocks, there can always only be one item driver
     * per item. If there are multiple ones, the first one that was registered
     * will be used.
     * <br>
     * This is a context-agnostic variant used mostly for "house-keeping"
     * stuff, such as querying slot types and tier.
     *
     * @param stack the item stack to get a driver for.
     * @return a driver for the item, or <code>null</code> if there is none.
     */
    public static DriverItem driverFor(ItemStack stack) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.driver.driverFor(stack);
    }

    /**
     * Looks up the environments associated with the specified item stack.
     * <br>
     * This will use the registered {@link EnvironmentProvider}s to find
     * environment types for the specified item stack. If none can be
     * found, returns an empty Set.
     *
     * @param stack the item stack to get the environment type for.
     * @return the type of environment associated with the stack, or an empty Set.
     */
    public static Set<Class<?>> environmentsFor(ItemStack stack) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.driver.environmentsFor(stack);
    }

    /**
     * Get an inventory implementation providing access to an item inventory.
     * <br>
     * This will use the registered {@link InventoryProvider}s to find an
     * inventory implementation providing access to the specified stack.
     * If none can be found, returns <code>null</code>.
     * <br>
     * Note that the specified <code>player</code> may be null, but will usually
     * be the <em>fake player</em> of the agent making use of this API.
     *
     * @param stack  the item stack to get the inventory access for.
     * @param player the player holding the item. May be <code>null</code>.
     * @return the inventory implementation interfacing the stack, or <code>null</code>.
     */
    public static Container inventoryFor(ItemStack stack, Player player) {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.driver.inventoryFor(stack, player);
    }

    /**
     * Get a list of all registered block drivers.
     * <br>
     * This is intended to allow checking for particular drivers using more
     * customized logic.
     * <br>
     * The returned collection is read-only.
     *
     * @return the list of all registered block drivers.
     */
    @SuppressWarnings("unused")
    public static Collection<DriverBlock> blockDrivers() {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.driver.blockDrivers();
    }

    /**
     * Get a list of all registered item drivers.
     * <br>
     * This is intended to allow checking for particular drivers using more
     * customized logic.
     * <br>
     * The returned collection is read-only.
     *
     * @return the list of all registered item drivers.
     */
    @SuppressWarnings("unused")
    public static Collection<DriverItem> itemDrivers() {
        if (API.driver == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.driver.itemDrivers();
    }

}
