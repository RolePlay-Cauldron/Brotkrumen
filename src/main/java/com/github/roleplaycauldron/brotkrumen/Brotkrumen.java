package com.github.roleplaycauldron.brotkrumen;

import com.github.roleplaycauldron.brotkrumen.command.bk.BkCommand;
import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommand;
import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.editor.EditorWaitingActionBarReminder;
import com.github.roleplaycauldron.brotkrumen.editor.WalkingListener;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Starting point of the plugin.
 */
public class Brotkrumen extends JavaPlugin implements Listener {

    private VisualizerRegistry reg;

    private Storage storage;

    private GraphServiceImpl graphService;

    /**
     * Default constructor.
     */
    public Brotkrumen() {
        super();
    }

    @Override
    public void onEnable() {
        final LoggerFactory loggerFactory = new LoggerFactory(getSLF4JLogger());
        final WrappedLogger log = loggerFactory.create(Brotkrumen.class);

        saveDefaultConfig();

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
        new BkCommand(this, graphService, graphNetworkService, storage, reg, loggerFactory, executor);

        getServer().getPluginManager().registerEvents(new WalkingListener(log, editorService), this);

        log.info("Brotkrumen enabled");
    }

    @Override
    public void onDisable() {
        if (reg != null) {
            reg.stopVisibilityUpdates();
        }

        if (storage != null) {
            storage.shutdown();
        }
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
}
