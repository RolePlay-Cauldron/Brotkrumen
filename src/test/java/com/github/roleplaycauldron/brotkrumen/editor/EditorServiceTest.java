package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepository;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepository;
import com.github.roleplaycauldron.brotkrumen.storage.repository.WarpRepository;
import com.github.roleplaycauldron.brotkrumen.visual.TestVisualDesigns;
import com.github.roleplaycauldron.brotkrumen.visual.Visualizer;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualPreset;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualPresetRegistry;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualRenderer;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EditorService}.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.UnitTestAssertionsShouldIncludeMessage", "PMD.ShortVariable", "PMD.CouplingBetweenObjects"})
class EditorServiceTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    private static final UUID WORLD_ID = UUID.randomUUID();

    @Mock
    private GraphRepository graphRepository;

    @Mock
    private GraphNetworkRepository graphNetworkRepository;

    @Mock
    private WarpRepository warpRepository;

    @Mock
    private World world;

    @Mock
    private Player player;

    @Mock
    private LoggerFactory loggerFactory;

    @Mock
    private WrappedLogger logger;

    @Mock
    private Brotkrumen plugin;

    @Mock
    private Server server;

    @Mock
    private BukkitScheduler scheduler;

    @Mock
    private VisualizerRegistry visualizerRegistry;

    private EditorService service;

    private EditorWaitingActionBarReminder reminder;

    @BeforeEach
    void setUp() {
        lenient().when(world.getUID()).thenReturn(WORLD_ID);
        lenient().when(loggerFactory.create(any())).thenReturn(logger);
        lenient().when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        lenient().when(plugin.getVisualPresetRegistry()).thenReturn(new VisualPresetRegistry(Map.of(
                "ember", new VisualPreset("ember", TestVisualDesigns.emberParticle(), TestVisualDesigns.emberBlock()),
                "prism", new VisualPreset("prism", TestVisualDesigns.prismParticle(), TestVisualDesigns.prismBlock())
        )));
        lenient().when(plugin.getServer()).thenReturn(server);
        lenient().when(server.getScheduler()).thenReturn(scheduler);
        runScheduledTasksImmediately();
        service = new EditorService(null, plugin, loggerFactory, null, graphRepository, graphNetworkRepository,
                warpRepository);
        reminder = new EditorWaitingActionBarReminder(service);
    }

    private void runScheduledTasksImmediately() {
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return mock(BukkitTask.class);
        }).when(scheduler).runTask(any(org.bukkit.plugin.Plugin.class), any(Runnable.class));
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return mock(BukkitTask.class);
        }).when(scheduler).runTaskAsynchronously(any(org.bukkit.plugin.Plugin.class), any(Runnable.class));
    }

    @Test
    void previewModePreventsMovementPlacement() {
        startCreation(defaultSettings(EditorService.PlacementMode.AUTO));

        assertTrue(service.preview(PLAYER_ID).success());
        assertTrue(service.handleMovement(PLAYER_ID, location(0.0D)).success());

        assertEquals(0, service.getWorkingGraph(PLAYER_ID).getNodes().size());
    }

    @Test
    void manualPlaceCreatesAndConnectsNodes() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));

        assertTrue(service.placeNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.placeNode(PLAYER_ID, location(5.0D)).success());

        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        assertEquals(2, graph.getNodes().size());
        assertEquals(2, graph.getEdges().size());
    }

    @Test
    void editManualPlaceRequiresAnchor() {
        final Graph graph = new Graph(7, "Existing");
        graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        when(graphRepository.getGraphByName("Existing")).thenReturn(Optional.of(graph));

        assertTrue(service.startGraphEdit(PLAYER_ID, "Existing", defaultSettings()).success());
        assertFalse(service.placeNode(PLAYER_ID, location(5.0D)).success());
        assertTrue(service.handleMovement(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.placeNode(PLAYER_ID, location(5.0D)).success());

        assertEquals(2, service.getWorkingGraph(PLAYER_ID).getNodes().size());
    }

    @Test
    void editPlacementModeCanStartInPreview() {
        final Graph graph = new Graph(7, "Existing");
        graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        when(graphRepository.getGraphByName("Existing")).thenReturn(Optional.of(graph));

        final EditorService.EditorSettings settings = new EditorService.EditorSettings(4,
                EditorService.PlacementMode.AUTO, EditorService.PlacementMode.PREVIEW, false, true, "ember");

        assertTrue(service.startGraphEdit(PLAYER_ID, "Existing", settings).success());
        assertFalse(service.isWaitingForAppendAnchor(PLAYER_ID));
        assertTrue(service.handleMovement(PLAYER_ID, location(0.0D)).success());
        assertFalse(service.placeNode(PLAYER_ID, location(5.0D)).success());

        assertEquals(1, service.getWorkingGraph(PLAYER_ID).getNodes().size());
    }

    @Test
    void placementCanSnapNewNodesToGround() {
        when(world.getHighestBlockYAt(any(Location.class))).thenReturn(63);
        startCreation(new EditorService.EditorSettings(4, EditorService.PlacementMode.PREVIEW,
                EditorService.PlacementMode.PREVIEW, true, true, "ember"));

        assertTrue(service.placeNode(PLAYER_ID, location(2.0D, 80.0D, 3.0D)).success());

        final Node node = service.getWorkingGraph(PLAYER_ID).getNodes().iterator().next();
        assertAll(
                () -> assertEquals(2.0D, node.x()),
                () -> assertEquals(64.0D, node.y()),
                () -> assertEquals(3.0D, node.z())
        );
    }

    @Test
    void continueCanWaitForNodeBeforeAutoPlacement() {
        final Graph graph = new Graph(7, "Existing");
        graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        when(graphRepository.getGraphByName("Existing")).thenReturn(Optional.of(graph));

        assertTrue(service.startGraphEdit(PLAYER_ID, "Existing", defaultSettings()).success());
        assertTrue(service.continuePlacement(PLAYER_ID).success());
        assertTrue(service.isWaitingForAppendAnchor(PLAYER_ID));
        reminder.sendWaitingAnchorActionBars(ignored -> player);
        reminder.sendWaitingAnchorActionBars(ignored -> player);
        verify(player, times(2)).sendActionBar(any(Component.class));
        assertTrue(service.handleMovement(PLAYER_ID, location(5.0D)).success());
        assertEquals(1, service.getWorkingGraph(PLAYER_ID).getNodes().size());

        assertTrue(service.handleMovement(PLAYER_ID, location(0.0D)).success());
        assertFalse(service.isWaitingForAppendAnchor(PLAYER_ID));
        assertTrue(service.handleMovement(PLAYER_ID, location(5.0D)).success());
        assertEquals(2, service.getWorkingGraph(PLAYER_ID).getNodes().size());
    }

    @Test
    void undoRemovesOnlySessionCreatedNodes() {
        final Graph graph = new Graph(7, "Existing");
        graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        when(graphRepository.getGraphByName("Existing")).thenReturn(Optional.of(graph));

        assertTrue(service.startGraphEdit(PLAYER_ID, "Existing", defaultSettings()).success());
        assertTrue(service.handleMovement(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.placeNode(PLAYER_ID, location(5.0D)).success());
        assertTrue(service.placeNode(PLAYER_ID, location(10.0D)).success());

        final EditorService.EditorResult result = service.undo(PLAYER_ID, 5);

        assertTrue(result.success());
        assertTrue(service.isWaitingForAppendAnchor(PLAYER_ID));
        assertEquals(1, service.getWorkingGraph(PLAYER_ID).getNodes().size());
        assertFalse(service.undo(PLAYER_ID, 1).success());
    }

    @Test
    void waitingReminderEligibilityStopsAfterPlacementModeChange() {
        startCreation(defaultSettings(EditorService.PlacementMode.AUTO));

        assertTrue(service.placeNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.undo(PLAYER_ID, 1).success());
        assertTrue(service.isWaitingForAppendAnchor(PLAYER_ID));
        reminder.sendWaitingAnchorActionBars(ignored -> player);
        reminder.sendWaitingAnchorActionBars(ignored -> player);
        verify(player, times(2)).sendActionBar(any(Component.class));

        assertTrue(service.updatePlacementMode(PLAYER_ID, EditorService.PlacementMode.PREVIEW).success());
        assertFalse(service.isWaitingForAppendAnchor(PLAYER_ID));
        reminder.sendWaitingAnchorActionBars(ignored -> player);
        verify(player, times(2)).sendActionBar(any(Component.class));
    }

    @Test
    void waitingReminderEligibilityStopsAfterFinishOrCancel() {
        startCreation(defaultSettings(EditorService.PlacementMode.WAITING_FOR_ANCHOR));

        assertTrue(service.isWaitingForAppendAnchor(PLAYER_ID));
        reminder.sendWaitingAnchorActionBars(ignored -> player);
        assertTrue(service.finishRouteCreation(PLAYER_ID).success());
        assertFalse(service.isWaitingForAppendAnchor(PLAYER_ID));
        reminder.sendWaitingAnchorActionBars(ignored -> player);

        startCreation(defaultSettings(EditorService.PlacementMode.WAITING_FOR_ANCHOR));
        reminder.sendWaitingAnchorActionBars(ignored -> player);
        assertTrue(service.cancel(PLAYER_ID).success());
        assertFalse(service.isWaitingForAppendAnchor(PLAYER_ID));
        reminder.sendWaitingAnchorActionBars(ignored -> player);
        verify(player, times(2)).sendActionBar(any(Component.class));
    }

    @Test
    void settingsAreTemporaryAndMutable() {
        startCreation(new EditorService.EditorSettings(3, EditorService.PlacementMode.PREVIEW, false, "ember"));

        assertEquals(new EditorService.EditorSettings(3, EditorService.PlacementMode.PREVIEW, false, "ember"),
                service.getSettings(PLAYER_ID));
        assertTrue(service.updateNodeDistance(PLAYER_ID, 9).success());
        assertTrue(service.updatePlacementMode(PLAYER_ID, EditorService.PlacementMode.AUTO).success());
        assertTrue(service.updateContinueRequiresNode(PLAYER_ID, true).success());
        assertTrue(service.updatePlaceNodesOnGround(PLAYER_ID, true).success());
        assertTrue(service.updatePreset(PLAYER_ID, "prism").success());

        assertEquals(new EditorService.EditorSettings(9, EditorService.PlacementMode.AUTO,
                        EditorService.PlacementMode.WAITING_FOR_ANCHOR, true, true, "prism"),
                service.getSettings(PLAYER_ID));
        assertEquals("true", service.settingsSummary(PLAYER_ID).replacements().get("place_nodes_on_ground"));
        assertTrue(service.cancel(PLAYER_ID).success());
        assertNull(service.getSettings(PLAYER_ID));
    }

    @Test
    void presetSettingValidatesAgainstActiveRendererRegistry() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("visualizer.defaultRenderer", "blockDisplay");
        final Brotkrumen plugin = mock(Brotkrumen.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getVisualPresetRegistry()).thenReturn(new VisualPresetRegistry(Map.of(
                "particle-only", new VisualPreset("particle-only", TestVisualDesigns.emberParticle(), null),
                "block-only", new VisualPreset("block-only", null, TestVisualDesigns.emberBlock())
        )));
        final EditorService registryBackedService = new EditorService(null, plugin, loggerFactory, null,
                graphRepository, graphNetworkRepository, warpRepository);
        when(graphRepository.getGraphByName("Route")).thenReturn(Optional.empty());

        assertTrue(registryBackedService.startGraphCreation(PLAYER_ID, "Route",
                        new EditorService.EditorSettings(3, EditorService.PlacementMode.PREVIEW, false, "block-only"))
                .success());
        assertEquals(Set.of("block-only"), registryBackedService.supportedPresetsForActiveRenderer(),
                "Suggestions should include only presets compatible with the active renderer");
        assertFalse(registryBackedService.updatePreset(PLAYER_ID, "particle-only").success(),
                "Particle-only presets should be rejected while blockDisplay is active");
        assertTrue(registryBackedService.updatePreset(PLAYER_ID, "block-only").success(),
                "Block-display-compatible presets should be accepted");
        assertEquals("block-only", registryBackedService.getSettings(PLAYER_ID).preset());
    }

    @Test
    void editorVisualizerRegistrationUsesParticleRendererByDefault() {
        when(graphRepository.getGraphByName("Route")).thenReturn(Optional.empty());
        final EditorService registryBackedService = new EditorService(visualizerRegistry, plugin, loggerFactory, null,
                graphRepository, graphNetworkRepository, warpRepository);

        assertTrue(registryBackedService.startGraphCreation(PLAYER_ID, "Route", defaultSettings()).success());

        verify(visualizerRegistry).register(eq(PLAYER_ID), any(Visualizer.class));
    }

    @Test
    void editorVisualizerRegistrationUsesBlockDisplayRendererWhenConfigured() {
        plugin.getConfig().set("visualizer.defaultRenderer", "blockDisplay");
        when(graphRepository.getGraphByName("Route")).thenReturn(Optional.empty());
        final EditorService registryBackedService = new EditorService(visualizerRegistry, plugin, loggerFactory, null,
                graphRepository, graphNetworkRepository, warpRepository);

        assertTrue(registryBackedService.startGraphCreation(PLAYER_ID, "Route", defaultSettings()).success());

        verify(visualizerRegistry).register(eq(PLAYER_ID), any(Visualizer.class));
    }

    @Test
    void editorVisualizerRefreshIsDelegatedWhenPresetChanges() {
        when(graphRepository.getGraphByName("Route")).thenReturn(Optional.empty());
        final EditorService registryBackedService = new EditorService(visualizerRegistry, plugin, loggerFactory, null,
                graphRepository, graphNetworkRepository, warpRepository);

        assertTrue(registryBackedService.startGraphCreation(PLAYER_ID, "Route", defaultSettings()).success());
        assertTrue(registryBackedService.updatePreset(PLAYER_ID, "prism").success());

        verify(visualizerRegistry).refresh(PLAYER_ID);
    }

    @Test
    void presetSettingRequiresRuntimeRegistry() {
        final Brotkrumen noRegistryPlugin = mock(Brotkrumen.class);
        when(noRegistryPlugin.getVisualPresetRegistry()).thenReturn(null);
        final EditorService noRegistryService = new EditorService(null, noRegistryPlugin, loggerFactory, null,
                graphRepository, graphNetworkRepository, warpRepository);

        assertTrue(noRegistryService.supportedPresetsForActiveRenderer().isEmpty());
        assertFalse(noRegistryService.startGraphCreation(PLAYER_ID, "Route",
                new EditorService.EditorSettings(3, EditorService.PlacementMode.PREVIEW, false, "ember")).success());
    }

    @Test
    void newGraphsReceiveConfiguredRendererSpecificPresetDefaults() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("visualizer.defaultSpellbookEffectPreset", "Prism_Value");
        config.set("visualizer.defaultBlockDisplayPreset", "Ember_Value");
        final Brotkrumen plugin = mock(Brotkrumen.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getVisualPresetRegistry()).thenReturn(new VisualPresetRegistry(Map.of(
                "ember", new VisualPreset("ember", TestVisualDesigns.emberParticle(), TestVisualDesigns.emberBlock())
        )));
        final EditorService pluginBackedService = new EditorService(null, plugin, loggerFactory, null,
                graphRepository, graphNetworkRepository, warpRepository);
        when(graphRepository.getGraphByName("Route")).thenReturn(Optional.empty());

        assertTrue(pluginBackedService.startGraphCreation(PLAYER_ID, "Route", defaultSettings()).success());

        final Graph graph = pluginBackedService.getWorkingGraph(PLAYER_ID);
        assertEquals("prism-value", graph.getSpellbookEffectPreset(),
                "New graph should store normalized configured Spellbook effect default");
        assertEquals("ember-value", graph.getBlockDisplayPreset(),
                "New graph should store normalized configured block-display default");
    }

    @Test
    void graphPresetUpdatesPersistedRendererSpecificPresetFields() {
        when(graphRepository.getGraphByName("Route")).thenReturn(Optional.empty());
        assertTrue(service.startGraphCreation(PLAYER_ID, "Route", defaultSettings()).success());

        assertTrue(service.updateGraphPreset(PLAYER_ID, VisualRenderer.SPELLBOOK_EFFECT, "Prism").success());
        assertTrue(service.updateGraphPreset(PLAYER_ID, VisualRenderer.BLOCK_DISPLAY, "Ember").success());

        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        assertEquals("prism", graph.getSpellbookEffectPreset());
        assertEquals("ember", graph.getBlockDisplayPreset());
        assertTrue(service.finishRouteCreation(PLAYER_ID).success());
        verify(graphRepository).saveGraph(argThat(saved -> "prism".equals(saved.getSpellbookEffectPreset())
                && "ember".equals(saved.getBlockDisplayPreset())));
    }

    @Test
    void graphPresetUpdateRejectsMissingRenderer() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));

        final EditorService.EditorResult result = service.updateGraphPreset(PLAYER_ID, null, "ember");

        assertFalse(result.success());
        assertEquals("commands.bkeditor.common.rendererRequired", result.message());
    }

    @Test
    void placementModeParsingAcceptsCommandNames() {
        assertEquals(Optional.of(EditorService.PlacementMode.AUTO), EditorService.PlacementMode.parse("auto"));
        assertEquals(Optional.of(EditorService.PlacementMode.PREVIEW), EditorService.PlacementMode.parse("preview"));
        assertEquals(Optional.of(EditorService.PlacementMode.WAITING_FOR_ANCHOR),
                EditorService.PlacementMode.parse("waiting-for-anchor"));
        assertTrue(EditorService.PlacementMode.parse("missing").isEmpty());
    }

    @Test
    void selectedEditNodeDoesNotBecomeAppendAnchor() {
        final Graph graph = new Graph(7, "Existing");
        graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        when(graphRepository.getGraphByName("Existing")).thenReturn(Optional.of(graph));

        assertTrue(service.startGraphEdit(PLAYER_ID, "Existing", defaultSettings()).success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());

        assertFalse(service.placeNode(PLAYER_ID, location(5.0D)).success(),
                "Edit selection should not become the placement append anchor");
    }

    @Test
    void selectsInspectsAndTeleportsToEdgeMidpoint() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        final Node first = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node second = graph.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        graph.addUndirectedEdge(first.graphId(), second.graphId(), 1.0D);

        assertTrue(service.selectNearbyEdge(PLAYER_ID, location(2.0D)).success());
        assertEquals("commands.bkeditor.selection.selectedEdgeDetailed", service.showSelection(PLAYER_ID).message());

        final EditorService.SelectionTeleportResult teleport = service.teleportToSelection(PLAYER_ID, location(2.0D));
        assertTrue(teleport.result().success());
        assertEquals(2.0D, teleport.destination().getX(), "Edge teleport should use the midpoint");
    }

    @Test
    void clearSelectionRemovesSelectedGraphElement() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        service.getWorkingGraph(PLAYER_ID)
                .addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));

        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.clearSelection(PLAYER_ID).success());

        assertFalse(service.showSelection(PLAYER_ID).success(), "Cleared selection should not be inspectable");
        assertNull(service.teleportToSelection(PLAYER_ID, location(0.0D)).destination(),
                "Cleared selection should not produce a teleport destination");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void selectedNodePairCreatesDirectedEdgeInSelectionOrder() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        final Node first = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node second = graph.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));

        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(4.0D)).success());
        assertTrue(service.createSelectedNodeEdge(PLAYER_ID, EditorService.EdgeType.DIRECTED).success());

        final Edge edge = graph.getEdges().iterator().next();
        assertEquals(1, graph.getEdges().size());
        assertEquals(first.graphId(), edge.source(), "First selected node should be directed source");
        assertEquals(second.graphId(), edge.target(), "Second selected node should be directed target");
        assertTrue(edge.flags().contains(EdgeFlag.DIRECTED));
        assertFalse(edge.flags().contains(EdgeFlag.BLOCKED), "Newly set edges should be open");
    }

    @Test
    void selectedNodePairCreatesUndirectedEdgeAndReplacesExistingRelationship() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        final Node first = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node second = graph.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        graph.addDirectedEdge(second.graphId(), first.graphId(), 1.0D);

        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(4.0D)).success());
        assertTrue(service.createSelectedNodeEdge(PLAYER_ID, EditorService.EdgeType.UNDIRECTED).success());

        assertEquals(2, graph.getEdgesBetween(first.graphId(), second.graphId()).size());
        assertTrue(graph.getEdgesBetween(first.graphId(), second.graphId()).stream()
                .allMatch(edge -> edge.flags().contains(EdgeFlag.UNDIRECTED)));
    }

    @Test
    void selectedEdgeClearsNodePairForEdgeCreation() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        final Node first = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node second = graph.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        graph.addDirectedEdge(first.graphId(), second.graphId(), 1.0D);

        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(4.0D)).success());
        assertTrue(service.selectNearbyEdge(PLAYER_ID, location(2.0D)).success());

        assertFalse(service.createSelectedNodeEdge(PLAYER_ID, EditorService.EdgeType.DIRECTED).success());
    }

    @Test
    void edgeCommandsRejectMissingSelection() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));

        assertFalse(service.createSelectedNodeEdge(PLAYER_ID, EditorService.EdgeType.DIRECTED).success());
        assertFalse(service.updateSelectedEdgeType(PLAYER_ID, EditorService.EdgeType.DIRECTED).success());
        assertFalse(service.updateSelectedEdgeState(PLAYER_ID, EditorService.EdgeState.BLOCKED).success());
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void selectedEdgeTypeAndStateCanChange() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        final Node first = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node second = graph.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        graph.addDirectedEdge(first.graphId(), second.graphId(), 1.0D, Set.of(EdgeFlag.BLOCKED));

        assertTrue(service.selectNearbyEdge(PLAYER_ID, location(2.0D)).success());
        assertTrue(service.updateSelectedEdgeType(PLAYER_ID, EditorService.EdgeType.UNDIRECTED).success());

        assertEquals(2, graph.getEdgesBetween(first.graphId(), second.graphId()).size());
        assertTrue(graph.getEdgesBetween(first.graphId(), second.graphId()).stream()
                .allMatch(edge -> edge.flags().contains(EdgeFlag.UNDIRECTED)));
        assertTrue(graph.getEdgesBetween(first.graphId(), second.graphId()).stream()
                .allMatch(edge -> edge.flags().contains(EdgeFlag.BLOCKED)));

        assertTrue(service.updateSelectedEdgeState(PLAYER_ID, EditorService.EdgeState.OPEN).success());

        assertTrue(graph.getEdgesBetween(first.graphId(), second.graphId()).stream()
                .allMatch(edge -> edge.flags().contains(EdgeFlag.UNDIRECTED)));
        assertTrue(graph.getEdgesBetween(first.graphId(), second.graphId()).stream()
                .noneMatch(edge -> edge.flags().contains(EdgeFlag.BLOCKED)));
    }

    @Test
    void selectedDirectedEdgeCanBeRemoved() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        final Node first = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node second = graph.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        graph.addDirectedEdge(first.graphId(), second.graphId(), 1.0D);

        assertTrue(service.selectNearbyEdge(PLAYER_ID, location(2.0D)).success());
        assertTrue(service.removeSelectedEdge(PLAYER_ID).success());

        assertTrue(graph.getEdgesBetween(first.graphId(), second.graphId()).isEmpty());
        assertFalse(service.removeSelectedEdge(PLAYER_ID).success(), "Removed edge should no longer be selected");
    }

    @Test
    void selectedUndirectedEdgeRemovalRemovesReciprocalRecords() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        final Node first = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node second = graph.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        graph.addUndirectedEdge(first.graphId(), second.graphId(), 1.0D);

        assertTrue(service.selectNearbyEdge(PLAYER_ID, location(2.0D)).success());
        assertTrue(service.removeSelectedEdge(PLAYER_ID).success());

        assertTrue(graph.getEdgesBetween(first.graphId(), second.graphId()).isEmpty());
    }

    @Test
    void edgeTypeAndStateParsingAcceptsCommandNames() {
        assertEquals(Optional.of(EditorService.EdgeType.DIRECTED), EditorService.EdgeType.parse("directed"));
        assertEquals(Optional.of(EditorService.EdgeType.UNDIRECTED), EditorService.EdgeType.parse("undirected"));
        assertEquals(Optional.of(EditorService.EdgeState.OPEN), EditorService.EdgeState.parse("open"));
        assertEquals(Optional.of(EditorService.EdgeState.BLOCKED), EditorService.EdgeState.parse("blocked"));
        assertTrue(EditorService.EdgeType.parse("missing").isEmpty());
        assertTrue(EditorService.EdgeState.parse("missing").isEmpty());
    }

    @Test
    void selectedWarpUsesDefaultsAndMarksTarget() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Node target = service.getWorkingGraph(PLAYER_ID)
                .addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));

        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.createSelectedWarp(PLAYER_ID, "spawn").success());

        verify(warpRepository, never()).saveWarp(any());
        assertTrue(service.getWorkingGraph(PLAYER_ID).getNodeById(target.graphId()).flags().contains(NodeFlag.WARP));

        assertTrue(service.finishRouteCreation(PLAYER_ID).success());
        verify(warpRepository).saveWarp(new Warp("spawn", target.graphId(), 1.0D, true, true));
    }

    @Test
    void hereWarpCreatesTargetNodeAndMetadataCanBeEdited() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));

        assertTrue(service.createWarpHere(PLAYER_ID, "spawn", location(6.0D)).success());
        final Node target = service.getWorkingGraph(PLAYER_ID).getNodes().iterator().next();

        assertTrue(service.updateWarpCost(PLAYER_ID, "spawn", 4.0D).success());
        assertTrue(service.updateWarpEnabled(PLAYER_ID, "spawn", false).success());
        assertTrue(service.updateWarpPermission(PLAYER_ID, "spawn", false).success());

        verify(warpRepository, never()).saveWarp(any());
        assertTrue(target.flags().contains(NodeFlag.WARP), "Here warp target should carry the warp flag");

        assertTrue(service.finishRouteCreation(PLAYER_ID).success());
        verify(warpRepository).saveWarp(new Warp("spawn", target.graphId(), 4.0D, false, false));
    }

    @Test
    void removalKeepsWarpFlagWhileAnotherWarpTargetsNode() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Node target = service.getWorkingGraph(PLAYER_ID).addNode(
                new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID, Set.of(NodeFlag.WARP)));
        final Warp removed = new Warp("spawn", target.graphId(), 1.0D, true, true);
        when(warpRepository.getWarp("spawn")).thenReturn(Optional.of(removed));
        when(warpRepository.getWarpsTargeting(target.graphId())).thenReturn(Set.of(
                new Warp("hub", target.graphId(), 1.0D, true, true)));

        assertTrue(service.removeWarp(PLAYER_ID, "spawn").success());

        assertTrue(service.getWorkingGraph(PLAYER_ID).getNodeById(target.graphId()).flags().contains(NodeFlag.WARP));
    }

    @Test
    void removalClearsWarpFlagAfterLastWarp() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Node target = service.getWorkingGraph(PLAYER_ID).addNode(
                new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID, Set.of(NodeFlag.WARP)));
        when(warpRepository.getWarp("spawn")).thenReturn(Optional.of(new Warp("spawn", target.graphId(), 1.0D, true, true)));
        when(warpRepository.getWarpsTargeting(target.graphId())).thenReturn(Set.of());

        assertTrue(service.removeWarp(PLAYER_ID, "spawn").success());

        assertFalse(service.getWorkingGraph(PLAYER_ID).getNodeById(target.graphId()).flags().contains(NodeFlag.WARP));
        verify(warpRepository, never()).removeWarp("spawn");

        assertTrue(service.finishRouteCreation(PLAYER_ID).success());
        verify(warpRepository).removeWarp("spawn");
    }

    @Test
    void activeGraphWarpListQueriesOnlyWorkingGraphNodeIds() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final UUID targetId = service.getWorkingGraph(PLAYER_ID)
                .addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID)).graphId();
        when(warpRepository.getWarpsTargeting(List.of(targetId))).thenReturn(Set.of());

        assertTrue(service.listWarps(PLAYER_ID, false).success());

        verify(warpRepository).getWarpsTargeting(List.of(targetId));
        verify(warpRepository, never()).getManagedWarps();
    }

    @Test
    void deleteSelectedNodeRemovesOnlyActiveGraphNode() {
        startCreation(defaultSettings(EditorService.PlacementMode.PREVIEW));
        final Graph graph = service.getWorkingGraph(PLAYER_ID);
        final Node first = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node second = graph.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        graph.addUndirectedEdge(first.graphId(), second.graphId(), 1.0D);

        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.deleteSelectedNode(PLAYER_ID).success());

        assertNull(graph.getNodeById(first.graphId()), "Selected node should be removed");
        assertTrue(graph.getEdges().isEmpty(), "Edges connected to the removed node should be removed");
        assertFalse(service.deleteSelectedNode(PLAYER_ID).success(), "Selection should be cleared after deletion");
    }

    @Test
    void deleteSelectedNodeRemovesPersistedInterGraphEdgesTouchingIt() {
        final Graph active = new Graph(1, "Active");
        final Graph reference = new Graph(2, "Reference");
        final Node activeNode = active.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node referenceNode = reference.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        final InterGraphEdge edge = new InterGraphEdge(10, UUID.randomUUID(),
                new NodeRef(1, activeNode.graphId()), new NodeRef(2, referenceNode.graphId()), 1.0D,
                Set.of(EdgeFlag.INTER_GRAPH, EdgeFlag.DIRECTED), true);
        when(graphRepository.getGraphByName("Active")).thenReturn(Optional.of(active));
        when(graphRepository.getGraphByName("Reference")).thenReturn(Optional.of(reference));
        when(graphNetworkRepository.loadInterGraphEdges(any())).thenReturn(Set.of(edge));

        assertTrue(service.startGraphEdit(PLAYER_ID, "Active", defaultSettings()).success());
        assertTrue(service.addReferenceGraph(PLAYER_ID, "Reference").success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.deleteSelectedNode(PLAYER_ID).success());
        assertTrue(service.finishRouteCreation(PLAYER_ID).success());

        verify(graphNetworkRepository).saveInterGraphEdges(org.mockito.Mockito
                .<com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork>argThat(network ->
                        network.getInterGraphEdges().isEmpty()
                                && network.hasGraph(1)
                                && network.hasGraph(2)));
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void referenceGraphViewEnablesCrossGraphEdgeAuthoringLifecycle() {
        final Graph active = new Graph(1, "Active");
        final Graph reference = new Graph(2, "Reference");
        final Node activeNode = active.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        final Node referenceNode = reference.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        when(graphRepository.getGraphByName("Active")).thenReturn(Optional.of(active));
        when(graphRepository.getGraphByName("Reference")).thenReturn(Optional.of(reference));
        when(graphNetworkRepository.loadInterGraphEdges(any())).thenReturn(Set.of());

        assertTrue(service.startGraphEdit(PLAYER_ID, "Active", defaultSettings()).success());
        assertTrue(service.addReferenceGraph(PLAYER_ID, "Reference").success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(4.0D)).success());
        assertTrue(service.createSelectedNodeEdge(PLAYER_ID, EditorService.EdgeType.UNDIRECTED).success());
        assertTrue(service.updateSelectedEdgeTraversal(PLAYER_ID, EditorService.EdgeTraversal.TELEPORT).success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        final EditorService.EditorResult connections = service.selectedNodeConnections(PLAYER_ID);
        assertEquals("commands.bkeditor.connections.header", connections.message());
        assertTrue(connections.extraMessages().stream()
                .anyMatch(message -> "commands.bkeditor.connections.scope.inter-graph"
                        .equals(message.localizedReplacements().get("scope"))));
        assertTrue(service.finishRouteCreation(PLAYER_ID).success());

        verify(graphNetworkRepository).saveInterGraphEdges(org.mockito.Mockito
                .<com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork>argThat(network ->
                        network.getInterGraphEdges().size() == 2
                                && network.getInterGraphEdges().stream()
                                .allMatch(edge -> edge.flags().contains(EdgeFlag.INTER_GRAPH))
                                && network.getInterGraphEdges().stream()
                                .allMatch(edge -> edge.flags().contains(EdgeFlag.TELEPORT))));
        assertTrue(active.getNodeById(activeNode.graphId()).flags().contains(NodeFlag.INTERGRAPH_TELEPORT));
        assertFalse(reference.getNodeById(referenceNode.graphId()).flags().contains(NodeFlag.INTERGRAPH_TELEPORT),
                "Reference graph copy should be mutated, not the persisted source instance");
    }

    @Test
    void createSessionRejectsInterGraphAuthoring() {
        final Graph reference = new Graph(2, "Reference");
        reference.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        when(graphRepository.getGraphByName("Route")).thenReturn(Optional.empty());
        when(graphRepository.getGraphByName("Reference")).thenReturn(Optional.of(reference));

        assertTrue(service.startGraphCreation(PLAYER_ID, "Route",
                defaultSettings(EditorService.PlacementMode.PREVIEW)).success());
        service.getWorkingGraph(PLAYER_ID).addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        assertTrue(service.addReferenceGraph(PLAYER_ID, "Reference").success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(4.0D)).success());

        assertFalse(service.createSelectedNodeEdge(PLAYER_ID, EditorService.EdgeType.DIRECTED).success());
        verify(graphNetworkRepository, never()).saveInterGraphEdges(anyCollection());
        verify(graphNetworkRepository, never()).saveInterGraphEdges(any(com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork.class));
    }

    @Test
    void cancelDiscardsStagedInterGraphEdges() {
        final Graph active = new Graph(1, "Active");
        final Graph reference = new Graph(2, "Reference");
        active.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID));
        reference.addNode(new Node(UUID.randomUUID(), 4.0D, 0.0D, 0.0D, WORLD_ID));
        when(graphRepository.getGraphByName("Active")).thenReturn(Optional.of(active));
        when(graphRepository.getGraphByName("Reference")).thenReturn(Optional.of(reference));
        when(graphNetworkRepository.loadInterGraphEdges(any())).thenReturn(Set.of());

        assertTrue(service.startGraphEdit(PLAYER_ID, "Active", defaultSettings()).success());
        assertTrue(service.addReferenceGraph(PLAYER_ID, "Reference").success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(0.0D)).success());
        assertTrue(service.selectNearbyNode(PLAYER_ID, location(4.0D)).success());
        assertTrue(service.createSelectedNodeEdge(PLAYER_ID, EditorService.EdgeType.DIRECTED).success());
        assertTrue(service.cancel(PLAYER_ID).success());

        verify(graphNetworkRepository, never()).saveInterGraphEdges(anyCollection());
        verify(graphNetworkRepository, never()).saveInterGraphEdges(any(com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork.class));
    }

    @Test
    void persistedGraphDeletionCleansDependentRecordsAndRejectsActiveGraph() {
        final Graph graph = new Graph(9, "DeleteMe");
        final UUID nodeId = graph.addNode(new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, WORLD_ID)).graphId();
        when(graphRepository.getGraphByName("DeleteMe")).thenReturn(Optional.of(graph));
        when(warpRepository.removeWarpsTargeting(List.of(nodeId))).thenReturn(1);
        when(graphNetworkRepository.deleteInterGraphEdgesForGraph(9)).thenReturn(2);

        final EditorService.EditorResult deleted = service.deletePersistedGraph("DeleteMe");

        assertTrue(deleted.success());
        assertEquals("commands.bkeditor.graph.deleted", deleted.message());
        assertEquals("1", deleted.replacements().get("warps"));
        assertEquals("2", deleted.replacements().get("edges"));
        verify(graphRepository).deleteGraph(9);
        verify(graphRepository).reloadGraphs();

        assertTrue(service.startGraphEdit(PLAYER_ID, "DeleteMe", defaultSettings()).success());
        assertFalse(service.deletePersistedGraph("DeleteMe").success());
    }

    @Test
    void traversalParsingAcceptsCommandNames() {
        assertEquals(Optional.of(EditorService.EdgeTraversal.NORMAL), EditorService.EdgeTraversal.parse("normal"));
        assertEquals(Optional.of(EditorService.EdgeTraversal.TELEPORT), EditorService.EdgeTraversal.parse("teleport"));
        assertTrue(EditorService.EdgeTraversal.parse("missing").isEmpty());
    }

    private void startCreation(final EditorService.EditorSettings settings) {
        when(graphRepository.getGraphByName("Route")).thenReturn(Optional.empty());
        assertTrue(service.startGraphCreation(PLAYER_ID, "Route", settings).success());
    }

    private EditorService.EditorSettings defaultSettings() {
        return defaultSettings(EditorService.PlacementMode.AUTO);
    }

    private EditorService.EditorSettings defaultSettings(final EditorService.PlacementMode placementMode) {
        return new EditorService.EditorSettings(4, placementMode, true, "ember");
    }

    private Location location(final double x) {
        return new Location(world, x, 0.0D, 0.0D);
    }

    private Location location(final double x, final double y, final double z) {
        return new Location(world, x, y, z);
    }
}
