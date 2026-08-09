package li.cil.oc.neoforge.client.gui;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.DynamicGuiContainer;
import li.cil.oc.core.impl.client.gui.ImageButton;
import li.cil.oc.neoforge.client.PacketSender;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class Rack extends DynamicGuiContainer<li.cil.oc.core.impl.common.container.Rack> {
    public final li.cil.oc.core.impl.common.blockentity.Rack rack;

    private static final int[] busMasterBlankUVs = {195, 14, 3, 5};
    private static final int[] busMasterPresentUVs = {194, 20, 5, 5};
    private static final int[] busSlaveBlankUVs = {195, 1, 3, 4};
    private static final int[] busSlavePresentUVs = {194, 6, 5, 4};

    private static final int[] connectorMasterUVs = {194, 26, 1, 3};
    private static final int[] connectorSlaveUVs = {194, 11, 1, 2};

    private static final int[] hoverMasterSize = {3, 3};
    private static final int[] hoverSlaveSize = {3, 2};

    private static final int[][] wireMasterUVs = {
            {186, 16, 6, 3},
            {186, 20, 6, 3},
            {186, 24, 6, 3},
            {186, 28, 6, 3},
            {186, 32, 6, 3}
    };
    private static final int[][] wireSlaveUVs = {
            {186, 1, 6, 2},
            {186, 4, 6, 2},
            {186, 7, 6, 2},
            {186, 10, 6, 2},
            {186, 13, 6, 2}
    };

    private static final int[][] busStart = {
            {45, 22},
            {56, 22},
            {67, 22},
            {78, 22},
            {89, 22}
    };

    private static final int busGap = 3;

    private static final int[][] connectorStart = {
            {37, 23},
            {37, 43},
            {37, 63},
            {37, 83}
    };

    private static final int connectorGap = 2;

    private static final int[] relayModeUVs = {195, 30, 4, 2};

    private static final int[][] wireRelay = {
            {50, 104},
            {61, 104},
            {72, 104},
            {83, 104}
    };

    private static final Direction[] busToSide = java.util.Arrays.stream(Direction.values())
            .filter(d -> d != Direction.SOUTH).toArray(Direction[]::new);

    private ImageButton relayButton;

    private final ImageButton[][][] wireButtons = new ImageButton[5][4][4];

    @SuppressWarnings("unused")
    public Rack(Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Rack rack) {
        super(new li.cil.oc.core.impl.common.container.Rack(Menus.RACK.get(), 0, playerInventory, rack, playerInventory.player));
        this.rack = rack;
        imageHeight = 210;
    }

    public Rack(li.cil.oc.core.impl.common.container.Rack container, Inventory inv, Component title) {
        super(container, inv, title);
        this.rack = (li.cil.oc.core.impl.common.blockentity.Rack) container.otherInventory;
        imageHeight = 210;
    }

    private static int encodeButtonId(int mountable, int connectable, int bus) {
        return 1 + mountable * 4 * 5 + connectable * 5 + bus;
    }

    @Override
    public void init() {
        super.init();

        relayButton = new ImageButton(0, leftPos + 101, topPos + 96, 65, 18, Textures.guiButtonRelay,
                Component.translatable("gui.opencomputers.rack.disabled").getString(), 0xE0E0E0, true, 0xA0A0A0, 0xFFFFA0, 18);
        addRenderableWidget(relayButton);
        relayButton.setPressHandler(this::actionPerformed);

        int mw = hoverMasterSize[0];
        int mh = hoverMasterSize[1];
        int sw = hoverSlaveSize[0];
        int sh = hoverSlaveSize[1];
        int mbh = busMasterBlankUVs[3];
        int sbh = busSlaveBlankUVs[3];

        for (int bus = 0; bus < 5; bus++) {
            for (int mountable = 0; mountable < rack.getContainerSize(); mountable++) {
                int offset = mountable * (mbh + sbh * 3 + busGap);
                int bx = busStart[bus][0];
                int by = busStart[bus][1];

                var masterBtn = new ImageButton(encodeButtonId(mountable, 0, bus),
                        leftPos + bx, topPos + by + offset + 1, mw, mh, null);
                addRenderableWidget(masterBtn);
                masterBtn.setPressHandler(this::actionPerformed);
                wireButtons[bus][mountable][0] = masterBtn;

                for (int connectable = 0; connectable < 3; connectable++) {
                    var slaveBtn = new ImageButton(encodeButtonId(mountable, connectable + 1, bus),
                            leftPos + bx, topPos + by + offset + 1 + mbh + sbh * connectable, sw, sh, null);
                    addRenderableWidget(slaveBtn);
                    slaveBtn.setPressHandler(this::actionPerformed);
                    wireButtons[bus][mountable][connectable + 1] = slaveBtn;
                }
            }
        }
    }

    protected void actionPerformed(ImageButton button) {
        if (button.getId() == 0) {
            PacketSender.sendRackRelayState(rack, !rack.isRelayEnabled);
        } else {
            int id = button.getId();
            int bus = (id - 1) % 5;
            int connectable = ((id - 1) / 5) % 4;
            int mountable = (id - 1) / 5 / 4;
            if (rack.nodeMapping[mountable][connectable] == busToSide[bus]) {
                PacketSender.sendRackMountableMapping(rack, mountable, connectable, null);
            } else {
                PacketSender.sendRackMountableMapping(rack, mountable, connectable, busToSide[bus]);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        for (int bus = 0; bus < 5; bus++) {
            for (int mountable = 0; mountable < rack.getContainerSize(); mountable++) {
                var presence = menu.nodePresence[mountable];
                for (int connectable = 0; connectable < 4; connectable++) {
                    wireButtons[bus][mountable][connectable].visible = presence[connectable];
                }
            }
        }
        relayButton.setMessage(Component.translatable(rack.isRelayEnabled ?
                "gui.opencomputers.rack.enabled" : "gui.opencomputers.rack.disabled"));
        super.render(guiGraphics, mouseX, mouseY, dt);
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY);

        guiGraphics.drawString(font, Component.translatable(rack.getInventoryName()).getString(), 8, 6, 0x404040, false);

        if (rack.isRelayEnabled) {
            int left = relayModeUVs[0];
            int top = relayModeUVs[1];
            int w = relayModeUVs[2];
            int h = relayModeUVs[3];
            for (int[] pos : wireRelay) {
                drawRect(guiGraphics, pos[0], pos[1], w, h, left, top);
            }
        }

        int mcx = connectorMasterUVs[0];
        int mcy = connectorMasterUVs[1];
        int mcw = connectorMasterUVs[2];
        int mch = connectorMasterUVs[3];
        int mbx = busMasterBlankUVs[0];
        int mby = busMasterBlankUVs[1];
        int mbw = busMasterBlankUVs[2];
        int mbh = busMasterBlankUVs[3];
        int mpx = busMasterPresentUVs[0];
        int mpy = busMasterPresentUVs[1];
        int mpw = busMasterPresentUVs[2];
        int mph = busMasterPresentUVs[3];
        int scx = connectorSlaveUVs[0];
        int scy = connectorSlaveUVs[1];
        int scw = connectorSlaveUVs[2];
        int sch = connectorSlaveUVs[3];
        int sbx = busSlaveBlankUVs[0];
        int sby = busSlaveBlankUVs[1];
        int sbw = busSlaveBlankUVs[2];
        int sbh = busSlaveBlankUVs[3];
        int spx = busSlavePresentUVs[0];
        int spy = busSlavePresentUVs[1];
        int spw = busSlavePresentUVs[2];
        int sph = busSlavePresentUVs[3];

        for (int mountable = 0; mountable < rack.getContainerSize(); mountable++) {
            boolean[] presence = menu.nodePresence[mountable];

            int cx = connectorStart[mountable][0];
            int cy = connectorStart[mountable][1];

            if (presence[0]) {
                drawRect(guiGraphics, cx, cy, mcw, mch, mcx, mcy);
                Direction mapping = rack.nodeMapping[mountable][0];
                if (mapping != null) {
                    int busIdx = sideToBus(mapping);
                    int[] wireUvs = wireMasterUVs[busIdx];
                    int mww = wireUvs[2];
                    int mwh = wireUvs[3];
                    for (int i = 0; i <= busIdx; i++) {
                        int xOff = mcw + i * (mpw + mww);
                        drawRect(guiGraphics, cx + xOff, cy, mww, mwh, wireUvs[0], wireUvs[1]);
                    }
                }
                for (int connectable = 1; connectable < 4; connectable++) {
                    Direction mappingSlave = rack.nodeMapping[mountable][connectable];
                    if (mappingSlave != null) {
                        int busIdx = sideToBus(mappingSlave);
                        int[] wireUvs = wireSlaveUVs[busIdx];
                        int sww = wireUvs[2];
                        int swh = wireUvs[3];
                        int yOff = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1);
                        for (int i = 0; i <= busIdx; i++) {
                            int xOff = scw + i * (spw + sww);
                            drawRect(guiGraphics, cx + xOff, cy + yOff, sww, swh, wireUvs[0], wireUvs[1]);
                        }
                    }
                }
            }

            for (int connectable = 1; connectable < 4; connectable++) {
                if (presence[connectable]) {
                    int yOff = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1);
                    drawRect(guiGraphics, cx, cy + yOff, scw, sch, scx, scy);
                }
            }

            int yOffset = mountable * (mbh + sbh * 3 + busGap);
            for (int bus = 0; bus < 5; bus++) {
                int bx = busStart[bus][0];
                int by = busStart[bus][1];
                if (presence[0]) {
                    drawRect(guiGraphics, bx - 1, by + yOffset, mpw, mph, mpx, mpy);
                } else {
                    drawRect(guiGraphics, bx, by + yOffset, mbw, mbh, mbx, mby);
                }
                for (int connectable = 0; connectable < 3; connectable++) {
                    if (presence[connectable + 1]) {
                        drawRect(guiGraphics, bx - 1, by + yOffset + mph + sph * connectable, spw, sph, spx, spy);
                    } else {
                        drawRect(guiGraphics, bx, by + yOffset + mbh + sbh * connectable, sbw, sbh, sbx, sby);
                    }
                }
            }
        }

        for (int bus = 0; bus < 5; bus++) {
            int x = 122;
            int y = 20 + bus * 11;
            guiGraphics.drawString(font, sideName(busToSide[bus]).getString(), x, y, 0x404040, false);
        }

        int relMouseX = mouseX - leftPos;
        int relMouseY = mouseY - topPos;

        if (relMouseX >= 122 && relMouseY >= 20 && relMouseX < 158 && relMouseY < 20 + 5 * 11) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.opencomputers.rack.orientationtooltip"));
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), relMouseX, relMouseY);
        }

        if (relayButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.opencomputers.rack.relaymodetooltip"));
            guiGraphics.renderTooltip(font, tooltip, java.util.Optional.empty(), relMouseX, relMouseY);
        }
    }

    @Override
    protected void drawSecondaryBackgroundLayer(GuiGraphics guiGraphics) {
        guiGraphics.blit(Textures.guiRack, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    private void drawRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int u, int v) {
        guiGraphics.blit(Textures.guiRack, x, y, w, h, u, v, w, h, 256, 256);
    }

    private static Component sideName(Direction side) {
        return switch (side) {
            case UP -> Component.translatable("gui.opencomputers.rack.top");
            case DOWN -> Component.translatable("gui.opencomputers.rack.bottom");
            case WEST -> Component.translatable("gui.opencomputers.rack.right");
            case EAST -> Component.translatable("gui.opencomputers.rack.left");
            case NORTH -> Component.translatable("gui.opencomputers.rack.back");
            default -> Component.translatable("gui.opencomputers.rack.none");
        };
    }

    private static int sideToBus(Direction side) {
        for (int i = 0; i < busToSide.length; i++) {
            if (busToSide[i] == side) return i;
        }
        return -1;
    }
}
