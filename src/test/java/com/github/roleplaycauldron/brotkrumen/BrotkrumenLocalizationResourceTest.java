package com.github.roleplaycauldron.brotkrumen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests locale-resource path derivation used by startup localization extraction.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class BrotkrumenLocalizationResourceTest {

    @Test
    void derivesLanguageResourcePathFromLocaleTag() {
        assertEquals("language/en-us.yml", Brotkrumen.localeResourcePath("en-US"), "Should derive en-us path");
        assertEquals("language/de-de.yml", Brotkrumen.localeResourcePath("de-de"), "Should derive de-de path");
        assertEquals("language/de-de-dark.yml", Brotkrumen.localeResourcePath("de-de-dark"), "Should derive de-de-dark path");
    }
}
