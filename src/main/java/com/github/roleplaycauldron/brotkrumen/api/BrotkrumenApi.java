package com.github.roleplaycauldron.brotkrumen.api;

import com.github.roleplaycauldron.brotkrumen.api.graph.search.PathSearchService;
import com.github.roleplaycauldron.brotkrumen.api.graph.search.SearchRegistry;
import com.github.roleplaycauldron.brotkrumen.api.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.api.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.api.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.api.visual.VisualizerService;

/**
 * Public Brotkrumen API facade registered through Paper's services manager.
 */
public interface BrotkrumenApi {

    /**
     * Returns the async graph service.
     *
     * @return graph service
     */
    GraphService graphs();

    /**
     * Returns the async graph network service.
     *
     * @return graph network service
     */
    GraphNetworkService graphNetworks();

    /**
     * Returns the async warp service.
     *
     * @return warp service
     */
    WarpService warps();

    /**
     * Returns the async path search service.
     *
     * @return path search service
     */
    PathSearchService pathSearch();

    /**
     * Returns the search registry used by path searches.
     *
     * @return search registry
     */
    SearchRegistry searchRegistry();

    /**
     * Returns the synchronous visualizer lifecycle service.
     *
     * @return visualizer service
     */
    VisualizerService visualizers();
}
