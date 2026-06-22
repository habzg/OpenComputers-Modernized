package li.cil.oc.core.impl.client.renderer.markdown.segment;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.core.client.renderer.markdown.MarkupFormat;
import li.cil.oc.core.impl.client.renderer.markdown.Document;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class RenderSegment extends Segment implements InteractiveSegment {
    public final String tooltipText;
    public final ImageRenderer imageRenderer;
    public int lastX;
    public int lastY;

    @SuppressWarnings("unused")
    public RenderSegment(Segment parent, String tooltipText, ImageRenderer imageRenderer) {
        super(parent);
        this.tooltipText = tooltipText;
        this.imageRenderer = imageRenderer;
    }

    @Override
    public String tooltip() {
        if (imageRenderer instanceof InteractiveImageRenderer interactive) {
            return interactive.getTooltip(tooltipText);
        }
        return tooltipText;
    }

    @Override
    public void onMouseClick(int mouseX, int mouseY) {
        if (imageRenderer instanceof InteractiveImageRenderer interactive) {
            interactive.onMouseClick(mouseX - lastX, mouseY - lastY);
        }
    }

    @Override
    public InteractiveSegment render(int x, int y, int indent, int maxWidth, Font renderer, MultiBufferSource bufferSource, int mouseX, int mouseY) {
        int width = imageWidth(maxWidth);
        int height = imageHeight(maxWidth);
        int xOffset = (maxWidth - width) / 2;
        int yOffset = 2 + (indent > 0 ? Document.lineHeight(renderer) : 0);
        float s = scale(maxWidth);

        lastX = x + xOffset;
        lastY = y + yOffset;

        InteractiveSegment hovered = checkHovered(mouseX, mouseY, lastX, lastY, width, height);

        var pose = new PoseStack();
        pose.pushPose();
        pose.translate(lastX, lastY, 0);
        pose.scale(s, s, s);

        if (hovered != null) {
            var m = pose.last().pose();
            var consumer = bufferSource.getBuffer(RenderType.gui());
            consumer.addVertex(m, 0, 0, 0).setColor(1, 1, 1, 0.15f);
            consumer.addVertex(m, 0, imageRenderer.getHeight(), 0).setColor(1, 1, 1, 0.15f);
            consumer.addVertex(m, imageRenderer.getWidth(), imageRenderer.getHeight(), 0).setColor(1, 1, 1, 0.15f);
            consumer.addVertex(m, imageRenderer.getWidth(), 0, 0).setColor(1, 1, 1, 0.15f);
        }

        imageRenderer.render(pose, bufferSource, mouseX - lastX, mouseY - lastY);

        pose.popPose();

        return hovered;
    }

    @Override
    public int nextX(int indent, int maxWidth, Font renderer) {
        return 0;
    }

    @Override
    public int nextY(int indent, int maxWidth, Font renderer) {
        return imageHeight(maxWidth) + (indent > 0 ? Document.lineHeight(renderer) : 0);
    }

    @Override
    public String toString(MarkupFormat format) {
        return switch (format) {
            case Markdown -> "![" + tooltipText + "](" + imageRenderer + ")";
        };
    }

    private float scale(int maxWidth) {
        return Math.min(1f, maxWidth / (float) imageRenderer.getWidth());
    }

    private int imageWidth(int maxWidth) {
        return Math.min(maxWidth, imageRenderer.getWidth());
    }

    private int imageHeight(int maxWidth) {
        return (int) Math.ceil(imageRenderer.getHeight() * scale(maxWidth)) + 4;
    }
}
