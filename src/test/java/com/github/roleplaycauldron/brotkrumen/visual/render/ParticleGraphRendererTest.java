package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.TestVisualDesigns;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleDesignSet;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleNodeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import com.github.roleplaycauldron.spellbook.effect.EffectBuilder;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutionConfig;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import com.github.roleplaycauldron.spellbook.effect.executor.RunningEffect;
import com.github.roleplaycauldron.spellbook.effect.shape.CubeShape;
import com.github.roleplaycauldron.spellbook.effect.shape.LineShape;
import com.github.roleplaycauldron.spellbook.effect.shape.MovingPointShape;
import com.github.roleplaycauldron.spellbook.effect.shape.Shape;
import com.github.roleplaycauldron.spellbook.effect.shape.SphereShape;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.AvoidAccessibilityAlteration", "PMD.CouplingBetweenObjects"})
class ParticleGraphRendererTest {

    @Test
    void mapsNodeDesignToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final EffectInstance expected = effect(Particle.HEART, new CubeShape(0.8f, 12));
        final ParticleNodeDesign design = new ParticleNodeDesign(Particle.HEART, expected);
        final EffectInstance effect = renderer.buildNodeEffect(design, null);

        assertSame(expected, effect, "Node design should expose the configured Spellbook effect instance");
    }

    @Test
    void mapsEdgeDesignToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final Node source = new Node(UUID.randomUUID(), 0, 0, 0, null);
        final Node target = new Node(UUID.randomUUID(), 3, 4, 0, null);
        final EffectInstance expected = effect(Particle.END_ROD, new MovingPointShape(1.0f, 0.2f, 8, false));
        final ParticleEdgeDesign design = new ParticleEdgeDesign(Particle.END_ROD, expected);
        final EffectInstance effect = renderer.buildEdgeEffect(design, null, visualNode(source), visualNode(target));

        assertSame(expected, effect, "Edge design should expose the configured Spellbook effect instance");
    }

    @Test
    void mapsCustomParticleNodeEffectToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final EffectInstance expected = effect(Particle.HEART, new SphereShape(0.8f, 24));
        final ParticleNodeDesign design = new ParticleNodeDesign(Particle.HEART, expected);

        final EffectInstance effect = renderer.buildNodeEffect(design, null);

        assertSame(expected, effect, "Custom particle node effect should be used");
    }

    @Test
    void mapsCustomParticleEdgeEffectToSpellbookEffect() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final EffectInstance expected = effect(Particle.END_ROD, new MovingPointShape(1.5f, 0.2f, 8, false));
        final ParticleEdgeDesign design = new ParticleEdgeDesign(Particle.END_ROD, expected);
        final Node source = new Node(UUID.randomUUID(), 0, 0, 0, null);
        final Node target = new Node(UUID.randomUUID(), 3, 4, 0, null);

        final EffectInstance effect = renderer.buildEdgeEffect(design, null, visualNode(source), visualNode(target));

        assertSame(expected, effect, "Custom particle edge effect should be used");
    }

    @Test
    void cubeAndSphereParticlePresetsAreAvailable() {
        assertAll(
                () -> assertNotNull(ParticleNodeDesign.cube(Particle.FLAME, 0.4f).effect(),
                        "Cube shape preset should expose an effect instance"),
                () -> assertNotNull(ParticleNodeDesign.sphere(Particle.END_ROD, 0.35f).effect(),
                        "Sphere shape preset should expose an effect instance")
        );
    }

    @Test
    void particleEdgeConstructorsCreateExpectedShapes() throws ReflectiveOperationException {
        final ParticleEdgeDesign line = ParticleEdgeDesign.line(Particle.FLAME, 7);
        final ParticleEdgeDesign moving = ParticleEdgeDesign.movingPoint(Particle.END_ROD, 0.4f);
        final ParticleEdgeDesign interGraph = ParticleEdgeDesign.defaultInterGraph();

        assertAll(
                () -> assertInstanceOf(LineShape.class, shape(line.effect()),
                        "Line edge design should use LineShape"),
                () -> assertInstanceOf(MovingPointShape.class, shape(moving.effect()),
                        "Moving edge design should use MovingPointShape"),
                () -> assertEquals(0.4f, spacing(shape(moving.effect())),
                        "Moving edge spacing should be passed through"),
                () -> assertInstanceOf(MovingPointShape.class, shape(interGraph.effect()),
                        "Default inter-graph edge design should use MovingPointShape")
        );
    }

    @Test
    void interGraphParticlePresetsUseDirectionSpecificShapes() throws ReflectiveOperationException {
        final ParticleDesignSet set = TestVisualDesigns.prismParticle();

        assertAll(
                () -> assertInstanceOf(MovingPointShape.class,
                        shape(set.edgeDesign(VisualEdgeRole.DIRECTED_INTER_GRAPH).effect()),
                        "Directed inter-graph preset should use moving particles"),
                () -> assertInstanceOf(LineShape.class,
                        shape(set.edgeDesign(VisualEdgeRole.UNDIRECTED_INTER_GRAPH).effect()),
                        "Undirected inter-graph preset should use line particles")
        );
    }

    @Test
    void movingPointSpacingIsClamped() throws ReflectiveOperationException {
        final ParticleEdgeDesign moving = ParticleEdgeDesign.movingPoint(Particle.END_ROD, 0.05f);

        assertEquals(0.2f, spacing(shape(moving.effect())),
                "Moving edge spacing should be clamped to the minimum");
    }

    @Test
    void sameNodeDesignCanUseDifferentExecutionLocations() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final ParticleNodeDesign design = ParticleNodeDesign.cube(Particle.FLAME, 0.4f);
        final Location first = new Location(null, 1.0D, 2.0D, 3.0D);
        final Location second = new Location(null, 4.0D, 5.0D, 6.0D);
        final Player player = mock(Player.class);

        final EffectInstance firstEffect = renderer.buildNodeEffect(design,
                visualNode(new Node(UUID.randomUUID(), 1, 2, 3, null)));
        final EffectInstance secondEffect = renderer.buildNodeEffect(design,
                visualNode(new Node(UUID.randomUUID(), 4, 5, 6, null)));
        final EffectExecutionConfig firstConfig = renderer.executionConfig(first, first, player);
        final EffectExecutionConfig secondConfig = renderer.executionConfig(second, second, player);

        assertAll(
                () -> assertSame(design.effect(), firstEffect, "First node should reuse the design effect"),
                () -> assertSame(design.effect(), secondEffect, "Second node should reuse the design effect"),
                () -> assertEquals(first, firstConfig.originAnchor().resolve(),
                        "First node location should come from execution config"),
                () -> assertEquals(second, secondConfig.originAnchor().resolve(),
                        "Second node location should come from execution config")
        );
    }

    @Test
    void sameEdgeDesignCanUseDifferentExecutionLocations() {
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(null, UUID.randomUUID(), null);
        final ParticleEdgeDesign design = ParticleEdgeDesign.movingPoint(Particle.END_ROD, 0.2f);
        final Location firstSource = new Location(null, 1.0D, 2.0D, 3.0D);
        final Location firstTarget = new Location(null, 4.0D, 5.0D, 6.0D);
        final Location secondSource = new Location(null, 7.0D, 8.0D, 9.0D);
        final Location secondTarget = new Location(null, 10.0D, 11.0D, 12.0D);
        final Player player = mock(Player.class);

        final EffectInstance firstEffect = renderer.buildEdgeEffect(design, null,
                visualNode(new Node(UUID.randomUUID(), 1, 2, 3, null)),
                visualNode(new Node(UUID.randomUUID(), 4, 5, 6, null)));
        final EffectInstance secondEffect = renderer.buildEdgeEffect(design, null,
                visualNode(new Node(UUID.randomUUID(), 7, 8, 9, null)),
                visualNode(new Node(UUID.randomUUID(), 10, 11, 12, null)));
        final EffectExecutionConfig firstConfig = renderer.executionConfig(firstSource, firstTarget, player);
        final EffectExecutionConfig secondConfig = renderer.executionConfig(secondSource, secondTarget, player);

        assertAll(
                () -> assertSame(design.effect(), firstEffect, "First edge should reuse the design effect"),
                () -> assertSame(design.effect(), secondEffect, "Second edge should reuse the design effect"),
                () -> assertEquals(firstSource, firstConfig.originAnchor().resolve(),
                        "First source location should come from execution config"),
                () -> assertEquals(firstTarget, firstConfig.targetAnchor().resolve(),
                        "First target location should come from execution config"),
                () -> assertEquals(secondSource, secondConfig.originAnchor().resolve(),
                        "Second source location should come from execution config"),
                () -> assertEquals(secondTarget, secondConfig.targetAnchor().resolve(),
                        "Second target location should come from execution config")
        );
    }

    @Test
    void visibilityOnlyUpdateKeepsExistingRunningEffects() {
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final BukkitTask task = mock(BukkitTask.class);
        final ParticleLifecycleHarness renderer = new ParticleLifecycleHarness(plugin(worldId, viewerId), viewerId, task);
        final VisualGraphSnapshot snapshot = visibleSnapshot(worldId);
        final GraphDesignResolver resolver = ProfileGraphDesignResolver.defaults();

        renderer.apply(snapshot, resolver);
        renderer.applyVisibilityOnly();
        renderer.shutdown();

        assertEquals(3, renderer.starts, "Initial apply should start two node effects and one edge effect");
        verify(task, times(3)).cancel();
    }

    @Test
    void changedSnapshotVersionKeepsUnchangedParticleEdgeEffectRunning() {
        final UUID worldId = UUID.randomUUID();
        final UUID viewerId = UUID.randomUUID();
        final World world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);
        final Brotkrumen plugin = plugin(worldId, viewerId);
        final EffectExecutor executor = mock(EffectExecutor.class);
        final BukkitTask firstNodeTask = mock(BukkitTask.class);
        final BukkitTask secondNodeTask = mock(BukkitTask.class);
        final BukkitTask edgeTask = mock(BukkitTask.class);
        final BukkitTask updatedFirstNodeTask = mock(BukkitTask.class);
        final BukkitTask updatedSecondNodeTask = mock(BukkitTask.class);
        when(executor.start(any(), any())).thenReturn(
                new RunningEffect(firstNodeTask),
                new RunningEffect(secondNodeTask),
                new RunningEffect(edgeTask),
                new RunningEffect(updatedFirstNodeTask),
                new RunningEffect(updatedSecondNodeTask)
        );
        final ParticleGraphRenderer renderer = new ParticleGraphRenderer(plugin, viewerId, executor);
        final VisualGraphSnapshot initial = visibleSnapshot(worldId);
        final VisualGraphSnapshot changedVersion = new VisualGraphSnapshot(initial.nodes(), initial.edges(), 2L);
        final GraphDesignResolver resolver = ProfileGraphDesignResolver.defaults();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            renderer.apply(initial, resolver);
            renderer.apply(changedVersion, resolver);
        }

        verify(executor, times(5)).start(any(), any());
        verify(edgeTask, never()).cancel();
    }

    private EffectInstance effect(final Particle particle, final Shape shape) {
        return EffectBuilder.create()
                .shape(shape)
                .particle(particle)
                .build();
    }

    private Shape shape(final EffectInstance effect) throws ReflectiveOperationException {
        final Field field = EffectInstance.class.getDeclaredField("shape");
        field.setAccessible(true);
        return (Shape) field.get(effect);
    }

    private float spacing(final Shape shape) throws ReflectiveOperationException {
        final Field field = MovingPointShape.class.getDeclaredField("spacing");
        field.setAccessible(true);
        return field.getFloat(shape);
    }

    private VisualNode visualNode(final Node node) {
        return new VisualNode(null, null, node);
    }

    private Brotkrumen plugin(final UUID worldId, final UUID viewerId) {
        final Brotkrumen plugin = mock(Brotkrumen.class);
        final Server server = mock(Server.class);
        final Player player = mock(Player.class);
        final World world = mock(World.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(server.getPlayer(viewerId)).thenReturn(player);
        final Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(0.5D);
        when(location.getY()).thenReturn(0.5D);
        when(location.getZ()).thenReturn(0.5D);
        when(player.getLocation()).thenReturn(location);
        when(world.getUID()).thenReturn(worldId);
        return plugin;
    }

    private VisualGraphSnapshot visibleSnapshot(final UUID worldId) {
        final UUID sourceId = UUID.randomUUID();
        final UUID targetId = UUID.randomUUID();
        final NodeRef sourceRef = new NodeRef(1, sourceId);
        final NodeRef targetRef = new NodeRef(1, targetId);
        final VisualNode source = new VisualNode(new VisualNodeId(sourceRef), sourceRef,
                new Node(sourceId, 0, 0, 0, worldId));
        final VisualNode target = new VisualNode(new VisualNodeId(targetRef), targetRef,
                new Node(targetId, 1, 0, 0, worldId));
        final VisualEdge edge = new VisualEdge(new LocalVisualEdgeId(1, UUID.randomUUID()), sourceRef, targetRef,
                VisualEdgeKind.LOCAL, 1.0D, Set.of(), VisualEdgeRole.DEFAULT_LOCAL);
        return new VisualGraphSnapshot(List.of(source, target), List.of(edge), 1L);
    }

    private static final class ParticleLifecycleHarness extends AbstractGraphRenderer<RunningEffect, RunningEffect> {

        private final BukkitTask task;

        private int starts;

        private ParticleLifecycleHarness(final Brotkrumen plugin, final UUID viewerId, final BukkitTask task) {
            super(plugin, viewerId);
            this.task = task;
        }

        @Override
        protected RunningEffect updateNode(final RunningEffect handle, final VisualNode node,
                                           final GraphDesignResolver designs, final Player player) {
            starts++;
            return new RunningEffect(task);
        }

        @Override
        protected RunningEffect updateEdge(final RunningEffect handle, final VisualEdge edge,
                                           final VisualGraphSnapshot snapshot, final GraphDesignResolver designs,
                                           final Player player) {
            starts++;
            return new RunningEffect(task);
        }

        @Override
        protected void removeNode(final RunningEffect handle) {
            handle.cancel();
        }

        @Override
        protected void removeEdge(final RunningEffect handle) {
            handle.cancel();
        }
    }
}
