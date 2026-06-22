package li.cil.oc.api;

import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import net.minecraft.world.entity.player.Player;

/**
 * This API allows interfacing with nanomachines.
 * <br>
 * It allows registering custom behavior providers as well as querying for all
 * presently registered providers and getting a controller for a player.
 */
public class Nanomachines {
    private Nanomachines() {
    }

    /**
     * Register a new behavior provider.
     * <br>
     * When a controller is reconfigured it will draw behaviors from all
     * registered providers and build a new random connection graph to
     * those behaviors.
     *
     * @param provider the provider to add.
     */
    public static void addProvider(BehaviorProvider provider) {
        if (API.nanomachines == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        API.nanomachines.addProvider(provider);
    }

    /**
     * Get a list of all currently registered providers.
     *
     * @return the list of all currently registered providers.
     */
    public static Iterable<BehaviorProvider> getProviders() {
        if (API.nanomachines == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.nanomachines.getProviders();
    }

    /**
     * Check whether a player has a nanomachine controller installed.
     *
     * @param player the player to check for.
     * @return <code>true</code> if the player has a controller, <code>false</code> otherwise.
     */
    public static boolean hasController(Player player) {
        if (API.nanomachines == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.nanomachines.hasController(player);
    }

    /**
     * Get the nanomachine controller of the specified player.
     * <br>
     * If the player has a controller installed, this will initialize the
     * controller if it has not already been loaded. If the player has no
     * controller, this will return <code>null</code>.
     *
     * @param player the player to get the controller for.
     * @return the controller for the specified player.
     */
    public static Controller getController(Player player) {
        if (API.nanomachines == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.nanomachines.getController(player);
    }

    /**
     * Install a controller for the specified player if it doesn't already
     * have one.
     * <br>
     * This will also initialize the controller if it has not already been
     * initialized.
     *
     * @param player the player to install a nanomachine controller for.
     */
    public static Controller installController(Player player) {
        if (API.nanomachines == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        return API.nanomachines.installController(player);
    }

    /**
     * Uninstall a controller from the specified player if it has one.
     * <br>
     * This will disable all active behaviors before disposing the controller.
     *
     * @param player the player to uninstall a nanomachine controller from.
     */
    public static void uninstallController(Player player) {
        if (API.nanomachines == null) throw new IllegalStateException(API.ERROR_NOT_INITIALIZED);
        API.nanomachines.uninstallController(player);
    }

}
