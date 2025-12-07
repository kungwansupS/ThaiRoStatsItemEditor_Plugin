package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AttributeEditorGUI {

    private final ItemEditorPlugin plugin;
    private final ItemAttributeManager attributeManager;
    private static final int INVENTORY_SIZE = 54;
    public static final String GUI_TITLE = "§0§lItem Attribute Editor";
    private static final int CONTENT_START_SLOT = 9;

    public AttributeEditorGUI(ItemEditorPlugin plugin, ItemAttributeManager attributeManager) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
    }

    public void open(Player player, ItemStack item, ItemCategory activeTab) {
        Inventory inv = Bukkit.createInventory(player, INVENTORY_SIZE, Component.text(GUI_TITLE));

        // Store the item being edited in the GUI (Slot 0)
        inv.setItem(0, item.clone());

        // Filler/Background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            // Fill slots if empty or in content area
            if (i > 8 && inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        // --- CONTROL ROW (R0) ---
        inv.setItem(8, createItem(Material.LIME_DYE, "§a§lSave & Close", "§7Save changes and return item to hand.")); // Modified: Save & Close
        inv.setItem(1, createItem(Material.PAPER, "§b§lEdit Custom Lore", "§7Click to edit the Custom Lore section."));
        inv.setItem(17, createItem(Material.BUCKET, "§e§lClear Vanilla Tags", // Slot 17 (R1 C8)
                "§7Click to remove all vanilla",
                "§7attributes, enchantments, and lore.",
                "§cUSE WITH CAUTION: Cannot be undone!"
        ));

        // --- PRESET MANAGEMENT BUTTONS (R0) ---
        inv.setItem(0, createItem(Material.ANVIL, "§6§lSave as Preset", "§7Saves the item (Slot 0) to Item Storage.")); // Slot 0 is replaced by save
        inv.setItem(16, createItem(Material.CHEST, "§6§lItem Storage", "§7View/Copy/Delete Saved Item Presets.")); // Slot 16 (R1 C7)


        // 1. Display Tab Buttons (Slots 2-7)
        displayTabs(inv, activeTab);

        // 2. Display Content for the active tab
        displayContent(inv, item, activeTab);

        player.openInventory(inv);
    }

    private void displayTabs(Inventory inv, ItemCategory activeTab) {
        for (ItemCategory category : ItemCategory.values()) {
            inv.setItem(category.getSlot(), createTabItem(category, activeTab));
        }
    }

    private void displayContent(Inventory inv, ItemStack editingItem, ItemCategory activeTab) {
        int slot = CONTENT_START_SLOT;

        // Clear old content area first (Clear slots 9-53)
        for(int i = CONTENT_START_SLOT; i < INVENTORY_SIZE; i++) {
            if(inv.getItem(i) != null && inv.getItem(i).getType() != Material.GRAY_STAINED_GLASS_PANE) {
                inv.setItem(i, null);
            } else if (inv.getItem(i) == null) {
                inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }

        Map<ItemCategory, List<ItemAttribute>> categorizedAttributes = ItemAttribute.getCategorizedAttributes();
        List<ItemAttribute> attributes = categorizedAttributes.get(activeTab);

        for (ItemAttribute attribute : attributes) {
            if (slot >= INVENTORY_SIZE) break;
            inv.setItem(slot, createAttributeIcon(editingItem, attribute));
            slot++;
        }
    }

    private ItemStack createTabItem(ItemCategory category, ItemCategory activeTab) {
        String name = (category == activeTab ? "§a§l" : "§f") + category.getDisplayName();
        ItemStack item = createItem(category.getMaterial(), name,
                "§7Click to view/edit",
                "§7" + category.getDisplayName()
        );
        if (category == activeTab) {
            item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }


    public ItemStack createAttributeIcon(ItemStack item, ItemAttribute attribute) {
        double currentValue = attributeManager.getAttribute(item, attribute);

        List<String> lore = new ArrayList<>();
        lore.add("§7Key: §f" + attribute.getKey());
        lore.add("§7Current Value: " + (currentValue != 0 ? "§a" : "§7") + String.format(attribute.getFormat(), currentValue));
        lore.add(" ");
        lore.add("§eLeft-Click: §7+" + String.format(attribute.getFormat(), attribute.getClickStep()));
        lore.add("§eRight-Click: §7-" + String.format(attribute.getFormat(), attribute.getClickStep()));
        lore.add("§eShift+Left-Click: §7+" + String.format(attribute.getFormat(), attribute.getRightClickStep()));
        lore.add("§eShift+Right-Click: §7-" + String.format(attribute.getFormat(), attribute.getRightClickStep()));
        lore.add("§eMiddle-Click: §7Reset to 0");

        return createItem(attribute.getMaterial(), attribute.getDisplayName(), lore.toArray(new String[0]));
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) loreList.add(Component.text(line));
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    public ItemCategory getCategoryBySlot(int slot) {
        for(ItemCategory category : ItemCategory.values()) {
            if(category.getSlot() == slot) {
                return category;
            }
        }
        if (slot == 16) return ItemCategory.SPECIAL_UTIL;
        return null;
    }
}