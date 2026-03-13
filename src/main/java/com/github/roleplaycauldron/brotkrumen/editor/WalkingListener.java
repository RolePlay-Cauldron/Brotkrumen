package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WalkingListener implements Listener {

    private final WrappedLogger log;

    private final EditorService editorService;

    private final Map<UUID, Location> lastNodeLocation = new HashMap<>();

    public WalkingListener(final WrappedLogger log, final EditorService editorService) {
        this.log = log;
        this.editorService = editorService;
    }

    @EventHandler
    public void onWalk(final PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final UUID uuid = event.getPlayer().getUniqueId();
        if (!editorService.isEditing(uuid)) {
            return;
        }

        final Location playerLoc = event.getPlayer().getLocation();
        final Location oldLoc = lastNodeLocation.get(uuid);
        if (oldLoc == null) {
            lastNodeLocation.put(uuid, playerLoc);
            editorService.addNodeToPath(uuid, playerLoc);
            return;
        }

        if (oldLoc.distance(playerLoc) > editorService.getNodeDistance(uuid)) {
            log.infoF("Player %s moved enough for a new Node", event.getPlayer().getName());
            lastNodeLocation.put(uuid, playerLoc);
            editorService.addNodeToPath(uuid, playerLoc);
        }
    }
}
