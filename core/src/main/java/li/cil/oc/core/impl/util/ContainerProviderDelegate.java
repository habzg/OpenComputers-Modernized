package li.cil.oc.core.impl.util;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;

public abstract class ContainerProviderDelegate {
    private static ContainerProviderDelegate instance;

    public static void setInstance(ContainerProviderDelegate inst) {
        instance = inst;
    }

    public static ContainerProviderDelegate get() {
        return instance;
    }

    public abstract MenuProvider getContainerProvider(int guiType, Level world, int x, int y, int z);
}
