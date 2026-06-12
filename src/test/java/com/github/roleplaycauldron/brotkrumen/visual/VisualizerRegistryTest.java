package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.mockito.Mockito.*;

class VisualizerRegistryTest {

    @Test
    void startVisibilityUpdatesDoesNotScheduleDuplicateTask() {
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.scheduleSyncRepeatingTask(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(42);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            final VisualizerRegistry registry = registry();

            registry.startVisibilityUpdates();
            registry.startVisibilityUpdates();

            verify(scheduler, times(1)).scheduleSyncRepeatingTask(any(), any(Runnable.class), anyLong(), anyLong());
        }
    }

    @Test
    void startAfterStopSchedulesNewTask() {
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.scheduleSyncRepeatingTask(any(), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(42)
                .thenReturn(43);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            final VisualizerRegistry registry = registry();

            registry.startVisibilityUpdates();
            registry.stopVisibilityUpdates();
            registry.startVisibilityUpdates();

            verify(scheduler, times(2)).scheduleSyncRepeatingTask(any(), any(Runnable.class), anyLong(), anyLong());
            verify(scheduler).cancelTask(42);
        }
    }

    @Test
    void showAndHideMapToSynchronousLifecycle() {
        final Visualizer visualizer = mock(Visualizer.class);
        final VisualizerRegistry registry = registry();
        final UUID viewerId = UUID.randomUUID();

        registry.show(viewerId, visualizer);
        registry.hide(viewerId);

        verify(visualizer).visibilityUpdate();
        verify(visualizer).shutdown();
    }

    private VisualizerRegistry registry() {
        final Brotkrumen plugin = mock(Brotkrumen.class);
        when(plugin.getServer()).thenReturn(mock(Server.class));
        return new VisualizerRegistry(plugin, mock(WrappedLogger.class));
    }
}
