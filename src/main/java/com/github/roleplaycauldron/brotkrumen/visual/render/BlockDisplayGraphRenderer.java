package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.visual.design.EdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.NodeDesign;
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
public class BlockDisplayGraphRenderer extends AbstractGraphRenderer<BlockDisplay, BlockDisplay> {

    /**
     * Creates a renderer.
     *
     * @param plugin   plugin
     * @param viewerId viewer id
     */
    public BlockDisplayGraphRenderer(final Brotkrumen plugin, final UUID viewerId) {
        super(plugin, viewerId);
    }

    @Override
    protected BlockDisplay updateNode(final BlockDisplay handle, final VisualNode node, final NodeDesign design,
                                      final Player player) {
        BlockDisplay display = handle;
        if (display == null || !display.isValid()) {
            display = spawnNodeDisplay(node.node().toCenterLocation(), design);
        }
        if (display != null) {
            display.setBlock(design.blockMaterial().createBlockData());
            display.setTransformation(nodeTransformation(design.scale()));
            display.teleport(node.node().toCenterLocation());
            player.showEntity(plugin, display);
        }
        return display;
    }

    @Override
    protected BlockDisplay updateEdge(final BlockDisplay handle, final VisualEdge edge,
                                      final VisualGraphSnapshot snapshot, final EdgeDesign design,
                                      final Player player) {
        final Map<com.github.roleplaycauldron.brotkrumen.graph.NodeRef, VisualNode> nodes = snapshot.nodesByRef();
        final VisualNode source = nodes.get(edge.source());
        final VisualNode target = nodes.get(edge.target());
        if (source == null || target == null) {
            return handle;
        }

        BlockDisplay display = handle;
        if (display == null || !display.isValid()) {
            display = spawnEdgeDisplay(source.node().toCenterLocation(), design);
        }
        if (display != null) {
            display.setBlock(design.blockMaterial().createBlockData());
            updateEdgeTransformation(display, source.node(), target.node(), design);
            player.showEntity(plugin, display);
        }
        return display;
    }

    @Override
    protected void removeNode(final BlockDisplay handle) {
        removeDisplay(handle);
    }

    @Override
    protected void removeEdge(final BlockDisplay handle) {
        removeDisplay(handle);
    }

    private BlockDisplay spawnNodeDisplay(final Location location, final NodeDesign design) {
        final World world = location.getWorld();
        if (world == null) {
            return null;
        }
        return world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(design.blockMaterial().createBlockData());
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setTransformation(nodeTransformation(design.scale()));
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

    private BlockDisplay spawnEdgeDisplay(final Location location, final EdgeDesign design) {
        final World world = location.getWorld();
        if (world == null) {
            return null;
        }
        return world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(design.blockMaterial().createBlockData());
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setTransformation(edgeTransformation(design, new Quaternionf(), 1.0f));
        });
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private void updateEdgeTransformation(final BlockDisplay display, final Node source, final Node target,
                                          final EdgeDesign design) {
        if (!source.worldId().equals(target.worldId())) {
            return;
        }

        final Vector3f sourceCenter = new Vector3f((float) source.x() + 0.5f, (float) source.y() + 0.5f, (float) source.z() + 0.5f);
        final Vector3f targetCenter = new Vector3f((float) target.x() + 0.5f, (float) target.y() + 0.5f, (float) target.z() + 0.5f);
        final Vector3f direction = new Vector3f(targetCenter).sub(sourceCenter);
        final float distance = direction.length();
        if (distance <= 0.0f) {
            return;
        }

        final float displayLength = Math.max(0.0f, distance - (2.0f * (float) design.nodeClearance()));
        final float startOffset = (distance - displayLength) / 2.0f;
        direction.normalize();

        final Vector3f displayStart = new Vector3f(sourceCenter).add(new Vector3f(direction).mul(startOffset));
        display.teleport(new Location(display.getWorld(), displayStart.x, displayStart.y, displayStart.z));
        final Quaternionf rotation = new Quaternionf().rotateTo(0.0f, 0.0f, 1.0f, direction.x, direction.y, direction.z);
        display.setTransformation(edgeTransformation(design, rotation, displayLength));
    }

    private Transformation edgeTransformation(final EdgeDesign design, final Quaternionf rotation, final float length) {
        final float thickness = design.thickness();
        return new Transformation(
                new Vector3f(-thickness / 2.0f, -thickness / 2.0f, 0.0f),
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
}
