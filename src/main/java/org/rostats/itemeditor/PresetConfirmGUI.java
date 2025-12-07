package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory; // <-- แก้ไข: เพิ่ม import
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PresetConfirmGUI {

    public static final String GUI_TITLE = "§c§lCONFIRM DELETION";
    private static final int INVENTORY_SIZE = 27;

    public static void open(Player player, UUID presetId, ItemStack presetItem) {
        Inventory inv = Bukkit.createInventory(player, INVENTORY_SIZE, Component.text(GUI_TITLE));

        // Store the UUID on the item to track it in the listener
        ItemStack confirmBtn = createConfirmButton(presetId);
        inv.setItem(11, confirmBtn);

        ItemStack cancelBtn = createItem(Material.RED_CONCRETE, "§aCancel", "§7Go back to storage menu.");
        inv.setItem(15, cancelBtn);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    // Helper to store UUID for listener tracking
    private static ItemStack createConfirmButton(UUID presetId) {
        ItemStack item = createItem(Material.LIME_CONCRETE, "§c§lYES, DELETE IT!",
                "§7Preset ID: §f" + ItemPresetManager.getShortId(presetId),
                "§cWARNING: This action is permanent!"
        );
        ItemMeta meta = item.getItemMeta();
        // Use a hidden tag to store the UUID
        meta.getPersistentDataContainer().set(new NamespacedKey(ItemEditorPlugin.getPluginInstance(), "DELETE_PRESET_ID"),
                PersistentDataType.STRING, presetId.toString());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
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
}