package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.command.bk.BkCommand;
import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommand;
import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.editor.EditorWaitingActionBarReminder;
import com.github.roleplaycauldron.brotkrumen.editor.WalkingListener;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkServiceImpl;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphServiceImpl;
import com.github.roleplaycauldron.brotkrumen.storage.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.storage.service.WarpServiceImpl;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * Starting point of the plugin.
 */
public class Brotkrumen extends JavaPlugin implements Listener {

    /**
     * The registry for managing visualizers.
     */
    private VisualizerRegistry reg;

    /**
     * The storage handler for database operations.
     */
    private Storage storage;

    /**
     * The service for graph-related operations.
     */
    private GraphServiceImpl graphService;

    /**
     * The runtime localization service.
     */
    private Localization localization;

    /**
     * The metrics service for plugin statistics.
     */
    private Metrics metrics;

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
        final LoggerFactory loggerFactory = new LoggerFactory(getSLF4JLogger());
        final WrappedLogger log = loggerFactory.create(Brotkrumen.class);

        saveDefaultConfig();
        loadLocalization(loggerFactory, log);

        final ConfigurationSection databaseSection = getConfig().getConfigurationSection("data");
        if (databaseSection == null || databaseSection.getKeys(false).isEmpty()) {
            log.error("Could not start the plugin because the data configuration is missing. Please check your config.yml file for errors.");
            return;
        }
        storage = new Storage(loggerFactory, databaseSection, getDataFolder());
        storage.initialize();

        graphService = new GraphServiceImpl(storage);
        final WarpServiceImpl warpService = new WarpServiceImpl(storage);
        final GraphNetworkServiceImpl graphNetworkService = new GraphNetworkServiceImpl(storage, graphService);

        final ServicesManager servicesManager = getServer().getServicesManager();
        servicesManager.register(GraphService.class, graphService, this, ServicePriority.Normal);
        servicesManager.register(GraphNetworkService.class, graphNetworkService, this, ServicePriority.Normal);
        servicesManager.register(WarpService.class, warpService, this, ServicePriority.Normal);

        this.reg = new VisualizerRegistry(this, loggerFactory.create(VisualizerRegistry.class));
        reg.startVisibilityUpdates();

        final EffectExecutor executor = new EffectExecutor(this);

        final EditorService editorService = new EditorService(reg, this, loggerFactory, executor, graphService,
                graphNetworkService, warpService);
        new EditorWaitingActionBarReminder(editorService).start(this);
        new EditorCommand(this, editorService, graphService);
        new BkCommand(this, graphService, graphNetworkService, storage, reg, loggerFactory, executor, localization);

        getServer().getPluginManager().registerEvents(new WalkingListener(log, editorService), this);

        metrics = new Metrics(this, 31_750);

        log.info("Brotkrumen enabled");
    }

    private void loadLocalization(final LoggerFactory loggerFactory, final WrappedLogger log) {
        final String defaultLocaleTag = configuredDefaultLocaleTag(getConfig().getString("localization.defaultLocale"), log);
        saveConfiguredDefaultLocaleResource(defaultLocaleTag);

        this.localization = new Localization(
                loggerFactory.create(Localization.class),
                this,
                defaultLocaleTag
        );
    }

    /* default */ void saveConfiguredDefaultLocaleResource(final String defaultLocaleTag) {
        final String resourcePath = localeResourcePath(defaultLocaleTag);
        if (getResource(resourcePath) != null) {
            saveResource(resourcePath, false);
        }
    }

    private String configuredDefaultLocaleTag(final String localeTag, final WrappedLogger log) {
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

    @Override
    public void onDisable() {
        if (reg != null) {
            reg.stopVisibilityUpdates();
        }

        if (storage != null) {
            storage.shutdown();
        }

        metrics.shutdown();
    }

    /**
     * Retrieves the GraphService instance used for graph-related operations.
     *
     * @return the GraphService instance, which provides an interface for managing
     * CRUD operations, retrieving, saving, and deleting graph data.
     */
    public GraphService getGraphService() {
        return graphService;
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
