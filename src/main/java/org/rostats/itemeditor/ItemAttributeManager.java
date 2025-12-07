package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.rostats.ROStatsPlugin;
import org.bukkit.entity.Player; // NEW IMPORT
import org.bukkit.inventory.EquipmentSlot; // NEW IMPORT
import org.bukkit.inventory.PlayerInventory; // NEW IMPORT
import org.rostats.data.PlayerData; // NEW IMPORT

import java.util.ArrayList;
import java.util.EnumMap; // NEW IMPORT
import java.util.List;
import java.util.Map; // NEW IMPORT
import java.util.Objects;

public class ItemAttributeManager {

    private final ROStatsPlugin plugin;
    private static final String MAIN_STATS_HEADER = "§f§l--- Main Stats ---";
    private static final String CUSTOM_LORE_HEADER = "§f§l--- Custom Lore ---";

    public ItemAttributeManager(ROStatsPlugin plugin) {
        this.plugin = plugin;
        // Initialize NamespacedKeys once
        for (ItemAttribute attribute : ItemAttribute.values()) {
            attribute.initialize(plugin);
        }
    }

    // --- Data Management ---

    public double getAttribute(ItemStack item, ItemAttribute attribute) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(attribute.getNamespacedKey(), PersistentDataType.DOUBLE, 0.0);
    }

    public void setAttribute(ItemStack item, ItemAttribute attribute, double value) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (value == 0.0) {
            pdc.remove(attribute.getNamespacedKey());
        } else {
            pdc.set(attribute.getNamespacedKey(), PersistentDataType.DOUBLE, value);
        }
        item.setItemMeta(meta);
        updateLore(item); // Update lore automatically upon setting attribute
    }

    // --- Core Lore Management (Contains updateLore) ---

    public void updateLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        List<String> currentLore = meta.hasLore() ? Objects.requireNonNull(meta.getLore()) : new ArrayList<>();

        List<String> newLore = new ArrayList<>();

        // 1. Separate existing lore to preserve CUSTOM LORE
        List<String> customLore = new ArrayList<>();
        boolean inCustomLore = false;

        for (String line : currentLore) {
            String stripped = line.replaceAll("§[0-9a-fk-or]", "");
            if (stripped.equals("--- Custom Lore ---")) {
                inCustomLore = true;
                continue;
            }
            if (stripped.equals("--- Main Stats ---")) { // Reset flag if we hit the start of the Main Stats (shouldn't happen if they are at the top)
                inCustomLore = false;
                continue;
            }

            if (inCustomLore) {
                customLore.add(line);
            }
        }

        // 2. Generate new Main Stats Lore
        List<String> mainStats = new ArrayList<>();
        for (ItemAttribute attribute : ItemAttribute.values()) {
            double value = getAttribute(item, attribute);
            if (value != 0.0) {
                String formattedValue = String.format(attribute.getFormat(), value);
                String line = attribute.getDisplayName() + ": §f" + formattedValue;
                mainStats.add(line);
            }
        }

        // 3. Combine Lore: Main Stats (Auto-generated) + Custom Lore (Preserved)
        if (!mainStats.isEmpty()) {
            newLore.add(MAIN_STATS_HEADER);
            newLore.addAll(mainStats);
        }

        if (!customLore.isEmpty()) {
            if (!newLore.isEmpty()) newLore.add(" "); // Add separator if Main Stats exist
            newLore.add(CUSTOM_LORE_HEADER);
            newLore.addAll(customLore);
        }

        // Apply new lore
        meta.lore(newLore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
    }

    // --- NEW: Accumulate item bonuses from equipped gear and apply to PlayerData ---
    public void updateAllEquippedAttributes(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId()); // Get PlayerData
        PlayerInventory inv = player.getInventory();

        // 1. Define slots to check (Hand, Off-hand, and Armor slots)
        EquipmentSlot[] slots = {
                EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        // 2. Initialize map to accumulate total bonuses
        Map<ItemAttribute, Double> accumulatedBonuses = new EnumMap<>(ItemAttribute.class);
        for (ItemAttribute attribute : ItemAttribute.values()) {
            accumulatedBonuses.put(attribute, 0.0);
        }

        // 3. Iterate through equipped items and accumulate bonuses
        for (EquipmentSlot slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                for (ItemAttribute attribute : ItemAttribute.values()) {
                    double value = getAttribute(item, attribute); // Uses PDC lookup
                    double currentTotal = accumulatedBonuses.get(attribute);
                    accumulatedBonuses.put(attribute, currentTotal + value);
                }
            }
        }

        // 4. Apply total accumulated bonuses to PlayerData
        applyAccumulatedBonuses(data, accumulatedBonuses);
    }

    // Helper to apply all 44 fields to PlayerData
    private void applyAccumulatedBonuses(PlayerData data, Map<ItemAttribute, Double> accumulated) {
        // Core Flat Bonuses
        data.setWeaponPAtk(accumulated.get(ItemAttribute.WEAPON_PATK));
        data.setWeaponMAtk(accumulated.get(ItemAttribute.WEAPON_MATK));
        data.setHitBonusFlat(accumulated.get(ItemAttribute.HIT_BONUS_FLAT));
        data.setFleeBonusFlat(accumulated.get(ItemAttribute.FLEE_BONUS_FLAT));
        data.setBaseMSPD(accumulated.get(ItemAttribute.BASE_MSPD));
        data.setPAtkBonusFlat(accumulated.get(ItemAttribute.PATK_FLAT));
        data.setMAtkBonusFlat(accumulated.get(ItemAttribute.MATK_FLAT));
        data.setPDmgBonusFlat(accumulated.get(ItemAttribute.PDMG_FLAT));
        data.setMDmgBonusFlat(accumulated.get(ItemAttribute.MDMG_FLAT));
        data.setTrueDamageFlat(accumulated.get(ItemAttribute.TRUE_DMG));

        // Percent Bonuses
        data.setMaxHPPercent(accumulated.get(ItemAttribute.MAXHP_PERCENT));
        data.setMaxSPPercent(accumulated.get(ItemAttribute.MAXSP_PERCENT));
        data.setPDmgBonusPercent(accumulated.get(ItemAttribute.PDMG_PERCENT));
        data.setMDmgBonusPercent(accumulated.get(ItemAttribute.MDMG_PERCENT));
        data.setFinalDmgPercent(accumulated.get(ItemAttribute.FINAL_DMG_PERCENT));
        data.setFinalDmgResPercent(accumulated.get(ItemAttribute.FINAL_DMG_RES_PERCENT));
        data.setCritDmgPercent(accumulated.get(ItemAttribute.CRIT_DMG_PERCENT));
        data.setFinalPDmgPercent(accumulated.get(ItemAttribute.FINAL_PDMG_PERCENT));
        data.setFinalMDmgPercent(accumulated.get(ItemAttribute.FINAL_MDMG_PERCENT));
        data.setMeleePDmgPercent(accumulated.get(ItemAttribute.MELEE_PDMG_PERCENT));
        data.setRangePDmgPercent(accumulated.get(ItemAttribute.RANGE_PDMG_PERCENT));
        data.setPveDmgBonusPercent(accumulated.get(ItemAttribute.PVE_DMG_BONUS_PERCENT));
        data.setPvpDmgBonusPercent(accumulated.get(ItemAttribute.PVP_DMG_BONUS_PERCENT));

        // Defense & Resistances
        data.setPDmgReductionPercent(accumulated.get(ItemAttribute.PDR_PERCENT));
        data.setMDmgReductionPercent(accumulated.get(ItemAttribute.MDR_PERCENT));
        data.setCritRes(accumulated.get(ItemAttribute.CRIT_RES));
        data.setCritDmgResPercent(accumulated.get(ItemAttribute.CRIT_DMG_RES_PERCENT));
        data.setPveDmgReductionPercent(accumulated.get(ItemAttribute.PVE_DMG_REDUCTION_PERCENT));
        data.setPvpDmgReductionPercent(accumulated.get(ItemAttribute.PVP_DMG_REDUCTION_PERCENT));

        // Penetration & Ignore Def
        // Note: P_PEN_FLAT/M_PEN_FLAT/P_PEN_PERCENT/M_PEN_PERCENT are in ItemAttribute but not in PlayerData.
        // Assuming they are unused or should be mapped to the Ignore Def fields if needed.
        data.setIgnorePDefPercent(accumulated.get(ItemAttribute.IGNORE_PDEF_PERCENT));
        data.setIgnoreMDefPercent(accumulated.get(ItemAttribute.IGNORE_MDEF_PERCENT));
        data.setIgnorePDefFlat(accumulated.get(ItemAttribute.IGNORE_PDEF_FLAT));
        data.setIgnoreMDefFlat(accumulated.get(ItemAttribute.IGNORE_MDEF_FLAT));

        // Speed & Casting
        data.setASpdPercent(accumulated.get(ItemAttribute.ASPD_PERCENT));
        data.setMSpdPercent(accumulated.get(ItemAttribute.MSPD_PERCENT));
        data.setVarCTPercent(accumulated.get(ItemAttribute.VAR_CT_PERCENT));
        data.setFixedCTPercent(accumulated.get(ItemAttribute.FIXED_CT_PERCENT));
        data.setVarCTFlat(accumulated.get(ItemAttribute.VAR_CT_FLAT));
        data.setFixedCTFlat(accumulated.get(ItemAttribute.FIXED_CT_FLAT));

        // Recovery & Lifesteal
        data.setHealingEffectPercent(accumulated.get(ItemAttribute.HEALING_EFFECT_PERCENT));
        data.setHealingReceivedPercent(accumulated.get(ItemAttribute.HEALING_RECEIVED_PERCENT));
        data.setLifestealPPercent(accumulated.get(ItemAttribute.LIFESTEAL_P_PERCENT));
        data.setLifestealMPercent(accumulated.get(ItemAttribute.LIFESTEAL_M_PERCENT));

        // Shield
        data.setShieldValueFlat(accumulated.get(ItemAttribute.SHIELD_VALUE_FLAT));
        data.setShieldRatePercent(accumulated.get(ItemAttribute.SHIELD_RATE_PERCENT));
    }
}