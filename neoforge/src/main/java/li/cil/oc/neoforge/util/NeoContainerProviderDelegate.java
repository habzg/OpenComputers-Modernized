package li.cil.oc.neoforge.util;

import li.cil.oc.core.impl.util.ContainerProviderDelegate;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;

public class NeoContainerProviderDelegate extends ContainerProviderDelegate {
    @Override
    public MenuProvider getContainerProvider(int guiType, Level world, int x, int y, int z) {
        return OpenComputers.getContainerProvider(guiType, world, x, y, z);
    }
}
