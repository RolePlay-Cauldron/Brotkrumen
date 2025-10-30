package brotkrumen.graph;

import org.bukkit.Location;

public class Node {
    private int id;

    private int x;

    private int y;

    private int z;

    public Node(int id, Location loc) {
        this(id, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Node(int id, int x, int y, int z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
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
}
