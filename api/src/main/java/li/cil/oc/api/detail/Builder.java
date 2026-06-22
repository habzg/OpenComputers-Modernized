package li.cil.oc.api.detail;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import org.jetbrains.annotations.NotNull;

/**
 * Used for building {@link Node}s via {@link li.cil.oc.api.Network#newNode}.
 *
 * @param <T> the type of the node created by this builder.
 */
public interface Builder<T extends Node> {
    /**
     * Finalizes the construction of the node.
     * <br>
     * This performs the actual creation of the node, initializes it to the
     * settings defined by the current builder and returns it.
     *
     * @return the final node.
     */
    T create();

    /**
     * Builder for basic nodes. These nodes merely allow network access and
     * take on no special role.
     */
    interface NodeBuilder extends Builder<Node> {
        /**
         * Makes the node a li.cil.oc.common.component.
         * <br>
         * Nodes that are components can be accessed from computers, methods
         * declared in them marked using the {@link li.cil.oc.api.machine.Callback} annotation can
         * be invoked from computers that can see the li.cil.oc.common.component.
         *
         * @param name       the name of the li.cil.oc.common.component.
         * @param visibility the visibility of the li.cil.oc.common.component.
         * @return a builder for a node that is also a li.cil.oc.common.component.
         * @see li.cil.oc.api.network.Component
         */
        @NotNull ComponentBuilder withComponent(@NotNull String name, @NotNull Visibility visibility);

        /**
         * Makes the node a li.cil.oc.common.component.
         * <br>
         * Like {@link #withComponent(String, Visibility)}, but with a default
         * visibility set to the <em>reachability</em> of the node.
         *
         * @param name the name of the li.cil.oc.common.component.
         * @return a builder for a node that is also a li.cil.oc.common.component.
         * @see li.cil.oc.api.network.Component
         */
        @NotNull ComponentBuilder withComponent(@NotNull String name);

        /**
         * Makes the node a connector.
         * <br>
         * A connector node can feed power into the network and extract power
         * from the network. This is used both for passive energy drain (such
         * as running screens and computers) and for active power consumption
         * (such as wireless message sending or robot actions).
         *
         * @param bufferSize the size of the local energy buffer.
         * @return a builder for a node that is also a connector.
         * @see li.cil.oc.api.network.Connector
         */
        @NotNull ConnectorBuilder withConnector(double bufferSize);

        /**
         * Makes the node a connector.
         * <br>
         * Like {@link #withConnector(double)}, but with a default buffer size
         * of zero.
         *
         * @return a builder for a node that is also a connector.
         * @see li.cil.oc.api.network.Connector
         */
        @NotNull ConnectorBuilder withConnector();
    }

    /**
     * Builder for component nodes. These node can be interacted with from
     * computers in the same network, that can <em>see</em> the li.cil.oc.common.component.
     */
    interface ComponentBuilder extends Builder<Component> {
        /**
         * Makes the node a connector.
         * <br>
         * A connector node can feed power into the network and extract power
         * from the network. This is used both for passive energy drain (such
         * as running screens and computers) and for active power consumption
         * (such as wireless message sending or robot actions).
         *
         * @param bufferSize the size of the local energy buffer.
         * @return a builder for a node that is also a connector.
         * @see li.cil.oc.api.network.Connector
         */
        @NotNull ComponentConnectorBuilder withConnector(double bufferSize);

        /**
         * Makes the node a connector.
         * <br>
         * Like {@link #withConnector(double)}, but with a default buffer size
         * of zero.
         *
         * @return a builder for a node that is also a connector.
         * @see li.cil.oc.api.network.Connector
         */
        @NotNull ComponentConnectorBuilder withConnector();
    }

    /**
     * Builder for connector nodes. These nodes can interact with the energy
     * stored in the network, i.e. increase or reduce it.
     */
    interface ConnectorBuilder extends Builder<Connector> {
        /**
         * Makes the node a li.cil.oc.common.component.
         * <br>
         * Nodes that are components can be accessed from computers, methods
         * declared in them marked using the {@link li.cil.oc.api.machine.Callback} annotation can
         * be invoked from computers that can see the li.cil.oc.common.component.
         *
         * @param name       the name of the li.cil.oc.common.component.
         * @param visibility the visibility of the li.cil.oc.common.component.
         * @return a builder for a node that is also a li.cil.oc.common.component.
         * @see li.cil.oc.api.network.Component
         */
        @SuppressWarnings("unused")
        @NotNull ComponentConnectorBuilder withComponent(@NotNull String name, @NotNull Visibility visibility);

        /**
         * Makes the node a li.cil.oc.common.component.
         * <br>
         * Like {@link #withComponent(String, Visibility)}, but with a default
         * visibility set to the <em>reachability</em> of the node.
         *
         * @param name the name of the li.cil.oc.common.component.
         * @return a builder for a node that is also a li.cil.oc.common.component.
         * @see li.cil.oc.api.network.Component
         */
        @SuppressWarnings("unused")
        @NotNull ComponentConnectorBuilder withComponent(@NotNull String name);
    }

    /**
     * Builder for nodes that are both component <em>and</em> connector node.
     */
    interface ComponentConnectorBuilder extends Builder<ComponentConnector> {
    }

}
