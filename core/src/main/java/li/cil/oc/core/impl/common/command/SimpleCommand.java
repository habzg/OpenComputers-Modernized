package li.cil.oc.core.impl.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public abstract class SimpleCommand {
    protected final String name;
    protected final List<String> aliases = new ArrayList<>();

    protected SimpleCommand(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public List<String> getAliases() {
        return aliases;
    }

    public boolean checkPermission(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    @SuppressWarnings("SameReturnValue")
    protected abstract int execute(CommandSourceStack source, String[] args) ;

    private LiteralArgumentBuilder<CommandSourceStack> createBuilder(String literalName) {
        return Commands.literal(literalName)
                .requires(this::checkPermission)
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(context -> {
                            String argsStr = StringArgumentType.getString(context, "args");
                            String[] args = argsStr.isEmpty() ? new String[0] : argsStr.split(" ");
                            return execute(context.getSource(), args);
                        }))
                .executes(context -> execute(context.getSource(), new String[0]));
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(createBuilder(name));
        for (String alias : aliases) {
            dispatcher.register(createBuilder(alias));
        }
    }
}
