package brotkrumen.graph;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class Node {
    private int id;

    private int x;

    private int y;

    private int z;

    private final Map<String, String> attrs;

    public Node(int id, Location loc) {
        this(id, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Node(int id, Location loc, Map<String, String> attrs) {
        this(id, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), attrs);
    }

    public Node(int id, int x, int y, int z) {
        this(id, x, y, z, new HashMap<>());
    }

    public Node(int id, int x, int y, int z, Map<String, String> attrs) {
        this.id = id; this.x = x; this.y = y; this.z = z; this.attrs = attrs;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public boolean hasAttr(String key) {
        return attrs.containsKey(key);
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public void setAttr(Map<String, String> updatedAttributes) {
        attrs.clear();
        attrs.putAll(updatedAttributes);
    }
}
