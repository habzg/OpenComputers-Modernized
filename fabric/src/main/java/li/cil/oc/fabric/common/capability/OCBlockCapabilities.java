package li.cil.oc.fabric.common.capability;

import li.cil.oc.api.capability.SimpleComponentProvider;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedComponent;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.fabric.OpenComputers;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public final class OCBlockCapabilities {
    public static final BlockApiLookup<Environment, @Nullable Direction> ENVIRONMENT =
            BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "environment"),
                    Environment.class, Direction.class);

    public static final BlockApiLookup<SidedEnvironment, @Nullable Direction> SIDED_ENVIRONMENT =
            BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "sided_environment"),
                    SidedEnvironment.class, Direction.class);

    public static final BlockApiLookup<Colored, @Nullable Direction> COLORED =
            BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "colored"),
                    Colored.class, Direction.class);

    public static final BlockApiLookup<SimpleComponentProvider, @Nullable Direction> SIMPLE_COMPONENT_PROVIDER =
            BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "simple_component_provider"),
                    SimpleComponentProvider.class, Direction.class);

    private OCBlockCapabilities() {
    }

    public static void register(final Block[] blocks) {
        ENVIRONMENT.registerForBlocks(
                (level, pos, state, blockEntity, side) ->
                        blockEntity instanceof Environment environment ? environment : null,
                blocks);

        SIDED_ENVIRONMENT.registerForBlocks(
                (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof SidedEnvironment sidedEnvironment) return sidedEnvironment;
                    if (blockEntity instanceof Environment environment && blockEntity instanceof SidedComponent sidedComponent) {
                        return new SidedComponentEnvironment(environment, sidedComponent);
                    }
                    return null;
                },
                blocks);

        COLORED.registerForBlocks(
                (level, pos, state, blockEntity, side) ->
                        blockEntity instanceof Colored colored ? colored : null,
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
