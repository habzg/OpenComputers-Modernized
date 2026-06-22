package li.cil.oc.core.impl.client.renderer.markdown.segment;

public interface InteractiveSegment {
    String tooltip();

    default void onMouseClick(int mouseX, int mouseY) {
    }

    default void notifyHover() {
    }

    default InteractiveSegment checkHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        if (mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h) return this;
        return null;
    }
}
