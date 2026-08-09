package li.cil.oc.api.internal;

/**
 * Implemented by objects with multiple internal tanks.
 * <br>
 * This is specifically for containers where the side does not matter when
 * accessing the internal tanks, only the index of the tank; unlike with a
 * fluid handler interface which also considers the side.
 */
public interface MultiTank {
    /**
     * The number of tanks currently installed.
     */
    int tankCount();

    /**
     * Get the installed fluid tank with the specified index.
     *
     * @param index the index of the tank to get.
     * @return the tank with the specified index.
     */
    Object getFluidTank(int index);
}
