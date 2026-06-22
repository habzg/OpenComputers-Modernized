package li.cil.oc.core.impl.server.command;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Packet;
import li.cil.oc.core.impl.common.command.SimpleCommand;
import li.cil.oc.core.server.network.DebugNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;


public class SendDebugMessageCommand extends SimpleCommand {
    public static final SendDebugMessageCommand INSTANCE = new SendDebugMessageCommand();

    private SendDebugMessageCommand() {
        super("oc_sendDebugMessage");
        aliases.add("oc_sdbg");
    }

    @Override
    protected int execute(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            source.sendFailure(Component.literal("no destination address specified."));
            return 0;
        }
        String destination = args[0];
        var endpoint = DebugNetwork.getEndpoint(destination);
        if (endpoint != null) {
            String[] rest = new String[args.length - 1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            Packet packet = Network.newPacket(source.getTextName(), destination, 0, rest);
            endpoint.receivePacket(packet);
        }
        return 0;
    }
}
