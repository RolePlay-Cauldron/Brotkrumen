package brotkrumen.graph;

public class TeleportRules {
    private final boolean enableGlobalTeleport;

    private final int globalTargetNodeId;

    private final double globalTeleportCost;

    public TeleportRules(boolean enableGlobalTeleport, int globalTargetNodeId, double globalTeleportCost) {
        this.enableGlobalTeleport = enableGlobalTeleport;
        this.globalTargetNodeId = globalTargetNodeId;
        this.globalTeleportCost = globalTeleportCost;
    }

    public static TeleportRules disabled(){
        return new TeleportRules(false, -1, Double.POSITIVE_INFINITY);
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
}
