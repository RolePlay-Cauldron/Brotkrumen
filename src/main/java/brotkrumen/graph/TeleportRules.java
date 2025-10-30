package brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TeleportRules {
    private final boolean enableWarps;

    private final boolean enableLocalTeleport;

    private final Map<String, Warp> warps;

    public TeleportRules(boolean enableWarps, boolean enableLocalTeleport, Collection<Warp> warps) {
        this.enableWarps = enableWarps;
        this.enableLocalTeleport = enableLocalTeleport;
        Map<String, Warp> warpMap = new HashMap<>();
        if (warps != null) {
            for (Warp warp : warps) warpMap.put(warp.key(), warp);
        }
        this.warps = Collections.unmodifiableMap(warpMap);
    }

    public static TeleportRules disableTeleports() {
        return new TeleportRules(false, false, new ArrayList<>());
    }

    public boolean isWarpingEnabled() {
        return enableWarps;
    }

    public boolean isLocalTeleportEnabled() {
        return enableLocalTeleport;
    }

    public Collection<Warp> getWarps() {
        return warps.values();
    }

    public Optional<Warp> getWarp(String key) {
        return Optional.ofNullable(warps.get(key));
    }
}
