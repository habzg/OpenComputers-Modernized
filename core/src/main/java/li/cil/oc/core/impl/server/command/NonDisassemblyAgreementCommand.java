package li.cil.oc.core.impl.server.command;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.command.SimpleCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class NonDisassemblyAgreementCommand extends SimpleCommand {
    public static final NonDisassemblyAgreementCommand INSTANCE = new NonDisassemblyAgreementCommand();

    private NonDisassemblyAgreementCommand() {
        super("oc_preventDisassembling");
        aliases.add("oc_nodis");
        aliases.add("oc_prevdis");
    }

    @Override
    protected int execute(CommandSourceStack source, String[] args) {
        if (source.getEntity() != null && !(source.getEntity() instanceof Player)) {
            source.sendFailure(Component.literal("Can only be used by players."));
            return 0;
        }
        if (source.getEntity() instanceof Player player) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty()) {
                CustomData _cd = stack.get(DataComponents.CUSTOM_DATA);
                CompoundTag nbt = _cd != null ? _cd.copyTag() : new CompoundTag();
                boolean preventDisassembly = args.length > 0 ?
                        Boolean.parseBoolean(args[0]) :
                        !nbt.getBoolean(OCSettings.namespace + "undisassemblable");
                if (preventDisassembly) {
                    nbt.putBoolean(OCSettings.namespace + "undisassemblable", true);
                } else {
                    nbt.remove(OCSettings.namespace + "undisassemblable");
                }
                if (nbt.isEmpty()) {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                } else {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                }
            }
        } else {
            source.sendFailure(Component.literal("Can only be used by players."));
        }
        return 0;
    }
}
