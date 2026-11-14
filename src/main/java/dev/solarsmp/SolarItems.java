package dev.solarsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Custom items + their crafting recipes (single source of truth, also used by the recipe GUI). */
public final class SolarItems {
    private SolarItems() {}

    public static final String CORE = "energy_core";
    public static final String REROLLER = "reroller";

    // 3x3 grids, row by row. Edit these to rebalance the recipes.
    public static final Material[] CORE_GRID = {
            Material.GOLD_BLOCK, Material.DIAMOND, Material.GOLD_BLOCK,
            Material.DIAMOND, Material.NETHER_STAR, Material.DIAMOND,
            Material.GOLD_BLOCK, Material.DIAMOND, Material.GOLD_BLOCK};
    public static final Material[] REROLL_GRID = {
            Material.DIAMOND_BLOCK, Material.ENDER_PEARL, Material.DIAMOND_BLOCK,
            Material.ENDER_PEARL, Material.HEART_OF_THE_SEA, Material.ENDER_PEARL,
            Material.DIAMOND_BLOCK, Material.ENDER_PEARL, Material.DIAMOND_BLOCK};

    private static Component name(String s, NamedTextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    private static Component lore(String s) {
        return Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    public static ItemStack energyCore() {
        ItemStack it = new ItemStack(Material.NETHER_STAR);
        it.editMeta(m -> {
            m.displayName(name("Energy Core", NamedTextColor.GOLD));
            m.lore(List.of(lore("A fragment of the Sun's power."), lore("Right-click: +1 Energy (max 5).")));
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(SolarSMP.KEY_ITEM, PersistentDataType.STRING, CORE);
        });
        return it;
    }

    public static ItemStack reroller() {
        ItemStack it = new ItemStack(Material.RECOVERY_COMPASS);
        it.editMeta(m -> {
            m.displayName(name("Solar Planet Reroller", NamedTextColor.LIGHT_PURPLE));
            m.lore(List.of(lore("Realigns your orbit."), lore("Right-click: get a new random planet.")));
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(SolarSMP.KEY_ITEM, PersistentDataType.STRING, REROLLER);
        });
        return it;
    }

    public static String tag(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(SolarSMP.KEY_ITEM, PersistentDataType.STRING);
    }

    public static void registerRecipes(SolarSMP plugin) {
        add(plugin, "energy_core", energyCore(), CORE_GRID);
        add(plugin, "reroller", reroller(), REROLL_GRID);
    }

    public static void unregisterRecipes(SolarSMP plugin) {
        Bukkit.removeRecipe(new NamespacedKey(plugin, "energy_core"));
        Bukkit.removeRecipe(new NamespacedKey(plugin, "reroller"));
    }

    private static void add(SolarSMP plugin, String id, ItemStack result, Material[] grid) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        Bukkit.removeRecipe(key);
        ShapedRecipe r = new ShapedRecipe(key, result);
        r.shape("abc", "def", "ghi");
        for (int i = 0; i < 9; i++) r.setIngredient((char) ('a' + i), grid[i]);
        Bukkit.addRecipe(r);
    }
}
