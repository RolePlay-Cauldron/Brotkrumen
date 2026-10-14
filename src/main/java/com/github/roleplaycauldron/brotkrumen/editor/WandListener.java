package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommandFeedback;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Handles editor wand clicks.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects"})
public final class WandListener implements Listener {

    private static final String NOTHING_NEARBY = "commands.bkeditor.wand.nothingNearby";

    private static final String NO_SELECTION = "commands.bkeditor.common.noSelection";

    private static final String CONFIRM_DELETE = "commands.bkeditor.wand.confirmDelete";

    private static final String WAND_EATEN = "commands.bkeditor.wand.eaten";

    private static final long DELETE_CONFIRM_WINDOW_MILLIS = 5_000L;

    private static final int PARTICLE_COUNT = 8;

    private static final double PARTICLE_SPREAD = 0.2D;

    private final EditorService editorService;

    private final WandItems wandItems;

    private final Supplier<WandSettings> settings;

    private final EditorCommandFeedback feedback;

    private final Map<UUID, PendingDelete> pendingDeletes = new HashMap<>();

    /**
     * Creates the wand listener.
     *
     * @param editorService editor service
     * @param wandItems     wand item helper
     * @param settings      supplier of the current wand settings
     * @param feedback      editor feedback renderer
     */
    public WandListener(final EditorService editorService, final WandItems wandItems,
                        final Supplier<WandSettings> settings, final EditorCommandFeedback feedback) {
        this.editorService = editorService;
        this.wandItems = wandItems;
        this.settings = settings;
        this.feedback = feedback;
    }

