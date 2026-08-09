package li.cil.oc.core.impl.server.command;

import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.core.impl.common.command.SimpleCommand;
import net.minecraft.commands.CommandSourceStack;

public class NetworkProfilingCommand extends SimpleCommand {
    public static final NetworkProfilingCommand INSTANCE = new NetworkProfilingCommand();

    private NetworkProfilingCommand() {
        super("oc_profileNetwork");
        aliases.add("oc_pn");
    }

    @Override
    protected int execute(CommandSourceStack source, String[] args) {
        if (source.getEntity() != null && !(source.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Can only be used by players."));
            return 0;
        }
        PacketBuilderBase.isProfilingEnabled = args.length > 0 ?
                Boolean.parseBoolean(args[0]) : !PacketBuilderBase.isProfilingEnabled;
        return 0;
    }
}
