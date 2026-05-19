package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Filters warps before they are supplied to pathfinding rules.
 */
public final class WarpPermissionHelper {

    /**
     * Prefix for warp permissions.
     */
    public static final String PERMISSION_PREFIX = "brotkrumen.warp.";

    private WarpPermissionHelper() {
    }

    /**
     * Filters enabled warps by their permission metadata.
     *
     * @param warps         configured warps
     * @param hasPermission permission predicate
     * @return enabled and allowed warps
     */
    public static List<Warp> allowedWarps(final Collection<Warp> warps, final Predicate<String> hasPermission) {
        if (warps == null || warps.isEmpty()) {
            return List.of();
        }
        final Predicate<String> permissionCheck = hasPermission == null ? permission -> false : hasPermission;
        return warps.stream()
                .filter(Objects::nonNull)
                .filter(Warp::enabled)
                .filter(warp -> isAllowed(warp, permissionCheck))
                .toList();
    }

    /**
     * Checks whether a warp is allowed for a permission context.
     *
     * @param warp          warp to check
     * @param hasPermission permission predicate
     * @return {@code true} if the warp can be supplied to teleport rules
     */
    public static boolean isAllowed(final Warp warp, final Predicate<String> hasPermission) {
        if (warp == null || !warp.enabled()) {
            return false;
        }
        if (!warp.needPermission()) {
            return true;
        }
        final Predicate<String> permissionCheck = hasPermission == null ? permission -> false : hasPermission;
        return permissionCheck.test(permissionByKey(warp)) || permissionCheck.test(permissionByNodeId(warp));
    }

    /**
     * Builds a permission string from the warp key.
     *
     * @param warp warp
     * @return key-based permission
     */
    public static String permissionByKey(final Warp warp) {
        return PERMISSION_PREFIX + sanitizeKey(warp.key());
    }

    /**
     * Builds a permission string from the target node id.
     *
     * @param warp warp
     * @return node-id-based permission
     */
    public static String permissionByNodeId(final Warp warp) {
        return PERMISSION_PREFIX + warp.targetNodeId();
    }

    /**
     * Sanitizes a warp key for permission usage.
     *
     * @param key warp key
     * @return sanitized suffix
     */
    public static String sanitizeKey(final String key) {
        if (key == null) {
            return "";
        }
        return key.trim().replaceAll("\\s+", "_").toLowerCase(Locale.ROOT);
    }
}
