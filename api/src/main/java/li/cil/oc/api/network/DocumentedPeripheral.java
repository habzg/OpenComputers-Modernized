package li.cil.oc.api.network;

import org.jetbrains.annotations.Nullable;

/**
 * Optional interface for {@link ManagedPeripheral}s
 * that can provide docstrings for the methods they expose.
 * <br>
 * Note: If you are writing a new addon, it is preferred that you
 * directly use the "doc" parameter of the @Callback annotation on
 * your methods instead. This interface is mainly for mods that
 * already have a different way of providing docstrings in place. (e.g Mekanism).
 */
public interface DocumentedPeripheral {
    /**
     * Get the documentation string for the specified method.
     *
     * @param method the method name.
     * @return the docstring for the method or null.
     */
    @Nullable
    String doc(String method);
}
