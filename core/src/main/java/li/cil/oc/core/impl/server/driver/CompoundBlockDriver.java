package li.cil.oc.core.impl.server.driver;

import com.google.common.base.Strings;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class CompoundBlockDriver implements li.cil.oc.api.driver.SidedBlock {
    private final li.cil.oc.api.driver.SidedBlock[] blocks;

    public CompoundBlockDriver(li.cil.oc.api.driver.SidedBlock[] blocks) {
        this.blocks = blocks;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, int x, int y, int z, Direction side) {
        var pos = new BlockPos(x, y, z);
        List<Object[]> list = new ArrayList<>();
        for (li.cil.oc.api.driver.SidedBlock driver : blocks) {
            ManagedEnvironment env = driver.createEnvironment(world, x, y, z, side);
            if (env != null) {
                list.add(new Object[]{driver.getClass().getName(), env});
            }
        }
        if (list.isEmpty()) return null;
        ManagedEnvironment[] envs = new ManagedEnvironment[list.size()];
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            names[i] = (String) list.get(i)[0];
            envs[i] = (ManagedEnvironment) list.get(i)[1];
        }
        return new CompoundBlockEnvironment(cleanName(tryGetName(world, pos, envs)), names, envs);
    }

    @Override
    public boolean worksWith(Level world, int x, int y, int z, Direction side) {
        for (li.cil.oc.api.driver.SidedBlock d : blocks) {
            if (!d.worksWith(world, x, y, z, side)) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompoundBlockDriver multi) {
            if (multi.blocks.length == blocks.length) {
                return new HashSet<>(Arrays.asList(blocks)).containsAll(java.util.Arrays.asList(multi.blocks));
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(blocks);
    }

    private String tryGetName(Level world, BlockPos pos, ManagedEnvironment[] environments) {
        NamedBlock best = null;
        for (ManagedEnvironment env : environments) {
            if (env instanceof NamedBlock named) {
                if (best == null || named.priority() > best.priority()) {
                    best = named;
                }
            }
        }
        if (best != null) return best.preferredName();
        try {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof Container && te instanceof net.minecraft.world.Nameable named) {
                named.getDisplayName();
                return named.getDisplayName().getString().replaceFirst("^container\\.", "");
            }
        } catch (Throwable ignored) {
        }
        try {
            Block block = world.getBlockState(pos).getBlock();
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
                return stack.getDescriptionId().replaceFirst("^block\\.", "").replaceFirst("^item\\.", "");
            }
        } catch (Throwable ignored) {
        }
        try {
            BlockEntity te = world.getBlockEntity(pos);
            if (te != null) {
                var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(te.getType());
                if (key != null) return key.toString();
            }
        } catch (Throwable ignored) {
        }
        return "component";
    }

    private String cleanName(String name) {
        String safeStart = name.matches("^[^a-zA-Z_]") ? "_" + name : name;
        String identifier = safeStart.replaceAll("[^\\w_]", "_").trim();
        if (Strings.isNullOrEmpty(identifier)) return "component";
        return identifier.toLowerCase();
    }
}
