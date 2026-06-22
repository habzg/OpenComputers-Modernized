package li.cil.oc.api.event;

import net.minecraft.world.entity.player.Player;

/**
 * Fired when an analyzer is used on a robot.
 * <br>
 * Use this to echo additional information for custom components.
 */
public interface RobotAnalyzeEvent extends RobotEvent {
    /**
     * The player that used the analyzer.
     */
    @SuppressWarnings("unused")
    Player player();
}
