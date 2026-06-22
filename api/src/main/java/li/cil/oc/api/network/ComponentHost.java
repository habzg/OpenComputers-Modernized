package li.cil.oc.api.network;

/**
 * A more in-depth implementation of an environment host, also providing
 * access to the hosted environments.
 */
@SuppressWarnings("unused")
public interface ComponentHost extends EnvironmentHost {
    /**
     * The list of components active in the component host.
     *
     * @return the list of components.
     */
    @SuppressWarnings("unused")
    Iterable<Environment> getComponents();
}
