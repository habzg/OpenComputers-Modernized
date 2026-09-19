package li.cil.oc.fabric.common.blockentity;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.blockentity.Rack;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class RackFabric extends Rack implements RenderDataBlockEntity {
  public record RackRenderData(ResourceLocation[] slotTextures) {
  }

  public RackFabric(BlockPos pos, BlockState state) {
    super(pos, state);
  }

  @Override
  public String inventoryName() {
    return "Rack";
  }

  @Override
  public Object getRenderData() {
    ResourceLocation[] textures = new ResourceLocation[getContainerSize()];
    for (int i = 0; i < getContainerSize(); i++) {
      var stack = getItem(i);
      if (!stack.isEmpty()) {
        textures[i] = baseTextureFor(li.cil.oc.api.Items.get(stack));
      }
    }
    return new RackRenderData(textures);
  }

  @Override
  public void readFromNBTForClient(CompoundTag nbt) {
    super.readFromNBTForClient(nbt);
    if (getLevel() != null) {
      getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }
  }

  private static ResourceLocation baseTextureFor(li.cil.oc.api.detail.ItemInfo itemInfo) {
    if (itemInfo == null) return null;
    if (itemInfo == DISK_DRIVE_MOUNTABLE) return Textures.blockRackDiskDrive;
    for (var server : SERVERS) {
      if (itemInfo == server) return Textures.blockRackServer;
    }
    if (itemInfo == TERMINAL_SERVER) return Textures.blockRackTerminalServer;
    return null;
  }

  private static final li.cil.oc.api.detail.ItemInfo DISK_DRIVE_MOUNTABLE = li.cil.oc.api.Items.get(Constants.ItemName.DiskDriveMountable);
  private static final li.cil.oc.api.detail.ItemInfo[] SERVERS = new li.cil.oc.api.detail.ItemInfo[]{
    li.cil.oc.api.Items.get(Constants.ItemName.ServerTier1),
    li.cil.oc.api.Items.get(Constants.ItemName.ServerTier2),
    li.cil.oc.api.Items.get(Constants.ItemName.ServerTier3),
    li.cil.oc.api.Items.get(Constants.ItemName.ServerCreative)
  };
  private static final li.cil.oc.api.detail.ItemInfo TERMINAL_SERVER = li.cil.oc.api.Items.get(Constants.ItemName.TerminalServer);
}
