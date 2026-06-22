package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.internal.Rack;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.server.component.DiskDriveMountableBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class DiskDriveMountable extends DiskDriveMountableBase {
    public DiskDriveMountable(Rack rack, int slot) {
        super(rack, slot);
    }

    @Override
    protected void openDiskDriveGui(@NotNull Player player, @NotNull BlockPosition pos, int slot) {
        player.openMenu(OpenComputers.getContainerProvider(GuiType.DiskDriveMountableInRack, rack.level(), pos.x(), GuiType.embedSlot(pos.y(), slot), pos.z()), (net.minecraft.network.RegistryFriendlyByteBuf buf) -> {
            buf.writeInt(GuiType.DiskDriveMountableInRack);
            buf.writeInt(pos.x());
            buf.writeInt(GuiType.embedSlot(pos.y(), slot));
            buf.writeInt(pos.z());
        });
    }
}
