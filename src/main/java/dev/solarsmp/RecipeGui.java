package dev.solarsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Read-only recipe viewer. */
public final class RecipeGui implements Listener {

    private static final class Holder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    public static void open(Player p) {
        Holder h = new Holder();
        Inventory inv = Bukkit.createInventory(h, 54, Component.text("Solar SMP - Recipes", NamedTextColor.GOLD));
        h.inv = inv;

        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        drawRecipe(inv, 0, SolarItems.CORE_GRID, SolarItems.energyCore());
        drawRecipe(inv, 3, SolarItems.REROLL_GRID, SolarItems.reroller());

        inv.setItem(7, info(Material.PAPER, "Energy",
                "You spawn with 0 Energy (max 5).",
                "Lower Energy = weaker abilities, longer cooldowns",
                "and fewer passives. Right-click an Energy Core",
                "to gain +1. You lose 1 Energy when you die."));
        inv.setItem(43, info(Material.PAPER, "Planet Reroller",
                "Right-click to swap to a new random planet.",
                "Your Energy is kept."));
        p.openInventory(inv);
    }

    /** rowStart: 0 or 3. Grid occupies columns 1-3, arrow col 4, result col 5. */
    private static void drawRecipe(Inventory inv, int rowStart, Material[] grid, ItemStack result) {
        for (int i = 0; i < 9; i++) {
            int row = rowStart + i / 3, col = 1 + i % 3;
            inv.setItem(row * 9 + col, new ItemStack(grid[i]));
        }
        inv.setItem((rowStart + 1) * 9 + 4, named(Material.ARROW, Component.text("Crafts", NamedTextColor.YELLOW)));
        inv.setItem((rowStart + 1) * 9 + 5, result);
    }

    private static ItemStack named(Material m, Component name) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> meta.displayName(name.decoration(TextDecoration.ITALIC, false)));
        return it;
    }

    private static ItemStack info(Material m, String title, String... lines) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> {
            meta.displayName(Component.text(title, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(java.util.Arrays.stream(lines)
                    .map(s -> (Component) Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .toList());
        });
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
