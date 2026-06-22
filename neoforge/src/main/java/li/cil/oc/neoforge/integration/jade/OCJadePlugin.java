package li.cil.oc.neoforge.integration.jade;

import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.neoforge.common.block.SimpleBlock;
import li.cil.oc.neoforge.common.init.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin
@SuppressWarnings("unused")
public class OCJadePlugin implements IWailaPlugin {
    @Override
    public void register(final IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(OCDataProvider.INSTANCE, SimpleBlock.class);
        registration.registerItemStorage(OCItemSuppressionProvider.INSTANCE, SimpleBlock.class);
        registration.registerItemStorage(OCItemSuppressionProvider.INSTANCE, Drone.class);
    }

    @Override
    public void registerClient(final IWailaClientRegistration registration) {
        registration.registerBlockComponent(OCDataProvider.INSTANCE, SimpleBlock.class);
        registration.registerEntityIcon(DroneIconProvider.INSTANCE, Drone.class);
        registration.registerItemStorageClient(OCItemSuppressionProvider.INSTANCE);
    }

    private enum DroneIconProvider implements IComponentProvider<EntityAccessor> {
        INSTANCE;

        private static final ItemStack DRONE_ICON = new ItemStack(Items.DRONE.get());
        private static final ResourceLocation UID = ResourceLocation.parse("opencomputers:drone_icon");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Nullable
        @Override
        public IElement getIcon(EntityAccessor accessor, IPluginConfig config, IElement currentIcon) {
            return IElementHelper.get().item(DRONE_ICON);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        }

        @Override
        public int getDefaultPriority() {
            return 100;
        }
    }
}
