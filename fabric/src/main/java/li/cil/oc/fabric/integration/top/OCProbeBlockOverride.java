package li.cil.oc.fabric.integration.top;

import li.cil.oc.core.impl.common.block.AbstractBlock;
import mcjty.theoneprobe.api.CompoundText;
import mcjty.theoneprobe.api.IBlockDisplayOverride;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public class OCProbeBlockOverride implements IBlockDisplayOverride {
  @Override
  public boolean overrideStandardInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data) {
    if (!(blockState.getBlock() instanceof AbstractBlock)) return false;
    ItemStack stack = data.getPickBlock();
    if (stack.isEmpty() || stack.getRarity() == Rarity.COMMON) return false;

    String modName = FabricLoader.getInstance()
      .getModContainer(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getNamespace())
      .map(container -> container.getMetadata().getName())
      .orElse("opencomputers");
    probeInfo.horizontal()
      .item(stack)
      .vertical()
      .mcText(Component.empty().append(stack.getHoverName()).withStyle(stack.getRarity().color()))
      .text(CompoundText.create().style(TextStyleClass.MODNAME).text(modName));
    return true;
  }
}
