package li.cil.oc.neoforge.server.command;

import li.cil.oc.core.impl.common.command.SimpleCommand;
import li.cil.oc.neoforge.common.nanomachines.ControllerImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class LogNanomachinesCommand extends SimpleCommand {
    public static final LogNanomachinesCommand INSTANCE = new LogNanomachinesCommand();

    private LogNanomachinesCommand() {
        super("oc_nanomachines");
        aliases.add("oc_nm");
    }

    @Override
    protected int execute(CommandSourceStack source, String[] args) {
        Player player;
        if (args.length > 0) {
            String playerName = args[0];
            player = source.getServer().getPlayerList().getPlayerByName(playerName);
        } else if (source.getEntity() instanceof Player p) {
            player = p;
        } else {
            source.sendFailure(Component.literal("Player entity not found."));
            return 0;
        }
        if (player == null) {
            source.sendFailure(Component.literal("Player entity not found."));
            return 0;
        }
        Object controller = li.cil.oc.api.Nanomachines.installController(player);
        if (controller instanceof ControllerImpl c) {
            c.print();
        }
        return 0;
    }
}
