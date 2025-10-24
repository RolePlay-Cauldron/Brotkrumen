package brotkrumen.graph;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class Edge {
    private int id;

    private int from;

    private int to;

    private double cost;

    private final Map<String, String> attrs;

    private final EnumSet<EdgeFlag> flags;

    public Edge(int from, int to, double cost) {
        this(from, to, cost, EnumSet.noneOf(EdgeFlag.class), new HashMap<>());
    }

    public Edge(int from, int to, double cost, EnumSet<EdgeFlag> flags, Map<String, String> attrs) {
        this.from = from; this.to = to; this.cost = cost;
        this.flags = flags == null ? EnumSet.noneOf(EdgeFlag.class) : EnumSet.copyOf(flags);
        this.attrs = attrs == null ? new HashMap<>() : new HashMap<>(attrs);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public boolean hasAttribute(String key) {
        return attrs.containsKey(key);
    }

    public Map<String, String> getAttributes() {
        return attrs;
    }

    public void setAttr(Map<String, String> updatedAttributes) {
        attrs.clear();
        attrs.putAll(updatedAttributes);
    }

    public boolean hasFlag(EdgeFlag flag) {
        return flags.contains(flag);
    }

    public EnumSet<EdgeFlag> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<EdgeFlag> updatedFlags) {
        flags.clear();
        flags.addAll(updatedFlags);
    }

    @Override public String toString(){
        return String.format("%d->%d (%f) %s", from, to, cost, flags);
    }
}
