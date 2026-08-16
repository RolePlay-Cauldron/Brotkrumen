package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepository;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepository;
import com.github.roleplaycauldron.brotkrumen.visual.design.VisualPresetLoadException;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.same;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class BkReloadSubcommandTest {

    private static final Component SUCCESS_MESSAGE = Component.text("Reload succeeded");

    private static final Component FAILURE_MESSAGE = Component.text("Reload failed");

    @Mock
    private Brotkrumen plugin;

    @Mock
    private Server server;

    @Mock
    private BukkitScheduler scheduler;

    @Mock
    private GraphRepository graphRepository;

    @Mock
    private GraphNetworkRepository graphNetworkRepository;

    @Mock
    private LoggerFactory loggerFactory;

    @Mock
    private WrappedLogger logger;

    @Mock
    private Localization localization;

    @Mock
    private CommandContext<CommandSourceStack> commandContext;

    @Mock
    private CommandSourceStack source;

    @Mock
    private CommandSender sender;

    private BkReloadSubcommand command;

    @BeforeEach
    void setUp() {
        lenient().when(plugin.getServer()).thenReturn(server);
        lenient().when(server.getScheduler()).thenReturn(scheduler);
        lenient().when(loggerFactory.create(any())).thenReturn(logger);
        lenient().when(localization.healthState()).thenReturn(Localization.HealthState.READY);
        lenient().when(localization.getPrefixedMessage("commands.bk.reload.success")).thenReturn(SUCCESS_MESSAGE);
        lenient().when(localization.getPrefixedMessage("commands.bk.reload.error.failed")).thenReturn(FAILURE_MESSAGE);
        lenient().when(commandContext.getSource()).thenReturn(source);
        lenient().when(source.getSender()).thenReturn(sender);
        runScheduledTasksImmediately();
        command = new BkReloadSubcommand(new BkCommandContext(plugin, graphRepository, graphNetworkRepository,
                null, null, null, loggerFactory, null, null, null, null), localization);
    }

    @Test
    void presetFailureSendsFailureFeedbackAndDoesNotStartGraphReload() {
        final VisualPresetLoadException failure = new VisualPresetLoadException("Invalid preset");
        doThrow(failure).when(plugin).reloadVisualPresets();

        final int result = command.execute(commandContext);

        assertEquals(0, result, "A synchronous preset failure should fail the command");
        verify(sender).sendMessage(FAILURE_MESSAGE);
        verify(graphRepository, never()).reloadGraphs();
        verify(graphNetworkRepository, never()).reloadGraphNetworks();
        verify(logger).error(contains("Invalid preset"), same(failure));
    }

    @Test
    void degradedLocalizationSendsLocalizedFailureFeedbackAndDoesNotReloadPresets() {
        when(localization.healthState()).thenReturn(Localization.HealthState.DEGRADED);

        final int result = command.execute(commandContext);

        assertEquals(0, result, "An unhealthy localization reload should fail the command");
        verify(sender).sendMessage(FAILURE_MESSAGE);
        verify(plugin, never()).reloadVisualPresets();
        verify(graphRepository, never()).reloadGraphs();
    }

    @Test
    void failedLocalizationSendsFallbackFeedbackAndDoesNotReloadPresets() {
        when(localization.healthState()).thenReturn(Localization.HealthState.FAILED);

        final int result = command.execute(commandContext);

        assertEquals(0, result, "An unhealthy localization reload should fail the command");
        verify(sender).sendMessage(Component.text("Reload failed. Check the console for details."));
        verify(plugin, never()).reloadVisualPresets();
        verify(graphRepository, never()).reloadGraphs();
    }

    @Test
    void asynchronousGraphFailureSendsFailureInsteadOfSuccess() {
        final RuntimeException failure = new RuntimeException("Database unavailable");
        doThrow(failure).when(graphRepository).reloadGraphs();

        final int result = command.execute(commandContext);

        assertEquals(Command.SINGLE_SUCCESS, result, "Scheduling accepted the command before its async failure");
        verify(sender).sendMessage(FAILURE_MESSAGE);
        verify(sender, never()).sendMessage(SUCCESS_MESSAGE);
        verify(graphNetworkRepository, never()).reloadGraphNetworks();
        verify(logger).error(contains("Database unavailable"), same(failure));
    }

    @Test
    void completeReloadSendsExactlyOneSuccessMessage() {
        final int result = command.execute(commandContext);

        assertEquals(Command.SINGLE_SUCCESS, result, "A complete reload should succeed");
        verify(graphRepository).reloadGraphs();
        verify(graphNetworkRepository).reloadGraphNetworks();
        verify(sender).sendMessage(SUCCESS_MESSAGE);
        verify(sender, never()).sendMessage(FAILURE_MESSAGE);
    }

    private void runScheduledTasksImmediately() {
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return mock(BukkitTask.class);
        }).when(scheduler).runTask(any(org.bukkit.plugin.Plugin.class), any(Runnable.class));
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return mock(BukkitTask.class);
        }).when(scheduler).runTaskAsynchronously(any(org.bukkit.plugin.Plugin.class), any(Runnable.class));
    }
}
