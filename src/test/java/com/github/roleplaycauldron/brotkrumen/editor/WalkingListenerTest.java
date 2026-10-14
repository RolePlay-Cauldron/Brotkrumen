package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommandFeedback;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WalkingListener}.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class WalkingListenerTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    @Mock
    private WrappedLogger log;

    @Mock
    private EditorService editorService;

    @Mock
    private WandItems wandItems;

    @Mock
    private EditorCommandFeedback feedback;

    @Mock
    private PlayerMoveEvent event;

    @Mock
    private Player player;

    @Mock
    private PlayerInventory inventory;

    @Mock
    private ItemStack wand;

    @Mock
    private ItemStack sword;

    private WandSettings settings;

    private WalkingListener listener;

    @BeforeEach
    void setUp() {
        settings = WandSettings.defaults();
        listener = new WalkingListener(log, editorService, wandItems, () -> settings, feedback);
        lenient().when(event.hasChangedBlock()).thenReturn(true);
        lenient().when(event.getPlayer()).thenReturn(player);
        lenient().when(player.getUniqueId()).thenReturn(PLAYER_ID);
        lenient().when(player.getInventory()).thenReturn(inventory);
        lenient().when(player.getLocation()).thenReturn(new Location(null, 0.0D, 0.0D, 0.0D));
        lenient().when(editorService.isEditing(PLAYER_ID)).thenReturn(true);
        lenient().when(wandItems.isWand(wand)).thenReturn(true);
        lenient().when(editorService.handleMovement(eq(PLAYER_ID), any(Location.class)))
                .thenReturn(EditorService.EditorResult.success(""));
    }

    @Test
    void walkingWithWandSkipsAutoPlacement() {
        when(inventory.getItemInMainHand()).thenReturn(wand);

        listener.onPlayerMove(event);

        verify(editorService, never()).handleMovement(any(), any());
        verify(editorService, never()).pauseForAppendAnchor(any(), any());
    }

    @Test
    void puttingWandAwayPausesForAnchorBeforeResuming() {
        final EditorService.EditorResult paused = EditorService.EditorResult.success(
                "commands.bkeditor.wand.pausedForAnchor");
        when(editorService.pauseForAppendAnchor(eq(PLAYER_ID), any(Location.class))).thenReturn(paused);
        when(inventory.getItemInMainHand()).thenReturn(wand);
        listener.onPlayerMove(event);

        when(inventory.getItemInMainHand()).thenReturn(sword);
        listener.onPlayerMove(event);
        listener.onPlayerMove(event);

        final InOrder order = inOrder(editorService, feedback);
        order.verify(editorService).pauseForAppendAnchor(eq(PLAYER_ID), any(Location.class));
        order.verify(feedback).send(player, paused);
        order.verify(editorService, times(2)).handleMovement(eq(PLAYER_ID), any(Location.class));
        verify(editorService, times(1)).pauseForAppendAnchor(eq(PLAYER_ID), any(Location.class));
    }

    @Test
    void walkingWithoutEverHoldingWandDoesNotPause() {
        when(inventory.getItemInMainHand()).thenReturn(sword);

        listener.onPlayerMove(event);

        verify(editorService, never()).pauseForAppendAnchor(any(), any());
        verify(editorService).handleMovement(eq(PLAYER_ID), any(Location.class));
    }

    @Test
    void autoPlacementWhileHeldKeepsPlacingWithoutPause() {
        settings = new WandSettings(settings.material(), true, false, false, false,
                settings.defaultEdgeType(), true);
        lenient().when(inventory.getItemInMainHand()).thenReturn(wand);

        listener.onPlayerMove(event);
        lenient().when(inventory.getItemInMainHand()).thenReturn(sword);
        listener.onPlayerMove(event);

        verify(editorService, never()).pauseForAppendAnchor(any(), any());
        verify(editorService, times(2)).handleMovement(eq(PLAYER_ID), any(Location.class));
    }
}
