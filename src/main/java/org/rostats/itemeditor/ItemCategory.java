package org.rostats.itemeditor;

import org.bukkit.Material;

public enum ItemCategory {
    CORE_BONUS("§cCore Bonuses (ATK, SPD)", Material.IRON_INGOT, 2),
    DAMAGE_BONUS("§aDamage Modifiers (%, Flat)", Material.DIAMOND_SWORD, 3),
    DEFENSE_RES("§9Defense & Resistances", Material.IRON_CHESTPLATE, 4),
    PENETRATION("§6Penetration & Ignore DEF", Material.ANVIL, 5),
    CAST_SPEED("§dCast & Speed", Material.CLOCK, 6),
    SPECIAL_UTIL("§eSpecial & Utility (HP/SP, LS)", Material.NETHER_STAR, 7);

    private final String displayName;
    private final Material material;
    private final int slot;

    ItemCategory(String displayName, Material material, int slot) {
        this.displayName = displayName;
        this.material = material;
        this.slot = slot;
    }

    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getSlot() { return slot; }
}