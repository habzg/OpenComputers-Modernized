package li.cil.oc.core.impl.server.command;

import li.cil.oc.core.impl.Settings;
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
        Settings.rTreeDebugRenderer = args.length > 0 ?
                Boolean.parseBoolean(args[0]) : !Settings.rTreeDebugRenderer;
        return 0;
    }
}
