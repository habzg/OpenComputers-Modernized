package li.cil.oc.core.impl.server.command;

import li.cil.oc.core.impl.common.command.SimpleCommand;
import li.cil.oc.core.impl.common.nanomachines.ControllerImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class DebugNanomachinesCommand extends SimpleCommand {
    public static final DebugNanomachinesCommand INSTANCE = new DebugNanomachinesCommand();

    private DebugNanomachinesCommand() {
        super("oc_debugNanomachines");
        aliases.add("oc_dn");
    }

    @Override
    protected int execute(CommandSourceStack source, String[] args) {
        if (source.getEntity() != null && !(source.getEntity() instanceof Player)) {
            source.sendFailure(Component.literal("Can only be used by players."));
            return 0;
        }
        if (source.getEntity() instanceof Player player) {
            Object controller = li.cil.oc.api.Nanomachines.installController(player);
            if (controller instanceof ControllerImpl c) {
                c.debug();
                source.sendSuccess(() -> Component.literal("Debug configuration created, see log for mappings."), false);
            }
        } else {
            source.sendFailure(Component.literal("Can only be used by players."));
        }
        return 0;
    }
}
