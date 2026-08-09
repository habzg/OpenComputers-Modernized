package li.cil.oc.core.impl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;

public final class NbtDataStream {

    public static boolean getShortArray(CompoundTag nbt, String key, short[][] array2d, int w, int h) {
        if (!nbt.contains(key)) {
            return false;
        }

        byte[] rawBytes = nbt.getByteArray(key);
        DataInputStream memReader = new DataInputStream(new ByteArrayInputStream(rawBytes));
        try {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (memReader.available() < 2) {
                        return true;
                    }
                    array2d[y][x] = memReader.readShort();
                }
            }
        } catch (IOException e) {
            return true;
        }
        return true;
    }

    public static void getIntArrayLegacy(CompoundTag nbt, String key, short[][] array2d, int w, int h) {
        if (!nbt.contains(key)) {
            return;
        }

        int[] c = nbt.getIntArray(key);
        for (int y = 0; y < h; y++) {
            short[] rowColor = array2d[y];
            for (int x = 0; x < w; x++) {
                int index = x + y * w;
                if (index >= c.length) {
                    return;
                }
                rowColor[x] = (short) c[index];
            }
        }
    }

    public static void setShortArray(CompoundTag nbt, String key, short[] array) {
        ByteArrayOutputStream rawByteWriter = new ByteArrayOutputStream();
        DataOutputStream memWriter = new DataOutputStream(rawByteWriter);
        try {
            for (short value : array) {
                memWriter.writeShort(value);
            }
        } catch (IOException ignored) {}
        nbt.putByteArray(key, rawByteWriter.toByteArray());
    }
}
