package li.cil.oc.neoforge.common.capability;

import li.cil.oc.api.capability.SimpleComponentProvider;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedComponent;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public final class OCBlockCapabilities {
    public static final BlockCapability<Environment, @Nullable Direction> ENVIRONMENT =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "environment"), Environment.class);

    public static final BlockCapability<SidedEnvironment, @Nullable Direction> SIDED_ENVIRONMENT =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "sided_environment"), SidedEnvironment.class);

    public static final BlockCapability<Colored, @Nullable Direction> COLORED =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "colored"), Colored.class);

    public static final BlockCapability<SimpleComponentProvider, @Nullable Direction> SIMPLE_COMPONENT_PROVIDER =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "simple_component_provider"), SimpleComponentProvider.class);

    private OCBlockCapabilities() {
    }

    public static void register(final RegisterCapabilitiesEvent event, final Block[] blocks) {
        event.registerBlock(ENVIRONMENT,
                (level, pos, state, blockEntity, side) -> blockEntity instanceof Environment environment ? environment : null,
                blocks);

        event.registerBlock(SIDED_ENVIRONMENT,
                (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof SidedEnvironment sidedEnvironment) return sidedEnvironment;
                    if (blockEntity instanceof Environment environment && blockEntity instanceof SidedComponent sidedComponent) {
                        return new SidedComponentEnvironment(environment, sidedComponent);
                    }
                    return null;
                },
                blocks);

        event.registerBlock(COLORED,
                (level, pos, state, blockEntity, side) -> blockEntity instanceof Colored colored ? colored : null,
                blocks);
    }

    private record SidedComponentEnvironment(Environment environment, SidedComponent sidedComponent) implements SidedEnvironment {
        @Override
        public Node sidedNode(final Direction side) {
            return sidedComponent.canConnectNode(side) ? environment.node() : null;
        }

        @Override
        public boolean canConnect(final Direction side) {
            return sidedComponent.canConnectNode(side);
        }
    }
}
