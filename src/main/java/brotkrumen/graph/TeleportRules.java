package brotkrumen.graph;

public class TeleportRules {
    private final boolean enableGlobalTeleport;

    private final int globalTargetNodeId;

    private final double globalTeleportCost;

    private final boolean enableLocalTeleport;

    public TeleportRules(boolean enableGlobalTeleport, int globalTargetNodeId, double globalTeleportCost, boolean enableLocalTeleport) {
        this.enableGlobalTeleport = enableGlobalTeleport;
        this.globalTargetNodeId = globalTargetNodeId;
        this.globalTeleportCost = globalTeleportCost;
        this.enableLocalTeleport = enableLocalTeleport;
    }

    public static TeleportRules disableTeleports() {
        return new TeleportRules(false, -1, Double.POSITIVE_INFINITY, false);
    }

    public boolean isGlobalTeleportEnabled() {
        return enableGlobalTeleport;
    }

    public int getGlobalTargetNodeId() {
        return globalTargetNodeId;
    }

    public double getGlobalTeleportCost() {
        return globalTeleportCost;
    }

    public boolean isLocalTeleportEnabled() {
        return enableLocalTeleport;
    }
}
