package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.visual.design.BlockDisplayDesignSet;
import com.github.roleplaycauldron.brotkrumen.visual.design.BlockEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.BlockNodeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleDesignSet;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleEdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.ParticleNodeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.EnumMap;
import java.util.Map;

/**
 * Explicit visual design fixtures for tests.
 */
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.CommentRequired"})
public final class TestVisualDesigns {

    private TestVisualDesigns() {
    }

    public static ParticleDesignSet emberParticle() {
        return new ParticleDesignSet(
                particleNodes(Particle.FLAME, Particle.ASH, Particle.LAVA, Particle.PORTAL),
                particleEdges(Particle.FLAME, Particle.FLAME, Particle.FLAME, Particle.SMOKE,
                        Particle.LAVA, Particle.LAVA, Particle.LAVA, 0.2f)
        );
    }

    public static ParticleDesignSet prismParticle() {
        return new ParticleDesignSet(
                particleNodes(Particle.END_ROD, Particle.PORTAL, Particle.REVERSE_PORTAL, Particle.WITCH),
                particleEdges(Particle.END_ROD, Particle.END_ROD, Particle.END_ROD, Particle.SMOKE,
                        Particle.WITCH, Particle.WITCH, Particle.WITCH, 0.18f)
        );
    }

    public static BlockDisplayDesignSet emberBlock() {
        return new BlockDisplayDesignSet(
                blockNodes(Material.COAL_BLOCK, Material.MAGMA_BLOCK, Material.REDSTONE_BLOCK, Material.OBSIDIAN),
                blockEdges(Material.ORANGE_WOOL, Material.GREEN_STAINED_GLASS, Material.LIME_STAINED_GLASS,
                        Material.RED_WOOL, Material.GOLD_BLOCK, Material.WHITE_STAINED_GLASS,
                        Material.WHITE_STAINED_GLASS)
        );
    }

    public static BlockDisplayDesignSet prismBlock() {
        return new BlockDisplayDesignSet(
                blockNodes(Material.LAPIS_BLOCK, Material.AMETHYST_BLOCK, Material.PURPUR_BLOCK, Material.END_STONE),
                blockEdges(Material.LIGHT_BLUE_STAINED_GLASS, Material.CYAN_STAINED_GLASS,
                        Material.BLUE_STAINED_GLASS, Material.RED_STAINED_GLASS, Material.BLACK_STAINED_GLASS,
                        Material.GRAY_STAINED_GLASS, Material.GRAY_STAINED_GLASS)
        );
    }

    private static Map<VisualNodeRole, ParticleNodeDesign> particleNodes(final Particle defaultParticle,
                                                                         final Particle localTeleportParticle,
                                                                         final Particle intergraphTeleportParticle,
                                                                         final Particle warpParticle) {
        final Map<VisualNodeRole, ParticleNodeDesign> result = new EnumMap<>(VisualNodeRole.class);
        result.put(VisualNodeRole.DEFAULT, ParticleNodeDesign.cube(defaultParticle, 0.45f));
        result.put(VisualNodeRole.LOCAL_TELEPORT, ParticleNodeDesign.sphere(localTeleportParticle, 0.55f));
        result.put(VisualNodeRole.INTERGRAPH_TELEPORT, ParticleNodeDesign.cube(intergraphTeleportParticle, 0.5f));
        result.put(VisualNodeRole.WARP, ParticleNodeDesign.sphere(warpParticle, 0.55f));
        result.put(VisualNodeRole.GUIDED_PATH_GOAL, ParticleNodeDesign.sphere(Particle.HAPPY_VILLAGER, 0.65f));
        return result;
    }

