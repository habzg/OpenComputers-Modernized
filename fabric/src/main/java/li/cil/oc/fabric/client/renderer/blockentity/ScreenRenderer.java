package li.cil.oc.fabric.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.ClientDistanceHelper;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import li.cil.oc.core.impl.common.blockentity.Screen;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class ScreenRenderer implements BlockEntityRenderer<Screen> {

    @SuppressWarnings("unused")
    public ScreenRenderer(BlockEntityRendererProvider.Context ignoredCtx) {
    }

    @Override
    public void render(Screen screen, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (screen.getLevel() == null) return;
        if (!screen.isOrigin()) return;

        for (Screen s : screen.screens) {
            poseStack.pushPose();
            double dx = s.getBlockPos().getX() - screen.getBlockPos().getX();
            double dy = s.getBlockPos().getY() - screen.getBlockPos().getY();
            double dz = s.getBlockPos().getZ() - screen.getBlockPos().getZ();
            poseStack.translate(dx, dy, dz);
            renderFrame(s, poseStack, buffers, packedLight);
            poseStack.popPose();
        }

        renderScreenContent(screen, partialTick, poseStack, buffers, packedLight);

        Player player = Minecraft.getInstance().player;
        if (player != null && !player.getMainHandItem().isEmpty()) {
            if (screen.facing() == Direction.UP || screen.facing() == Direction.DOWN) {
                ItemStack held = player.getMainHandItem();
                var info = li.cil.oc.api.Items.get(held);
                boolean isScreenBlock = info != null && info.block() instanceof li.cil.oc.core.impl.common.block.Screen;
                boolean isWrench = Wrench.holdsApplicableWrench(player, BlockPosition.apply(screen.getBlockPos().getX(), screen.getBlockPos().getY(), screen.getBlockPos().getZ(), screen.getLevel()));
                if (isScreenBlock || isWrench) {
                    drawOverlay(screen, poseStack, buffers);
                }
            }
        }
    }

    private void drawOverlay(Screen screen, PoseStack poseStack, MultiBufferSource buffers) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        applyTransform(screen, poseStack);

        float cx = screen.width / 2.0f - 0.5f;
        float cy = screen.height / 2.0f - 0.5f;
        poseStack.translate(cx, cy, 0.01f);

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath("opencomputers", "block/overlay/screen_up_indicator"));
        if (sprite == null) {
            poseStack.popPose();
            return;
        }

        Matrix4f m = poseStack.last().pose();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        var normal = poseStack.last();
        vc.addVertex(m, 0, 1, 0).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u0, v1).setLight(0xF000F0).setNormal(normal, 0, 0, 1);
        vc.addVertex(m, 1, 1, 0).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u1, v1).setLight(0xF000F0).setNormal(normal, 0, 0, 1);
        vc.addVertex(m, 1, 0, 0).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u1, v0).setLight(0xF000F0).setNormal(normal, 0, 0, 1);
        vc.addVertex(m, 0, 0, 0).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u0, v0).setLight(0xF000F0).setNormal(normal, 0, 0, 1);

        poseStack.popPose();
    }

    private void renderFrame(Screen screen, PoseStack poseStack, MultiBufferSource buffers, int ignoredPackedLight) {
        if (screen.getLevel() == null) return;
        Screen origin = screen.origin;
        boolean isReady = origin != null;

        int[] local = screen.localPosition();
        int lx = local[0];
        int ly = local[1];

        for (Direction side : Direction.values()) {
            Level w = screen.getLevel();
            if (w == null) return;

            BlockPos adj = screen.getBlockPos().relative(side);
            BlockEntity neighborBE = w.getBlockEntity(adj);
            if (neighborBE instanceof Screen neighbor) {
                if (neighbor.origin == screen.origin) {
                    continue;
                }
            }

            boolean isFront = screen.toLocal(side) == Direction.SOUTH;

            String textureName;
            if (!isReady) {
                textureName = isFront ? "screen/f" : "screen/b";
            } else {
                textureName = isFront
                        ? getFrontTexture(screen, lx, ly)
                        : getSideTexture(screen, lx, ly, screen.toLocal(side), side);
            }

            int rotation = 0;
            if (side == Direction.UP) {
                rotation = screen.yaw().get2DDataValue();
            } else if (side == Direction.DOWN) {
                rotation = (6 - screen.yaw().get2DDataValue()) % 4;
            }

            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.fromNamespaceAndPath("opencomputers", "block/" + textureName));

            if (sprite != null) {
                int adjBlock = w.getBrightness(LightLayer.BLOCK, adj);
                int adjSky = w.getBrightness(LightLayer.SKY, adj);
                int faceLight = adjBlock << 4 | adjSky << 20;

                renderFace(poseStack, buffers.getBuffer(RenderType.cutout()), side, sprite,
                        screen.color, rotation, faceLight);
            }
        }
    }

    private String getFrontTexture(Screen screen, int lx, int ly) {
        Direction facing = screen.facing();

        int px = xy2part(lx, screen.width - 1);
        int py = xy2part(ly, screen.height - 1);

        int screenY;
        int screenX;
        int variant;
        if (facing == Direction.UP) {
            screenY = py;
            screenX = px;
            variant = 1;
        } else if (facing == Direction.DOWN) {
            screenY = 2 - py;
            screenX = 2 - px;
            variant = 0;
        } else {
            screenY = py;
            screenX = px;
            variant = screen.pitch() == Direction.UP || screen.pitch() == Direction.DOWN ? 1 : 0;
        }

        if (screen.width == 1 && screen.height == 1)
            return facing == Direction.DOWN ? "screen/f2" : "screen/f";
        if (screen.width == 1) return VERTICAL_FRONT[variant][screenY];
        if (screen.height == 1) return HORIZONTAL_FRONT[variant][screenX];
        return MULTI_FRONT[variant][screenY][screenX];
    }

    private String getSideTexture(Screen screen, int lx, int ly,
                                  Direction localSide, Direction worldSide) {
        Direction facing = screen.facing();
        int pitch = screen.pitch() == Direction.UP || screen.pitch() == Direction.DOWN ? 1 : 0;

        int px = xy2part(lx, screen.width - 1);
        int py = xy2part(ly, screen.height - 1);

        if (facing == Direction.UP) {
            px = 2 - px;
            py = 2 - py;
        }

        int finalPx = px;
        int finalPy = py;
        if (facing == Direction.DOWN && localSide == Direction.NORTH) finalPx = 2 - px;
        if (facing == Direction.UP && localSide == Direction.NORTH) finalPx = 2 - px;
        if (facing == Direction.DOWN && localSide == Direction.NORTH) finalPy = 2 - py;
        if (facing == Direction.UP && localSide == Direction.NORTH) finalPy = 2 - py;
        int sideOrd = localSide.ordinal();
        if ((facing == Direction.UP || facing == Direction.DOWN) &&
                (localSide == Direction.EAST || localSide == Direction.WEST)) {
            sideOrd = localSide == Direction.EAST ? Direction.WEST.ordinal() : Direction.EAST.ordinal();
        }
        if ((facing == Direction.UP || facing == Direction.DOWN) &&
                (localSide == Direction.UP || localSide == Direction.DOWN)) {
            finalPx = 2 - px;
        }

        if (screen.width == 1 && screen.height == 1)
            return worldSide.getAxis() == Direction.Axis.Y ? "screen/b" : "screen/b2";
        if (screen.width == 1) return VERTICAL[pitch][finalPy][sideOrd];
        if (screen.height == 1) return HORIZONTAL[pitch][finalPx][sideOrd];
        return MULTI[pitch][finalPy][finalPx][sideOrd];
    }

    private void renderScreenContent(Screen screen, float partialTick, PoseStack poseStack,
                                     MultiBufferSource buffers, int ignoredLight) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        Vec3 eyePos = p.getEyePosition(partialTick);
        Direction screenFacing = screen.facing().getOpposite();
        Vec3 center = ClientDistanceHelper.project(screen.getLevel(),
                new Vec3(screen.getBlockPos().getX() + 0.5, screen.getBlockPos().getY() + 0.5, screen.getBlockPos().getZ() + 0.5));
        double dx = center.x - eyePos.x;
        double dy = center.y - eyePos.y;
        double dz = center.z - eyePos.z;

        if (screenFacing.getStepX() * dx + screenFacing.getStepY() * dy + screenFacing.getStepZ() * dz < -1.5) return;

        double distSq = ClientDistanceHelper.distanceSquared(screen.getLevel(),
                screen.getBlockPos().getX() + 0.5, screen.getBlockPos().getY() + 0.5, screen.getBlockPos().getZ() + 0.5, p);
        double distance = Math.sqrt(distSq) / Math.min(screen.width, screen.height);
        double maxDist = OCSettings.get().maxScreenTextRenderDistance;
        double fadeStart = OCSettings.get().screenTextFadeStartDistance;
        if (distance > maxDist) return;
        float textAlpha = 1.0f;
        if (distance > fadeStart) {
            textAlpha = (float) Math.max(0, 1 - (distance - fadeStart) / (maxDist - fadeStart));
        }
        if (textAlpha <= 0) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        applyTransform(screen, poseStack);

        float innerWidth = screen.buffer().renderWidth();
        float innerHeight = screen.buffer().renderHeight();

        float sx = screen.width;
        float sy = screen.height;

        float isx = sx - (4.5f / 16.0f);
        float isy = sy - (4.5f / 16.0f);

        float scaleX = isx / innerWidth;
        float scaleY = isy / innerHeight;

        float scale;
        if (scaleX > scaleY) {
            scale = scaleY;
            poseStack.translate(innerWidth * 0.5f * (scaleX - scaleY), 0, 0);
        } else {
            scale = scaleX;
            poseStack.translate(0, innerHeight * 0.5f * (scaleY - scaleX), 0);
        }
        poseStack.scale(scale, scale, 1.0f);
        poseStack.translate(2.25f / 16.0f / scale, 2.25f / 16.0f / scale, 0.001f);

        Matrix4f textTransform = new Matrix4f(poseStack.last().pose());
        renderScreenText(screen, buffers, textTransform, textAlpha);

        poseStack.popPose();
    }

    private void renderScreenText(Screen screen, MultiBufferSource buffers, Matrix4f textTransform, float alpha) {
        var buffer = screen.buffer;

        if (buffer.isRenderingEnabled() && buffer.hasLitContent()) {
            if (buffer.isBufferDirty()) {
                for (var line : buffer.data.buffer) {
                    TextBufferRenderCache.renderer.generateChars(line);
                }
                buffer.clearBufferDirty();
            }

            ScreenBufferTextRenderer.render(
                    buffers,
                    buffer.data,
                    buffer.getViewportWidth(),
                    buffer.getViewportHeight(),
                    textTransform,
                    alpha
            );
        }
    }

    private void applyTransform(Screen screen, PoseStack stack) {
        Direction yaw = screen.yaw();
        if (yaw == Direction.WEST) stack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-90)));
        else if (yaw == Direction.NORTH) stack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180)));
        else if (yaw == Direction.EAST) stack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(90)));

        Direction facing = screen.facing();
        if (facing == Direction.DOWN) stack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(90)));
        else if (facing == Direction.UP) stack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-90)));

        stack.translate(-0.5f, -0.5f, 0.5f);
        stack.translate(0, screen.height, 0);
        stack.scale(1, -1, 1);
    }

    private static int xy2part(int value, int max) {
        if (max <= 0) return 0;
        if (value == 0) return 2;
        if (value == max) return 0;
        return 1;
    }

    private void renderFace(PoseStack poseStack, VertexConsumer vc, Direction face,
                            TextureAtlasSprite sprite, int color, int rotation, int packedLight) {
        Matrix4f m = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        float[] us = {u0, u1, u1, u0};
        float[] vs = {v1, v1, v0, v0};

        int rot = ((rotation % 4) + 4) % 4;
        for (int i = 0; i < rot; i++) {
            float tu = us[0], tv = vs[0];
            us[0] = us[1];
            us[1] = us[2];
            us[2] = us[3];
            us[3] = tu;
            vs[0] = vs[1];
            vs[1] = vs[2];
            vs[2] = vs[3];
            vs[3] = tv;
        }

        float[][] pos = getFloats(face);

        for (int i = 0; i < 4; i++) {
            vc.addVertex(m, pos[i][0], pos[i][1], pos[i][2])
                    .setColor(r, g, b, 1.0f)
                    .setUv(us[i], vs[i])
                    .setLight(packedLight)
                    .setNormal(poseStack.last(), face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }

    private static float @NotNull [][] getFloats(Direction face) {
        float e = 0f;
        return switch (face) {
            case SOUTH -> new float[][]{{-e, -e, 1 + e}, {1 + e, -e, 1 + e}, {1 + e, 1 + e, 1 + e}, {-e, 1 + e, 1 + e}};
            case NORTH -> new float[][]{{1 + e, -e, -e}, {-e, -e, -e}, {-e, 1 + e, -e}, {1 + e, 1 + e, -e}};
            case EAST -> new float[][]{{1 + e, -e, 1 + e}, {1 + e, -e, -e}, {1 + e, 1 + e, -e}, {1 + e, 1 + e, 1 + e}};
            case WEST -> new float[][]{{-e, -e, -e}, {-e, -e, 1 + e}, {-e, 1 + e, 1 + e}, {-e, 1 + e, -e}};
            case UP -> new float[][]{{-e, 1 + e, 1 + e}, {1 + e, 1 + e, 1 + e}, {1 + e, 1 + e, -e}, {-e, 1 + e, -e}};
            case DOWN -> new float[][]{{-e, -e, -e}, {1 + e, -e, -e}, {1 + e, -e, 1 + e}, {-e, -e, 1 + e}};
        };
    }

    private static final String[][][] HORIZONTAL = {
            {
                    {"screen/bht", "screen/bhb", "screen/bht", "screen/bht", "screen/b2", "screen/b2"},
                    {"screen/bhm", "screen/bhm", "screen/bhm2", "screen/bhm2", "screen/b", "screen/b"},
                    {"screen/bhb", "screen/bht", "screen/bhb2", "screen/bhb2", "screen/b2", "screen/b2"}
            },
            {
                    {"screen/bhb2", "screen/bht2", "screen/bht", "screen/bhb", "screen/b2", "screen/b2"},
                    {"screen/bhm2", "screen/bhm2", "screen/bhm", "screen/bhm", "screen/b", "screen/b"},
                    {"screen/bht2", "screen/bhb2", "screen/bhb", "screen/bht", "screen/b2", "screen/b2"}
            }
    };

    private static final String[][] HORIZONTAL_FRONT = {
            {"screen/fhb2", "screen/fhm2", "screen/fht2"},
            {"screen/fhb", "screen/fhm", "screen/fht"}
    };

    private static final String[][][] VERTICAL = {
            {
                    {"screen/b", "screen/b", "screen/bvt", "screen/bvt", "screen/bvt", "screen/bvt"},
                    {"screen/b", "screen/b", "screen/bvm", "screen/bvm", "screen/bvm", "screen/bvm"},
                    {"screen/b", "screen/b", "screen/bvb2", "screen/bvb2", "screen/bvb2", "screen/bvb2"}
            },
            {
                    {"screen/b2", "screen/b2", "screen/bvt", "screen/bvt", "screen/bht2", "screen/bhb2"},
                    {"screen/b", "screen/b", "screen/bvm", "screen/bvm", "screen/bhm2", "screen/bhm2"},
                    {"screen/b2", "screen/b2", "screen/bvb", "screen/bvb", "screen/bhb2", "screen/bht2"}
            }
    };

    private static final String[][] VERTICAL_FRONT = {
            {"screen/fvt", "screen/fvm", "screen/fvb2"},
            {"screen/fvt", "screen/fvm", "screen/fvb"}
    };

    private static final String[][][][] MULTI = {
            {
                    {
                            {"screen/bht", "screen/bhb", "screen/btl", "screen/btr", "screen/bvb", "screen/bvt"},
                            {"screen/bhm", "screen/bhm", "screen/btm", "screen/btm", "screen/b", "screen/b"},
                            {"screen/bhb", "screen/bht", "screen/btr", "screen/btl", "screen/bvt", "screen/bvb"}
                    },
                    {
                            {"screen/b", "screen/b", "screen/bml", "screen/bmr", "screen/bvm", "screen/bvm"},
                            {"screen/b", "screen/b", "screen/bmm", "screen/bmm", "screen/b", "screen/b"},
                            {"screen/b", "screen/b", "screen/bmr", "screen/bml", "screen/bvm", "screen/bvt"}
                    },
                    {
                            {"screen/bht", "screen/bhb", "screen/bbl2", "screen/bbr2", "screen/bvt", "screen/bvb2"},
                            {"screen/bhm", "screen/bhm", "screen/bbm2", "screen/bbm2", "screen/b", "screen/b"},
                            {"screen/bhb", "screen/bht", "screen/bbr2", "screen/bbl2", "screen/bvb2", "screen/bvt"}
                    }
            },
            {
                    {
                            {"screen/bhb2", "screen/bht2", "screen/btl", "screen/btr", "screen/bht2", "screen/bhb2"},
                            {"screen/bhm2", "screen/bhm2", "screen/btm", "screen/btm", "screen/b", "screen/b"},
                            {"screen/bht2", "screen/bhb2", "screen/btr", "screen/btl", "screen/bht2", "screen/bhb2"}
                    },
                    {
                            {"screen/b", "screen/b", "screen/bml", "screen/bml", "screen/bhm2", "screen/bhm2"},
                            {"screen/b", "screen/b", "screen/bmm", "screen/bmm", "screen/b", "screen/b"},
                            {"screen/b", "screen/b", "screen/bmr", "screen/bmr", "screen/bhm2", "screen/bhm2"}
                    },
                    {
                            {"screen/bhb2", "screen/bht2", "screen/bbl", "screen/bbr", "screen/bhb2", "screen/bht2"},
                            {"screen/bhm2", "screen/bhm2", "screen/bbm", "screen/bbm", "screen/b", "screen/b"},
                            {"screen/bht2", "screen/bhb2", "screen/bbr", "screen/bbl", "screen/bhb2", "screen/bht2"}
                    }
            }
    };

    private static final String[][][] MULTI_FRONT = {
            {
                    {"screen/ftr", "screen/ftm", "screen/ftl"},
                    {"screen/fmr", "screen/fmm", "screen/fml"},
                    {"screen/fbr2", "screen/fbm2", "screen/fbl2"}
            },
            {
                    {"screen/ftr", "screen/ftm", "screen/ftl"},
                    {"screen/fmr", "screen/fmm", "screen/fml"},
                    {"screen/fbr", "screen/fbm", "screen/fbl"}
            }
    };
}
