package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.language.Localization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
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
        final List<Component> lore = Arrays.stream(localization.getRawMessage(ITEM_LORE_KEY + mode.configValue())
                        .split("\\R"))
                .map(line -> localization.getMessageFromString(line)
                        .decoration(TextDecoration.ITALIC, false))
                .toList();
        meta.lore(lore);
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
     * Checks whether an item can be eaten, which is the case for the default bread wand.
     *
     * @param item item to check, may be null
     * @return true if the item material is edible
     */
    public boolean isEdible(final ItemStack item) {
        return item != null && item.getType().isEdible();
    }

    /**
     * Gives a wand to a player unless they already carry one.
     *
     * @param player target player
     * @param mode   wand mode shown on the new item
     * @return whether a wand was added, or why not
     */
    public GiveResult give(final Player player, final WandMode mode) {
        final PlayerInventory inventory = player.getInventory();
        for (final ItemStack stack : inventory.getContents()) {
            if (isWand(stack)) {
                return GiveResult.ALREADY_CARRIED;
            }
        }
        if (isWand(player.getItemOnCursor())) {
            return GiveResult.ALREADY_CARRIED;
        }
        if (inventory.firstEmpty() == -1) {
            return GiveResult.INVENTORY_FULL;
        }
        inventory.addItem(create(mode));
        return GiveResult.GIVEN;
    }

    /**
     * Removes all wands from a player's inventory, ender chest and cursor.
     *
     * @param player target player
     */
    public void removeFrom(final Player player) {
        removeFrom(player.getInventory());
        removeFrom(player.getEnderChest());
        if (isWand(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }

    private void removeFrom(final Inventory inventory) {
        final ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isWand(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    /**
     * Outcome of giving a wand to a player.
     */
    public enum GiveResult {
        /**
         * A new wand was added to the inventory.
         */
        GIVEN("commands.bkeditor.wand.given"),

        /**
         * The player already carries a wand.
         */
        ALREADY_CARRIED("commands.bkeditor.wand.alreadyCarried"),

        /**
         * The inventory has no free slot for the wand.
         */
        INVENTORY_FULL("commands.bkeditor.wand.inventoryFull");

        private final String key;

        GiveResult(final String messageKey) {
            this.key = messageKey;
        }

        /**
         * Returns the localization key describing this outcome.
         *
         * @return message key
         */
        public String messageKey() {
            return key;
        }
    }
}
