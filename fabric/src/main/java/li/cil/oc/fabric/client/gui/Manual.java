package li.cil.oc.fabric.client.gui;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.core.impl.client.renderer.markdown.Document;
import li.cil.oc.core.impl.client.renderer.markdown.segment.InteractiveSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.Segment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class Manual extends Screen {
    @SuppressWarnings("unused")
    public Manual() {
        super(Component.literal("manual"));
    }

    public final int documentMaxWidth = 230;
    public final int documentMaxHeight = 176;
    public final int scrollPosX = 244;
    public final int scrollPosY = 6;
    public final int scrollWidth = 6;
    public final int scrollHeight = 180;
    public final int tabPosX = -23;
    public final int tabPosY = 7;
    public final int tabWidth = 23;
    public final int tabHeight = 26;
    public final int maxTabsPerSide = 7;
    public boolean isDragging = false;
    public Segment document;
    public int documentHeight = 0;
    public InteractiveSegment currentSegment = null;
    private int leftPos;
    private int topPos;
    private ImageButton scrollButton;

    public static String resolveLink(String path, String current) {
        if (path.startsWith("/")) return path;
        int splitAt = current.lastIndexOf('/');
        if (splitAt >= 0) return current.substring(0, splitAt) + "/" + path;
        return path;
    }

    private boolean canScroll() {
        return maxOffset() > 0;
    }

    public int offset() {
        return li.cil.oc.fabric.client.Manual.INSTANCE.history.getFirst().offset;
    }

    public int maxOffset() {
        return Math.max(0, documentHeight - documentMaxHeight);
    }

    public void refreshPage() {
        var entry = li.cil.oc.fabric.client.Manual.INSTANCE.history.getFirst();
        var maybeContent = li.cil.oc.fabric.client.Manual.INSTANCE.contentFor(entry.path);
        Iterable<String> content = maybeContent != null ? maybeContent : java.util.List.of("Document not found: " + entry.path);
        document = Document.parse(content);
        documentHeight = Document.height(document, documentMaxWidth, font);
        scrollTo(offset());
    }

    public void pushPage(String path) {
        if (!path.equals(li.cil.oc.fabric.client.Manual.INSTANCE.history.getFirst().path)) {
            li.cil.oc.fabric.client.Manual.INSTANCE.history.push(new li.cil.oc.fabric.client.Manual.History(path));
            refreshPage();
        }
    }

    public void popPage() {
        if (li.cil.oc.fabric.client.Manual.INSTANCE.history.size() > 1) {
            li.cil.oc.fabric.client.Manual.INSTANCE.history.pop();
            refreshPage();
        } else {
            var player = Minecraft.getInstance().player;
            if (player != null) player.closeContainer();
        }
    }

    public void actionPerformed(ImageButton button) {
        if (button.getId() >= 0 && button.getId() < li.cil.oc.fabric.client.Manual.INSTANCE.tabs.size()) {
            li.cil.oc.fabric.client.Manual.INSTANCE.navigate(li.cil.oc.fabric.client.Manual.INSTANCE.tabs.get(button.getId()).path());
        }
    }

    @Override
    public void init() {
        super.init();
        leftPos = (width - 256) / 2;
        topPos = (height - 192) / 2;
        for (int i = 0; i < Math.min(li.cil.oc.fabric.client.Manual.INSTANCE.tabs.size(), maxTabsPerSide); i++) {
            int x = leftPos + tabPosX;
            int y = topPos + tabPosY + i * (tabHeight - 1);
            ImageButton tabButton = new ImageButton(i, x, y, tabWidth, tabHeight, Textures.guiManualTab);
            tabButton.setPressHandler(this::actionPerformed);
            addRenderableWidget(tabButton);
        }
        scrollButton = new ImageButton(-1, leftPos + scrollPosX, topPos + scrollPosY, 6, 13, Textures.guiButtonScroll);
        addRenderableWidget(scrollButton);
        refreshPage();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        guiGraphics.blit(Textures.guiManual, leftPos, topPos, 0, 0, 256, 192, 256, 192);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        super.render(guiGraphics, mouseX, mouseY, dt);
        scrollButton.active = canScroll();
        scrollButton.hoverOverride = isDragging;
        int tabCount = Math.min(li.cil.oc.fabric.client.Manual.INSTANCE.tabs.size(), maxTabsPerSide);
        for (int i = 0; i < tabCount; i++) {
            ImageButton button = (ImageButton) children().get(i);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(button.getX() + 5, button.getY() + 5, 0);
            li.cil.oc.fabric.client.Manual.INSTANCE.tabs.get(i).renderer().render(guiGraphics);
            guiGraphics.pose().popPose();
        }
        Document.setCurrentPagePath(li.cil.oc.fabric.client.Manual.INSTANCE.history.getFirst().path);
        int clipX = leftPos + 7;
        int clipY = topPos + 7;
        int clipW = documentMaxWidth + 2;
        int clipH = documentMaxHeight + 2;
        guiGraphics.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
        try {
            currentSegment = Document.render(document, leftPos + 8, topPos + 8, documentMaxWidth, documentMaxHeight, offset(), font, guiGraphics, mouseX, mouseY);
            guiGraphics.flush();
        } finally {
            guiGraphics.disableScissor();
        }
        if (!isDragging) {
            if (currentSegment != null) {
                String tooltipText = currentSegment.tooltip();
                if (tooltipText != null && !tooltipText.isEmpty()) {
                    guiGraphics.renderTooltip(font, Component.literal(Component.translatable(tooltipText).getString()), mouseX, mouseY);
                }
            }
        }
        if (!isDragging) {
            for (int i = 0; i < tabCount; i++) {
                ImageButton button = (ImageButton) children().get(i);
                if (mouseX > button.getX() && mouseX < button.getX() + tabWidth && mouseY > button.getY() && mouseY < button.getY() + tabHeight) {
                    String tabTooltip = li.cil.oc.fabric.client.Manual.INSTANCE.tabs.get(i).tooltip();
                    if (tabTooltip != null)
                        guiGraphics.renderTooltip(font, Component.literal(Component.translatable(tabTooltip).getString()), mouseX, mouseY);
                }
            }
        }
        if (canScroll() && (isCoordinateOverScrollBar(mouseX - leftPos, mouseY - topPos) || isDragging)) {
            guiGraphics.renderTooltip(font, Component.literal((offset() * 100 / Math.max(1, maxOffset())) + " %"), leftPos + scrollPosX + scrollWidth, scrollButton.getY() + scrollButton.getHeight() + 1);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyJump.matches(keyCode, scanCode)) {
            popPage();
            return true;
        }
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            var player = Minecraft.getInstance().player;
            if (player != null) player.closeContainer();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) scrollDown();
        else scrollUp();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (canScroll() && button == 0 && isCoordinateOverScrollBar((int) mouseX - leftPos, (int) mouseY - topPos)) {
            isDragging = true;
            scrollMouse((int) mouseY);
        } else if (button == 0) {
            if (currentSegment != null) currentSegment.onMouseClick((int) mouseX, (int) mouseY);
        } else if (button == 1) {
            popPage();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            scrollMouse((int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void scrollMouse(int mouseY) {
        scrollTo((int) Math.round((mouseY - topPos - scrollPosY - 6.5) * maxOffset() / (scrollHeight - 13.0)));
    }

    private void scrollUp() {
        scrollTo(offset() - Document.lineHeight(font) * 3);
    }

    private void scrollDown() {
        scrollTo(offset() + Document.lineHeight(font) * 3);
    }

    private void scrollTo(int row) {
        li.cil.oc.fabric.client.Manual.INSTANCE.history.getFirst().offset = Math.clamp(row, 0, maxOffset());
        int yMin = topPos + scrollPosY;
        if (maxOffset() > 0) {
            scrollButton.setY(yMin + (scrollHeight - 13) * offset() / maxOffset());
        } else {
            scrollButton.setY(yMin);
        }
    }

    private boolean isCoordinateOverScrollBar(int x, int y) {
        return x > scrollPosX && x < scrollPosX + scrollWidth && y >= scrollPosY && y < scrollPosY + scrollHeight;
    }
}
