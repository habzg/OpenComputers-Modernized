package li.cil.oc.core.impl.server.command;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.command.SimpleCommand;
import net.minecraft.commands.CommandSourceStack;

public class WirelessRenderingCommand extends SimpleCommand {
    public static final WirelessRenderingCommand INSTANCE = new WirelessRenderingCommand();

    private WirelessRenderingCommand() {
        super("oc_renderWirelessNetwork");
        aliases.add("oc_wlan");
    }

    @Override
    protected int execute(CommandSourceStack source, String[] args) {
        if (source.getEntity() != null && !(source.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Can only be used by players."));
            return 0;
        }
        OCSettings.rTreeDebugRenderer = args.length > 0 ?
                Boolean.parseBoolean(args[0]) : !OCSettings.rTreeDebugRenderer;
        return 0;
    }
}
