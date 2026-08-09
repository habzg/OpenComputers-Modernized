package li.cil.oc.core.impl.common.component.traits;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.common.component.TextBufferBase;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.core.util.ExtendedUnicodeHelper;

public interface TextBufferProxy extends li.cil.oc.api.internal.TextBuffer {
    li.cil.oc.core.impl.util.TextBuffer data();

    @Override
    default int getWidth() {
        return data().width;
    }

    @Override
    default int getHeight() {
        return data().height;
    }

    @Override
    default boolean setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
        if (depth.ordinal() > getMaximumColorDepth().ordinal())
            throw new IllegalArgumentException("unsupported depth");
        data().format_$eq(PackedColor.Depth.format(depth));
        return true;
    }

    @Override
    default li.cil.oc.api.internal.TextBuffer.ColorDepth getColorDepth() {
        return data().format().depth();
    }

    default void onBufferPaletteChange(int index) {
    }

    @Override
    default void setPaletteColor(int index, int color) {
        if (data().format() instanceof PackedColor.MutablePaletteFormat palette) {
            palette.set(index, color);
            onBufferPaletteChange(index);
        } else throw new RuntimeException("palette not available");
    }

    @Override
    default int getPaletteColor(int index) {
        if (data().format() instanceof PackedColor.MutablePaletteFormat palette) {
            return palette.get(index);
        } else throw new RuntimeException("palette not available");
    }

    default void onBufferColorChange() {
    }

    @Override
    default void setForegroundColor(int color, boolean isFromPalette) {
        PackedColor.Color value = new PackedColor.Color(color, isFromPalette);
        if (value.isPalette() && !(data().format() instanceof PackedColor.PaletteFormat)) {
            value = new PackedColor.Color(color, false);
        }
        if (data().foreground().value() != value.value()) {
            data().foreground_$eq(value);
            onBufferColorChange();
        }
    }

    @Override
    default int getForegroundColor() {
        return data().foreground().value();
    }

    @Override
    default void setForegroundColor(int color) {
        setForegroundColor(color, false);
    }

    @Override
    default boolean isForegroundFromPalette() {
        return data().foreground().isPalette();
    }

    @Override
    default void setBackgroundColor(int color, boolean isFromPalette) {
        PackedColor.Color value = new PackedColor.Color(color, isFromPalette);
        if (value.isPalette() && !(data().format() instanceof PackedColor.PaletteFormat)) {
            value = new PackedColor.Color(color, false);
        }
        if (data().background().value() != value.value()) {
            data().background_$eq(value);
            onBufferColorChange();
        }
    }

    @Override
    default int getBackgroundColor() {
        return data().background().value();
    }

    @Override
    default void setBackgroundColor(int color) {
        setBackgroundColor(color, false);
    }

    @Override
    default boolean isBackgroundFromPalette() {
        return data().background().isPalette();
    }

    default void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
    }

    default void copy(int col, int row, int w, int h, int tx, int ty) {
        if (data().copy(col, row, w, h, tx, ty))
            onBufferCopy(col, row, w, h, tx, ty);
    }

    default void onBufferFill(int col, int row, int w, int h, int c) {
    }

    default void fill(int col, int row, int w, int h, int c) {
        if (data().fill(col, row, w, h, c))
            onBufferFill(col, row, w, h, c);
    }

    default void onBufferSet(int col, int row, String s, boolean vertical) {
    }

    default String truncate(String s, int sLength, int leftOffset, int maxWidth) {
        int subFrom = s.offsetByCodePoints(0, leftOffset);
        int width = Math.min(sLength, maxWidth);
        if (width <= 0) return "";
        else if ((sLength - leftOffset) <= width) return s;
        else return s.substring(subFrom, s.offsetByCodePoints(subFrom, width));
    }

    @SuppressWarnings("SuspiciousNameCombination")
    default void set(int col, int row, String s, boolean vertical) {
        int sLength = ExtendedUnicodeHelper.length(s);
        if (col < data().width && (col >= 0 || -col < sLength)) {
            int x, y;
            String truncated;
            if (vertical) {
                if (row < 0) {
                    x = col;
                    y = 0;
                    truncated = truncate(s, sLength, -row, data().height);
                } else {
                    x = col;
                    y = row;
                    truncated = truncate(s, sLength, 0, data().height - row);
                }
            } else {
                if (col < 0) {
                    x = 0;
                    y = row;
                    truncated = truncate(s, sLength, -col, data().width);
                } else {
                    x = col;
                    y = row;
                    truncated = truncate(s, sLength, 0, data().width - col);
                }
            }
            if (data().set(x, y, truncated, vertical))
                onBufferSet(x, row, truncated, vertical);
        }
    }

    default int getCodePoint(int col, int row) {
        return data().get(col, row);
    }

    @Override
    default int getForegroundColor(int column, int row) {
        if (isForegroundFromPalette(column, row))
            return PackedColor.extractForeground(color(column, row));
        else
            return PackedColor.unpackForeground(color(column, row), data().format());
    }

    @Override
    default boolean isForegroundFromPalette(int column, int row) {
        return data().format().isFromPalette(PackedColor.extractForeground(color(column, row)));
    }

    @Override
    default int getBackgroundColor(int column, int row) {
        if (isBackgroundFromPalette(column, row))
            return PackedColor.extractBackground(color(column, row));
        else
            return PackedColor.unpackBackground(color(column, row), data().format());
    }

    @Override
    default boolean isBackgroundFromPalette(int column, int row) {
        return data().format().isFromPalette(PackedColor.extractBackground(color(column, row)));
    }

    @Override
    default void rawSetText(int col, int row, int[][] text) {
        for (int y = row; y < Math.min(row + text.length, data().height); y++) {
            int[] line = text[y - row];
            System.arraycopy(line, 0, data().buffer[y], col, Math.min(line.length, data().width - col));
        }
    }

    @Override
    default void rawSetForeground(int col, int row, int[][] color) {
        for (int y = row; y < Math.min(row + color.length, data().height); y++) {
            int[] line = color[y - row];
            for (int x = col; x < Math.min(col + line.length, data().width); x++) {
                int packedBackground = data().color[y][x] & 0x00FF;
                int packedForeground = (data().format().deflate(new PackedColor.Color(line[x - col])) << PackedColor.ForegroundShift) & 0xFF00;
                data().color[y][x] = (short) (packedForeground | packedBackground);
            }
        }
    }

    @Override
    default void rawSetBackground(int col, int row, int[][] color) {
        for (int y = row; y < Math.min(row + color.length, data().height); y++) {
            int[] line = color[y - row];
            for (int x = col; x < Math.min(col + line.length, data().width); x++) {
                int packedBackground = data().format().deflate(new PackedColor.Color(line[x - col])) & 0x00FF;
                int packedForeground = data().color[y][x] & 0xFF00;
                data().color[y][x] = (short) (packedForeground | packedBackground);
            }
        }
    }

    default short color(int column, int row) {
        if (column < 0 || column >= getWidth() || row < 0 || row >= getHeight())
            throw new IndexOutOfBoundsException();
        return data().color[row][column];
    }

    @Override
    @SuppressWarnings("unused")
    default boolean renderText(PoseStack stack) {
        if (this instanceof TextBufferBase base && base.hasLitContent()) {
            boolean wasDirty = base.isBufferDirty();
            if (wasDirty) {
                for (int[] line : data().buffer) {
                    TextBufferRenderCache.renderer.generateChars(line);
                }
                base.clearBufferDirty();
            }
            return wasDirty;
        }
        return false;
    }
}
