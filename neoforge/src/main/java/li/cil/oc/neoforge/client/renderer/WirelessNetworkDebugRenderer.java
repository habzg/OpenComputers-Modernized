package li.cil.oc.neoforge.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.network.WirelessNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class WirelessNetworkDebugRenderer {
    private static final int[] colors = {0xFF0000, 0x00FFFF, 0x00FF00, 0x0000FF, 0xFF00FF, 0xFFFF00, 0xFFFFFF, 0x000000};

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onRenderWorldLastEvent(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        if (!OCSettings.rTreeDebugRenderer) return;

        var world = Minecraft.getInstance().level;
        if (world == null) return;
        var dim = world.dimension();
        var tree = WirelessNetworkManager.getTree(dim);
        if (tree != null) {
            var camera = e.getCamera();
            float px = (float) camera.getPosition().x;
            float py = (float) camera.getPosition().y;
            float pz = (float) camera.getPosition().z;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            try {
                var allBounds = tree.allBounds();
                var tesselator = Tesselator.getInstance();
                var builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (var entry : allBounds) {
                    var boundsPair = (Object[]) entry[0];
                    var min = (double[]) boundsPair[0];
                    var max = (double[]) boundsPair[1];
                    int level = (Integer) entry[1];
                    int color = colors[level % colors.length];
                    float r = ((color >> 16) & 0xFF) / 255f;
                    float g = ((color >> 8) & 0xFF) / 255f;
                    float b = ((color) & 0xFF) / 255f;
                    double size = 0.5 - level * 0.05;
                    drawBoxOutline(builder, min[0] - size - px, min[1] - size - py, min[2] - size - pz,
                            max[0] + size - px, max[1] + size - py, max[2] + size - pz, r, g, b, 0.5f);
                }
                var mesh = builder.build();
                if (mesh != null) BufferUploader.drawWithShader(mesh);
            } finally {
                RenderSystem.enableDepthTest();
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }
        }

    }

    @SuppressWarnings("SameParameterValue")
    private static void drawBoxOutline(BufferBuilder builder, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a) {
        addLine(builder, (float) minX, (float) minY, (float) minZ, (float) maxX, (float) minY, (float) minZ, r, g, b, a);
        addLine(builder, (float) maxX, (float) minY, (float) minZ, (float) maxX, (float) minY, (float) maxZ, r, g, b, a);
        addLine(builder, (float) maxX, (float) minY, (float) maxZ, (float) minX, (float) minY, (float) maxZ, r, g, b, a);
        addLine(builder, (float) minX, (float) minY, (float) maxZ, (float) minX, (float) minY, (float) minZ, r, g, b, a);
        addLine(builder, (float) minX, (float) maxY, (float) minZ, (float) maxX, (float) maxY, (float) minZ, r, g, b, a);
        addLine(builder, (float) maxX, (float) maxY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, r, g, b, a);
        addLine(builder, (float) maxX, (float) maxY, (float) maxZ, (float) minX, (float) maxY, (float) maxZ, r, g, b, a);
        addLine(builder, (float) minX, (float) maxY, (float) maxZ, (float) minX, (float) maxY, (float) minZ, r, g, b, a);
        addLine(builder, (float) minX, (float) minY, (float) minZ, (float) minX, (float) maxY, (float) minZ, r, g, b, a);
        addLine(builder, (float) maxX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) minZ, r, g, b, a);
        addLine(builder, (float) maxX, (float) minY, (float) maxZ, (float) maxX, (float) maxY, (float) maxZ, r, g, b, a);
        addLine(builder, (float) minX, (float) minY, (float) maxZ, (float) minX, (float) maxY, (float) maxZ, r, g, b, a);
    }

    private static void addLine(BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        builder.addVertex(x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(x2, y2, z2).setColor(r, g, b, a);
    }
}
