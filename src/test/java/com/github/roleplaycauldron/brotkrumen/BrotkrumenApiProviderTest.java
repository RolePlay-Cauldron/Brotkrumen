package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.api.BrotkrumenApi;
import com.github.roleplaycauldron.brotkrumen.api.graph.search.PathSearchService;
import com.github.roleplaycauldron.brotkrumen.api.graph.search.SearchRegistry;
import com.github.roleplaycauldron.brotkrumen.api.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.api.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.api.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.api.visual.VisualizerService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.SimpleServicesManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BrotkrumenApiProviderTest {

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void facadeExposesProvidedServicesAndCanBeRegistered() {
        final GraphService graphService = mock(GraphService.class);
        final GraphNetworkService graphNetworkService = mock(GraphNetworkService.class);
        final WarpService warpService = mock(WarpService.class);
        final PathSearchService pathSearchService = mock(PathSearchService.class);
        final SearchRegistry searchRegistry = mock(SearchRegistry.class);
        final VisualizerService visualizerService = mock(VisualizerService.class);
        final BrotkrumenApiProvider provider = new BrotkrumenApiProvider(graphService, graphNetworkService,
                warpService, pathSearchService, searchRegistry, visualizerService);
        final SimpleServicesManager services = new SimpleServicesManager();
        final Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);

            services.register(BrotkrumenApi.class, provider, mock(Plugin.class), ServicePriority.Normal);

            assertSame(provider, services.load(BrotkrumenApi.class), "Registered facade should be discoverable");
        }
        assertSame(graphService, provider.graphs(), "Facade should expose graph service");
        assertSame(graphNetworkService, provider.graphNetworks(), "Facade should expose graph network service");
        assertSame(warpService, provider.warps(), "Facade should expose warp service");
        assertSame(pathSearchService, provider.pathSearch(), "Facade should expose path search service");
        assertSame(searchRegistry, provider.searchRegistry(), "Facade should expose search registry");
        assertSame(visualizerService, provider.visualizers(), "Facade should expose visualizer service");
    }
}
