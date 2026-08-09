package li.cil.oc.fabric.client;

import li.cil.oc.core.impl.util.Color;
import li.cil.oc.fabric.OpenComputers;
import li.cil.oc.fabric.model.CableModel;
import li.cil.oc.fabric.model.DroneModel;
import li.cil.oc.fabric.model.FloppyModel;
import li.cil.oc.fabric.model.NetSplitterModel;
import li.cil.oc.fabric.model.PrintModel;
import li.cil.oc.fabric.model.RobotModel;
import li.cil.oc.fabric.model.ScreenModel;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.resources.ResourceLocation;

public final class ModelRegistry {
    private ModelRegistry() {
    }

    public static void init() {
        ModelLoadingPlugin.register(pluginContext -> {
            for (var dyeName : Color.dyes) {
                pluginContext.addModels(
                        ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "item/floppy_" + dyeName.toLowerCase())
                );
            }

            var floppyDyeIds = new ResourceLocation[16];
            for (int i = 0; i < 16; i++) {
                floppyDyeIds[i] = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "item/floppy_" + Color.dyes[i].toLowerCase());
            }

            var cableLoc = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "block/cable");
            var capLoc = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "block/cablecap");

            var printId = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "print");
            var beaconId = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "beaconbaseprint");
            var netSplitterId = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "netsplitter");

            pluginContext.modifyModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                var topLevelId = context.topLevelId();
                var id = topLevelId != null ? topLevelId.id() : context.resourceId();
                if (id == null) return model;
                var variant = topLevelId != null ? topLevelId.variant() : "inventory";

                if (id.equals(printId)) {
                    return model != null ? new PrintModel(model) : null;
                }

                if (id.equals(beaconId)) {
                    return model != null ? new PrintModel(model) : null;
                }

                if (id.equals(netSplitterId)) {
                    return new NetSplitterModel();
                }

                if (!"inventory".equals(variant)) return model;

                if (id.equals(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "cable"))) {
                    return model != null ? new CableModel(cableLoc, capLoc, model) : null;
                }

                if (id.equals(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "floppy"))) {
                    return new FloppyModel(floppyDyeIds);
                }

                if (id.equals(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "robot"))) {
                    return model != null ? new RobotModel(model) : null;
                }

                if (id.equals(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "drone"))) {
                    return new DroneModel();
                }

                if (id.equals(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "screen1")) ||
                        id.equals(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "screen2")) ||
                        id.equals(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "screen3"))) {
                    return new ScreenModel();
                }

                return model;
            });
        });
    }
}