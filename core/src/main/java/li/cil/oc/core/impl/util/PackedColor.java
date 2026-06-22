package li.cil.oc.core.impl.util;

import li.cil.oc.api.Persistable;
import li.cil.oc.core.impl.Settings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public final class PackedColor {

    public static final int ForegroundShift = 8;
    public static final int BackgroundMask = 0x000000FF;
    private static final int rShift32 = 16;
    private static final int gShift32 = 8;
    private static final int bShift32 = 0;

    public static int[] extract(int value) {
        int r = (value >>> rShift32) & 0xFF;
        int g = (value >>> gShift32) & 0xFF;
        int b = (value >>> bShift32) & 0xFF;
        return new int[]{r, g, b};
    }

    public static short pack(Color foreground, Color background, ColorFormat format) {
        return (short) (((format.deflate(foreground) & 0xFF) << ForegroundShift) | (format.deflate(background) & 0xFF));
    }

    public static int extractForeground(short color) {
        return (color & 0xFFFF) >>> ForegroundShift;
    }

    public static int extractBackground(short color) {
        return color & BackgroundMask;
    }

    public static int unpackForeground(short color, ColorFormat format) {
        return format.inflate(extractForeground(color));
    }

    public static int unpackBackground(short color, ColorFormat format) {
        return format.inflate(extractBackground(color));
    }

    public static final class Depth {
        public static int bits(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
            return switch (depth) {
                case OneBit -> 1;
                case FourBit -> 4;
                case EightBit -> 8;
            };
        }

        public static ColorFormat format(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
            return switch (depth) {
                case OneBit -> SingleBitFormat.INSTANCE;
                case FourBit -> new MutablePaletteFormat();
                case EightBit -> new HybridFormat();
            };
        }
    }

    public abstract static class ColorFormat implements Persistable {
        public abstract li.cil.oc.api.internal.TextBuffer.ColorDepth depth();

        public abstract int inflate(int value);

        public boolean isFromPalette(int value) {
            return false;
        }

        public void validate(Color value) {
            if (value.isPalette) {
                throw new IllegalArgumentException("color palette not supported");
            }
        }

        public abstract byte deflate(Color value);

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        }

    }

    public static class SingleBitFormat extends ColorFormat {
        public static final SingleBitFormat INSTANCE = new SingleBitFormat(Settings.get().monochromeColor);
        public final int color;

        public SingleBitFormat(int color) {
            this.color = color;
        }

        @Override
        public li.cil.oc.api.internal.TextBuffer.ColorDepth depth() {
            return li.cil.oc.api.internal.TextBuffer.ColorDepth.OneBit;
        }

        @Override
        public int inflate(int value) {
            return value == 0 ? 0x000000 : color;
        }

        @Override
        public byte deflate(Color value) {
            return (byte) (value.value == 0 ? 0 : 1);
        }
    }

    public abstract static class PaletteFormat extends ColorFormat {
        protected static double delta(int colorA, int colorB) {
            int[] a = extract(colorA);
            int[] b = extract(colorB);
            double dr = a[0] - b[0];
            double dg = a[1] - b[1];
            double db = a[2] - b[2];
            return 0.2126 * dr * dr + 0.7152 * dg * dg + 0.0722 * db * db;
        }

        @Override
        public int inflate(int value) {
            return palette()[Math.clamp(value, 0, palette().length - 1)];
        }

        @Override
        public boolean isFromPalette(int value) {
            return true;
        }

        @Override
        public void validate(Color value) {
            if (value.isPalette && (value.value < 0 || value.value >= palette().length)) {
                throw new IllegalArgumentException("invalid palette index");
            }
        }

        @Override
        public byte deflate(Color value) {
            if (value.isPalette) {
                return (byte) (Math.max(0, value.value) % palette().length);
            }
            int[] palette = palette();
            int bestIdx = 0;
            double bestDelta = Double.MAX_VALUE;
            for (int i = 0; i < palette.length; i++) {
                double d = delta(value.value, palette[i]);
                if (d < bestDelta) {
                    bestDelta = d;
                    bestIdx = i;
                }
            }
            return (byte) bestIdx;
        }

        protected abstract int[] palette();
    }

    public static class MutablePaletteFormat extends PaletteFormat {
        protected final int[] palette = {
                0xFFFFFF, 0xFFCC33, 0xCC66CC, 0x6699FF,
                0xFFFF33, 0x33CC33, 0xFF6699, 0x333333,
                0xCCCCCC, 0x336699, 0x9933CC, 0x333399,
                0x663300, 0x336600, 0xFF3333, 0x000000
        };

        @Override
        public li.cil.oc.api.internal.TextBuffer.ColorDepth depth() {
            return li.cil.oc.api.internal.TextBuffer.ColorDepth.FourBit;
        }

        public int get(int index) {
            return palette[index];
        }

        public void set(int index, int value) {
            palette[index] = value;
        }

        @Override
        protected int[] palette() {
            return palette;
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            int[] loaded = nbt.getIntArray("palette");
            System.arraycopy(loaded, 0, palette, 0, Math.min(loaded.length, palette.length));
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            nbt.putIntArray("palette", palette);
        }
    }

    public static class HybridFormat extends MutablePaletteFormat {
        private static final int reds = 6;
        private static final int greens = 8;
        private static final int blues = 5;

        private final int[] staticPalette = new int[240];

        public HybridFormat() {
            for (int index = 0; index < staticPalette.length; index++) {
                int idxB = index % blues;
                int idxG = (index / blues) % greens;
                int idxR = (index / blues / greens) % reds;
                int r = (int) (idxR * 0xFF / (reds - 1.0) + 0.5);
                int g = (int) (idxG * 0xFF / (greens - 1.0) + 0.5);
                int b = (int) (idxB * 0xFF / (blues - 1.0) + 0.5);
                staticPalette[index] = (r << rShift32) | (g << gShift32) | (b << bShift32);
            }
            for (int i = 0; i < palette.length; i++) {
                int shade = 0xFF * (i + 1) / (palette.length + 1);
                palette[i] = (shade << rShift32) | (shade << gShift32) | (shade << bShift32);
            }
        }

        @Override
        public li.cil.oc.api.internal.TextBuffer.ColorDepth depth() {
            return li.cil.oc.api.internal.TextBuffer.ColorDepth.EightBit;
        }

        @Override
        public int inflate(int value) {
            if (isFromPalette(value)) return super.inflate(value);
            return staticPalette[(value - palette.length) % 240];
        }

        @Override
        public byte deflate(Color value) {
            byte paletteIndex = super.deflate(value);
            if (value.isPalette) return paletteIndex;
            int[] rgb = extract(value.value);
            int idxR = (int) (rgb[0] * (reds - 1.0) / 0xFF + 0.5);
            int idxG = (int) (rgb[1] * (greens - 1.0) / 0xFF + 0.5);
            int idxB = (int) (rgb[2] * (blues - 1.0) / 0xFF + 0.5);
            byte deflated = (byte) (palette.length + idxR * greens * blues + idxG * blues + idxB);
            if (delta(inflate(deflated & 0xFF), value.value) < delta(inflate(paletteIndex & 0xFF), value.value)) {
                return deflated;
            }
            return paletteIndex;
        }

        @Override
        public boolean isFromPalette(int value) {
            return value >= 0 && value < palette.length;
        }
    }

    public record Color(int value, boolean isPalette) {
        public Color(int value) {
            this(value, false);
        }

    }
}
