package li.cil.oc.core.impl.client.renderer.markdown.segment;

import li.cil.oc.core.client.renderer.markdown.MarkupFormat;
import li.cil.oc.core.impl.client.renderer.markdown.Document;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

import java.net.URI;

public class LinkSegment extends TextSegment implements InteractiveSegment {
    public final String href;

    private static final int NORMAL_COLOR = 0x66FF66;
    private static final int NORMAL_HOVER_COLOR = 0xAAFFAA;
    private static final int ERROR_COLOR = 0xFF6666;
    private static final int ERROR_HOVER_COLOR = 0xFFAAAA;
    private static final long FADE_TIME = 500;

    private final boolean isExternal;
    private long lastHovered = System.currentTimeMillis() - FADE_TIME;
    private Boolean cachedIsValid = null;

    @SuppressWarnings("unused")
    public LinkSegment(Segment parent, String text, String href) {
        super(parent, text);
        this.href = href;
        this.isExternal = href.startsWith("http://") || href.startsWith("https://");
    }

    private boolean isLinkValid() {
        if (cachedIsValid != null) return cachedIsValid;
        if (isExternal) {
            cachedIsValid = true;
        } else {
            try {
                String resolved = makeRelative(href, Document.getCurrentPagePath());
                cachedIsValid = li.cil.oc.api.Manual.contentFor(resolved) != null;
            } catch (Throwable t) {
                cachedIsValid = false;
            }
        }
        return cachedIsValid;
    }

    @Override
    protected int color() {
        boolean valid = isLinkValid();
        int c = valid ? NORMAL_COLOR : ERROR_COLOR;
        int hc = valid ? NORMAL_HOVER_COLOR : ERROR_HOVER_COLOR;
        long timeSinceHover = System.currentTimeMillis() - lastHovered;
        if (timeSinceHover > FADE_TIME) return c;
        return fadeColor(hc, c, (float) timeSinceHover / FADE_TIME);
    }

    @Override
    public String tooltip() {
        return href;
    }

    @Override
    public void onMouseClick(int mouseX, int mouseY) {
        if (isExternal) {
            handleUrl(href);
        } else {
            li.cil.oc.api.Manual.navigate(makeRelative(href, Document.getCurrentPagePath()));
        }
    }

    @Override
    public void notifyHover() {
        lastHovered = System.currentTimeMillis();
    }

    public static String makeRelative(String path, String base) {
        if (path == null || path.startsWith("/")) return path;
        if (base != null) {
            int splitAt = base.lastIndexOf('/');
            if (splitAt >= 0) return base.substring(0, splitAt) + "/" + path;
        }
        return path;
    }

    private static int fadeColor(int c1, int c2, float t) {
        int r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static void handleUrl(String url) {
        try {
            Util.getPlatform().openUri(new URI(url));
        } catch (Throwable t) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Could not open link: " + t));
            }
        }
    }

    @Override
    public String toString(MarkupFormat format) {
        return switch (format) {
            case Markdown -> "[" + text() + "](" + href + ")";
        };
    }
}
