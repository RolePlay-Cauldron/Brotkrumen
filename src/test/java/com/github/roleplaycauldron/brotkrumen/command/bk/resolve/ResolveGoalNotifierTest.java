package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.language.Localization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyFloat;

/**
 * Tests for {@link ResolveGoalNotifier}.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ResolveGoalNotifierTest {

    private static final String MESSAGE_KEY = "commands.bk.resolve.status.guidanceComplete";

    private static final String TITLE_KEY = "commands.bk.resolve.status.guidanceCompleteTitle";

    private static final String SUBTITLE_KEY = "commands.bk.resolve.status.guidanceCompleteSubtitle";

    @Test
    void sendsEnabledDefaultEffects() {
        final Harness harness = harness();

        harness.notifier().notify(harness.player(), ResolveGoalOptions.defaults());

        verify(harness.localization()).getPrefixedMessage(MESSAGE_KEY);
        verify(harness.player()).sendMessage(any(Component.class));
        verify(harness.player()).playSound(any(Location.class), eq("entity.player.levelup"), eq(1.0F), eq(1.0F));
        verify(harness.player(), never()).showTitle(any(Title.class));
    }

    @Test
    void skipsDisabledMessageAndSound() {
        final Harness harness = harness();
        final ResolveGoalOptions options = new ResolveGoalOptions(false, false, "entity.player.levelup",
                1.0F, 1.0F, false, 10, 40, 10);

        harness.notifier().notify(harness.player(), options);

        verify(harness.localization(), never()).getPrefixedMessage(MESSAGE_KEY);
        verify(harness.player(), never()).sendMessage(any(Component.class));
        verify(harness.player(), never()).playSound(any(Location.class), any(String.class), anyFloat(), anyFloat());
    }

    @Test
    void sendsEnabledTitleFromLocalization() {
        final Harness harness = harness();
        final ResolveGoalOptions options = new ResolveGoalOptions(false, false, "entity.player.levelup",
                1.0F, 1.0F, true, 5, 30, 7);

        harness.notifier().notify(harness.player(), options);

        verify(harness.localization()).getFormattedMessage(TITLE_KEY);
        verify(harness.localization()).getFormattedMessage(SUBTITLE_KEY);
        verify(harness.player()).showTitle(any(Title.class));
    }

    @Test
    void skipsInvalidSoundName() {
        final Harness harness = harness();
        final ResolveGoalOptions options = new ResolveGoalOptions(false, true, "Invalid Sound",
                1.0F, 1.0F, false, 10, 40, 10);

        harness.notifier().notify(harness.player(), options);

        verify(harness.player(), never()).playSound(any(Location.class), any(String.class), anyFloat(), anyFloat());
    }

    private Harness harness() {
        final Player player = mock(Player.class);
        final Localization localization = mock(Localization.class);
        when(player.getLocation()).thenReturn(new Location(null, 0.0D, 64.0D, 0.0D));
        when(localization.getPrefixedMessage(MESSAGE_KEY)).thenReturn(Component.text("complete"));
        when(localization.getFormattedMessage(TITLE_KEY)).thenReturn(Component.text("title"));
        when(localization.getFormattedMessage(SUBTITLE_KEY)).thenReturn(Component.text("subtitle"));
        return new Harness(player, localization, new ResolveGoalNotifier(localization));
    }

    private record Harness(Player player, Localization localization, ResolveGoalNotifier notifier) {
    }
}
