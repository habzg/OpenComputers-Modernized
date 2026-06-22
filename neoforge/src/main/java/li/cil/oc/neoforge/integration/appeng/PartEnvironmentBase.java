package li.cil.oc.neoforge.integration.appeng;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.externalstorage.GenericStackInv;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface PartEnvironmentBase extends ManagedEnvironment {
    IPartHost partHost();

    default Object[] getPartConfig(Context ignoredContext, Arguments args) {
        Direction side = ExtendedArguments.checkSideAny(args, 0);
        IPart part = partHost().getPart(side);
        if (!(part instanceof IConfigInvHost configHost)) {
            return ResultWrapper.result(null, "no matching part");
        }
        GenericStackInv config = configHost.getConfig();
        int slot = Math.max(0, args.optInteger(1, 1) - 1);
        if (slot >= config.size()) return ResultWrapper.result(ItemStack.EMPTY);
        var stack = config.getStack(slot);
        if (stack != null && stack.what() instanceof AEItemKey itemKey) {
            return ResultWrapper.result(itemKey.toStack((int) stack.amount()));
        }
        return ResultWrapper.result(ItemStack.EMPTY);
    }

    default Object[] setPartConfig(Context context, Arguments args) {
        Direction side = ExtendedArguments.checkSideAny(args, 0);
        IPart part = partHost().getPart(side);
        if (!(part instanceof IConfigInvHost configHost)) {
            return ResultWrapper.result(null, "no matching part");
        }
        GenericStackInv config = configHost.getConfig();
        int slot;
        int valOffset;
        if (args.isInteger(1)) {
            slot = args.checkInteger(1) - 1;
            valOffset = 2;
        } else {
            slot = 0;
            valOffset = 1;
        }
        if (slot < 0 || slot >= config.size()) {
            throw new IllegalArgumentException("invalid slot");
        }
        ItemStack stack;
        if (args.count() > 2) {
            stack = DatabaseAccess.getStackFromDatabase(node(), args, valOffset);
        } else {
            stack = ItemStack.EMPTY;
        }
        if (stack != null && !stack.isEmpty()) {
            config.setStack(slot, GenericStack.fromItemStack(stack));
        } else {
            config.setStack(slot, null);
        }
        context.pause(0.5);
        return ResultWrapper.result(true);
    }
}
