package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

/**
 * Role-keyed block-display designs for one graph or network.
 *
 * @param nodeDesigns block-display node designs by role
 * @param edgeDesigns block-display edge designs by role
 */
public record BlockDisplayDesignSet(Map<VisualNodeRole, BlockNodeDesign> nodeDesigns,
                                    Map<VisualEdgeRole, BlockEdgeDesign> edgeDesigns) {

    /**
     * Creates the default block-display design set.
     *
     * @return default block-display design set
     */
    public static BlockDisplayDesignSet defaults() {
        return emberPreset();
    }

    /**
     * Creates a warm block-display preset.
     *
     * @return warm block-display preset
     */
    public static BlockDisplayDesignSet emberPreset() {
        return new BlockDisplayDesignSet(
                nodeDesignMap(new BlockNodeDesign(Material.COAL_BLOCK, 0.45f),
                        new BlockNodeDesign(Material.MAGMA_BLOCK, 0.55f),
                        new BlockNodeDesign(Material.REDSTONE_BLOCK, 0.5f),
                        new BlockNodeDesign(Material.OBSIDIAN, 0.55f)),
                edgeDesignMap(
                        new BlockEdgeDesign(Material.ORANGE_WOOL, 0.16f, 0.8D),
                        new BlockEdgeDesign(Material.GREEN_STAINED_GLASS, 0.16f, 0.8D),
                        new BlockEdgeDesign(Material.GREEN_STAINED_GLASS, 0.16f, 0.8D),
                        new BlockEdgeDesign(Material.RED_WOOL, 0.16f, 0.8D),
                        new BlockEdgeDesign(Material.GOLD_BLOCK, 0.16f, 0.8D),
                        new BlockEdgeDesign(Material.WHITE_STAINED_GLASS, 0.16f, 0.8D),
                        new BlockEdgeDesign(Material.WHITE_STAINED_GLASS, 0.18f, 0.8D))
        );
    }

    /**
     * Creates a cool block-display preset.
     *
     * @return cool block-display preset
     */
    public static BlockDisplayDesignSet prismPreset() {
        return new BlockDisplayDesignSet(
                nodeDesignMap(new BlockNodeDesign(Material.LAPIS_BLOCK, 0.45f),
                        new BlockNodeDesign(Material.AMETHYST_BLOCK, 0.55f),
                        new BlockNodeDesign(Material.PURPUR_BLOCK, 0.5f),
                        new BlockNodeDesign(Material.END_STONE, 0.55f)),
                edgeDesignMap(
                        new BlockEdgeDesign(Material.LIGHT_BLUE_STAINED_GLASS, 0.16f, 0.8D),
                        new BlockEdgeDesign(Material.CYAN_STAINED_GLASS, 0.18f, 0.8D),
                        new BlockEdgeDesign(Material.CYAN_STAINED_GLASS, 0.2f, 0.8D),
                        new BlockEdgeDesign(Material.RED_STAINED_GLASS, 0.22f, 0.8D),
                        new BlockEdgeDesign(Material.BLACK_STAINED_GLASS, 0.22f, 0.8D),
                        new BlockEdgeDesign(Material.GRAY_STAINED_GLASS, 0.22f, 0.8D),
                        new BlockEdgeDesign(Material.GRAY_STAINED_GLASS, 0.24f, 0.8D))
        );
    }

    private static Map<VisualNodeRole, BlockNodeDesign> nodeDesignMap(final BlockNodeDesign defaultNode,
                                                                      final BlockNodeDesign localTeleportNode,
                                                                      final BlockNodeDesign intergraphTeleportNode,
                                                                      final BlockNodeDesign warpNode) {
        final Map<VisualNodeRole, BlockNodeDesign> result = new EnumMap<>(VisualNodeRole.class);
        result.put(VisualNodeRole.DEFAULT, defaultNode);
        result.put(VisualNodeRole.LOCAL_TELEPORT, localTeleportNode);
        result.put(VisualNodeRole.INTERGRAPH_TELEPORT, intergraphTeleportNode);
        result.put(VisualNodeRole.WARP, warpNode);
        return result;
    }

    private static Map<VisualEdgeRole, BlockEdgeDesign> edgeDesignMap(final BlockEdgeDesign localEdge,
                                                                      final BlockEdgeDesign directedEdge,
                                                                      final BlockEdgeDesign undirectedEdge,
                                                                      final BlockEdgeDesign blockedEdge,
                                                                      final BlockEdgeDesign interGraphEdge,
                                                                      final BlockEdgeDesign directedInterGraphEdge,
                                                                      final BlockEdgeDesign undirectedInterGraphEdge) {
        final Map<VisualEdgeRole, BlockEdgeDesign> result = new EnumMap<>(VisualEdgeRole.class);
        result.put(VisualEdgeRole.DEFAULT_LOCAL, localEdge);
        result.put(VisualEdgeRole.DIRECTED_LOCAL, directedEdge);
        result.put(VisualEdgeRole.UNDIRECTED_LOCAL, undirectedEdge);
        result.put(VisualEdgeRole.BLOCKED, blockedEdge);
        result.put(VisualEdgeRole.INTER_GRAPH, interGraphEdge);
        result.put(VisualEdgeRole.DIRECTED_INTER_GRAPH, directedInterGraphEdge);
        result.put(VisualEdgeRole.UNDIRECTED_INTER_GRAPH, undirectedInterGraphEdge);
        return result;
    }

    /**
     * Resolves a block-display node design by role.
     *
     * @param role visual node role
     * @return matching design or default node design
     */
    public BlockNodeDesign nodeDesign(final VisualNodeRole role) {
        return nodeDesigns.getOrDefault(role, nodeDesigns.get(VisualNodeRole.DEFAULT));
    }

    /**
     * Resolves a block-display edge design by role.
     *
     * @param role visual edge role
     * @return matching design or default local edge design
     */
    public BlockEdgeDesign edgeDesign(final VisualEdgeRole role) {
        return edgeDesigns.getOrDefault(role, edgeDesigns.get(VisualEdgeRole.DEFAULT_LOCAL));
    }
}
