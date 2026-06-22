package li.cil.oc.core.impl.client.renderer.markdown.segment;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.core.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class TextSegment extends BasicTextSegment {
    private final String _text;

    @SuppressWarnings("unused")
    public TextSegment(Segment parent, String text) {
        super(parent);
        this._text = text;
    }

    @Override
    public String text() {
        return _text;
    }

    @Override
    public InteractiveSegment render(int x, int y, int indent, int maxWidth, Font renderer, MultiBufferSource bufferSource, int mouseX, int mouseY) {
        var pose = new PoseStack();

        int currentX = x + indent;
        int currentY = y;
        String chars = _text;
        if (indent == 0) chars = stripLeadingWhitespace(chars);
        int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        InteractiveSegment hovered = null;

        int resolvedColor = resolveColor();
        Style resolvedStyle = resolveStyle();

        while (!chars.isEmpty()) {
            if (numChars > 0) {
                int n = Math.min(numChars, chars.length());
                String part = chars.substring(0, n);

                int partWidth = (int) (stringWidth(part, renderer) * resolveScale());

                InteractiveSegment self = resolveInteractive();
                if (self != null) {
                    InteractiveSegment h = self.checkHovered(mouseX, mouseY, currentX, currentY, partWidth, lineHeight(renderer));
                    if (h != null) hovered = h;
                }

                renderText(pose, bufferSource, renderer, part, currentX, currentY, resolvedColor, resolvedStyle, resolveScale());

                if (n < chars.length()) {
                    chars = chars.substring(n).stripLeading();
                } else {
                    break;
                }
            } else {
                chars = chars.stripLeading();
            }

            currentX = x + wrapIndent;
            currentY += lineHeight(renderer);
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
        }

        return hovered;
    }

    protected void renderText(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource source, Font renderer, String part, int x, int y, int color, Style style, float scale) {
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, scale);
        var seq = FormattedCharSequence.forward(part, style);
        renderer.drawInBatch(seq, 0, 0, color, false, pose.last().pose(), source, Font.DisplayMode.SEE_THROUGH, 0, 0x00F000F0);
        pose.popPose();
    }

    @Override
    public List<Segment> refine(Pattern pattern, java.util.function.BiFunction<Segment, MatchResult, Segment> factory) {
        var result = new ArrayList<Segment>();
        var matcher = pattern.matcher(_text);
        int textStart = 0;
        while (matcher.find()) {
            if (matcher.start() > textStart) {
                result.add(new TextSegment(this, _text.substring(textStart, matcher.start())));
            }
            textStart = matcher.end();
            result.add(factory.apply(this, matcher.toMatchResult()));
        }
        if (textStart == 0) {
            result.add(this);
        } else if (textStart < _text.length()) {
            result.add(new TextSegment(this, _text.substring(textStart)));
        }
        return result;
    }

    @Override
    public int nextX(int indent, int maxWidth, Font renderer) {
        if (isLast()) return 0;
        int currentX = indent;
        String chars = _text;
        if (ignoreLeadingWhitespace() && indent == 0) chars = stripLeadingWhitespace(chars);
        int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        while (chars.length() > numChars) {
            chars = chars.substring(Math.max(0, numChars)).stripLeading();
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
            currentX = wrapIndent;
        }
        if (chars.isEmpty()) return 0;
        return currentX + stringWidth(chars, renderer);
    }

    @Override
    public int nextY(int indent, int maxWidth, Font renderer) {
        int lines = 0;
        String chars = _text;
        if (ignoreLeadingWhitespace() && indent == 0) chars = stripLeadingWhitespace(chars);
        int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        while (chars.length() > numChars) {
            lines++;
            chars = chars.substring(Math.max(0, numChars)).stripLeading();
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
        }
        if (isLast()) lines++;
        return lines * lineHeight(renderer);
    }

    @Override
    public String toString(MarkupFormat format) {
        return _text;
    }

    @Override
    protected int lineHeight(Font renderer) {
        return (int) (super.lineHeight(renderer) * resolveScale());
    }

    @Override
    protected int stringWidth(String s, Font renderer) {
        var seq = FormattedCharSequence.forward(s, resolveStyle());
        return (int) (renderer.width(seq) * resolveScale());
    }

    protected int color() {
        return -1;
    }

    protected float scale() {
        return Float.NaN;
    }

    protected Style style() {
        return Style.EMPTY;
    }

    private int resolveColor() {
        int c = color();
        if (c >= 0) return c;
        if (parent instanceof TextSegment ts) return ts.resolveColor();
        return 0xDDDDDD;
    }

    private float resolveScale() {
        float s = scale();
        float parentScale = (parent instanceof TextSegment ts) ? ts.resolveScale() : 1f;
        return Float.isNaN(s) ? parentScale : s * parentScale;
    }

    private Style resolveStyle() {
        Style s = style();
        if (parent instanceof TextSegment ts) return s.applyTo(ts.resolveStyle());
        return s;
    }

    private InteractiveSegment resolveInteractive() {
        if (this instanceof InteractiveSegment) return (InteractiveSegment) this;
        if (parent instanceof TextSegment ts) return ts.resolveInteractive();
        return null;
    }

    private static String stripLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }
}
