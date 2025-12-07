package org.rostats.itemeditor;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.rostats.ROStatsPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemPresetManager {

    private final ItemEditorPlugin plugin;
    private final File dataFile;
    private final YamlConfiguration config;
    private final Map<UUID, ItemStack> presets = new HashMap<>(); // <Preset UUID, ItemStack>

    public ItemPresetManager(ItemEditorPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "item_presets.yml");

        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create item_presets.yml!");
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(dataFile);
        loadPresets();
    }

    private void loadPresets() {
        presets.clear();
        if (config.isConfigurationSection("presets")) {
            for (String key : config.getConfigurationSection("presets").getKeys(false)) {
                ItemStack item = config.getItemStack("presets." + key);
                if (item != null) {
                    try {
                        presets.put(UUID.fromString(key), item);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID format for preset key: " + key);
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded " + presets.size() + " item presets.");
    }

    public void savePresets() {
        // Clear previous section to prevent old/deleted items from reappearing
        config.set("presets", null);

        for (Map.Entry<UUID, ItemStack> entry : presets.entrySet()) {
            config.set("presets." + entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save item_presets.yml!");
            e.printStackTrace();
        }
    }

    // Public API
    public Map<UUID, ItemStack> getAllPresets() {
        return new HashMap<>(presets);
    }

    public void savePreset(ItemStack item, UUID presetId) {
        presets.put(presetId, item.clone());
        savePresets();
    }

    public void deletePreset(UUID presetId) {
        presets.remove(presetId);
        savePresets();
    }

    // Generates a unique short ID for display (e.g., first 8 chars of UUID)
    public static String getShortId(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }
}