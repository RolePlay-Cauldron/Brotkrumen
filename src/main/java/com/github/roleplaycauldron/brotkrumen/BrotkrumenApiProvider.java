package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.api.BrotkrumenApi;
import com.github.roleplaycauldron.brotkrumen.api.graph.search.PathSearchService;
import com.github.roleplaycauldron.brotkrumen.api.graph.search.SearchRegistry;
import com.github.roleplaycauldron.brotkrumen.api.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.api.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.api.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.api.visual.VisualizerService;

/**
 * Default public API provider.
 */
public final class BrotkrumenApiProvider implements BrotkrumenApi {

    private final GraphService graphService;

    private final GraphNetworkService graphNetworkService;

    private final WarpService warpService;

    private final PathSearchService pathSearchService;

    private final SearchRegistry pathSearchRegistry;

    private final VisualizerService visualizerService;

    /**
     * Creates a public API provider.
     *
     * @param graphService        graph service
     * @param graphNetworkService graph network service
     * @param warpService         warp service
     * @param pathSearchService   path search service
     * @param searchRegistry      search registry
     * @param visualizerService   visualizer service
     */
    public BrotkrumenApiProvider(final GraphService graphService,
                                 final GraphNetworkService graphNetworkService,
                                 final WarpService warpService,
                                 final PathSearchService pathSearchService,
                                 final SearchRegistry searchRegistry,
                                 final VisualizerService visualizerService) {
        this.graphService = graphService;
        this.graphNetworkService = graphNetworkService;
        this.warpService = warpService;
        this.pathSearchService = pathSearchService;
        this.pathSearchRegistry = searchRegistry;
        this.visualizerService = visualizerService;
    }

    @Override
    public GraphService graphs() {
        return graphService;
    }

    @Override
    public GraphNetworkService graphNetworks() {
        return graphNetworkService;
    }

    @Override
    public WarpService warps() {
        return warpService;
    }

    @Override
    public PathSearchService pathSearch() {
        return pathSearchService;
    }

    @Override
    public SearchRegistry searchRegistry() {
        return pathSearchRegistry;
    }

    @Override
    public VisualizerService visualizers() {
        return visualizerService;
    }
}
