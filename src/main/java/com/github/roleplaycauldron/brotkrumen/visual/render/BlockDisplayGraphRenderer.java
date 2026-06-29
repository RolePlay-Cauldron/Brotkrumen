package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.BlockEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.BlockNodeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;

/**
 * Block display renderer for visual graph snapshots.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class BlockDisplayGraphRenderer extends AbstractGraphRenderer<BlockDisplay, BlockDisplay> {

    private static final String SCOREBOARD_TAG_OWNER = "brotkrumen";

    private static final String SCOREBOARD_TAG_NODE = "brotkrumen:block-display:node";

    private static final String SCOREBOARD_TAG_EDGE = "brotkrumen:block-display:edge";

    /**
     * Creates a renderer.
     *
     * @param plugin   plugin
     * @param viewerId viewer id
     */
    public BlockDisplayGraphRenderer(final Brotkrumen plugin, final UUID viewerId) {
        super(plugin, viewerId);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    /* default */
    static EdgePlacement edgePlacement(final Node source, final Node target, final BlockEdgeDesign design) {
        final Vector3f sourceCenter = visualCenter(source);
        final Vector3f targetCenter = visualCenter(target);
        final Vector3f direction = new Vector3f(targetCenter).sub(sourceCenter);
        final float distance = direction.length();
        if (distance <= 0.0f) {
            return null;
        }
        final float displayLength = Math.max(0.0f, distance - (2.0f * (float) design.nodeClearance()));
        final float startOffset = (distance - displayLength) / 2.0f;
        direction.normalize();

        final Vector3f displayStart = new Vector3f(sourceCenter).add(new Vector3f(direction).mul(startOffset));
        final Quaternionf rotation = new Quaternionf()
                .rotateTo(0.0f, 0.0f, 1.0f, direction.x, direction.y, direction.z);
        final float halfThickness = design.thickness() / 2.0f;
        final Vector3f rotatedCenteringOffset = rotation.transform(new Vector3f(halfThickness, halfThickness, 0.0f));
        return new EdgePlacement(displayStart.sub(rotatedCenteringOffset), rotation, displayLength, displayStart);
    }

    private static Vector3f visualCenter(final Node node) {
        return new Vector3f(centerCoordinate(node.x()), centerCoordinate(node.y()), centerCoordinate(node.z()));
    }

    private static float centerCoordinate(final double value) {
        return (float) Math.floor(value) + 0.5f;
    }

    @Override
    protected BlockDisplay updateNode(final BlockDisplay handle, final VisualNode node,
                                      final GraphDesignResolver designs,
                                      final Player player) {
        final BlockNodeDesign design = designs.resolveBlockNode(node);
        BlockDisplay display = handle;
        if (display == null || !display.isValid()) {
            display = spawnNodeDisplay(node.node().toCenterLocation(), design);
        }
        if (display != null) {
            display.setBlock(design.blockMaterial().createBlockData());
            display.setTransformation(nodeTransformation(design.scale()));
            display.teleport(node.node().toCenterLocation());
        }
        return display;
    }

    @Override
    protected BlockDisplay updateEdge(final BlockDisplay handle, final VisualEdge edge,
                                      final VisualGraphSnapshot snapshot, final GraphDesignResolver designs,
                                      final Player player) {
        final Map<NodeRef, VisualNode> nodes = snapshot.nodesByRef();
        final VisualNode source = nodes.get(edge.source());
        final VisualNode target = nodes.get(edge.target());
        if (source == null || target == null) {
            return handle;
        }

        final BlockEdgeDesign design = designs.resolveBlockEdge(edge);
        BlockDisplay display = handle;
        if (display == null || !display.isValid()) {
            display = spawnEdgeDisplay(source.node().toCenterLocation(), design);
        }
        if (display != null) {
            display.setBlock(design.blockMaterial().createBlockData());
            updateEdgeTransformation(display, source.node(), target.node(), design);
        }
        return display;
    }

    @Override
    protected void updateNodeVisibility(final BlockDisplay handle, final Player player, final boolean visible) {
        updateDisplayVisibility(handle, player, visible);
    }

    @Override
    protected void updateEdgeVisibility(final BlockDisplay handle, final Player player, final boolean visible) {
        updateDisplayVisibility(handle, player, visible);
    }

    @Override
    protected void removeNode(final BlockDisplay handle) {
        removeDisplay(handle);
    }

    @Override
    protected void removeEdge(final BlockDisplay handle) {
        removeDisplay(handle);
    }

    @Override
    protected boolean retainsInvisibleHandles() {
        return true;
    }

    private BlockDisplay spawnNodeDisplay(final Location location, final BlockNodeDesign design) {
        final World world = location.getWorld();
        if (world == null) {
            return null;
        }
        return world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(design.blockMaterial().createBlockData());
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setTransformation(nodeTransformation(design.scale()));
            applyScoreboardTags(entity, SCOREBOARD_TAG_NODE);
        });
    }

    private Transformation nodeTransformation(final float scale) {
        final float halfExtent = scale / 2.0f;
        return new Transformation(
                new Vector3f(-halfExtent, -halfExtent, -halfExtent),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        );
    }

    private BlockDisplay spawnEdgeDisplay(final Location location, final BlockEdgeDesign design) {
        final World world = location.getWorld();
        if (world == null) {
            return null;
        }
        return world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(design.blockMaterial().createBlockData());
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setTransformation(edgeTransformation(design, new Quaternionf(), 1.0f));
            applyScoreboardTags(entity, SCOREBOARD_TAG_EDGE);
        });
    }

    private void applyScoreboardTags(final BlockDisplay display, final String roleTag) {
        display.addScoreboardTag(SCOREBOARD_TAG_OWNER);
        display.addScoreboardTag(roleTag);
    }

    private void updateEdgeTransformation(final BlockDisplay display, final Node source, final Node target,
                                          final BlockEdgeDesign design) {
        if (!source.worldId().equals(target.worldId())) {
            return;
        }

        final EdgePlacement placement = edgePlacement(source, target, design);
        if (placement == null) {
            return;
        }

        final Vector3f displayOrigin = placement.displayOrigin();
        display.teleport(new Location(display.getWorld(), displayOrigin.x, displayOrigin.y, displayOrigin.z));
        display.setTransformation(edgeTransformation(design, placement.rotation(), placement.length()));
    }

    private Transformation edgeTransformation(final BlockEdgeDesign design, final Quaternionf rotation,
                                              final float length) {
        final float thickness = design.thickness();
        return new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(thickness, thickness, length),
                new Quaternionf()
        );
    }

    private void removeDisplay(final BlockDisplay display) {
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    private void updateDisplayVisibility(final BlockDisplay display, final Player player, final boolean visible) {
        if (display == null || !display.isValid()) {
            return;
        }
        if (visible) {
            player.showEntity(plugin, display);
        } else {
            player.hideEntity(plugin, display);
        }
    }

    /**
     * Represents the geometric placement of an edge within a graph rendering context.
     * The placement includes positional and rotational information, as well as the
     * calculated edge length and adjustments for alignment along a defined path.
     * <p>
     * This record encapsulates the key properties required to visualize an edge as
     * part of the rendered graph, enabling transformations and alignment to be
     * performed accurately within a 3D environment.
     *
     * @param displayOrigin   The origin point in 3D space from which the edge rendering begins,
     *                        typically relative to some global or local coordinate system.
     * @param rotation        The orientation of the edge in 3D space, represented as a quaternion.
     * @param length          The length of the edge, determining the scalar extension along its direction.
     * @param centerlineStart The starting point of the edge's centerline, used to define its alignment.
     */
    /* default */
    record EdgePlacement(Vector3f displayOrigin, Quaternionf rotation, float length, Vector3f centerlineStart) {
    }
}
