package li.cil.oc.fabric.integration.opencomputers;

import java.util.Arrays;
import java.util.HashSet;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.block.SimpleBlock;
import li.cil.oc.core.impl.common.item.DelegateItem;
import li.cil.oc.fabric.integration.util.JEI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public final class ModOpenComputers implements li.cil.oc.core.integration.ModProxy {
    @Override
    public li.cil.oc.core.integration.Mod getMod() {
        return li.cil.oc.fabric.integration.Mods.OpenComputers;
    }

    @Override
    public void initialize() {
        JEI.hide(li.cil.oc.api.Items.get(Constants.BlockName.RobotAfterimage).block());
        JEI.hide(li.cil.oc.api.Items.get(Constants.BlockName.Print).block());
        JEI.hide(li.cil.oc.api.Items.get(Constants.BlockName.BeaconBasePrint).block());
        JEI.hide((DelegateItem) li.cil.oc.api.Items.get(Constants.ItemName.Present).item());
    }

    public static final class DefinitionPathProvider implements PathProvider {
        private static final HashSet<String> Blacklist = new HashSet<>(Arrays.asList(
                Constants.ItemName.APUCreative,
                Constants.ItemName.Debugger,
                Constants.ItemName.DiamondChip,
                Constants.ItemName.Present,
                Constants.BlockName.CarpetedCapacitor,
                Constants.BlockName.Endstone,
                Constants.BlockName.RobotAfterimage));

        private static String checkBlacklisted(ItemInfo info) {
            if (info == null || Blacklist.contains(info.name())) {
                return null;
            }
            if (info.name().equals(Constants.BlockName.BeaconBasePrint)) {
                return "%LANGUAGE%/block/" + Constants.BlockName.Print + ".md";
            }
            if (info.block() != null) {
                return "%LANGUAGE%/block/" + info.name() + ".md";
            }
            return "%LANGUAGE%/item/" + info.name() + ".md";
        }

        @Override
        public String pathFor(ItemStack stack) {
            var info = li.cil.oc.api.Items.get(stack);
            return info != null ? checkBlacklisted(info) : null;
        }

        @Override
        public String pathFor(Level world, BlockPos pos) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof SimpleBlock) {
                ItemInfo info = li.cil.oc.api.Items.get(new ItemStack(block));
                if (info != null) {
                    return checkBlacklisted(info);
                }
            }
            return null;
        }
    }
}
