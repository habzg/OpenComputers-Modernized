package li.cil.oc.api.event;

/**
 * This event is fired when a robot tries to place a block and has no point of
 * reference, i.e. the place would have to be placed in "thin air". Per default
 * this fails (because players can't do this, either).
 * <br>
 * This is primarily intended for the 'Angel Upgrade', but it might be useful
 * for other upgrades, too.
 */
public interface RobotPlaceInAirEvent extends RobotEvent {
    /**
     * Whether the placement is allowed. Defaults to <code>false</code>.
     */
    boolean isAllowed();

    /**
     * Set whether the placement is allowed, can be used to allow robots to
     * place blocks in thin air.
     */
    @SuppressWarnings("unused")
    void setAllowed(boolean value);
}
