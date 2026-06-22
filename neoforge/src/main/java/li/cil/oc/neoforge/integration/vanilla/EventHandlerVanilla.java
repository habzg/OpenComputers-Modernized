package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.event.GeolyzerEventImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;

@SuppressWarnings("unused")
public final class EventHandlerVanilla {
    @SubscribeEvent
    public static void onGeolyzerScan(GeolyzerEventImpl.Scan e) {
        var world = e.host().level();
        var blockPos = BlockPosition.apply(e.host());
        var includeReplaceable = e.options().get("includeReplaceable") instanceof Boolean value ? value : true;

        var noise = new byte[e.data().length];
        var random = world.random;
        for (int i = 0; i < noise.length; i++) {
            noise[i] = (byte) random.nextInt();
        }
        for (int i = 0; i < e.data().length; i++) {
            e.data()[i] = (noise[i] / 128f / 33f);
        }

        int w = e.maxX() - e.minX() + 1;
        int d = e.maxZ() - e.minZ() + 1;
        for (int ry = e.minY(); ry <= e.maxY(); ry++) {
            for (int rz = e.minZ(); rz <= e.maxZ(); rz++) {
                for (int rx = e.minX(); rx <= e.maxX(); rx++) {
                    int x = blockPos.x() + rx;
                    int y = blockPos.y() + ry;
                    int z = blockPos.z() + rz;
                    int index = (rx - e.minX()) + ((rz - e.minZ()) + (ry - e.minY()) * d) * w;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && !world.isEmptyBlock(pos)) {
                        Block block = world.getBlockState(pos).getBlock();
                        if (includeReplaceable || isFluid(block) || !block.defaultBlockState().canBeReplaced()) {
                            double dx = blockPos.x() - x;
                            double dy = blockPos.y() - y;
                            double dz = blockPos.z() - z;
                            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                            e.data()[index] = e.data()[index] * distance * Settings.get().geolyzerNoise + block.defaultBlockState().getDestroySpeed(world, pos);
                        } else {
                            e.data()[index] = 0;
                        }
                    } else {
                        e.data()[index] = 0;
                    }
                }
            }
        }
    }

    private static boolean isFluid(Block block) {
        return !block.defaultBlockState().getFluidState().isEmpty();
    }

    @SubscribeEvent
    public static void onGeolyzerAnalyze(GeolyzerEventImpl.Analyze e) {
        var world = e.host().level();
        var blockPos = new BlockPos(e.x(), e.y(), e.z());
        var state = world.getBlockState(blockPos);
        var block = state.getBlock();

        e.data().put("name", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString());
        e.data().put("metadata", 0);
        e.data().put("hardness", state.getDestroySpeed(world, blockPos));
        e.data().put("harvestLevel", getHarvestLevel(state));
        e.data().put("harvestTool", getHarvestTool(state));
        e.data().put("color", state.getMapColor(world, blockPos).col);
        {
            var props = new java.util.LinkedHashMap<String, String>();
            for (var prop : state.getProperties()) {
                props.put(prop.getName(), state.getValue(prop).toString());
            }
            e.data().put("properties", props);
        }

        if (Settings.get().insertIdsInConverters) {
            e.data().put("id", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(block));
        }

        Float growth = null;
        if (block instanceof CropBlock crop) {
            growth = Math.clamp(crop.getAge(state) / (float) crop.getMaxAge(), 0, 1);
        } else if (block instanceof StemBlock) {
            int age = state.getValue(net.minecraft.world.level.block.StemBlock.AGE);
            growth = Math.clamp(age / 7f, 0, 1);
        } else if (block instanceof CocoaBlock) {
            growth = Math.clamp((state.getValue(CocoaBlock.AGE) >> 2) / 2f, 0, 1);
        } else if (block instanceof NetherWartBlock) {
            growth = Math.clamp(state.getValue(NetherWartBlock.AGE) / 3f, 0, 1);
        } else if (block instanceof ChorusFlowerBlock) {
            growth = Math.clamp(state.getValue(ChorusFlowerBlock.AGE) / 5f, 0, 1);
        } else if (block == net.minecraft.world.level.block.Blocks.MELON || block instanceof PumpkinBlock || block instanceof CactusBlock || block instanceof SugarCaneBlock || block instanceof ChorusPlantBlock) {
            growth = 1f;
        }
        if (growth != null) {
            e.data().put("growth", growth);
        }
    }

    private static String getHarvestTool(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return "pickaxe";
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return "axe";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return "shovel";
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return "hoe";
        return "";
    }

    private static int getHarvestLevel(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }
}
