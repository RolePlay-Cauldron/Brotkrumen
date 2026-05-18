package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractGraphRendererTest {

    private final List<World> worlds = new ArrayList<>();

    @Test
    void defaultDistancesPreserveExistingVisibilityRadius() {
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final RendererHarness renderer = new RendererHarness(plugin(new YamlConfiguration(), worldId, viewerId), viewerId);
        final VisualGraphSnapshot snapshot = snapshot(worldId, 31, 33);

        renderer.apply(snapshot, ProfileGraphDesignResolver.defaults());

        assertEquals(1, renderer.nodeUpdates, "Default radius should include nodes within 32 blocks");
    }

    @Test
    void configuredDistancesControlVisibilityRadius() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("visualizer.viewDistance", 4.0D);
        config.set("visualizer.spawnDistanceBuffer", 1.0D);
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final RendererHarness renderer = new RendererHarness(plugin(config, worldId, viewerId), viewerId);
        final VisualGraphSnapshot snapshot = snapshot(worldId, 4, 6);

        renderer.apply(snapshot, ProfileGraphDesignResolver.defaults());

        assertEquals(1, renderer.nodeUpdates, "Configured radius should include only nodes within 5 blocks");
    }

    @Test
    void negativeDistancesFallBackToDefaults() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("visualizer.viewDistance", -1.0D);
        config.set("visualizer.spawnDistanceBuffer", -1.0D);
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final RendererHarness renderer = new RendererHarness(plugin(config, worldId, viewerId), viewerId);
        final VisualGraphSnapshot snapshot = snapshot(worldId, 31, 33);

        renderer.apply(snapshot, ProfileGraphDesignResolver.defaults());

        assertEquals(1, renderer.nodeUpdates, "Negative values should fall back to the 32 block default radius");
    }

    @Test
    void edgesAreVisibleOnlyWhenBothEndpointsAreVisible() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("visualizer.viewDistance", 4.0D);
        config.set("visualizer.spawnDistanceBuffer", 1.0D);
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final RendererHarness renderer = new RendererHarness(plugin(config, worldId, viewerId), viewerId);
        final VisualGraphSnapshot snapshot = snapshot(worldId, 4, 6);

        renderer.apply(snapshot, ProfileGraphDesignResolver.defaults());

        assertEquals(0, renderer.edgeUpdates, "Edge should be hidden when one endpoint is outside the configured radius");
    }

    @Test
    void endpointOnlyTeleportEdgesSkipEdgeRendering() {
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final RendererHarness renderer = new RendererHarness(plugin(new YamlConfiguration(), worldId, viewerId), viewerId);
        final VisualGraphSnapshot snapshot = snapshot(worldId, 1, 2, VisualEdgeRole.TELEPORT);

        renderer.apply(snapshot, ProfileGraphDesignResolver.defaults());

        assertEquals(2, renderer.nodeUpdates, "Teleport endpoints should still render as nodes");
        assertEquals(0, renderer.edgeUpdates, "Endpoint-only teleport edge should not render a full edge");
    }

    @Test
    void fullEdgeStrategyRendersTeleportEdges() {
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final RendererHarness renderer = new RendererHarness(plugin(new YamlConfiguration(), worldId, viewerId), viewerId);
        final VisualGraphSnapshot snapshot = snapshot(worldId, 1, 2, VisualEdgeRole.TELEPORT);
        final ProfileGraphDesignResolver resolver = new ProfileGraphDesignResolver(GraphNetworkDesignProfile.builder()
                .edgeRenderStrategy(VisualEdgeRole.TELEPORT, EdgeRenderStrategy.FULL_EDGE)
                .build());

        renderer.apply(snapshot, resolver);

        assertEquals(2, renderer.nodeUpdates, "Teleport endpoints should render as nodes");
        assertEquals(1, renderer.edgeUpdates, "Full-edge strategy should render the teleport edge");
    }

    private Brotkrumen plugin(final YamlConfiguration config, final UUID worldId, final UUID viewerId) {
        final Brotkrumen plugin = mock(Brotkrumen.class);
        final Server server = mock(Server.class);
        final Player player = mock(Player.class);
        final World world = mock(World.class);
        worlds.add(world);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPlayer(viewerId)).thenReturn(player);
        when(world.getUID()).thenReturn(worldId);
        when(player.getLocation()).thenReturn(new Location(world, 0.5D, 0.5D, 0.5D));

        return plugin;
    }

    private VisualGraphSnapshot snapshot(final UUID worldId, final int visibleX, final int hiddenX) {
        return snapshot(worldId, visibleX, hiddenX, VisualEdgeRole.DEFAULT_LOCAL);
    }

    private VisualGraphSnapshot snapshot(final UUID worldId, final int visibleX, final int hiddenX,
                                         final VisualEdgeRole edgeRole) {
        final UUID firstNodeId = UUID.randomUUID();
        final UUID secondNodeId = UUID.randomUUID();
        final VisualNode first = visualNode(worldId, firstNodeId, visibleX);
        final VisualNode second = visualNode(worldId, secondNodeId, hiddenX);
        final VisualEdge edge = new VisualEdge(
                new LocalVisualEdgeId(1, UUID.randomUUID()),
                new NodeRef(1, firstNodeId),
                new NodeRef(1, secondNodeId),
                VisualEdgeKind.LOCAL,
                1.0D,
                Set.of(),
                edgeRole
        );
        return new VisualGraphSnapshot(List.of(first, second), List.of(edge), 1L);
    }

    private VisualNode visualNode(final UUID worldId, final UUID nodeId, final int nodeX) {
        final NodeRef ref = new NodeRef(1, nodeId);
        return new VisualNode(new VisualNodeId(ref), ref, new Node(nodeId, nodeX, 0, 0, worldId));
    }

    private static final class RendererHarness extends AbstractGraphRenderer<Object, Object> {

        private int nodeUpdates;

        private int edgeUpdates;

        private RendererHarness(final Brotkrumen plugin, final UUID viewerId) {
            super(plugin, viewerId);
        }

        @Override
        protected Object updateNode(final Object handle, final VisualNode node, final GraphDesignResolver designs,
                                    final Player player) {
            nodeUpdates++;
            return new Object();
        }

        @Override
        protected Object updateEdge(final Object handle, final VisualEdge edge, final VisualGraphSnapshot snapshot,
                                    final GraphDesignResolver designs, final Player player) {
            edgeUpdates++;
            return new Object();
        }

        @Override
        protected void removeNode(final Object handle) {
            // Nothing to remove in the test renderer.
        }

        @Override
        protected void removeEdge(final Object handle) {
            // Nothing to remove in the test renderer.
        }
    }
}
