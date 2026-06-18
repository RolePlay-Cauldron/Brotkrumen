package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.visual.design.BlockEdgeDesign;
import org.bukkit.Material;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class BlockDisplayGraphRendererTest {

    private static final float EPSILON = 0.0001f;

    private static Vector3f centerlineStart(final BlockDisplayGraphRenderer.EdgePlacement placement,
                                            final BlockEdgeDesign design) {
        final float halfThickness = design.thickness() / 2.0f;
        final Vector3f offset = placement.rotation().transform(new Vector3f(halfThickness, halfThickness, 0.0f));
        return new Vector3f(placement.displayOrigin()).add(offset);
    }

    private static void assertVectorEquals(final Vector3f expected, final Vector3f actual) {
        assertEquals(expected.x, actual.x, EPSILON, "x");
        assertEquals(expected.y, actual.y, EPSILON, "y");
        assertEquals(expected.z, actual.z, EPSILON, "z");
    }

    @Test
    void edgePlacementCentersRotatedThicknessOnVerticalEdge() {
        final UUID worldId = UUID.randomUUID();
        final Node source = new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, worldId);
        final Node target = new Node(UUID.randomUUID(), 0.0D, 4.0D, 0.0D, worldId);
        final BlockEdgeDesign design = new BlockEdgeDesign(Material.GLASS, 0.2f, 0.5D);

        final BlockDisplayGraphRenderer.EdgePlacement placement =
                BlockDisplayGraphRenderer.edgePlacement(source, target, design);

        assertNotNull(placement, "The placement should not be null");
        assertVectorEquals(new Vector3f(0.5f, 1.0f, 0.5f), centerlineStart(placement, design));
        assertEquals(3.0f, placement.length(), EPSILON, "The placement length should be 3.0");
    }

    @Test
    void edgePlacementCentersRotatedThicknessOnDiagonalEdge() {
        final UUID worldId = UUID.randomUUID();
        final Node source = new Node(UUID.randomUUID(), 0.0D, 0.0D, 0.0D, worldId);
        final Node target = new Node(UUID.randomUUID(), 3.0D, 4.0D, 0.0D, worldId);
        final BlockEdgeDesign design = new BlockEdgeDesign(Material.GLASS, 0.2f, 0.5D);

        final BlockDisplayGraphRenderer.EdgePlacement placement =
                BlockDisplayGraphRenderer.edgePlacement(source, target, design);

        assertNotNull(placement, "The placement should not be null");
        assertVectorEquals(new Vector3f(0.8f, 0.9f, 0.5f), centerlineStart(placement, design));
        assertEquals(4.0f, placement.length(), EPSILON, "The placement length should be 4.0");
    }

    @Test
    void edgePlacementUsesBlockCentersForFractionalNodeCoordinates() {
        final UUID worldId = UUID.randomUUID();
        final Node source = new Node(UUID.randomUUID(), 10.8D, 64.0D, -3.2D, worldId);
        final Node target = new Node(UUID.randomUUID(), 12.1D, 64.0D, -3.2D, worldId);
        final BlockEdgeDesign design = new BlockEdgeDesign(Material.GLASS, 0.2f, 0.5D);

        final BlockDisplayGraphRenderer.EdgePlacement placement =
                BlockDisplayGraphRenderer.edgePlacement(source, target, design);

        assertNotNull(placement, "The placement should not be null");
        assertVectorEquals(new Vector3f(11.0f, 64.5f, -3.5f), centerlineStart(placement, design));
        assertEquals(1.0f, placement.length(), EPSILON, "The placement length should be 1.0");
    }

    @Test
    void edgePlacementRejectsZeroLengthEdges() {
        final UUID worldId = UUID.randomUUID();
        final UUID nodeId = UUID.randomUUID();
        final Node source = new Node(nodeId, 0.0D, 0.0D, 0.0D, worldId);
        final Node target = new Node(nodeId, 0.0D, 0.0D, 0.0D, worldId);
        final BlockEdgeDesign design = new BlockEdgeDesign(Material.GLASS, 0.2f, 0.5D);

        assertNull(BlockDisplayGraphRenderer.edgePlacement(source, target, design), "The placement should be null for zero-length edges");
    }
}
