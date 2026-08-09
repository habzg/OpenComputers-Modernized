package li.cil.oc.fabric.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.ClientDistanceHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
public final class MFUTargetRenderer {
    private static final int color = 0x00FF00;
    private static final ItemInfo mfu = li.cil.oc.api.Items.get(Constants.ItemName.MFU);

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            var mc = Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;
            var stack = player.getMainHandItem();
            if (li.cil.oc.api.Items.get(stack) == mfu) {
                var cd = stack.get(DataComponents.CUSTOM_DATA);
                if (cd != null && !cd.isEmpty()) {
                    var data = cd.copyTag();
                    if (data.contains(OCSettings.namespace + "coord", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
                        var coords = data.getIntArray(OCSettings.namespace + "coord");
                        if (coords.length < 5) return;
                        if (player.level().dimension().location().hashCode() != coords[3]) return;
                        int x = coords[0], y = coords[1], z = coords[2], side = coords[4];

                        if (ClientDistanceHelper.distanceSquared(player.level(), x, y, z, player) > 64 * 64) return;

                        var target = ClientDistanceHelper.project(player.level(), new Vec3(x, y, z));
                        float tx = (float) target.x, ty = (float) target.y, tz = (float) target.z;

                        var camera = context.camera();
                        float px = (float) camera.getPosition().x;
                        float py = (float) camera.getPosition().y;
                        float pz = (float) camera.getPosition().z;

                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableDepthTest();
                        RenderSystem.disableCull();
                        RenderSystem.setShader(GameRenderer::getPositionColorShader);
                        try {
                            float r = ((color >> 16) & 0xFF) / 255f;
                            float g = ((color >> 8) & 0xFF) / 255f;
                            float b = ((color) & 0xFF) / 255f;

                            var tesselator = Tesselator.getInstance();
                            var builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                            drawBoxOutline(builder, tx - 0.1f - px, ty - 0.1f - py, tz - 0.1f - pz, tx + 1.1f - px, ty + 1.1f - py, tz + 1.1f - pz, r, g, b, 0.5f);
                            var mesh1 = builder.build();
                            if (mesh1 != null) BufferUploader.drawWithShader(mesh1);

                            builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                            drawFace(builder, tx - 0.1f - px, ty - 0.1f - py, tz - 0.1f - pz, tx + 1.1f - px, ty + 1.1f - py, tz + 1.1f - pz, side, r, g, b, 0.5f);
                            var mesh2 = builder.build();
                            if (mesh2 != null) BufferUploader.drawWithShader(mesh2);
                        } finally {
                            RenderSystem.enableDepthTest();
                            RenderSystem.enableCull();
                            RenderSystem.disableBlend();
                        }
                    }
                }
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawBoxOutline(BufferBuilder builder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        addLine(builder, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addLine(builder, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addLine(builder, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addLine(builder, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        addLine(builder, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(builder, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(builder, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addLine(builder, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        addLine(builder, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addLine(builder, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(builder, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(builder, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void addLine(BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        builder.addVertex(x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(x2, y2, z2).setColor(r, g, b, a);
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawFace(BufferBuilder builder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int side, float r, float g, float b, float a) {
        switch (side) {
            case 0 -> {
                builder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
                builder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
                builder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
                builder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
            }
            case 1 -> {
                builder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
                builder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
                builder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
                builder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
            }
            case 2 -> {
                builder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
                builder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
                builder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
                builder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
            }
            case 3 -> {
                builder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
                builder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
                builder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
                builder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
            }
            case 4 -> {
                builder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
                builder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
                builder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
                builder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
            }
            case 5 -> {
                builder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
                builder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
                builder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
                builder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
            }
        }
    }
}
