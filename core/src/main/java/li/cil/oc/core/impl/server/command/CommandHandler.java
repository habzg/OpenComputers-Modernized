package li.cil.oc.core.impl.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public final class CommandHandler {
    private CommandHandler() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        DebugNanomachinesCommand.INSTANCE.register(dispatcher);
        LogNanomachinesCommand.INSTANCE.register(dispatcher);
        NetworkProfilingCommand.INSTANCE.register(dispatcher);
        NonDisassemblyAgreementCommand.INSTANCE.register(dispatcher);
        WirelessRenderingCommand.INSTANCE.register(dispatcher);
        SpawnComputerCommand.INSTANCE.register(dispatcher);
        DebugWhitelistCommand.INSTANCE.register(dispatcher);
        SendDebugMessageCommand.INSTANCE.register(dispatcher);
    }
}
