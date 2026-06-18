package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.api.BrotkrumenApi;
import com.github.roleplaycauldron.brotkrumen.api.graph.search.PathSearchService;
import com.github.roleplaycauldron.brotkrumen.api.graph.search.SearchRegistry;
import com.github.roleplaycauldron.brotkrumen.api.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.api.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.api.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.api.visual.VisualizerService;
import com.github.roleplaycauldron.brotkrumen.command.bk.BkCommand;
import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommand;
import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.editor.EditorWaitingActionBarReminder;
import com.github.roleplaycauldron.brotkrumen.editor.WalkingListener;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathFinder;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathSearchServiceImpl;
import com.github.roleplaycauldron.brotkrumen.graph.search.SearchRegistryImpl;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.AStarAlgorithm;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.DijkstraAlgorithm;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.service.AsyncGraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.service.AsyncGraphService;
import com.github.roleplaycauldron.brotkrumen.service.AsyncWarpService;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepositoryImpl;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepositoryImpl;
import com.github.roleplaycauldron.brotkrumen.storage.repository.WarpRepositoryImpl;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualPresetLoadException;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualPresetLoader;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualPresetRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.core.scheduler.SimplePaperScheduler;
import com.github.roleplaycauldron.spellbook.core.scheduler.SimpleScheduler;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Starting point of the plugin.
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
public class Brotkrumen extends JavaPlugin {

    /**
     * The logger factory for creating loggers.
     */
    private LoggerFactory loggerFactory;

    /**
     * The service for warp-related operations.
     */
    private WrappedLogger log;

    /**
     * The storage handler for database operations.
     */
    private Storage storage;

    /**
     * The registry for managing visualizers.
     */
    private VisualizerRegistry reg;

    /**
     * The runtime localization service.
     */
    private Localization localization;

    /**
     * Runtime visual preset cache.
     */
    private VisualPresetRegistry visualPresetRegistry;

    /**
     * The service for graph-related operations.
     */
    private GraphRepositoryImpl graphRepository;

    /**
     * The service for graph network-related operations.
     */
    private GraphNetworkRepositoryImpl graphNetworkRepository;

    /**
     * The metrics service for plugin statistics.
     */
    private Metrics metrics;

    /**
     * Public API facade provider.
     */
    private BrotkrumenApiProvider apiProvider;

    /**
     * Default constructor.
     */
    public Brotkrumen() {
        super();
    }

    /* default */
    static String localeResourcePath(final String localeTag) {
        return "language/" + localeTag.toLowerCase(Locale.ROOT) + ".yml";
    }

    @Override
    public void onEnable() {
        this.loggerFactory = new LoggerFactory(getSLF4JLogger());
        this.log = loggerFactory.create(Brotkrumen.class);

        saveDefaultConfig();
        loadLocalization(loggerFactory);
        if (!loadVisualPresets()) {
            return;
        }

        final ConfigurationSection databaseSection = getConfig().getConfigurationSection("data");
        if (databaseSection == null || databaseSection.getKeys(false).isEmpty()) {
            log.error("Could not start the plugin because the data configuration is missing. "
                    + "Please check your config.yml file for errors.");
            return;
        }
        storage = new Storage(loggerFactory, databaseSection, getDataFolder());
        storage.initialize();

        final SimpleScheduler scheduler = new SimplePaperScheduler(this, getServer().getScheduler());

        graphRepository = new GraphRepositoryImpl(storage);
        graphNetworkRepository = new GraphNetworkRepositoryImpl(storage, graphRepository);
        final WarpRepositoryImpl warpRepository = new WarpRepositoryImpl(storage);

        final ServicesManager servicesManager = getServer().getServicesManager();

        this.reg = new VisualizerRegistry(this, loggerFactory.create(VisualizerRegistry.class));
        reg.startVisibilityUpdates();

        final AsyncGraphService asyncGraphService = new AsyncGraphService(graphRepository, scheduler);
        final AsyncGraphNetworkService asyncGraphNetworkService =
                new AsyncGraphNetworkService(graphNetworkRepository, scheduler);
        final AsyncWarpService asyncWarpService = new AsyncWarpService(warpRepository, scheduler);
        final SearchRegistryImpl searchRegistry = new SearchRegistryImpl();
        searchRegistry.register(new AStarAlgorithm());
        searchRegistry.register(new DijkstraAlgorithm());
        final PathSearchServiceImpl pathSearchService = new PathSearchServiceImpl(new PathFinder(searchRegistry),
                scheduler);
        apiProvider = new BrotkrumenApiProvider(asyncGraphService, asyncGraphNetworkService, asyncWarpService,
                pathSearchService, searchRegistry, reg);
        registerPublicApiServices(servicesManager, asyncGraphService, asyncGraphNetworkService, asyncWarpService,
                pathSearchService, searchRegistry);

        final EffectExecutor executor = new EffectExecutor(this);
        final EditorService editorService = new EditorService(reg, this, loggerFactory, executor, graphRepository,
                graphNetworkRepository, warpRepository);

        registerCommands(executor, editorService);
        registerListeners(editorService);

        metrics = new Metrics(this, 31_750);

        log.info("Brotkrumen enabled");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (reg != null) {
            reg.stopVisibilityUpdates();
        }
        if (storage != null) {
            storage.shutdown();
        }
        if (metrics != null) {
            metrics.shutdown();
        }
    }

