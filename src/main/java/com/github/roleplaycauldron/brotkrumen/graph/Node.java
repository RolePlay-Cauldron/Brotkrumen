package com.github.roleplaycauldron.brotkrumen.graph;

import org.bukkit.Location;

public class Node {

    private int id;

    private int x;

    private int y;

    private int z;

    public Node(final int id, final Location loc) {
        this(id, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Node(final int id, final int x, final int y, final int z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(final int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(final int z) {
        this.z = z;
    }
}
