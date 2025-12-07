package org.rostats.itemeditor;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.rostats.ROStatsPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemEditorPlugin extends JavaPlugin {

    private ROStatsPlugin corePlugin;
    private ItemAttributeManager attributeManager;
    private ItemPresetManager presetManager;

    // NEW: Map to track players currently using the chat lore editor
    private final Map<UUID, StringBuilder> loreEditingPlayers = new HashMap<>();

    @Override
    public void onEnable() {
        // 1. Check for Core Plugin dependency
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ThaiRoStats-Core");
        if (plugin instanceof ROStatsPlugin roStatsPlugin) {
            this.corePlugin = roStatsPlugin;
            getLogger().info("Found ThaiRoStats-Core, integrating item editor.");
        } else {
            getLogger().severe("ThaiRoStats-Core not found! Disabling Item Editor.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Initialize Managers
        this.attributeManager = new ItemAttributeManager(corePlugin);
        this.presetManager = new ItemPresetManager(this);

        // 3. Register Command
        PluginCommand command = getCommand("roitemedit");
        if (command != null) {
            command.setExecutor(new ItemEditorCommand(this, attributeManager));
        }

        // 4. Register Listener
        getServer().getPluginManager().registerEvents(new GUIListener(this, attributeManager, presetManager), this);

        getLogger().info("✅ ThaiRoStats Item Editor Enabled!");
    }

    @Override
    public void onDisable() {
        // Save presets on disable
        if (presetManager != null) {
            presetManager.savePresets();
        }
        getLogger().info("❌ ThaiRoStats Item Editor Disabled!");
    }

    public ROStatsPlugin getCorePlugin() {
        return corePlugin;
    }

    public ItemAttributeManager getAttributeManager() {
        return attributeManager;
    }

    public ItemPresetManager getPresetManager() {
        return presetManager;
    }

    // NEW: Lore Editor Management Getters
    public Map<UUID, StringBuilder> getLoreEditingPlayers() {
        return loreEditingPlayers;
    }
    public static Plugin getPluginInstance() {
        return getPlugin(ItemEditorPlugin.class);
    }
}