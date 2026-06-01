package com.github.roleplaycauldron.brotkrumen.language;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalizationTest {

    /* default */
    @TempDir
    Path tempDir;

    private static String validLocaleContent(final String locale, final String welcomeText) {
        return validLocaleContent(locale, "<gray>", welcomeText);
    }

    private static String validLocaleContent(final String locale, final String prefix, final String welcomeText) {
        return """
                schemaVersion: 1
                locale: "%s"
                prefix: "%s"
                messages:
                  welcome: "%s"
                """.formatted(locale, prefix, welcomeText);
    }

    @Test
    void rejectsLocaleWithoutSchemaVersion() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "Welcome!"));
        writeLanguageFile("de-de.yml", """
                locale: "de-DE"
                prefix: "<gray>"
                messages:
                  welcome: "Willkommen!"
                """);

        final Localization localization = localization("de-de");

        assertEquals(Localization.HealthState.DEGRADED, localization.healthState(),
                "Invalid files should degrade, not hard-fail, while valid locales remain");
        assertEquals("Welcome!", localization.getRawMessageWithoutFormats("de-de", "welcome"),
                "Invalid locale files must be ignored during fallback");
    }

    @Test
    void playerFallbackUsesLanguageThenConfiguredDefaultThenHardFallback() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "Welcome!"));
        writeLanguageFile("de.yml", validLocaleContent("de", "Willkommen!"));
        writeLanguageFile("fr-fr.yml", validLocaleContent("fr-FR", "Bienvenue!"));

        final Localization localization = localization("fr-fr");

        assertEquals("Willkommen!", localization.getRawMessageWithoutFormats("de-de", "welcome"),
                "Player locale should fall back to language-only locale");
        assertEquals("Bienvenue!", localization.getRawMessageWithoutFormats("es-es", "welcome"),
                "Player locale should fall back to configured default before en-US");
    }

    @Test
    void nonPlayerFallbackUsesConfiguredDefaultLanguage() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "Welcome!"));
        writeLanguageFile("de.yml", validLocaleContent("de", "Willkommen!"));

        final Localization localization = localization("de-de");

        assertEquals("Willkommen!", localization.getRawMessageWithoutFormats("welcome"),
                "Non-player context should use configured default locale with language-only fallback");
    }

    @Test
    void supportsCustomThemeTags() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "Welcome!"));
        writeLanguageFile("de-dark.yml", validLocaleContent("de-de-dark", "Willkommen (Dark)!"));

        final Localization localization = localization("de-de-dark");

        assertEquals("Willkommen (Dark)!", localization.getRawMessageWithoutFormats("welcome"),
                "Custom themed language tags should be usable as configured defaults");
    }

    @Test
    void stillAttemptsLookupWhenLocalizationFailed() {
        final Localization localization = localization("de-de");

        assertTrue(localization.getRawMessageWithoutFormats("de-de", "welcome")
                        .contains("No message to the key"),
                "Normal lookup should still attempt localization instead of using raw fallback automatically");
    }

    @Test
    void fallsBackToHardFallbackWhenConfiguredDefaultMissing() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "Welcome!"));

        final Localization localization = localization("it-it");

        assertEquals("Welcome!", localization.getRawMessageWithoutFormats("es-es", "welcome"),
                "Hard fallback en-US should be used when configured default locale cannot resolve");
    }

    @Test
    void rendersLocalizedComponentForPlayerLanguageTag() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "Welcome!"));

        final Localization localization = localization("en-us");

        final Component message = localization.getFormattedMessage("en-us", "welcome");

        assertEquals("Welcome!", PlainTextComponentSerializer.plainText().serialize(message),
                "Localized player-context rendering should return a component");
    }

    @Test
    void rendersLocalizedComponentForNonPlayerDefaultLocale() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "Welcome!"));
        writeLanguageFile("de.yml", validLocaleContent("de", "Willkommen!"));

        final Localization localization = localization("de-de");

        final Component message = localization.getFormattedMessage("welcome");

        assertEquals("Willkommen!", PlainTextComponentSerializer.plainText().serialize(message),
                "Non-player component rendering should use configured default locale fallback");
    }

    @Test
    void parsesMiniMessageReplacementsInRenderedMessage() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "<gray>Hello <player>!</gray>"));

        final Localization localization = localization("en-us");

        final Component message = localization.getFormattedMessage("en-us", "welcome", Map.of("player", "<red>Kai"));

        assertEquals("Hello Kai!", PlainTextComponentSerializer.plainText().serialize(message),
                "Replacement values should be parsed as MiniMessage");
        assertTrue(MiniMessage.miniMessage().serialize(message).contains("<red>Kai"),
                "Rendered component should retain parsed replacement formatting");
    }

    @Test
    void prefixesLocalizedMessagesWithLocalePrefix() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "<gray>[BK] </gray>", "Welcome, <player>!"));

        final Localization localization = localization("en-us");

        final Component message = localization.getPrefixedMessage("en-us", "welcome", Map.of("player", "Sam"));

        assertEquals("[BK] Welcome, Sam!", PlainTextComponentSerializer.plainText().serialize(message),
                "Prefixed localization rendering should include locale prefix and message body");
    }

    @Test
    void prefixesExistingComponentsWithLocalePrefix() throws IOException {
        writeLanguageFile("en-us.yml", validLocaleContent("en-US", "<gray>[BK] </gray>", "Welcome!"));

        final Localization localization = localization("en-us");

        final Component message = localization.getPrefixedMessage("en-us", Component.text("Body"));

        assertEquals("[BK] Body", PlainTextComponentSerializer.plainText().serialize(message),
                "Prefix composition should work for already-rendered component messages");
    }

    private Localization localization(final String configuredDefaultTag) {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        return new Localization(mock(WrappedLogger.class), plugin, configuredDefaultTag);
    }

    private void writeLanguageFile(final String fileName, final String content) throws IOException {
        final Path languageDir = tempDir.resolve("language");
        Files.createDirectories(languageDir);
        Files.writeString(languageDir.resolve(fileName), content);
    }
}
