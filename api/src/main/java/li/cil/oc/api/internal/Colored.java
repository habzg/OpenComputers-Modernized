package li.cil.oc.api.internal;

/**
 * This interface is implemented by tile entities that can be colored by
 * players, such as screens, computer cases and cables.
 * <br>
 * Colors are in the common <code>RRGGBB</code> format.
 * <br>
 * This interface is <em>not meant to be implemented</em>, just used.
 */
public interface Colored {
    /**
     * Get the current color value.
     *
     * @return the current color value.
     */
    @SuppressWarnings("unused")
    int getColor();

    /**
     * Set the color value.
     *
     * @param value the new color value.
     */
    @SuppressWarnings("unused")
    void setColor(int value);

    /**
     * Whether colors on this block affect connectivity (i.e. cables only
     * connect to cables of the same color).
     *
     * @return {@code true} if color affects connectivity.
     */
    @SuppressWarnings("unused")
    default boolean controlsConnectivity() {
        return false;
    }
}
