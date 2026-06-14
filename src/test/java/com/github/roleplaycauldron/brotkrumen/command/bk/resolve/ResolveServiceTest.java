package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepository;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ResolveService}.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ResolveServiceTest {

    private static final UUID WORLD = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void clampsResolveRadiusToViewDistance() {
        assertEquals(10.0D, ResolveOptions.effectiveNearestNodeRadius(15.0D, 10.0D),
                "Radius should be clamped to view distance");
        assertEquals(8.0D, ResolveOptions.effectiveNearestNodeRadius(8.0D, 10.0D),
                "Radius below view distance should be used as-is");
    }

    @Test
    void loadsResolveFinishDefaults() {
        final ResolveOptions options = ResolveOptions.fromConfig(new YamlConfiguration());

        assertEquals(4.0D, options.finishRadius(), "Finish radius should use default when not configured");
        assertEquals(5, options.finishCleanupDelaySeconds(), "Cleanup delay should default to 5 seconds");
        assertTrue(options.goalMarkerEnabled(), "Goal marker should be enabled by default");
        assertTrue(options.goalOptions().messageEnabled(), "Goal message should be enabled by default");
        assertTrue(options.goalOptions().soundEnabled(), "Goal sound should be enabled by default");
        assertEquals("entity.player.levelup", options.goalOptions().soundName(),
                "Goal sound should use the default configured sound");
        assertEquals(1.0F, options.goalOptions().soundVolume(), "Goal sound volume should default to one");
        assertEquals(1.0F, options.goalOptions().soundPitch(), "Goal sound pitch should default to one");
        assertFalse(options.goalOptions().titleEnabled(), "Goal title should be disabled by default");
        assertEquals(10, options.goalOptions().titleFadeInTicks(), "Goal title fade-in should default to ten ticks");
        assertEquals(40, options.goalOptions().titleStayTicks(), "Goal title stay should default to forty ticks");
        assertEquals(10, options.goalOptions().titleFadeOutTicks(), "Goal title fade-out should default to ten ticks");
        assertEquals("LOCAL_INTERGRAPH_WARP", options.teleportRules(), "Teleport rules should allow all by default");
        assertTrue(options.autoTeleportOptions().enabled(), "Auto teleport should be enabled by default");
        assertTrue(options.autoTeleportOptions().localTeleportEnabled(), "Local auto teleport should be enabled");
        assertTrue(options.autoTeleportOptions().interGraphTeleportEnabled(), "Inter-graph auto teleport should be enabled");
        assertTrue(options.autoTeleportOptions().warpEnabled(), "Warp auto teleport should be enabled");
        assertTrue(options.autoTeleportOptions().startFromWarpWhenNoNearbyNode(), "Warp-start fallback should be enabled");
        assertEquals(5.0D, options.autoTeleportOptions().cancelRange(), "Cancel range should default to five blocks");
        assertTrue(options.awayCancellationOptions().enabled(), "Away-cancellation should be enabled by default");
        assertEquals(10.0D, options.awayCancellationOptions().distance(),
                "Away-cancellation distance should default to ten blocks");
        assertTrue(options.awayCancellationOptions().warningEnabled(),
                "Away-cancellation warning should be enabled by default");
        assertEquals(2, options.awayCancellationOptions().warningGraceSeconds(),
                "Away-cancellation grace should default to two seconds");
    }

    @Test
    void loadsResolveFinishOverrides() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("commands.resolve.finishRadius", 6.5D);
        config.set("commands.resolve.finishCleanupDelaySeconds", 9);
        config.set("commands.resolve.goalMarkerEnabled", false);
        config.set("commands.resolve.goal.message.enabled", false);
        config.set("commands.resolve.goal.sound.enabled", true);
        config.set("commands.resolve.goal.sound.name", "block.note_block.pling");
        config.set("commands.resolve.goal.sound.volume", 0.75D);
        config.set("commands.resolve.goal.sound.pitch", 1.4D);
        config.set("commands.resolve.goal.title.enabled", true);
        config.set("commands.resolve.goal.title.fadeInTicks", 5);
        config.set("commands.resolve.goal.title.stayTicks", 30);
        config.set("commands.resolve.goal.title.fadeOutTicks", 7);
        config.set("commands.resolve.autoTeleport.enabled", false);
        config.set("commands.resolve.autoTeleport.delaySeconds", 4);
        config.set("commands.resolve.autoTeleport.messageEnabled", false);
        config.set("commands.resolve.autoTeleport.cooldownSeconds", 8);
        config.set("commands.resolve.autoTeleport.cancelWhenPlayerMovesAway", false);
        config.set("commands.resolve.autoTeleport.cancelRange", 7.0D);
        config.set("commands.resolve.autoTeleport.localTeleportEnabled", false);
        config.set("commands.resolve.autoTeleport.interGraphTeleportEnabled", false);
        config.set("commands.resolve.autoTeleport.warpEnabled", false);
        config.set("commands.resolve.autoTeleport.startFromWarpWhenNoNearbyNode", false);
        config.set("commands.resolve.cancelWhenAway.enabled", false);
        config.set("commands.resolve.cancelWhenAway.distance", 12.0D);
        config.set("commands.resolve.cancelWhenAway.warningEnabled", false);
        config.set("commands.resolve.cancelWhenAway.warningGraceSeconds", 4);

        final ResolveOptions options = ResolveOptions.fromConfig(config);

        assertEquals(6.5D, options.finishRadius(), "Configured finish radius should be loaded");
        assertEquals(9, options.finishCleanupDelaySeconds(), "Configured cleanup delay should be loaded");
        assertFalse(options.goalMarkerEnabled(), "Configured goal marker flag should be loaded");
        assertEquals(180L, options.finishCleanupDelayTicks(), "Cleanup delay should convert to ticks");
        assertFalse(options.goalOptions().messageEnabled(), "Configured goal message flag should be loaded");
        assertTrue(options.goalOptions().soundEnabled(), "Configured goal sound flag should be loaded");
        assertEquals("block.note_block.pling", options.goalOptions().soundName(),
                "Configured goal sound name should be loaded");
        assertEquals(0.75F, options.goalOptions().soundVolume(), "Configured goal sound volume should be loaded");
        assertEquals(1.4F, options.goalOptions().soundPitch(), "Configured goal sound pitch should be loaded");
        assertTrue(options.goalOptions().titleEnabled(), "Configured goal title flag should be loaded");
        assertEquals(5, options.goalOptions().titleFadeInTicks(), "Configured goal title fade-in should be loaded");
        assertEquals(30, options.goalOptions().titleStayTicks(), "Configured goal title stay should be loaded");
        assertEquals(7, options.goalOptions().titleFadeOutTicks(), "Configured goal title fade-out should be loaded");
        assertFalse(options.autoTeleportOptions().enabled(), "Configured auto teleport flag should be loaded");
        assertEquals(4, options.autoTeleportOptions().delaySeconds(), "Configured delay should be loaded");
        assertFalse(options.autoTeleportOptions().messageEnabled(), "Configured message flag should be loaded");
        assertEquals(8, options.autoTeleportOptions().cooldownSeconds(), "Configured cooldown should be loaded");
        assertFalse(options.autoTeleportOptions().cancelWhenPlayerMovesAway(), "Configured cancel flag should be loaded");
        assertEquals(7.0D, options.autoTeleportOptions().cancelRange(), "Configured cancel range should be loaded");
        assertFalse(options.autoTeleportOptions().localTeleportEnabled(), "Configured local flag should be loaded");
        assertFalse(options.autoTeleportOptions().interGraphTeleportEnabled(), "Configured inter-graph flag should be loaded");
        assertFalse(options.autoTeleportOptions().warpEnabled(), "Configured warp flag should be loaded");
        assertFalse(options.autoTeleportOptions().startFromWarpWhenNoNearbyNode(), "Configured fallback flag should be loaded");
        assertFalse(options.awayCancellationOptions().enabled(), "Configured away-cancellation flag should be loaded");
        assertEquals(12.0D, options.awayCancellationOptions().distance(),
                "Configured away-cancellation distance should be loaded");
        assertFalse(options.awayCancellationOptions().warningEnabled(),
                "Configured away-cancellation warning flag should be loaded");
        assertEquals(4, options.awayCancellationOptions().warningGraceSeconds(),
                "Configured away-cancellation grace should be loaded");
        assertEquals(80L, options.awayCancellationOptions().warningGraceTicks(),
                "Configured away-cancellation grace should convert to ticks");
    }

    @Test
    void normalizesAutoTeleportOptions() {
        final ResolveAutoTeleportOptions options = new ResolveAutoTeleportOptions(true, -1, true, -2, true,
                -3.0D, true, true, true, true);

        assertEquals(0, options.delaySeconds(), "Negative delay should normalize to zero");
        assertEquals(0, options.cooldownSeconds(), "Negative cooldown should normalize to zero");
        assertEquals(0.0D, options.cancelRange(), "Negative cancel range should normalize to zero");
    }

    @Test
    void normalizesAwayCancellationOptions() {
        final ResolveAwayCancellationOptions options = new ResolveAwayCancellationOptions(true, -1.0D, true, -2);

        assertEquals(0.0D, options.distance(), "Negative away-cancellation distance should normalize to zero");
        assertEquals(0, options.warningGraceSeconds(), "Negative grace should normalize to zero");
        assertEquals(0L, options.warningGraceTicks(), "Zero grace should convert to zero ticks");
    }

    @Test
    void normalizesGoalOptions() {
        final ResolveGoalOptions options = new ResolveGoalOptions(true, true, " ", -1.0F, -2.0F, true,
                -3, -4, -5);

        assertEquals("entity.player.levelup", options.soundName(), "Blank sound should use the default");
        assertEquals(0.0F, options.soundVolume(), "Negative sound volume should normalize to zero");
        assertEquals(0.0F, options.soundPitch(), "Negative sound pitch should normalize to zero");
        assertEquals(0, options.titleFadeInTicks(), "Negative fade-in should normalize to zero");
        assertEquals(0, options.titleStayTicks(), "Negative stay should normalize to zero");
        assertEquals(0, options.titleFadeOutTicks(), "Negative fade-out should normalize to zero");
    }

    @Test
    void resolvesGraphByNameBeforeId() {
        final Graph graph = new Graph(12, "12");
        final GraphRepository graphRepository = mock(GraphRepository.class);
        when(graphRepository.getGraphByName("12")).thenReturn(Optional.of(graph));

        final Optional<Graph> resolved = new ResolveService(graphRepository).resolveGraph("12");

        assertSame(graph, resolved.orElseThrow(), "Graph name should win over numeric id parsing");
        verify(graphRepository, never()).getGraphById(12);
    }

    @Test
    void findsNearestNodeWithinRadius() {
        final Graph graph = new Graph(1, "sternchen");
        final Node near = graph.addNode(new Node(UUID.randomUUID(), 1.0D, 0.0D, 0.0D, WORLD));
        graph.addNode(new Node(UUID.randomUUID(), 5.0D, 0.0D, 0.0D, WORLD));
        final ResolveService service = new ResolveService(mock(GraphRepository.class));

        final Optional<Node> nearest = service.nearestNode(graph, new ResolveLocation(WORLD, 0.0D, 0.0D, 0.0D),
                2.0D);

        assertEquals(near, nearest.orElseThrow(), "Nearest node inside radius should be selected");
    }

    @Test
    void rejectsNearestNodeOutsideRadius() {
        final Graph graph = new Graph(1, "sternchen");
        graph.addNode(new Node(UUID.randomUUID(), 3.0D, 0.0D, 0.0D, WORLD));
        final ResolveService service = new ResolveService(mock(GraphRepository.class));

        final Optional<Node> nearest = service.nearestNode(graph, new ResolveLocation(WORLD, 0.0D, 0.0D, 0.0D),
                2.0D);

        assertTrue(nearest.isEmpty(), "No node should be selected outside radius");
    }

    @Test
    void resolvesNodeTargetsOnlyWhenAllAreInOneGraph() {
        final Graph graph = new Graph(1, "sternchen");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        graph.addNode(new Node(first, 0.0D, 0.0D, 0.0D, WORLD));
        graph.addNode(new Node(second, 1.0D, 0.0D, 0.0D, WORLD));
        final GraphRepository graphRepository = mock(GraphRepository.class);
        when(graphRepository.getAllGraphs()).thenReturn(Set.of(graph));

        final ResolveService.NodeTargetResolution result = new ResolveService(graphRepository)
                .resolveNodeTargets(Set.of(first, second));

        assertTrue(result.success(), "Nodes in the same graph should resolve");
        assertSame(graph, result.graph(), "Containing graph should be returned");
    }

    @Test
    void rejectsNodeTargetsAcrossGraphs() {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final Graph firstGraph = new Graph(1, "first");
        firstGraph.addNode(new Node(first, 0.0D, 0.0D, 0.0D, WORLD));
        final Graph secondGraph = new Graph(2, "second");
        secondGraph.addNode(new Node(second, 1.0D, 0.0D, 0.0D, WORLD));
        final GraphRepository graphRepository = mock(GraphRepository.class);
        when(graphRepository.getAllGraphs()).thenReturn(Set.of(firstGraph, secondGraph));

        final ResolveService.NodeTargetResolution result = new ResolveService(graphRepository)
                .resolveNodeTargets(Set.of(first, second));

        assertFalse(result.success(), "Nodes across graphs should be rejected");
    }

    @Test
    void resolvesNodeRefsAcrossGraphs() {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final Graph firstGraph = new Graph(1, "first");
        firstGraph.addNode(new Node(first, 0.0D, 0.0D, 0.0D, WORLD));
        final Graph secondGraph = new Graph(2, "second");
        secondGraph.addNode(new Node(second, 1.0D, 0.0D, 0.0D, WORLD));
        final GraphRepository graphRepository = mock(GraphRepository.class);
        when(graphRepository.getAllGraphs()).thenReturn(Set.of(firstGraph, secondGraph));

        final ResolveService.NodeRefTargetResolution result = new ResolveService(graphRepository)
                .resolveNodeRefTargets(Set.of(first, second));

        assertTrue(result.success(), "Node refs across graphs should resolve for network pathing");
        assertEquals(Set.of(new NodeRef(1, first), new NodeRef(2, second)), Set.copyOf(result.nodeRefs()),
                "Resolved refs should preserve graph ids");
    }

    @Test
    void findsNetworkPathToTargetGraphEntryPoint() {
        final Graph current = new Graph(1, "current");
        final Graph target = new Graph(2, "target");
        final UUID start = UUID.randomUUID();
        final UUID exit = UUID.randomUUID();
        final UUID entry = UUID.randomUUID();
        current.addNode(new Node(start, 0.0D, 0.0D, 0.0D, WORLD));
        current.addNode(new Node(exit, 1.0D, 0.0D, 0.0D, WORLD));
        target.addNode(new Node(entry, 2.0D, 0.0D, 0.0D, WORLD));
        current.addUndirectedEdge(start, exit, 1.0D);
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(current);
        network.addGraph(target);
        network.addDirectedInterGraphEdge(new NodeRef(1, exit), new NodeRef(2, entry), 1.0D);
        final GraphRepository graphRepository = mock(GraphRepository.class);
        final GraphNetworkRepository graphNetworkRepository = mock(GraphNetworkRepository.class);
        when(graphNetworkRepository.loadGraphNetworks()).thenReturn(List.of(network));
        final ResolveService service = new ResolveService(graphRepository, graphNetworkRepository);

        final PathResult path = service.findPath(network, new NodeRef(1, start), 2, TeleportRules.disableTeleports());

        assertEquals(new NodeRef(2, entry), path.nodes().getLast(),
                "Target graph path should end at an entry point into the target graph");
    }

    @Test
    void findsGraphLocalPathToBestGoal() {
        final Graph graph = new Graph(1, "sternchen");
        final UUID start = UUID.randomUUID();
        final UUID middle = UUID.randomUUID();
        final UUID goal = UUID.randomUUID();
        graph.addNode(new Node(start, 0.0D, 0.0D, 0.0D, WORLD));
        graph.addNode(new Node(middle, 1.0D, 0.0D, 0.0D, WORLD));
        graph.addNode(new Node(goal, 2.0D, 0.0D, 0.0D, WORLD));
        graph.addEdge(start, middle, 1.0D, Set.of(EdgeFlag.UNDIRECTED));
        graph.addEdge(middle, goal, 1.0D, Set.of(EdgeFlag.UNDIRECTED));

        final PathResult result = new ResolveService(mock(GraphRepository.class)).findPath(graph, start, Set.of(goal), TeleportRules.disableTeleports());

        assertFalse(result.nodes().isEmpty(), "Path should be found");
        assertEquals(goal, result.nodes().getLast().nodeId(), "Path should end at requested goal");
    }
}
