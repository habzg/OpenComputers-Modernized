package li.cil.oc.neoforge.integration.mekanism;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.DocumentedPeripheral;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.core.util.ResultWrapper;
import mekanism.common.integration.computer.BoundMethodHolder;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.Convertable;
import mekanism.common.integration.computer.IComputerTile;
import mekanism.common.integration.computer.MethodHelpData;

public class EnvironmentMekanismMachine extends ManagedBlockEntityEnvironment<IComputerTile> implements ManagedPeripheral, NamedBlock, DocumentedPeripheral {

    private final Holder holder = new Holder();

    public EnvironmentMekanismMachine(final IComputerTile tile) {
        super(tile, componentName(tile));
        tile.getComputerMethods(holder);
    }

    @Override
    public String doc(final String method) {
        final var candidates = holder.methods(method);
        if (candidates.isEmpty()) {
            return "";
        }
        final var data = candidates.iterator().next();
        if (data.isHelpMethod()) {
            return "";
        }
        final MethodHelpData help = MethodHelpData.from(data);
        final StringBuilder sb = new StringBuilder("function(");
        final List<MethodHelpData.Param> params = help.params();
        if (params != null && !params.isEmpty()) {
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                final MethodHelpData.Param param = params.get(i);
                sb.append(param.name()).append(": ").append(shortType(param.type()));
            }
        }
        sb.append(")");
        final String returnType = shortType(help.returns().type());
        if (!"nothing".equals(returnType)) {
            sb.append(": ").append(returnType);
        }
        final String description = help.description();
        if (description != null && !description.isEmpty()) {
            sb.append(" -- ").append(description.trim());
        }
        return sb.toString();
    }

    private static String shortType(final String type) {
        final int space = type.indexOf(' ');
        final String first = space < 0 ? type : type.substring(0, space);
        return first.toLowerCase(Locale.ROOT);
    }

    @Override
    public String preferredName() {
        return componentName(BlockEntity);
    }

    @Override
    public int priority() {
        return 1;
    }

    private static String componentName(final IComputerTile tile) {
        final String name = tile.getComputerName();
        if (name != null && !name.isEmpty()) {
            return name;
        }
        if (tile instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
            final var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
          return key.toString();
        }
        return "mekanism_machine";
    }

    @Override
    public String[] methods() {
        return holder.methodNames();
    }

    @Override
    public Object[] invoke(final String method, final Context context, final Arguments args) {
        final Collection<BoundMethodHolder.BoundMethodData<?>> candidates = holder.methods(method);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("no such method: " + method);
        }
        final BoundMethodHolder.BoundMethodData<?> data = select(candidates, args.count());
        try {
            final Object result = call(data, new OCComputerHelper(args));
            return result == OCComputerHelper.VOID ? ResultWrapper.result() : ResultWrapper.result(result);
        } catch (ComputerException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private static BoundMethodHolder.BoundMethodData<?> select(final Collection<BoundMethodHolder.BoundMethodData<?>> candidates, final int argCount) {
        for (final BoundMethodHolder.BoundMethodData<?> md : candidates) {
            if (md.argumentNames().length == argCount) {
                return md;
            }
        }
        final StringBuilder expected = new StringBuilder();
        for (final BoundMethodHolder.BoundMethodData<?> md : candidates) {
            if (!expected.isEmpty()) {
                expected.append(" or ");
            }
            expected.append(md.argumentNames().length);
        }
        throw new IllegalArgumentException(String.format("Found %d arguments, expected %s", argCount, expected));
    }

    private static Object call(final BoundMethodHolder.BoundMethodData<?> data, final OCComputerHelper helper) throws ComputerException {
        final Object result = data.call(helper);
        if (result instanceof Convertable<?> convertable) {
            return convertable.convert(helper);
        }
        return result;
    }

    private static final class Holder extends BoundMethodHolder {
        String[] methodNames() {
            return methodNames.get();
        }

        Collection<BoundMethodHolder.BoundMethodData<?>> methods(final String name) {
            return methods.get(name);
        }
    }
}
