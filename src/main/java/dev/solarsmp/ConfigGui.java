package dev.solarsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin GUI.  Columns 0-7 = the 8 planets.
 *  row 0: planet icon | row 1: ability 1 cooldown | row 2: ability 1 damage
 *  row 3: ability 2 cooldown | row 4: ability 2 damage | row 5: global settings
 * Left-click = increase, right-click = decrease, shift = bigger step.
 */
public final class ConfigGui implements Listener {
    private final SolarSMP plugin;

    public ConfigGui(SolarSMP plugin) { this.plugin = plugin; }

    private static final class Holder implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    private static final int G_CD = 45, G_DMG = 46, G_DEATH = 47, G_RESET = 53;

    public void open(Player p) {
        Holder h = new Holder();
        h.inv = Bukkit.createInventory(h, 54, Component.text("Solar SMP - Config", NamedTextColor.DARK_RED));
        render(h.inv);
        p.openInventory(h.inv);
    }

    private static Component t(String s, NamedTextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material m, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> { meta.displayName(name); meta.lore(lore); });
        return it;
    }

    private static List<Component> lines(String... s) {
        List<Component> l = new ArrayList<>();
        for (String x : s) l.add(t(x, NamedTextColor.GRAY));
        return l;
    }

    private static String fmt(double d) { return String.format("%.1f", d); }

    private void render(Inventory inv) {
        SolarConfig c = plugin.cfg;
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        Planet[] ps = Planet.values();
        for (int col = 0; col < ps.length; col++) {
            Planet p = ps[col];
            inv.setItem(col, item(p.icon, t(p.display, p.color), lines(p.passiveName + " (passive)")));
            for (int slot = 1; slot <= 2; slot++) {
                int baseRow = slot == 1 ? 1 : 3;
                inv.setItem(baseRow * 9 + col, item(Material.CLOCK,
                        t(p.abilityName(slot) + " - Cooldown", NamedTextColor.YELLOW),
                        List.of(t("Base cooldown: " + fmt(c.cooldown(p, slot)) + "s", NamedTextColor.WHITE),
                                t("(shrinks with player energy)", NamedTextColor.DARK_GRAY),
                                t("Left-click: +1s   Right-click: -1s", NamedTextColor.GRAY),
                                t("Shift: 5s steps", NamedTextColor.GRAY))));
                inv.setItem((baseRow + 1) * 9 + col, item(Material.IRON_SWORD,
                        t(p.abilityName(slot) + " - Damage", NamedTextColor.RED),
                        List.of(t("Damage multiplier: x" + fmt(c.damage(p, slot)), NamedTextColor.WHITE),
                                t("1.0 = default, 0 = no damage", NamedTextColor.DARK_GRAY),
                                t("Left-click: +0.1   Right-click: -0.1", NamedTextColor.GRAY),
                                t("Shift: 0.5 steps", NamedTextColor.GRAY))));
            }
        }
        inv.setItem(G_CD, item(Material.REPEATER, t("Global Cooldown Multiplier", NamedTextColor.GOLD),
                List.of(t("Current: x" + fmt(c.globalCooldown()), NamedTextColor.WHITE),
                        t("Applies to every ability", NamedTextColor.DARK_GRAY),
                        t("Left: +0.1   Right: -0.1   Shift: 0.5", NamedTextColor.GRAY))));
        inv.setItem(G_DMG, item(Material.DIAMOND_SWORD, t("Global Damage Multiplier", NamedTextColor.GOLD),
                List.of(t("Current: x" + fmt(c.globalDamage()), NamedTextColor.WHITE),
                        t("Applies to every ability", NamedTextColor.DARK_GRAY),
                        t("Left: +0.1   Right: -0.1   Shift: 0.5", NamedTextColor.GRAY))));
        boolean on = c.loseEnergyOnDeath();
        inv.setItem(G_DEATH, item(on ? Material.LIME_DYE : Material.GRAY_DYE,
                t("Lose 1 Energy On Death: " + (on ? "ON" : "OFF"), on ? NamedTextColor.GREEN : NamedTextColor.RED),
                lines("Click to toggle")));
        inv.setItem(G_RESET, item(Material.TNT, t("Reset Everything To Defaults", NamedTextColor.DARK_RED),
                lines("SHIFT + RIGHT-CLICK to confirm")));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.round(Math.max(lo, Math.min(hi, v)) * 10) / 10.0;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.hasPermission("solarsmp.admin")) { p.closeInventory(); return; }

        ClickType ct = e.getClick();
        boolean left = ct == ClickType.LEFT || ct == ClickType.SHIFT_LEFT;
        boolean right = ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT;
        if (!left && !right) return;
        boolean shift = ct.isShiftClick();
        int dir = left ? 1 : -1;
        int slot = e.getRawSlot();
        SolarConfig c = plugin.cfg;

        if (slot == G_RESET) {
            if (ct != ClickType.SHIFT_RIGHT) return;
            c.resetAll();
        } else if (slot == G_CD) {
            c.setGlobalCooldown(clamp(c.globalCooldown() + dir * (shift ? 0.5 : 0.1), 0.1, 5));
        } else if (slot == G_DMG) {
            c.setGlobalDamage(clamp(c.globalDamage() + dir * (shift ? 0.5 : 0.1), 0, 10));
        } else if (slot == G_DEATH) {
            c.setLoseEnergyOnDeath(!c.loseEnergyOnDeath());
        } else {
            int row = slot / 9, col = slot % 9;
            Planet[] ps = Planet.values();
            if (col >= ps.length || row < 1 || row > 4) return;
            Planet pl = ps[col];
            int ab = row <= 2 ? 1 : 2;
            boolean isCooldown = row == 1 || row == 3;
            if (isCooldown) c.setCooldown(pl, ab, Math.max(1, Math.min(600, c.cooldown(pl, ab) + dir * (shift ? 5 : 1))));
            else c.setDamage(pl, ab, clamp(c.damage(pl, ab) + dir * (shift ? 0.5 : 0.1), 0, 10));
        }
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, left ? 1.4f : 0.9f);
        render(e.getView().getTopInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
