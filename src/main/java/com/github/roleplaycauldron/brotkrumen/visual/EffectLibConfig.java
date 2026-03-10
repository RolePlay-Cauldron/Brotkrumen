package com.github.roleplaycauldron.brotkrumen.visual;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents the configuration for an effect in the EffectLib system.
 * This record holds the class name of the effect and its associated settings.
 * <p>
 * It is primarily used for registering and managing effects with specific configurations
 * in a plugin or application that uses the EffectLib library.
 *
 * @param effectClass the fully qualified class name of the effect to be instantiated
 * @param settings    the configuration section containing customizable parameters for the effect
 */
public record EffectLibConfig(String effectClass, ConfigurationSection settings) {
}
