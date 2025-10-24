package brotkrumen;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Starting point of the plugin.
 */
public class Brotkrumen extends JavaPlugin {

    /**
     * Default constructor.
     */
    public Brotkrumen() {
        super();
    }

    @Override
    public void onEnable() {
        getLogger().info("brotkrumen.Brotkrumen enabled");
    }
}
