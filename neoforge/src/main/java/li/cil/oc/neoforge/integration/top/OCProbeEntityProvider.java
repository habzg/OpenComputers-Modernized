package li.cil.oc.neoforge.integration.top;

import li.cil.oc.api.internal.Agent;
import mcjty.theoneprobe.api.CompoundText;
import mcjty.theoneprobe.api.IEntityDisplayOverride;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class OCProbeEntityProvider implements IProbeInfoEntityProvider, IEntityDisplayOverride {

    @Override
    public String getID() {
        return "opencomputers:entity";
    }

    @Override
    public boolean overrideStandardInfo(ProbeMode mode, IProbeInfo probeInfo, Player player,
                                        Level world, Entity entity, IProbeHitEntityData data) {
        if (entity instanceof li.cil.oc.core.impl.common.entity.Drone) {
            ItemStack icon = new ItemStack(li.cil.oc.neoforge.common.init.Items.DRONE.get());
            probeInfo.horizontal()
                    .item(icon)
                    .vertical()
                    .itemLabel(icon)
                    .text(CompoundText.create().style(TextStyleClass.MODNAME).text("OpenComputers"));
            return true;
        }
        return false;
    }

    @Override
    public void addProbeEntityInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world,
                                   Entity entity, IProbeHitEntityData data) {
        if (!(entity instanceof Agent agent)) return;

        Map<String, ItemStack> merged = new LinkedHashMap<>();
        addItems(merged, agent.equipmentInventory());
        addItems(merged, agent.mainInventory());
        for (ItemStack stack : merged.values()) {
            probeInfo.horizontal().item(stack).itemLabel(stack);
        }
    }

    private void addItems(Map<String, ItemStack> merged, net.minecraft.world.Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                        + "\0" + stack.getHoverName().getString();
                if (merged.containsKey(key)) {
                    merged.get(key).grow(stack.getCount());
                } else {
                    merged.put(key, stack.copy());
                }
            }
        }
    }
}
