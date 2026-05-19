package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import org.bukkit.Particle;

import java.util.EnumMap;
import java.util.Map;

/**
 * Role-keyed particle designs for one graph or network.
 *
 * @param nodeDesigns particle node designs by role
 * @param edgeDesigns particle edge designs by role
 */
public record ParticleDesignSet(Map<VisualNodeRole, ParticleNodeDesign> nodeDesigns,
                                Map<VisualEdgeRole, ParticleEdgeDesign> edgeDesigns) {

    /**
     * Creates the default particle design set.
     *
     * @return default particle design set
     */
    public static ParticleDesignSet defaults() {
        return cubePreset();
    }

    /**
     * Creates a default cube-shaped particle design set.
     *
     * @return cube particle preset
     */
    public static ParticleDesignSet cubePreset() {
        return new ParticleDesignSet(
                nodeDesignMap(ParticleNodeDesign.cube(Particle.FLAME, 0.4f),
                        ParticleNodeDesign.cube(Particle.PORTAL, 0.55f),
                        ParticleNodeDesign.sphere(Particle.REVERSE_PORTAL, 0.55f),
                        ParticleNodeDesign.sphere(Particle.END_ROD, 0.55f)),
                edgeDesignMap(ParticleEdgeDesign.line(Particle.FLAME, 20),
                        ParticleEdgeDesign.movingPoint(Particle.FLAME, 0.2f),
                        ParticleEdgeDesign.line(Particle.FLAME, 20),
                        ParticleEdgeDesign.line(Particle.SMOKE, 20),
                        ParticleEdgeDesign.movingPoint(Particle.END_ROD, 0.2f),
                        ParticleEdgeDesign.movingPoint(Particle.END_ROD, 0.2f),
                        ParticleEdgeDesign.line(Particle.END_ROD, 20))
        );
    }

    /**
     * Creates a default sphere-shaped particle design set.
     *
     * @return sphere particle preset
     */
    public static ParticleDesignSet spherePreset() {
        return new ParticleDesignSet(
                nodeDesignMap(ParticleNodeDesign.sphere(Particle.END_ROD, 0.35f),
                        ParticleNodeDesign.sphere(Particle.PORTAL, 0.45f),
                        ParticleNodeDesign.cube(Particle.REVERSE_PORTAL, 0.5f),
                        ParticleNodeDesign.sphere(Particle.WITCH, 0.5f)),
                edgeDesignMap(ParticleEdgeDesign.line(Particle.END_ROD, 20),
                        ParticleEdgeDesign.movingPoint(Particle.END_ROD, 0.2f),
                        ParticleEdgeDesign.line(Particle.END_ROD, 20),
                        ParticleEdgeDesign.line(Particle.SMOKE, 20),
                        ParticleEdgeDesign.movingPoint(Particle.WITCH, 0.18f),
                        ParticleEdgeDesign.movingPoint(Particle.WITCH, 0.2f),
                        ParticleEdgeDesign.line(Particle.WITCH, 20))
        );
    }

    /**
     * Creates a warm particle preset.
     *
     * @return warm particle preset
     */
    public static ParticleDesignSet emberPreset() {
        return new ParticleDesignSet(
                nodeDesignMap(ParticleNodeDesign.cube(Particle.FLAME, 0.45f),
                        ParticleNodeDesign.sphere(Particle.ASH, 0.55f),
                        ParticleNodeDesign.cube(Particle.LAVA, 0.5f),
                        ParticleNodeDesign.sphere(Particle.PORTAL, 0.55f)),
                edgeDesignMap(ParticleEdgeDesign.line(Particle.FLAME, 20),
                        ParticleEdgeDesign.movingPoint(Particle.FLAME, 0.2f),
                        ParticleEdgeDesign.line(Particle.FLAME, 20),
                        ParticleEdgeDesign.line(Particle.SMOKE, 20),
                        ParticleEdgeDesign.line(Particle.LAVA, 20),
                        ParticleEdgeDesign.movingPoint(Particle.LAVA, 0.2f),
                        ParticleEdgeDesign.line(Particle.LAVA, 20))
        );
    }

    /**
     * Creates a cool particle preset.
     *
     * @return cool particle preset
     */
    public static ParticleDesignSet prismPreset() {
        return new ParticleDesignSet(
                nodeDesignMap(ParticleNodeDesign.cube(Particle.END_ROD, 0.45f),
                        ParticleNodeDesign.sphere(Particle.PORTAL, 0.55f),
                        ParticleNodeDesign.cube(Particle.REVERSE_PORTAL, 0.5f),
                        ParticleNodeDesign.sphere(Particle.WITCH, 0.55f)),
                edgeDesignMap(ParticleEdgeDesign.line(Particle.END_ROD, 20),
                        ParticleEdgeDesign.movingPoint(Particle.END_ROD, 0.2f),
                        ParticleEdgeDesign.line(Particle.END_ROD, 20),
                        ParticleEdgeDesign.line(Particle.SMOKE, 20),
                        ParticleEdgeDesign.movingPoint(Particle.WITCH, 0.18f),
                        ParticleEdgeDesign.movingPoint(Particle.WITCH, 0.2f),
                        ParticleEdgeDesign.line(Particle.WITCH, 20))
        );
    }

    private static Map<VisualNodeRole, ParticleNodeDesign> nodeDesignMap(final ParticleNodeDesign defaultNode,
                                                                         final ParticleNodeDesign localTeleportNode,
                                                                         final ParticleNodeDesign intergraphTeleportNode,
                                                                         final ParticleNodeDesign warpNode) {
        final Map<VisualNodeRole, ParticleNodeDesign> result = new EnumMap<>(VisualNodeRole.class);
        result.put(VisualNodeRole.DEFAULT, defaultNode);
        result.put(VisualNodeRole.LOCAL_TELEPORT, localTeleportNode);
        result.put(VisualNodeRole.INTERGRAPH_TELEPORT, intergraphTeleportNode);
        result.put(VisualNodeRole.WARP, warpNode);
        return result;
    }

    private static Map<VisualEdgeRole, ParticleEdgeDesign> edgeDesignMap(final ParticleEdgeDesign localEdge,
                                                                         final ParticleEdgeDesign directedEdge,
                                                                         final ParticleEdgeDesign undirectedEdge,
                                                                         final ParticleEdgeDesign blockedEdge,
                                                                         final ParticleEdgeDesign interGraphEdge,
                                                                         final ParticleEdgeDesign directedInterGraphEdge,
                                                                         final ParticleEdgeDesign undirectedInterGraphEdge) {
        final Map<VisualEdgeRole, ParticleEdgeDesign> result = new EnumMap<>(VisualEdgeRole.class);
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
     * Resolves a particle node design by role.
     *
     * @param role visual node role
     * @return matching design or default node design
     */
    public ParticleNodeDesign nodeDesign(final VisualNodeRole role) {
        return nodeDesigns.getOrDefault(role, nodeDesigns.get(VisualNodeRole.DEFAULT));
    }

    /**
     * Resolves a particle edge design by role.
     *
     * @param role visual edge role
     * @return matching design or default local edge design
     */
    public ParticleEdgeDesign edgeDesign(final VisualEdgeRole role) {
        return edgeDesigns.getOrDefault(role, edgeDesigns.get(VisualEdgeRole.DEFAULT_LOCAL));
    }
}
