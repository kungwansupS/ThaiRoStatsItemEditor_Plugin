package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects; // <-- แก้ไข: เพิ่ม import

public class PresetStorageGUI {

    public static final String GUI_TITLE = "§0§lItem Preset Storage";
    private static final int INVENTORY_SIZE = 54;
    private final ItemEditorPlugin plugin;
    private final ItemPresetManager presetManager;

    public PresetStorageGUI(ItemEditorPlugin plugin, ItemPresetManager presetManager) {
        this.plugin = plugin;
        this.presetManager = presetManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, INVENTORY_SIZE, Component.text(GUI_TITLE));

        Map<UUID, ItemStack> presets = presetManager.getAllPresets();
        int slot = 0;

        for (Map.Entry<UUID, ItemStack> entry : presets.entrySet()) {
            if (slot >= INVENTORY_SIZE) break;

            UUID presetId = entry.getKey();
            ItemStack originalItem = entry.getValue();

            // 1. Preset Item (Slot: 0-44)
            inv.setItem(slot, createPresetItem(originalItem, presetId));
            slot++;
        }

        // Control Buttons (Bottom Row)
        ItemStack backBtn = createItem(Material.ARROW, "§cBack to Editor", "§7Return to Attribute Editor.");
        inv.setItem(45, backBtn);

        ItemStack saveBtn = createItem(Material.BOOK, "§aSave Item to Preset", "§7Saves the currently held item (Attribute Editor's Slot 0) to a new preset slot.");
        inv.setItem(49, saveBtn);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 46; i <= 53; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }


        player.openInventory(inv);
    }

    public ItemStack createPresetItem(ItemStack item, UUID presetId) {
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();

        // FIX: Ensure List<Component> is used correctly, handling nulls robustly
        List<Component> currentLore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            currentLore.addAll(Objects.requireNonNull(meta.lore()));
        }

        currentLore.add(Component.text(" "));
        currentLore.add(Component.text("§6§l--- Preset Actions ---"));
        currentLore.add(Component.text("§7ID: §f" + ItemPresetManager.getShortId(presetId)));
        currentLore.add(Component.text("§aLeft-Click: §7Copy to Inventory"));
        currentLore.add(Component.text("§cRight-Click: §7Delete Preset"));

        meta.lore(currentLore);
        displayItem.setItemMeta(meta);
        return displayItem;
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

}