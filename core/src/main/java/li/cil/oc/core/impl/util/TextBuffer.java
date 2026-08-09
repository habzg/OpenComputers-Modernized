package li.cil.oc.core.impl.util;

import java.util.Arrays;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.util.ExtendedUnicodeHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class TextBuffer {
    public int width;
    public int height;
    public short[][] color;
    public int[][] buffer;
    private PackedColor.ColorFormat _format;
    private PackedColor.Color _foreground = new PackedColor.Color(0xFFFFFF);
    private PackedColor.Color _background = new PackedColor.Color(0x000000);
    private short packed;

    public TextBuffer(int width, int height, PackedColor.ColorFormat initialFormat) {
        this.width = width;
        this.height = height;
        this._format = initialFormat;
        this.packed = PackedColor.pack(_foreground, _background, _format);
        this.color = new short[height][width];
        this.buffer = new int[height][width];
        for (int y = 0; y < height; y++) {
            Arrays.fill(color[y], packed);
            Arrays.fill(buffer[y], 0x20);
        }
    }

    public TextBuffer(int[] size, PackedColor.ColorFormat format) {
        this(size[0], size[1], format);
    }

    public PackedColor.Color foreground() {
        return _foreground;
    }

    @SuppressWarnings("UnusedReturnValue")
    public TextBuffer foreground_$eq(PackedColor.Color value) {
        _format.validate(value);
        _foreground = value;
        packed = PackedColor.pack(_foreground, _background, _format);
        return this;
    }

    public PackedColor.Color background() {
        return _background;
    }

    @SuppressWarnings("UnusedReturnValue")
    public TextBuffer background_$eq(PackedColor.Color value) {
        _format.validate(value);
        _background = value;
        packed = PackedColor.pack(_foreground, _background, _format);
        return this;
    }

    public PackedColor.ColorFormat format() {
        return _format;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean format_$eq(PackedColor.ColorFormat value) {
        if (_format.depth() != value.depth()) {
            for (int row = 0; row < height; row++) {
                short[] rowColor = color[row];
                for (int col = 0; col < width; col++) {
                    short p = rowColor[col];
                    PackedColor.Color fg = new PackedColor.Color(PackedColor.unpackForeground(p, _format));
                    PackedColor.Color bg = new PackedColor.Color(PackedColor.unpackBackground(p, _format));
                    rowColor[col] = PackedColor.pack(fg, bg, value);
                }
            }
            _format = value;
            packed = PackedColor.pack(_foreground, _background, _format);
            return true;
        }
        return false;
    }

    public int[] size() {
        return new int[]{width, height};
    }

    public void size_$eq(int[] value) {
        int iw = value[0], ih = value[1];
        int w = Math.max(iw, 1), h = Math.max(ih, 1);
        if (width != w || height != h) {
            int[][] newBuffer = new int[h][w];
            short[][] newColor = new short[h][w];
            for (int y = 0; y < h; y++) {
                Arrays.fill(newBuffer[y], 0x20);
                Arrays.fill(newColor[y], packed);
            }
            for (int y = 0; y < Math.min(h, height); y++) {
                System.arraycopy(buffer[y], 0, newBuffer[y], 0, Math.min(w, width));
                System.arraycopy(color[y], 0, newColor[y], 0, Math.min(w, width));
            }
            buffer = newBuffer;
            color = newColor;
            width = w;
            height = h;
        }
    }

    public int get(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height)
            throw new IndexOutOfBoundsException();
        return buffer[row][col];
    }

    public boolean set(int col, int row, String s, boolean vertical) {
        int sLength = ExtendedUnicodeHelper.length(s);
        if (vertical) {
            if (col < 0 || col >= width) return false;
            boolean changed = false;
            int cx = 0;
            for (int y = row; y < Math.min(row + sLength, height); y++) {
                if (y >= 0) {
                    int[] line = buffer[y];
                    short[] lineColor = color[y];
                    int c = s.codePointAt(cx);
                    changed = changed || (line[col] != c) || (lineColor[col] != packed);
                    setChar(line, lineColor, col, c);
                    cx = s.offsetByCodePoints(cx, 1);
                }
            }
            return changed;
        } else {
            if (row < 0 || row >= height) return false;
            boolean changed = false;
            int[] line = buffer[row];
            short[] lineColor = color[row];
            int bx = Math.max(col, 0);
            int cx = 0;
            for (int x = bx; x < Math.min(col + sLength, width) && bx < line.length; x++) {
                int c = s.codePointAt(cx);
                changed = changed || (line[bx] != c) || (lineColor[bx] != packed);
                setChar(line, lineColor, bx, c);
                bx += Math.max(1, FontUtils.wcwidth(c));
                cx = s.offsetByCodePoints(cx, 1);
            }
            return changed;
        }
    }

    public boolean fill(int col, int row, int w, int h, int c) {
        if (w <= 0 || h <= 0) return false;
        if (col + w < 0 || row + h < 0 || col >= width || row >= height) return false;
        boolean changed = false;
        for (int y = Math.max(row, 0); y < Math.min(row + h, height); y++) {
            int[] line = buffer[y];
            short[] lineColor = color[y];
            int bx = Math.max(col, 0);
            for (int x = bx; x < Math.min(col + w, width) && bx < line.length; x++) {
                changed = changed || (line[bx] != c) || (lineColor[bx] != packed);
                setChar(line, lineColor, bx, c);
                bx += Math.max(1, FontUtils.wcwidth(c));
            }
        }
        return changed;
    }

    public boolean copy(int col, int row, int w, int h, int tx, int ty) {
        if (w <= 0 || h <= 0) return false;
        if (tx == 0 && ty == 0) return false;

        int _dx0 = Math.clamp(col + tx + w - 1, 0, width - 1);
        int _dx1 = Math.clamp(col + tx, 0, width);
        int dx0 = tx > 0 ? _dx0 : _dx1;
        int dx1 = tx > 0 ? _dx1 : _dx0;
        int leftEdge = Math.min(dx0, dx1) - 1;
        if (leftEdge >= width - 1) return false;

        int _dy0 = Math.clamp(row + ty + h - 1, 0, height - 1);
        int _dy1 = Math.clamp(row + ty, 0, height);
        int dy0 = ty > 0 ? _dy0 : _dy1;
        int dy1 = ty > 0 ? _dy1 : _dy0;

        int sx = tx > 0 ? -1 : 1;
        int sy = ty > 0 ? -1 : 1;

        boolean changed = false;
        for (int ny = dy0; (sy > 0 ? ny <= dy1 : ny >= dy1); ny += sy) {
            int[] nl = buffer[ny];
            short[] nc = color[ny];
            int oy = ny - ty;
            if (oy >= 0 && oy < height) {
                int[] ol = buffer[oy];
                short[] oc = color[oy];
                for (int nx = dx0; (sx > 0 ? nx <= dx1 : nx >= dx1); nx += sx) {
                    int ox = nx - tx;
                    if (ox >= 0 && ox < width) {
                        changed = changed || (nl[nx] != ol[ox]) || (nc[nx] != oc[ox]);
                        nl[nx] = ol[ox];
                        nc[nx] = oc[ox];
                        for (int offset = 1; offset < FontUtils.wcwidth(nl[nx]); offset++) {
                            nl[nx + offset] = ' ';
                            nc[nx + offset] = oc[nx];
                        }
                    }
                }
                if (leftEdge >= 0 && FontUtils.wcwidth(nl[leftEdge]) > 1) {
                    nl[leftEdge] = ' ';
                    changed = true;
                }
            }
        }
        return changed;
    }

    public boolean rawcopy(int col, int row, int w, int h, TextBuffer src, int fromCol, int fromRow) {
        boolean changed = false;
        int colIndex = col - 1;
        int rowIndex = row - 1;
        for (int yOffset = 0; yOffset < h; yOffset++) {
            int[] dstCharLine = buffer[rowIndex + yOffset];
            short[] dstColorLine = color[rowIndex + yOffset];
            for (int xOffset = 0; xOffset < w; xOffset++) {
                int srcChar = src.buffer[fromRow + yOffset - 1][fromCol + xOffset - 1];
                short srcColor = src.color[fromRow + yOffset - 1][fromCol + xOffset - 1];

                if (this._format.depth() != src._format.depth()) {
                    PackedColor.Color fg = new PackedColor.Color(PackedColor.unpackForeground(srcColor, src._format));
                    PackedColor.Color bg = new PackedColor.Color(PackedColor.unpackBackground(srcColor, src._format));
                    srcColor = PackedColor.pack(fg, bg, _format);
                }

                if (srcChar != dstCharLine[colIndex + xOffset] || srcColor != dstColorLine[colIndex + xOffset]) {
                    changed = true;
                    dstCharLine[colIndex + xOffset] = srcChar;
                    dstColorLine[colIndex + xOffset] = srcColor;
                }
            }
        }
        return changed;
    }

    private void setChar(int[] line, short[] lineColor, int x, int c) {
        if (FontUtils.wcwidth(c) > 1 && x >= line.length - 1) {
            return;
        }
        line[x] = c;
        lineColor[x] = packed;
        for (int x1 = x + 1; x1 < x + FontUtils.wcwidth(c); x1++) {
            line[x1] = ' ';
            lineColor[x1] = packed;
        }
        if (x > 0 && FontUtils.wcwidth(line[x - 1]) > 1) {
            line[x - 1] = ' ';
        }
    }

    public void load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        int maxResolution = Math.max(OCSettings.screenResolutionsByTier[OCSettings.screenResolutionsByTier.length - 1][0], OCSettings.screenResolutionsByTier[OCSettings.screenResolutionsByTier.length - 1][1]);
        int w = Math.min(nbt.getInt("width"), maxResolution);
        w = Math.max(w, 1);
        int h = Math.min(nbt.getInt("height"), maxResolution);
        h = Math.max(h, 1);
        size_$eq(new int[]{w, h});

        net.minecraft.nbt.ListTag b = nbt.getList("buffer", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(h, b.size()); i++) {
            String value = b.getString(i);
            java.util.PrimitiveIterator.OfInt valueIt = value.codePoints().iterator();
            int j = 0;
            while (j < buffer[i].length && valueIt.hasNext()) {
                buffer[i][j] = valueIt.nextInt();
                j++;
            }
        }

        li.cil.oc.api.internal.TextBuffer.ColorDepth[] depths = li.cil.oc.api.internal.TextBuffer.ColorDepth.values();
        int depthIdx = Math.min(nbt.getInt("depth"), depths.length - 1);
        depthIdx = Math.max(depthIdx, 0);
        li.cil.oc.api.internal.TextBuffer.ColorDepth depth = depths[depthIdx];
        _format = PackedColor.Depth.format(depth);
        _format.load(nbt, provider);
        foreground_$eq(new PackedColor.Color(nbt.getInt("foreground"), nbt.getBoolean("foregroundIsPalette")));
        background_$eq(new PackedColor.Color(nbt.getInt("background"), nbt.getBoolean("backgroundIsPalette")));

        if (!NbtDataStream.getShortArray(nbt, "colors", color, w, h)) {
            NbtDataStream.getIntArrayLegacy(nbt, "color", color, w, h);
        }
    }

    public void save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        nbt.putInt("width", width);
        nbt.putInt("height", height);

        net.minecraft.nbt.ListTag b = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < height; i++) {
            b.add(net.minecraft.nbt.StringTag.valueOf(lineToString(i)));
        }
        nbt.put("buffer", b);

        nbt.putInt("depth", _format.depth().ordinal());
        _format.save(nbt, provider);
        nbt.putInt("foreground", _foreground.value());
        nbt.putBoolean("foregroundIsPalette", _foreground.isPalette());
        nbt.putInt("background", _background.value());
        nbt.putBoolean("backgroundIsPalette", _background.isPalette());

        short[] flat = new short[width * height];
        for (int y = 0; y < height; y++) {
            if (width >= 0) System.arraycopy(color[y], 0, flat, y * width, width);
        }
        NbtDataStream.setShortArray(nbt, "colors", flat);
    }

    public String lineToString(int y) {
        StringBuilder b = new StringBuilder();
        if (buffer.length > 0) {
            for (int x = 0; x < width; x++) {
                b.appendCodePoint(buffer[y][x]);
            }
        }
        return b.toString();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (buffer.length > 0) {
            for (int x = 0; x < width; x++) {
                b.appendCodePoint(buffer[0][x]);
            }
            for (int y = 1; y < height; y++) {
                b.append('\n');
                for (int x = 0; x < width; x++) {
                    b.appendCodePoint(buffer[y][x]);
                }
            }
        }
        return b.toString();
    }
}