    private void registerPublicApiServices(final ServicesManager servicesManager,
                                           final AsyncGraphService asyncGraphService,
                                           final AsyncGraphNetworkService asyncGraphNetworkService,
                                           final AsyncWarpService asyncWarpService,
                                           final PathSearchServiceImpl pathSearchService,
                                           final SearchRegistryImpl searchRegistry) {
        servicesManager.register(BrotkrumenApi.class, apiProvider,
                this, ServicePriority.Normal);
        servicesManager.register(GraphService.class,
                asyncGraphService, this, ServicePriority.Normal);
        servicesManager.register(GraphNetworkService.class,
                asyncGraphNetworkService, this, ServicePriority.Normal);
        servicesManager.register(WarpService.class,
                asyncWarpService, this, ServicePriority.Normal);
        servicesManager.register(PathSearchService.class,
                pathSearchService, this, ServicePriority.Normal);
        servicesManager.register(SearchRegistry.class,
                searchRegistry, this, ServicePriority.Normal);
        servicesManager.register(VisualizerService.class,
                reg, this, ServicePriority.Normal);
    }

    private void loadLocalization(final LoggerFactory loggerFactory) {
        final String defaultLocaleTag = configuredDefaultLocaleTag(getConfig().getString("localization.defaultLocale"));
        saveConfiguredDefaultLocaleResource(defaultLocaleTag);

        this.localization = new Localization(
                loggerFactory.create(Localization.class),
                this,
                defaultLocaleTag
        );
    }

    private boolean loadVisualPresets() {
        try {
            this.visualPresetRegistry = new VisualPresetLoader(this, loggerFactory.create(VisualPresetLoader.class))
                    .loadForStartup();
            return true;
        } catch (final VisualPresetLoadException failure) {
            log.error("Could not start the plugin because visual presets are unavailable: " + failure.getMessage());
            return false;
        }
    }

    private void registerCommands(final EffectExecutor executor, final EditorService editorService) {
        new EditorWaitingActionBarReminder(editorService).start(this);
        new EditorCommand(this, loggerFactory, editorService, graphRepository, localization);
        new BkCommand(this, graphRepository, graphNetworkRepository, editorService.warpRepository(), storage, reg,
                loggerFactory, executor, localization);
    }

    private void registerListeners(final EditorService editorService) {
        getServer().getPluginManager().registerEvents(new WalkingListener(log, editorService), this);
    }

    private void saveConfiguredDefaultLocaleResource(final String defaultLocaleTag) {
        final String resourcePath = localeResourcePath(defaultLocaleTag);
        if (new File(getDataFolder(), resourcePath).isFile()) {
            return;
        }
        try (InputStream resource = getResource(resourcePath)) {
            if (resource != null) {
                saveResource(resourcePath, false);
            }
        } catch (final IOException failure) {
            log.error("Could not check locale resource '" + resourcePath + "': " + failure.getMessage());
        }
    }

    private String configuredDefaultLocaleTag(final String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return "en-us";
        }
        final String normalized = localeTag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            log.error("Invalid localization.defaultLocale '" + localeTag + "', using en-us.");
            return "en-us";
        }
        return normalized;
    }

    /**
     * Reloads localization files using the currently configured default locale.
     */
    public void reloadLocalization() {
        final String defaultLocaleTag = configuredDefaultLocaleTag(getConfig().getString("localization.defaultLocale"));
        saveConfiguredDefaultLocaleResource(defaultLocaleTag);
        localization.reload(defaultLocaleTag);
    }

    /**
     * Reloads visual presets from presets.yml.
     */
    public void reloadVisualPresets() {
        this.visualPresetRegistry = new VisualPresetLoader(this, loggerFactory.create(VisualPresetLoader.class))
                .reload(visualPresetRegistry);
    }

    /**
     * Returns the current visual preset cache.
     *
     * @return visual preset registry
     */
    public VisualPresetRegistry getVisualPresetRegistry() {
        return visualPresetRegistry;
    }

    /**
     * Returns the runtime localization service.
     *
     * @return localization service
     */
    public Localization getLocalization() {
        return localization;
    }
}
