package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommandFeedback;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WandListener}.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.CouplingBetweenObjects"})
class WandListenerTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    @Mock
    private EditorService editorService;

    @Mock
    private WandItems wandItems;

    @Mock
    private EditorCommandFeedback feedback;

    @Mock
    private PlayerInteractEvent event;

    @Mock
    private Player player;

    @Mock
    private ItemStack item;

    @Mock
    private Block block;

    @Mock
    private Block relative;

    private WandSettings settings;

    private WandListener listener;

    @BeforeEach
    void setUp() {
        settings = WandSettings.defaults();
        listener = new WandListener(editorService, wandItems, () -> settings, feedback);
        lenient().when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        lenient().when(event.getPlayer()).thenReturn(player);
        lenient().when(event.getItem()).thenReturn(item);
        lenient().when(event.getClickedBlock()).thenReturn(block);
        lenient().when(event.getBlockFace()).thenReturn(BlockFace.UP);
        lenient().when(player.getUniqueId()).thenReturn(PLAYER_ID);
        lenient().when(wandItems.isWand(item)).thenReturn(true);
        lenient().when(editorService.wandMode(PLAYER_ID)).thenReturn(Optional.of(WandMode.CRUMB));
        lenient().when(block.getLocation()).thenReturn(new Location(null, 10.0D, 64.0D, 20.0D));
        lenient().when(block.getRelative(BlockFace.EAST)).thenReturn(relative);
        lenient().when(relative.getLocation()).thenReturn(new Location(null, 11.0D, 64.0D, 20.0D));
        lenient().when(editorService.placeNodeAtExactLocation(eq(PLAYER_ID), any(Location.class)))
                .thenReturn(EditorService.EditorResult.success("commands.bkeditor.status.nodePlaced"));
    }

    @Test
    void rightClickPlacesNodeOnTopOfBlockAndCancelsEvent() {
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        listener.onInteract(event);

        final ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(editorService).placeNodeAtExactLocation(eq(PLAYER_ID), captor.capture());
        assertEquals(10.5D, captor.getValue().getX());
        assertEquals(65.0D, captor.getValue().getY());
        assertEquals(20.5D, captor.getValue().getZ());
        verify(event).setCancelled(true);
    }

    @Test
    void rightClickInAirPlacesNodeAtPlayerLocation() {
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getClickedBlock()).thenReturn(null);
        final Location playerLocation = new Location(null, 1.5D, 70.0D, -3.5D);
        when(player.getLocation()).thenReturn(playerLocation);

        listener.onInteract(event);

        verify(editorService).placeNodeAtExactLocation(PLAYER_ID, playerLocation);
    }

    @Test
    void sneakRightClickUndoesLastNode() {
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(player.isSneaking()).thenReturn(true);
        when(player.getLocation()).thenReturn(new Location(null, 0.0D, 0.0D, 0.0D));
        when(editorService.undo(PLAYER_ID, 1)).thenReturn(EditorService.EditorResult.success("undo"));

        listener.onInteract(event);

        verify(editorService).undo(PLAYER_ID, 1);
        verify(editorService, never()).placeNodeAtExactLocation(any(), any());
    }

    @Test
    void sideFaceOffsetPlacesNodeNextToClickedFace() {
        settings = new WandSettings(settings.material(), true, true, false, false,
                settings.defaultEdgeType(), false);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getBlockFace()).thenReturn(BlockFace.EAST);

        listener.onInteract(event);

        final ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(editorService).placeNodeAtExactLocation(eq(PLAYER_ID), captor.capture());
        assertEquals(11.5D, captor.getValue().getX());
        assertEquals(64.0D, captor.getValue().getY());
    }

    @Test
    void leftClickSelectsNodeAndFallsBackToEdge() {
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        when(editorService.selectNearbyNode(eq(PLAYER_ID), any(Location.class)))
                .thenReturn(EditorService.EditorResult.failure("commands.bkeditor.common.noNearbyNode"));
        when(editorService.selectNearbyEdge(eq(PLAYER_ID), any(Location.class)))
                .thenReturn(EditorService.EditorResult.success("edge"));

        listener.onInteract(event);

        verify(editorService).selectNearbyEdge(eq(PLAYER_ID), any(Location.class));
        verify(editorService, never()).placeNodeAtExactLocation(any(), any());
    }

    @Test
    void crumbLeftClickOnNodeMakesItTheAppendAnchor() {
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        when(editorService.selectNearbyNode(eq(PLAYER_ID), any(Location.class)))
                .thenReturn(EditorService.EditorResult.success("commands.bkeditor.selection.selectedNode"));
        when(editorService.useSelectedNodeAsAppendAnchor(PLAYER_ID))
                .thenReturn(EditorService.EditorResult.success("commands.bkeditor.status.appendAnchorSelected"));

        listener.onInteract(event);

        verify(editorService).useSelectedNodeAsAppendAnchor(PLAYER_ID);
    }

    @Test
    void threadLeftClickDoesNotChangeAppendAnchor() {
        when(editorService.wandMode(PLAYER_ID)).thenReturn(Optional.of(WandMode.THREAD));
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        when(editorService.selectNearbyNode(eq(PLAYER_ID), any(Location.class)))
                .thenReturn(EditorService.EditorResult.success("commands.bkeditor.selection.selectedNode"));

        listener.onInteract(event);

        verify(editorService, never()).useSelectedNodeAsAppendAnchor(any());
    }

    @Test
    void sneakLeftClickInAirTogglesModeAndRefreshesWands() {
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);
        when(player.isSneaking()).thenReturn(true);
        when(player.getLocation()).thenReturn(new Location(null, 0.0D, 0.0D, 0.0D));
        when(editorService.toggleWandMode(PLAYER_ID))
                .thenReturn(EditorService.EditorResult.success("", "commands.bkeditor.wand.mode.thread"));
        when(editorService.wandMode(PLAYER_ID)).thenReturn(Optional.of(WandMode.THREAD));

        listener.onInteract(event);

        verify(editorService).toggleWandMode(PLAYER_ID);
        verify(wandItems).refresh(player, WandMode.THREAD);
    }

    @Test
    void threadRightClickBlockConnectsOrCyclesWithConfiguredEdgeType() {
        settings = new WandSettings(settings.material(), true, false, false, false,
                EditorService.EdgeType.DIRECTED, false);
        when(editorService.wandMode(PLAYER_ID)).thenReturn(Optional.of(WandMode.THREAD));
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(player.getLocation()).thenReturn(new Location(null, 0.0D, 0.0D, 0.0D));
        when(editorService.wandPrimaryAction(PLAYER_ID, EditorService.EdgeType.DIRECTED))
                .thenReturn(EditorService.EditorResult.success("commands.bkeditor.edge.created"));

        listener.onInteract(event);

        verify(editorService).wandPrimaryAction(PLAYER_ID, EditorService.EdgeType.DIRECTED);
        verify(editorService, never()).placeNodeAtExactLocation(any(), any());
    }

    @Test
    void threadRightClickInAirAdvancesPropertyCursor() {
        when(editorService.wandMode(PLAYER_ID)).thenReturn(Optional.of(WandMode.THREAD));
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(player.getLocation()).thenReturn(new Location(null, 0.0D, 0.0D, 0.0D));
        when(editorService.advanceWandProperty(PLAYER_ID))
                .thenReturn(EditorService.EditorResult.success("", "commands.bkeditor.wand.property.state"));

        listener.onInteract(event);

        verify(editorService).advanceWandProperty(PLAYER_ID);
    }

    @Test
    void threadSneakRightClickDeletesSelectedNodeOrRemovesSelectedEdge() {
        when(editorService.wandMode(PLAYER_ID)).thenReturn(Optional.of(WandMode.THREAD));
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(player.isSneaking()).thenReturn(true);
        when(player.getLocation()).thenReturn(new Location(null, 0.0D, 0.0D, 0.0D));
        when(editorService.wandSelection(PLAYER_ID)).thenReturn(EditorService.WandSelection.NODE);

        listener.onInteract(event);
        verify(editorService, never()).deleteSelectedNodeAsync(any(), any());

        listener.onInteract(event);
        verify(editorService).deleteSelectedNodeAsync(eq(PLAYER_ID), any());

        when(editorService.wandSelection(PLAYER_ID)).thenReturn(EditorService.WandSelection.EDGE);
        when(editorService.removeSelectedEdge(PLAYER_ID))
                .thenReturn(EditorService.EditorResult.success("commands.bkeditor.edge.removed"));

        listener.onInteract(event);
        verify(editorService, never()).removeSelectedEdge(any());

        listener.onInteract(event);
        verify(editorService).removeSelectedEdge(PLAYER_ID);
    }

    @Test
    void ignoresClicksWithoutWandOrSession() {
        when(wandItems.isWand(item)).thenReturn(false);
        listener.onInteract(event);

        when(wandItems.isWand(item)).thenReturn(true);
        when(editorService.wandMode(PLAYER_ID)).thenReturn(Optional.empty());
        listener.onInteract(event);

        verify(event, never()).setCancelled(true);
        verify(editorService, never()).placeNodeAtExactLocation(any(), any());
    }

    @Test
    void offHandWandIsHandledLikeMainHandWand() {
        final ItemStack mainHandItem = mock(ItemStack.class);
        final ItemStack offHandWand = mock(ItemStack.class);
        when(wandItems.isWand(mainHandItem)).thenReturn(false);
        when(wandItems.isWand(offHandWand)).thenReturn(true);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        when(event.getItem()).thenReturn(mainHandItem);
        listener.onInteract(event);
        verify(editorService, never()).placeNodeAtExactLocation(any(), any());

        lenient().when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(event.getItem()).thenReturn(offHandWand);
        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(editorService).placeNodeAtExactLocation(eq(PLAYER_ID), any(Location.class));
    }

    @Test
    void deleteConfirmationIsResetWhenSelectionChanges() {
        when(editorService.wandMode(PLAYER_ID)).thenReturn(Optional.of(WandMode.THREAD));
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(player.isSneaking()).thenReturn(true);
        when(editorService.wandSelection(PLAYER_ID)).thenReturn(EditorService.WandSelection.EDGE);
        when(editorService.selectionIdentity(PLAYER_ID)).thenReturn(Optional.of("node:a"));

        listener.onInteract(event);
        when(editorService.selectionIdentity(PLAYER_ID)).thenReturn(Optional.of("edge:b"));
        listener.onInteract(event);

        verify(editorService, never()).removeSelectedEdge(any());
        verify(feedback, times(2)).send(player, EditorService.EditorResult.failure("commands.bkeditor.wand.confirmDelete"));
    }

    @Test
    void survivalPlayerMayEatEdibleWandWhileClicking() {
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(wandItems.isEdible(item)).thenReturn(true);

        listener.onInteract(event);

        verify(event).setUseItemInHand(Event.Result.DEFAULT);
        verify(event).setUseInteractedBlock(Event.Result.DENY);
        verify(event, never()).setCancelled(true);
        verify(editorService).placeNodeAtExactLocation(eq(PLAYER_ID), any(Location.class));
    }

    @Test
    void creativePlayerCannotEatWand() {
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        listener.onInteract(event);

        verify(event).setUseItemInHand(Event.Result.DENY);
        verify(event).setCancelled(true);
    }

    @Test
    void eatenWandIsReplacedDuringSession() {
        final PlayerItemConsumeEvent consume = mock(PlayerItemConsumeEvent.class);
        final ItemStack replacement = mock(ItemStack.class);
        when(consume.getPlayer()).thenReturn(player);
        when(consume.getItem()).thenReturn(item);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(wandItems.create(WandMode.CRUMB)).thenReturn(replacement);

        listener.onConsume(consume);

        verify(consume).setReplacement(replacement);
        verify(consume, never()).setCancelled(true);
        verify(feedback).send(player, EditorService.EditorResult.success("commands.bkeditor.wand.eaten"));
    }

    @Test
    void wandIsNotEatenOutsideSurvival() {
        final PlayerItemConsumeEvent consume = mock(PlayerItemConsumeEvent.class);
        when(consume.getPlayer()).thenReturn(player);
        when(consume.getItem()).thenReturn(item);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        listener.onConsume(consume);

        verify(consume).setCancelled(true);
        verify(consume, never()).setReplacement(any());
    }

    @Test
    void entityInteractionWithWandIsCancelledDuringSession() {
        final PlayerInteractEntityEvent entityEvent = mock(PlayerInteractEntityEvent.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        when(entityEvent.getPlayer()).thenReturn(player);
        when(entityEvent.getHand()).thenReturn(EquipmentSlot.HAND);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(item);

        listener.onInteractEntity(entityEvent);

        verify(entityEvent).setCancelled(true);
    }

    @Test
    void armorStandInteractionWithWandIsCancelledDuringSession() {
        final PlayerInteractAtEntityEvent entityEvent = mock(PlayerInteractAtEntityEvent.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        when(entityEvent.getPlayer()).thenReturn(player);
        when(entityEvent.getHand()).thenReturn(EquipmentSlot.HAND);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(item);

        listener.onInteractAtEntity(entityEvent);

        verify(entityEvent).setCancelled(true);
    }

    @Test
    void placingWandFromCursorIntoContainerIsCancelled() {
        final InventoryClickEvent click = click(mock(Inventory.class), InventoryAction.PLACE_ALL);
        when(click.getCursor()).thenReturn(item);

        listener.onInventoryClick(click);

        verify(click).setCancelled(true);
    }

    @Test
    void creativeSlotChangesFollowTheSameRules() {
        final InventoryCreativeEvent intoCraftingGrid = mock(InventoryCreativeEvent.class);
        when(intoCraftingGrid.getClickedInventory()).thenReturn(mock(CraftingInventory.class));
        when(intoCraftingGrid.getCursor()).thenReturn(item);
        listener.onInventoryClick(intoCraftingGrid);
        verify(intoCraftingGrid).setCancelled(true);

        final InventoryCreativeEvent ownSlot = mock(InventoryCreativeEvent.class);
        when(ownSlot.getClickedInventory()).thenReturn(mock(PlayerInventory.class));
        when(ownSlot.getAction()).thenReturn(InventoryAction.PLACE_ALL);
        lenient().when(ownSlot.getCursor()).thenReturn(item);
        listener.onInventoryClick(ownSlot);
        verify(ownSlot, never()).setCancelled(anyBoolean());
    }

    @Test
    void hotbarSwappingWandIntoContainerIsCancelled() {
        final InventoryClickEvent click = click(mock(Inventory.class), InventoryAction.HOTBAR_SWAP);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        when(click.getClick()).thenReturn(ClickType.NUMBER_KEY);
        when(click.getHotbarButton()).thenReturn(2);
        when(click.getWhoClicked()).thenReturn(player);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(2)).thenReturn(item);

        listener.onInventoryClick(click);

        verify(click).setCancelled(true);
    }

    @Test
    void shiftClickingWandIntoContainerIsCancelledButAllowedInOwnInventory() {
        final InventoryClickEvent intoChest = click(mock(PlayerInventory.class), InventoryAction.MOVE_TO_OTHER_INVENTORY);
        when(intoChest.getCurrentItem()).thenReturn(item);
        when(intoChest.getView().getTopInventory().getType()).thenReturn(InventoryType.CHEST);
        listener.onInventoryClick(intoChest);
        verify(intoChest).setCancelled(true);

        final InventoryClickEvent withinOwn = click(mock(PlayerInventory.class), InventoryAction.MOVE_TO_OTHER_INVENTORY);
        when(withinOwn.getCurrentItem()).thenReturn(item);
        when(withinOwn.getView().getTopInventory().getType()).thenReturn(InventoryType.CRAFTING);
        listener.onInventoryClick(withinOwn);
        verify(withinOwn, never()).setCancelled(anyBoolean());
    }

    @Test
    void puttingWandIntoBundleIsCancelledButMovingItInOwnInventoryIsAllowed() {
        final ItemStack bundle = mock(ItemStack.class);
        when(bundle.getType()).thenReturn(Material.BUNDLE);
        final InventoryClickEvent intoBundle = click(mock(PlayerInventory.class), InventoryAction.SWAP_WITH_CURSOR);
        when(intoBundle.getCursor()).thenReturn(item);
        when(intoBundle.getCurrentItem()).thenReturn(bundle);
        listener.onInventoryClick(intoBundle);
        verify(intoBundle).setCancelled(true);

        final InventoryClickEvent ownSlot = click(mock(PlayerInventory.class), InventoryAction.PLACE_ALL);
        when(ownSlot.getCursor()).thenReturn(item);
        listener.onInventoryClick(ownSlot);
        verify(ownSlot, never()).setCancelled(anyBoolean());
    }

    @Test
    void draggingWandIntoContainerIsCancelledButNotWithinOwnInventory() {
        final InventoryDragEvent intoChest = drag(Set.of(3, 40));
        listener.onInventoryDrag(intoChest);
        verify(intoChest).setCancelled(true);

        final InventoryDragEvent withinOwn = drag(Set.of(30, 40));
        listener.onInventoryDrag(withinOwn);
        verify(withinOwn, never()).setCancelled(anyBoolean());
    }

    private InventoryClickEvent click(final Inventory clicked, final InventoryAction action) {
        final InventoryClickEvent click = mock(InventoryClickEvent.class);
        final InventoryView view = mock(InventoryView.class);
        lenient().when(view.getTopInventory()).thenReturn(mock(Inventory.class));
        lenient().when(click.getView()).thenReturn(view);
        lenient().when(click.getClickedInventory()).thenReturn(clicked);
        lenient().when(click.getAction()).thenReturn(action);
        lenient().when(click.getClick()).thenReturn(ClickType.LEFT);
        lenient().when(click.getHotbarButton()).thenReturn(-1);
        return click;
    }

    private InventoryDragEvent drag(final Set<Integer> rawSlots) {
        final InventoryDragEvent drag = mock(InventoryDragEvent.class);
        final InventoryView view = mock(InventoryView.class);
        final Inventory top = mock(Inventory.class);
        when(drag.getView()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(top);
        when(top.getSize()).thenReturn(27);
        when(drag.getOldCursor()).thenReturn(item);
        when(drag.getRawSlots()).thenReturn(rawSlots);
        return drag;
    }
}
