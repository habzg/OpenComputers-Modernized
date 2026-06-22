package li.cil.oc.core.impl.client.renderer.markdown.segment;

import li.cil.oc.core.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public abstract class Segment {
    public final Segment parent;
    public Segment next;

    @SuppressWarnings("unused")
    public Segment(Segment parent) {
        this.parent = parent;
    }

    public Segment root() {
        Segment s = this;
        while (s.parent != null) s = s.parent;
        return s;
    }

    public boolean isLast() {
        return next == null || root() != next.root();
    }

    public abstract InteractiveSegment render(int x, int y, int indent, int maxWidth, Font renderer, MultiBufferSource bufferSource, int mouseX, int mouseY);

    public abstract int nextX(int indent, int maxWidth, Font renderer);

    public abstract int nextY(int indent, int maxWidth, Font renderer);

    public List<Segment> refine(Pattern pattern, java.util.function.BiFunction<Segment, java.util.regex.MatchResult, Segment> factory) {
        return Collections.singletonList(this);
    }

    public String toString(MarkupFormat format) {
        return "";
    }

    @Override
    public String toString() {
        return toString(MarkupFormat.Markdown);
    }
}
