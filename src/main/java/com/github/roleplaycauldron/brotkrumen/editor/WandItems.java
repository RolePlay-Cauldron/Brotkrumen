package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.language.Localization;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.Supplier;

/**
 * Creates, recognizes and removes editor wand items.
 */
public final class WandItems {

    private static final String ITEM_NAME_KEY = "commands.bkeditor.wand.itemName.";

    private static final String ITEM_LORE_KEY = "commands.bkeditor.wand.itemLore.";

    private final NamespacedKey markerKey;

    private final Supplier<WandSettings> settings;

    private final Localization localization;

    /**
     * Creates the wand item helper.
     *
     * @param plugin       owning plugin, used for the marker key
     * @param settings     supplier of the current wand settings
     * @param localization localization used for item name and lore
     */
    public WandItems(final Plugin plugin, final Supplier<WandSettings> settings, final Localization localization) {
        this.markerKey = new NamespacedKey(plugin, "wand");
        this.settings = settings;
        this.localization = localization;
    }

    /**
     * Creates a new wand item showing the given mode.
     *
     * @param mode wand mode shown in name and lore
     * @return wand item
     */
    public ItemStack create(final WandMode mode) {
        final ItemStack item = new ItemStack(settings.get().material());
        item.editMeta(meta -> {
            applyMode(meta, mode);
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    private void applyMode(final ItemMeta meta, final WandMode mode) {
        meta.displayName(localization.getFormattedMessage(ITEM_NAME_KEY + mode.configValue()));
        meta.lore(List.<Component>of(localization.getFormattedMessage(ITEM_LORE_KEY + mode.configValue())));
    }

    /**
     * Updates name and lore of all wands in a player inventory to show the given mode.
     *
     * @param player target player
     * @param mode   wand mode to show
     */
    public void refresh(final Player player, final WandMode mode) {
        for (final ItemStack stack : player.getInventory().getContents()) {
            if (isWand(stack)) {
                stack.editMeta(meta -> applyMode(meta, mode));
            }
        }
    }

    /**
     * Checks whether an item carries the wand marker.
     *
     * @param item item to check, may be null
     * @return true if the item is a wand
     */
    public boolean isWand(final ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    /**
     * Gives a wand to a player unless they already carry one.
     *
     * @param player target player
     * @param mode   wand mode shown on the new item
     * @return true if a wand was added to the inventory
     */
    public boolean give(final Player player, final WandMode mode) {
        final PlayerInventory inventory = player.getInventory();
        for (final ItemStack stack : inventory.getContents()) {
            if (isWand(stack)) {
                return false;
            }
        }
        inventory.addItem(create(mode)).values()
                .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        return true;
    }

    /**
     * Removes all wands from a player's inventory.
     *
     * @param player target player
     */
    public void removeFrom(final Player player) {
        final PlayerInventory inventory = player.getInventory();
        final ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isWand(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }
}
