package li.cil.oc.api.driver;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory providers are used to access contents of item inventories.
 * <br>
 * In OpenComputers an example for this would be servers, in other mods
 * this can be backpacks and the like. Inventory providers are used to
 * allow agents (robots, drones) to interact with such inventories using
 * the inventory controller upgrade, for example.
 * <br>
 * Implementations returned by {@link #getInventory} should save changes
 * back to the item stack when  is called.
 * Return <code>null</code> if the specified stack is not supported.
 */
public interface InventoryProvider {
    /**
     * Checks whether this provider works for the specified item stack.
     *
     * @param stack  the item stack to check for.
     * @param player the player holding the item, may be <code>null</code>.
     * @return <code>true</code> if the stack is supported, <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    boolean worksWith(ItemStack stack, Player player);

    /**
     * Get an inventory implementation that allows interfacing with the
     * item inventory represented by the specified item stack.
     * <br>
     * Note that the specified player may be <code>null</code>, but will
     * usually be the <em>fake player</em> of the agent using the
     * inventory controller upgrade to access the item inventory.
     *
     * @param stack  the item stack to get the inventory for.
     * @param player the player holding the item, may be <code>null</code>.
     * @return the inventory representing the contents, or <code>null</code>.
     */
    @SuppressWarnings("unused")
    Container getInventory(ItemStack stack, Player player);
}
