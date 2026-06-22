package li.cil.oc.neoforge.integration.top;

import li.cil.oc.core.impl.common.tileentity.Case;
import li.cil.oc.core.impl.common.tileentity.Microcontroller;
import li.cil.oc.core.impl.common.tileentity.Rack;
import li.cil.oc.neoforge.common.tileentity.RobotProxy;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfig.ConfigMode;
import mcjty.theoneprobe.api.IProbeConfigProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public class OCProbeConfigProvider implements IProbeConfigProvider {

    @Override
    public void getProbeConfig(IProbeConfig config, Player player, Level world,
                               BlockState blockState, IProbeHitData data) {
        BlockEntity be = world.getBlockEntity(data.getPos());
        if (be instanceof Case || be instanceof Microcontroller || be instanceof Rack || be instanceof RobotProxy) {
            config.showChestContents(ConfigMode.NOT);
        }
    }

    @Override
    public void getProbeConfig(IProbeConfig config, Player player, Level world,
                               Entity entity, IProbeHitEntityData data) {
    }
}