    /**
     * Handles wand interactions for players with an active editor session.
     *
     * @param event interaction event
     */
    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            return;
        }
        final Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();
        if (!wandItems.isWand(event.getItem())) {
            return;
        }
        final Optional<WandMode> mode = editorService.wandMode(playerId);
        if (mode.isEmpty()) {
            return;
        }
        blockVanillaUse(event, player);

        final Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR && player.isSneaking()) {
            toggleMode(player);
            return;
        }
        final Block block = event.getClickedBlock();
        if (action == Action.LEFT_CLICK_BLOCK) {
            if (block != null) {
                select(player, placementLocation(block, event.getBlockFace()),
                        mode.get() == WandMode.CRUMB);
            }
            return;
        }
        if (mode.get() == WandMode.CRUMB) {
            handleCrumb(player, action, block, event.getBlockFace());
        } else {
            handleThread(player, action);
        }
    }

    /**
     * Blocks vanilla entity interactions while a wand is held during an editor session.
     *
     * @param event entity interaction event
     */
    @EventHandler
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final ItemStack held = player.getInventory().getItem(event.getHand());
        if (wandItems.isWand(held) && editorService.wandMode(player.getUniqueId()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /**
     * Blocks vanilla interactions at an entity position, such as putting the wand on an armor stand. This event
     * has its own handler list, so {@link #onInteractEntity(PlayerInteractEntityEvent)} does not receive it.
     *
     * @param event entity interaction event
     */
    @EventHandler
    public void onInteractAtEntity(final PlayerInteractAtEntityEvent event) {
        onInteractEntity(event);
    }

    /**
     * Keeps wands out of containers, crafting grids and bundles, so they cannot outlive the editor session. Creative
     * inventory changes arrive here as well, because {@code InventoryCreativeEvent} shares this handler list.
     *
     * @param event inventory click event
     */
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (movesWandOutOfPlayerInventory(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * Keeps wands from being dragged into containers or crafting grids.
     *
     * @param event inventory drag event
     */
    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        final int topSize = event.getView().getTopInventory().getSize();
        if (wandItems.isWand(event.getOldCursor())
                && event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    private boolean movesWandOutOfPlayerInventory(final InventoryClickEvent event) {
        final Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return false;
        }
        if (!(clicked instanceof PlayerInventory)) {
            return wandItems.isWand(event.getCursor()) || wandItems.isWand(swappedItem(event));
        }
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return wandItems.isWand(event.getCurrentItem())
                    && event.getView().getTopInventory().getType() != InventoryType.CRAFTING;
        }
        return wandItems.isWand(event.getCursor()) && isBundle(event.getCurrentItem())
                || wandItems.isWand(event.getCurrentItem()) && isBundle(event.getCursor());
    }

    private ItemStack swappedItem(final InventoryClickEvent event) {
        final PlayerInventory inventory = event.getWhoClicked().getInventory();
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            return inventory.getItemInOffHand();
        }
        final int button = event.getHotbarButton();
        return button < 0 ? null : inventory.getItem(button);
    }

    private boolean isBundle(final ItemStack item) {
        // Name check, because colored bundles are not part of the compiled API version.
        return item != null && item.getType().name().endsWith("BUNDLE");
    }

    /**
     * Prevents wands from being dropped into the world while a session is active.
     *
     * @param event drop event
     */
    @EventHandler
    public void onDrop(final PlayerDropItemEvent event) {
        if (wandItems.isWand(event.getItemDrop().getItemStack())
                && editorService.wandMode(event.getPlayer().getUniqueId()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /**
     * Replaces a wand eaten during an editor session with a new one, and keeps non-survival players from eating it.
     *
     * @param event consume event
     */
    @EventHandler
    public void onConsume(final PlayerItemConsumeEvent event) {
        final Player player = event.getPlayer();
        if (!wandItems.isWand(event.getItem())) {
            return;
        }
        final Optional<WandMode> mode = editorService.wandMode(player.getUniqueId());
        if (mode.isEmpty()) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL) {
            event.setCancelled(true);
            return;
        }
        event.setReplacement(wandItems.create(mode.get()));
        feedback.send(player, EditorService.EditorResult.success(WAND_EATEN));
    }

    /**
     * Removes wands left over from sessions that ended without cleanup, for example after a crash.
     *
     * @param event join event
     */
    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (editorService.wandMode(player.getUniqueId()).isEmpty()) {
            wandItems.removeFrom(player);
        }
        pendingDeletes.remove(player.getUniqueId());
    }

    /**
     * Drops a pending delete confirmation of a player who leaves.
     *
     * @param event quit event
     */
    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        pendingDeletes.remove(event.getPlayer().getUniqueId());
    }

    private void blockVanillaUse(final PlayerInteractEvent event, final Player player) {
        if (mayEat(player, event.getItem())) {
            // Survival players may still eat an edible wand; the consume handler hands out a fresh one.
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DEFAULT);
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
    }

    private boolean mayEat(final Player player, final ItemStack item) {
        return player.getGameMode() == GameMode.SURVIVAL && wandItems.isEdible(item);
    }

    private void toggleMode(final Player player) {
        final EditorService.EditorResult result = editorService.toggleWandMode(player.getUniqueId());
        editorService.wandMode(player.getUniqueId()).ifPresent(mode -> wandItems.refresh(player, mode));
        sendResult(player, result, player.getLocation());
    }

    private void handleThread(final Player player, final Action action) {
        final UUID playerId = player.getUniqueId();
        switch (action) {
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> {
                if (player.isSneaking()) {
                    removeSelection(player);
                } else if (action == Action.RIGHT_CLICK_AIR) {
                    sendResult(player, editorService.advanceWandProperty(playerId), player.getLocation());
                } else {
                    sendResult(player, editorService.wandPrimaryAction(playerId, settings.get().defaultEdgeType()),
                            player.getLocation());
                }
            }
            default -> {
                // Other actions are intentionally ignored in Thread mode.
            }
        }
    }

    private void removeSelection(final Player player) {
        final UUID playerId = player.getUniqueId();
        final EditorService.WandSelection selection = editorService.wandSelection(playerId);
        if (selection == EditorService.WandSelection.NONE) {
            sendResult(player, EditorService.EditorResult.failure(NO_SELECTION), player.getLocation());
            return;
        }
        final long now = System.currentTimeMillis();
        final String selected = editorService.selectionIdentity(playerId).orElse("");
        final PendingDelete requested = pendingDeletes.remove(playerId);
        if (requested == null || now - requested.requestedAt() > DELETE_CONFIRM_WINDOW_MILLIS
                || !requested.selection().equals(selected)) {
            pendingDeletes.put(playerId, new PendingDelete(now, selected));
            feedback.send(player, EditorService.EditorResult.failure(CONFIRM_DELETE));
            return;
        }
        switch (editorService.wandSelection(playerId)) {
            case NODE -> editorService.deleteSelectedNodeAsync(playerId,
                    result -> sendResult(player, result, player.getLocation()));
            case EDGE -> sendResult(player, editorService.removeSelectedEdge(playerId), player.getLocation());
            default -> sendResult(player, EditorService.EditorResult.failure(NO_SELECTION), player.getLocation());
        }
    }

    private void handleCrumb(final Player player, final Action action, final Block block, final BlockFace face) {
        switch (action) {
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> {
                if (player.isSneaking()) {
                    sendResult(player, editorService.undo(player.getUniqueId(), 1), player.getLocation());
                } else {
                    final Location target = block == null ? player.getLocation() : placementLocation(block, face);
                    sendResult(player, editorService.placeNodeAtExactLocation(player.getUniqueId(), target), target);
                }
            }
            default -> {
                // Other actions are intentionally ignored in Crumb mode.
            }
        }
    }

    private void select(final Player player, final Location target, final boolean setAnchor) {
        EditorService.EditorResult result = editorService.selectNearbyNode(player.getUniqueId(), target);
        if (result.success() && setAnchor) {
            final EditorService.EditorResult anchor = editorService.useSelectedNodeAsAppendAnchor(player.getUniqueId());
            if (anchor.success()) {
                result = anchor;
            }
        }
        if (!result.success()) {
            final EditorService.EditorResult edgeResult = editorService.selectNearbyEdge(player.getUniqueId(), target);
            result = edgeResult.success() ? edgeResult : EditorService.EditorResult.failure(NOTHING_NEARBY);
        }
        sendResult(player, result, target);
    }

    private Location placementLocation(final Block block, final BlockFace face) {
        if (settings.get().offsetFromSideFace() && face != null) {
            return block.getRelative(face).getLocation().add(0.5D, 0.0D, 0.5D);
        }
        return block.getLocation().add(0.5D, 1.0D, 0.5D);
    }

    private void sendResult(final Player player, final EditorService.EditorResult result,
                            final Location effectLocation) {
        feedback.send(player, result);
        if (!result.success()) {
            return;
        }
        final WandSettings current = settings.get();
        if (current.soundFeedback()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 1.5F);
        }
        if (current.particleFeedback()) {
            player.spawnParticle(Particle.HAPPY_VILLAGER, effectLocation, PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD,
                    PARTICLE_SPREAD, 0.0D);
        }
    }

    /**
     * Delete request waiting for confirmation.
     *
     * @param requestedAt time of the first click in epoch milliseconds
     * @param selection   identity of the selection the request was made for
     */
    private record PendingDelete(long requestedAt, String selection) {
    }
}
