package li.cil.oc.neoforge.integration.mekanism;

import java.util.Map;

import li.cil.oc.api.machine.Arguments;
import mekanism.common.integration.computer.BaseComputerHelper;
import mekanism.common.integration.computer.ComputerException;
import org.jetbrains.annotations.NotNull;

public class OCComputerHelper extends BaseComputerHelper {

    /**
     * Marker returned for void methods; the caller translates this into an
     * empty result instead of a single nil value.
     */
    public static final Object VOID = new Object();

    private final Arguments arguments;

    public OCComputerHelper(final Arguments arguments) {
        this.arguments = arguments;
    }

    @Override
    public boolean getBoolean(final int param) throws ComputerException {
        try {
            return arguments.checkBoolean(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public byte getByte(final int param) throws ComputerException {
        try {
            return (byte) arguments.checkInteger(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public short getShort(final int param) throws ComputerException {
        try {
            return (short) arguments.checkInteger(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public int getInt(final int param) throws ComputerException {
        try {
            return arguments.checkInteger(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public long getLong(final int param) throws ComputerException {
        try {
            return (long) arguments.checkDouble(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public char getChar(final int param) throws ComputerException {
        try {
            return arguments.checkString(param).charAt(0);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public float getFloat(final int param) throws ComputerException {
        try {
            return (float) arguments.checkDouble(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public double getDouble(final int param) throws ComputerException {
        try {
            return arguments.checkDouble(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public @NotNull String getString(final int param) throws ComputerException {
        try {
            return arguments.checkString(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public @NotNull Map<?, ?> getMap(final int param) throws ComputerException {
        try {
            return arguments.checkTable(param);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new ComputerException(e.getMessage());
        }
    }

    @Override
    public Object voidResult() {
        return VOID;
    }
}
