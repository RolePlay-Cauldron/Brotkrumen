package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommandFeedback;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WandListener}.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.UnitTestAssertionsShouldIncludeMessage"})
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
        lenient().when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(editorService).placeNodeAtExactLocation(eq(PLAYER_ID), any(Location.class));
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
}
