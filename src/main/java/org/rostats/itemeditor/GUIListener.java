package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final ItemEditorPlugin plugin;
    private final ItemAttributeManager attributeManager;
    private final ItemPresetManager presetManager;

    // Keys
    private static final NamespacedKey ACTIVE_TAB_KEY = new NamespacedKey(ItemEditorPlugin.getPluginInstance(), "ACTIVE_TAB");
    private static final NamespacedKey DELETE_PRESET_ID_KEY = new NamespacedKey(ItemEditorPlugin.getPluginInstance(), "DELETE_PRESET_ID");

    private static final int CONTENT_START_SLOT = 9;
    private final Map<ItemCategory, List<ItemAttribute>> categorizedAttributes = ItemAttribute.getCategorizedAttributes();

    public GUIListener(ItemEditorPlugin plugin, ItemAttributeManager attributeManager, ItemPresetManager presetManager) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
        this.presetManager = presetManager;
    }

    // --- Helper Methods ---

    private ItemCategory getActiveTab(Player player) {
        PersistentDataContainer playerPDC = player.getPersistentDataContainer();
        String tabName = playerPDC.getOrDefault(ACTIVE_TAB_KEY, PersistentDataType.STRING, ItemCategory.CORE_BONUS.name());
        try {
            return ItemCategory.valueOf(tabName);
        } catch (IllegalArgumentException e) {
            return ItemCategory.CORE_BONUS;
        }
    }

    private void setActiveTab(Player player, ItemCategory tab) {
        PersistentDataContainer playerPDC = player.getPersistentDataContainer();
        playerPDC.set(ACTIVE_TAB_KEY, PersistentDataType.STRING, tab.name());
    }

    // --- Core GUI Listeners ---

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        // 1. Handle Preset Confirmation GUI
        if (title.contains(PresetConfirmGUI.GUI_TITLE)) {
            event.setCancelled(true);

            if (event.getSlot() == 11 && clickedItem.getType() == Material.LIME_CONCRETE) { // CONFIRM DELETE
                String idStr = clickedItem.getItemMeta().getPersistentDataContainer().get(DELETE_PRESET_ID_KEY, PersistentDataType.STRING);
                if (idStr != null) {
                    try {
                        UUID id = UUID.fromString(idStr);
                        presetManager.deletePreset(id);
                        player.sendMessage("§a[Preset] Preset " + ItemPresetManager.getShortId(id) + " deleted successfully!");
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> new PresetStorageGUI(plugin, presetManager).open(player), 1L); // Reopen storage
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§c[Preset] Error deleting preset.");
                    }
                }
            } else if (event.getSlot() == 15 && clickedItem.getType() == Material.RED_CONCRETE) { // CANCEL
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> new PresetStorageGUI(plugin, presetManager).open(player), 1L); // Reopen storage
            }
            return;
        }


        // 2. Handle Preset Storage GUI
        if (title.contains(PresetStorageGUI.GUI_TITLE)) {
            event.setCancelled(true);

            Inventory inv = event.getInventory();

            if (event.getSlot() == 45) { // BACK to Editor
                player.closeInventory();
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (!handItem.getType().isAir()) {
                    attributeManager.updateLore(handItem);
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> new AttributeEditorGUI(plugin, attributeManager).open(player, handItem, getActiveTab(player)), 1L);
                } else {
                    player.sendMessage("§c[Editor] Please hold an item to return to the editor.");
                }
                return;
            }

            // Save currently held item (Attribute Editor's Slot 0 is usually an ANVIL button)
            if (event.getSlot() == 49 && clickedItem.getType() == Material.BOOK) {
                // We cannot access the editing item from the storage GUI directly.
                // We assume the user has the item they want to save in their main hand when they open the Preset GUI from the Editor.
                ItemStack itemToSave = player.getInventory().getItemInMainHand();
                if (!itemToSave.getType().isAir()) {
                    UUID newId = UUID.randomUUID();
                    presetManager.savePreset(itemToSave, newId);
                    player.sendMessage("§a[Preset] Item in hand saved as preset: §f" + ItemPresetManager.getShortId(newId));
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> new PresetStorageGUI(plugin, presetManager).open(player), 1L);
                } else {
                    player.sendMessage("§c[Preset] Please hold an item in your main hand to save.");
                }
                return;
            }

            if (event.getClick() == ClickType.LEFT && event.getSlot() < 45) { // COPY/GET
                UUID id = getPresetId(clickedItem);
                if (id != null) {
                    ItemStack copy = presetManager.getAllPresets().get(id);
                    if (copy != null) {
                        player.getInventory().addItem(copy.clone());
                        player.sendMessage("§a[Preset] Item copied to inventory.");
                    }
                }
                return;
            }

            if (event.getClick() == ClickType.RIGHT && event.getSlot() < 45) { // DELETE
                UUID id = getPresetId(clickedItem);
                if (id != null) {
                    PresetConfirmGUI.open(player, id, clickedItem);
                }
                return;
            }
            return;
        }

        // 3. Handle Attribute Editor GUI
        if (!title.contains(AttributeEditorGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        Inventory inv = event.getInventory();
        ItemStack editingItem = inv.getItem(0);

        if (editingItem == null || editingItem.getType().isAir()) return;

        int slot = event.getSlot();
        AttributeEditorGUI gui = new AttributeEditorGUI(plugin, attributeManager);
        ItemCategory activeTab = getActiveTab(player);


        // --- A. Control Buttons ---

        // A1. Save & Close (Slot 8)
        if (slot == 8 && clickedItem.getType() == Material.LIME_DYE) {
            player.getInventory().setItemInMainHand(editingItem);
            player.closeInventory();
            player.sendMessage("§aItem editing saved and item replaced.");
            return;
        }

        // A2. Edit Custom Lore (Slot 1)
        if (slot == 1 && clickedItem.getType() == Material.PAPER) {
            player.closeInventory();
            startLoreEditing(player, editingItem);
            return;
        }

        // A3. Save as Preset (Slot 0 - ANVIL button)
        if (slot == 0 && clickedItem.getType() == Material.ANVIL) {
            UUID newId = UUID.randomUUID();
            presetManager.savePreset(editingItem, newId);
            player.sendMessage("§a[Preset] Item saved as preset: §f" + ItemPresetManager.getShortId(newId));
            return;
        }

        // A4. Item Storage (Slot 16)
        if (slot == 16 && clickedItem.getType() == Material.CHEST) {
            player.closeInventory();
            // Store the item back to hand before opening storage for consistency
            player.getInventory().setItemInMainHand(editingItem);
            Bukkit.getScheduler().runTaskLater(plugin, () -> new PresetStorageGUI(plugin, presetManager).open(player), 1L);
            return;
        }

        // A5. Clear Vanilla Tags (Slot 17)
        if (slot == 17 && clickedItem.getType() == Material.BUCKET) {
            attributeManager.clearVanillaAttributesAndLore(editingItem);
            gui.open(player, editingItem, activeTab);
            player.sendMessage("§e[Editor] Vanilla attributes, enchantments, and lore cleared from item.");
            return;
        }

        // --- B. Tab Switching ---
        ItemCategory targetCategory = gui.getCategoryBySlot(slot);
        if (targetCategory != null) {
            if (targetCategory != activeTab) {
                setActiveTab(player, targetCategory);
                gui.open(player, editingItem, targetCategory);
            }
            return;
        }

        // 4. Handle Attribute Modification Clicks (Slots 9+)
        if (slot < CONTENT_START_SLOT) return; // Only process clicks in the content area

        List<ItemAttribute> currentAttributes = categorizedAttributes.get(activeTab);

        int attributeIndex = slot - CONTENT_START_SLOT;
        if (attributeIndex >= currentAttributes.size()) return;

        ItemAttribute attribute = currentAttributes.get(attributeIndex);
        if (attribute == null) return;


        ClickType click = event.getClick();
        double currentValue = attributeManager.getAttribute(editingItem, attribute);
        double newValue = currentValue;
        double step = attribute.getClickStep();
        double largeStep = attribute.getRightClickStep();

        if (click == ClickType.LEFT) {
            newValue += event.isShiftClick() ? largeStep : step;
        } else if (click == ClickType.RIGHT) {
            newValue -= event.isShiftClick() ? largeStep : step;
        } else if (click == ClickType.MIDDLE) {
            newValue = 0.0;
        }

        // Clamp & Round
        newValue = Math.max(-1000.0, Math.min(1000.0, newValue));
        newValue = Math.round(newValue * 1000.0) / 1000.0;

        // Apply changes and refresh
        if (newValue != currentValue) {
            attributeManager.setAttribute(editingItem, attribute, newValue);

            // Update the item in the GUI slot 0
            inv.setItem(0, editingItem.clone());

            // Update the clicked attribute icon (slot)
            inv.setItem(slot, gui.createAttributeIcon(editingItem, attribute));
        }
    }

    // --- Custom Lore Chat Handler ---

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Check if the player was in the middle of lore editing when they closed the inventory
        if (plugin.getLoreEditingPlayers().containsKey(player.getUniqueId())) {
            plugin.getLoreEditingPlayers().remove(player.getUniqueId());
            player.sendMessage("§c[Lore Editor] Editing session aborted (Inventory closed).");
        }
    }

    private void startLoreEditing(Player player, ItemStack item) {
        plugin.getLoreEditingPlayers().put(player.getUniqueId(), new StringBuilder());
        player.sendMessage("§e[Lore Editor] Enter custom lore lines now.");
        player.sendMessage("§eType 'DONE' or 'ยกเลิก' (Cancel) when finished.");

        // Get existing custom lore and initialize the StringBuilder with it
        List<String> customLore = attributeManager.getCustomLore(item);
        if (!customLore.isEmpty()) {
            player.sendMessage("§7Current Custom Lore: ");
            for (String line : customLore) {
                player.sendMessage(line);
                plugin.getLoreEditingPlayers().get(player.getUniqueId()).append(line).append("\n");
            }
        } else {
            player.sendMessage("§7Current Custom Lore: (Empty)");
        }
        player.sendMessage("§e----------------------------------");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getLoreEditingPlayers().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            StringBuilder currentLore = plugin.getLoreEditingPlayers().get(player.getUniqueId());

            if (message.equalsIgnoreCase("DONE")) {
                finishLoreEditing(player, currentLore);
                return;
            }
            if (message.equalsIgnoreCase("ยกเลิก")) {
                plugin.getLoreEditingPlayers().remove(player.getUniqueId());
                player.sendMessage("§c[Lore Editor] Editing cancelled. Lore reverted.");
                return;
            }

            // Append line, replace standard color codes (&) with Bukkit color codes (§)
            currentLore.append(message.replaceAll("&", "§")).append("\n");
            player.sendMessage("§7Added line: " + message.replaceAll("&", "§"));
        }
    }

    private void finishLoreEditing(Player player, StringBuilder loreBuilder) {
        plugin.getLoreEditingPlayers().remove(player.getUniqueId());

        // The lore is applied to the item currently in the player's hand.
        ItemStack item = player.getInventory().getItemInMainHand();

        // Process collected lines
        List<String> newCustomLore = new java.util.ArrayList<>();
        String[] lines = loreBuilder.toString().split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                newCustomLore.add(line);
            }
        }

        attributeManager.setCustomLore(item, newCustomLore);
        player.sendMessage("§a[Lore Editor] Custom Lore updated successfully!");

        // Optional: Reopen the editor for confirmation
        attributeManager.updateLore(item);
        new AttributeEditorGUI(plugin, attributeManager).open(player, item, getActiveTab(player));
    }

    // Helper to extract Preset UUID from an ItemStack in the storage GUI
    private UUID getPresetId(ItemStack item) {
        // This is a reliable but potentially slow method for a huge number of presets.
        // It compares the item passed from the GUI against the managed presets.
        for (Map.Entry<UUID, ItemStack> entry : presetManager.getAllPresets().entrySet()) {
            if (entry.getValue().isSimilar(item)) {
                return entry.getKey();
            }
        }
        return null;
    }
}