package brotkrumen.graph;

import java.util.EnumSet;

public class Edge {
    private int id;

    private int from;

    private int to;

    private double cost;

    private EnumSet<EdgeFlag> flags;

    public Edge(final int id, final int from, final int to, final double cost) {
        this(id, from, to, cost, EnumSet.noneOf(EdgeFlag.class));
    }

    public Edge(final int id, final int from, final int to, final double cost, final EnumSet<EdgeFlag> flags) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.cost = cost;
        this.flags = flags == null ? EnumSet.noneOf(EdgeFlag.class) : EnumSet.copyOf(flags);
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(final int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(final int to) {
        this.to = to;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(final double cost) {
        this.cost = cost;
    }

    public boolean hasFlag(final EdgeFlag flag) {
        return flags.contains(flag);
    }

    public EnumSet<EdgeFlag> getFlags() {
        return flags;
    }

    public void setFlags(final EnumSet<EdgeFlag> updatedFlags) {
        if (updatedFlags == null) {
            flags = EnumSet.noneOf(EdgeFlag.class);
            return;
        }
        flags = EnumSet.copyOf(updatedFlags);
    }

    @Override
    public String toString() {
        return String.format("%d %d->%d (%.2f) %s", id, from, to, cost, flags);
    }
}
