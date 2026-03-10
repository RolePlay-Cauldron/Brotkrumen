package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import de.slikey.effectlib.EffectManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ParticleVisualiser extends GraphVisualizer {

    private final Brotkrumen plugin;

    private final EffectManager effectManager;

    private final EffectLibConfig effectConfig;

    private final UUID ownerId;

    private final VisualMode mode;

    private final List<Node> path;

    public ParticleVisualiser(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                              final Graph graph, final EffectManager effectManager, final EffectLibConfig effectConfig,
                              final UUID ownerId, final VisualMode mode, final List<Node> path) {
        super(loggerFactory, graph);
        this.plugin = plugin;
        this.effectManager = effectManager;
        this.effectConfig = effectConfig;
        this.ownerId = ownerId;
        this.mode = mode;
        this.path = path;
    }

    @Override
    public void shutdown() {
        // ToDo
    }

    @Override
    void visibilityUpdate() {
        final Player player = plugin.getServer().getPlayer(ownerId);
        if (player == null) {
            shutdown();
        }

        runNodeEffect(player, path);
        runEdgeEffect();
    }

    private void runNodeEffect(final Player player, final List<Node> nodesToVisualize) {
        for (final Node node : nodesToVisualize) {
            effectManager.start(effectConfig.effectClass(), effectConfig.settings(), node.toCenterLocation(), player);
        }
        // ToDo

    }

    private void runEdgeEffect() {
        // ToDo
    }
}