    private static Map<VisualEdgeRole, ParticleEdgeDesign> particleEdges(final Particle localParticle,
                                                                         final Particle directedParticle,
                                                                         final Particle undirectedParticle,
                                                                         final Particle blockedParticle,
                                                                         final Particle interGraphParticle,
                                                                         final Particle directedInterGraphParticle,
                                                                         final Particle undirectedInterGraphParticle,
                                                                         final float interGraphSpacing) {
        final Map<VisualEdgeRole, ParticleEdgeDesign> result = new EnumMap<>(VisualEdgeRole.class);
        result.put(VisualEdgeRole.DEFAULT_LOCAL, ParticleEdgeDesign.line(localParticle, 20));
        result.put(VisualEdgeRole.DIRECTED_LOCAL, ParticleEdgeDesign.movingPoint(directedParticle, 0.2f));
        result.put(VisualEdgeRole.UNDIRECTED_LOCAL, ParticleEdgeDesign.line(undirectedParticle, 20));
        result.put(VisualEdgeRole.BLOCKED, ParticleEdgeDesign.line(blockedParticle, 20));
        result.put(VisualEdgeRole.INTER_GRAPH, ParticleEdgeDesign.movingPoint(interGraphParticle, interGraphSpacing));
        result.put(VisualEdgeRole.DIRECTED_INTER_GRAPH,
                ParticleEdgeDesign.movingPoint(directedInterGraphParticle, 0.2f));
        result.put(VisualEdgeRole.UNDIRECTED_INTER_GRAPH, ParticleEdgeDesign.line(undirectedInterGraphParticle, 20));
        return result;
    }

    private static Map<VisualNodeRole, BlockNodeDesign> blockNodes(final Material defaultMaterial,
                                                                   final Material localTeleportMaterial,
                                                                   final Material intergraphTeleportMaterial,
                                                                   final Material warpMaterial) {
        final Map<VisualNodeRole, BlockNodeDesign> result = new EnumMap<>(VisualNodeRole.class);
        result.put(VisualNodeRole.DEFAULT, new BlockNodeDesign(defaultMaterial, 0.45f));
        result.put(VisualNodeRole.LOCAL_TELEPORT, new BlockNodeDesign(localTeleportMaterial, 0.55f));
        result.put(VisualNodeRole.INTERGRAPH_TELEPORT, new BlockNodeDesign(intergraphTeleportMaterial, 0.5f));
        result.put(VisualNodeRole.WARP, new BlockNodeDesign(warpMaterial, 0.55f));
        result.put(VisualNodeRole.GUIDED_PATH_GOAL, new BlockNodeDesign(Material.GOLD_BLOCK, 0.65f));
        return result;
    }

    private static Map<VisualEdgeRole, BlockEdgeDesign> blockEdges(final Material localMaterial,
                                                                   final Material directedMaterial,
                                                                   final Material undirectedMaterial,
                                                                   final Material blockedMaterial,
                                                                   final Material interGraphMaterial,
                                                                   final Material directedInterGraphMaterial,
                                                                   final Material undirectedInterGraphMaterial) {
        final Map<VisualEdgeRole, BlockEdgeDesign> result = new EnumMap<>(VisualEdgeRole.class);
        result.put(VisualEdgeRole.DEFAULT_LOCAL, new BlockEdgeDesign(localMaterial, 0.16f, 0.8D));
        result.put(VisualEdgeRole.DIRECTED_LOCAL, new BlockEdgeDesign(directedMaterial, 0.16f, 0.8D));
        result.put(VisualEdgeRole.UNDIRECTED_LOCAL, new BlockEdgeDesign(undirectedMaterial, 0.18f, 0.8D));
        result.put(VisualEdgeRole.BLOCKED, new BlockEdgeDesign(blockedMaterial, 0.16f, 0.8D));
        result.put(VisualEdgeRole.INTER_GRAPH, new BlockEdgeDesign(interGraphMaterial, 0.16f, 0.8D));
        result.put(VisualEdgeRole.DIRECTED_INTER_GRAPH,
                new BlockEdgeDesign(directedInterGraphMaterial, 0.16f, 0.8D));
        result.put(VisualEdgeRole.UNDIRECTED_INTER_GRAPH,
                new BlockEdgeDesign(undirectedInterGraphMaterial, 0.18f, 0.8D));
        return result;
    }
}
