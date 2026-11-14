package dev.solarsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;

public final class SolarSMP extends JavaPlugin implements Listener, TabExecutor {

    public static NamespacedKey KEY_PLANET, KEY_ENERGY, KEY_ITEM;
    public static final int MAX_ENERGY = 5;
    /** Players currently under Warlord's Rage -> expiry millis. */
    public static final Map<UUID, Long> RAGING = new HashMap<>();

    public SolarConfig cfg;
    private ConfigGui configGui;
    private final Map<UUID, long[]> cooldowns = new HashMap<>();

    /** 0 energy = 25% power, 5 energy = 100% power. */
    public static double power(int energy) { return 0.25 + 0.15 * Math.max(0, Math.min(MAX_ENERGY, energy)); }

    // ───────────────────────── lifecycle ─────────────────────────

    @Override public void onEnable() {
        KEY_PLANET = new NamespacedKey(this, "planet");
        KEY_ENERGY = new NamespacedKey(this, "energy");
        KEY_ITEM = new NamespacedKey(this, "item");
        Fx.init(this);
        cfg = new SolarConfig(this);
        SolarItems.registerRecipes(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new RecipeGui(), this);
        configGui = new ConfigGui(this);
        Bukkit.getPluginManager().registerEvents(configGui, this);
        for (String c : List.of("solarsmp", "setsolar", "setenergy")) {
            var pc = getCommand(c);
            if (pc != null) { pc.setExecutor(this); pc.setTabCompleter(this); }
        }
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) applyPassive(p);
        }, 20, 20);
    }

    @Override public void onDisable() {
        Fx.clearAll();
        SolarItems.unregisterRecipes(this);
    }

    // ───────────────────────── player data ─────────────────────────

    public static Planet getPlanet(Player p) {
        String s = p.getPersistentDataContainer().get(KEY_PLANET, PersistentDataType.STRING);
        return s == null ? null : Planet.parse(s);
    }

    public static void setPlanet(Player p, Planet pl) {
        p.getPersistentDataContainer().set(KEY_PLANET, PersistentDataType.STRING, pl.name());
    }

    public static int getEnergy(Player p) {
        return p.getPersistentDataContainer().getOrDefault(KEY_ENERGY, PersistentDataType.INTEGER, 0);
    }

    public static void setEnergy(Player p, int e) {
        p.getPersistentDataContainer().set(KEY_ENERGY, PersistentDataType.INTEGER, Math.max(0, Math.min(MAX_ENERGY, e)));
    }

    private static void msg(CommandSender s, String text, NamedTextColor c) { s.sendMessage(Component.text(text, c)); }

    private static String bar(int energy) {
        return "\u2588".repeat(energy) + "\u2591".repeat(MAX_ENERGY - energy);
    }

    // ───────────────────────── events ─────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (getPlanet(p) == null) {
            Planet pl = Planet.random(null);
            setPlanet(p, pl);
            if (!pdc.has(KEY_ENERGY, PersistentDataType.INTEGER)) setEnergy(p, 0);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                p.showTitle(Title.title(Component.text("You are " + pl.display, pl.color),
                        Component.text("Use /solarsmp info to see your powers", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }, 20);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        int en = getEnergy(p);
        if (en > 0 && cfg.loseEnergyOnDeath()) {
            setEnergy(p, en - 1);
            msg(p, "Your energy flickers... (" + (en - 1) + "/" + MAX_ENERGY + ")", NamedTextColor.RED);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        String tag = SolarItems.tag(it);
        if (tag == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (getPlanet(p) == null) return;

        if (tag.equals(SolarItems.CORE)) {
            int en = getEnergy(p);
            if (en >= MAX_ENERGY) { msg(p, "Your energy is already full.", NamedTextColor.RED); return; }
            it.setAmount(it.getAmount() - 1);
            setEnergy(p, en + 1);
            p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 40, .4, .8, .4, 0.1);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.3f);
            msg(p, "Energy: " + bar(en + 1) + " (" + (en + 1) + "/" + MAX_ENERGY + ")", NamedTextColor.GOLD);
        } else if (tag.equals(SolarItems.REROLLER)) {
            Planet next = Planet.random(getPlanet(p));
            it.setAmount(it.getAmount() - 1);
            setPlanet(p, next);
            p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 80, .5, 1, .5, 0.5);
            p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.6f);
            p.showTitle(Title.title(Component.text("You are now " + next.display, next.color),
                    Component.text("Your orbit has shifted", NamedTextColor.GRAY)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Planet pl = getPlanet(p);
        if (pl == null) return;
        int en = getEnergy(p);
        if (pl == Planet.JUPITER && en >= 1 && e.getCause() == EntityDamageEvent.DamageCause.FALL) e.setCancelled(true);
        else if (pl == Planet.URANUS && en >= 1 && e.getCause() == EntityDamageEvent.DamageCause.FREEZE) e.setCancelled(true);
        else if (pl == Planet.SATURN && en > 0) e.setDamage(e.getDamage() * (1 - 0.05 * en));
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            Long until = RAGING.get(p.getUniqueId());
            if (until != null) {
                if (until > System.currentTimeMillis()) e.getEntity().setFireTicks(80);
                else RAGING.remove(p.getUniqueId());
            }
        }
    }

    // ───────────────────────── passives ─────────────────────────

    private static void eff(Player p, PotionEffectType t, int amp) {
        p.addPotionEffect(new PotionEffect(t, 60, amp, true, false, false));
    }

    private void applyPassive(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        Planet pl = getPlanet(p);
        if (pl == null) return;
        int en = getEnergy(p);
        if (en < 1) return;
        switch (pl) {
            case MERCURY -> eff(p, PotionEffectType.SPEED, en >= 4 ? 1 : 0);
            case VENUS -> eff(p, PotionEffectType.FIRE_RESISTANCE, 0);
            case EARTH -> {
                eff(p, PotionEffectType.HASTE, 0);
                if (en >= 5) eff(p, PotionEffectType.RESISTANCE, 1);
                else if (en >= 3) eff(p, PotionEffectType.RESISTANCE, 0);
            }
            case MARS -> {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && p.getHealth() <= max.getValue() / 2) eff(p, PotionEffectType.STRENGTH, en >= 4 ? 1 : 0);
            }
            case JUPITER -> { if (en >= 4) eff(p, PotionEffectType.HEALTH_BOOST, 0); }
            case URANUS -> eff(p, PotionEffectType.JUMP_BOOST, en >= 4 ? 1 : 0);
            case NEPTUNE -> {
                eff(p, PotionEffectType.WATER_BREATHING, 0);
                eff(p, PotionEffectType.DOLPHINS_GRACE, 0);
                if (en >= 4) eff(p, PotionEffectType.CONDUIT_POWER, 0);
            }
            default -> { }
        }
    }

    // ───────────────────────── commands ─────────────────────────

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        switch (cmd.getName().toLowerCase()) {
            case "setsolar" -> {
                if (!s.hasPermission("solarsmp.admin")) { msg(s, "No permission.", NamedTextColor.RED); return true; }
                if (a.length < 2) { msg(s, "Usage: /setsolar <player> <planet>", NamedTextColor.RED); return true; }
                Player t = Bukkit.getPlayerExact(a[0]);
                Planet pl = Planet.parse(a[1]);
                if (t == null) { msg(s, "Player not online.", NamedTextColor.RED); return true; }
                if (pl == null) { msg(s, "Unknown planet. Use: " + names(), NamedTextColor.RED); return true; }
                setPlanet(t, pl);
                msg(s, t.getName() + " is now " + pl.display + ".", NamedTextColor.GREEN);
                if (!s.equals(t)) msg(t, "An admin set your planet to " + pl.display + ".", pl.color);
            }
            case "setenergy" -> {
                if (!s.hasPermission("solarsmp.admin")) { msg(s, "No permission.", NamedTextColor.RED); return true; }
                if (a.length < 2) { msg(s, "Usage: /setenergy <player> <0-5>", NamedTextColor.RED); return true; }
                Player t = Bukkit.getPlayerExact(a[0]);
                if (t == null) { msg(s, "Player not online.", NamedTextColor.RED); return true; }
                int v;
                try { v = Integer.parseInt(a[1]); } catch (NumberFormatException ex) { msg(s, "Energy must be 0-5.", NamedTextColor.RED); return true; }
                if (v < 0 || v > MAX_ENERGY) { msg(s, "Energy must be 0-5.", NamedTextColor.RED); return true; }
                setEnergy(t, v);
                msg(s, t.getName() + "'s energy set to " + v + ".", NamedTextColor.GREEN);
                if (!s.equals(t)) msg(t, "Your energy is now " + v + "/" + MAX_ENERGY + ".", NamedTextColor.GOLD);
            }
            case "solarsmp" -> solarCommand(s, a);
        }
        return true;
    }

    private void solarCommand(CommandSender s, String[] a) {
        if (a.length == 0) { msg(s, "Usage: /solarsmp <ability <1|2> | recipes | info>", NamedTextColor.YELLOW); return; }
        switch (a[0].toLowerCase()) {
            case "ability" -> {
                if (!(s instanceof Player p)) { msg(s, "Players only.", NamedTextColor.RED); return; }
                if (a.length < 2 || !(a[1].equals("1") || a[1].equals("2"))) { msg(p, "Usage: /solarsmp ability <1|2>", NamedTextColor.RED); return; }
                int slot = Integer.parseInt(a[1]);
                Planet pl = getPlanet(p);
                if (pl == null) { msg(p, "You have no planet yet.", NamedTextColor.RED); return; }
                int en = getEnergy(p);
                long[] cd = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new long[3]);
                long now = System.currentTimeMillis();
                if (cd[slot] > now) {
                    msg(p, pl.abilityName(slot) + " is on cooldown: " + String.format("%.1f", (cd[slot] - now) / 1000.0) + "s", NamedTextColor.RED);
                    return;
                }
                double dm = cfg.damage(pl, slot) * cfg.globalDamage();
                if (Abilities.cast(p, pl, slot, en, dm)) {
                    double mult = 1.5 - 0.1 * en;
                    cd[slot] = now + (long) (cfg.cooldown(pl, slot) * cfg.globalCooldown() * mult * 1000);
                    p.sendActionBar(Component.text(pl.abilityName(slot), pl.color)
                            .append(Component.text("  [Energy " + en + "/" + MAX_ENERGY + " | " + (int) (power(en) * 100) + "% power]", NamedTextColor.GRAY)));
                }
            }
            case "recipes" -> {
                if (!(s instanceof Player p)) { msg(s, "Players only.", NamedTextColor.RED); return; }
                RecipeGui.open(p);
            }
            case "info" -> {
                if (!(s instanceof Player p)) { msg(s, "Players only.", NamedTextColor.RED); return; }
                Planet pl = getPlanet(p);
                if (pl == null) { msg(p, "You have no planet yet.", NamedTextColor.RED); return; }
                int en = getEnergy(p);
                p.sendMessage(Component.text("=== ", NamedTextColor.DARK_GRAY).append(pl.component())
                        .append(Component.text(" ===", NamedTextColor.DARK_GRAY)));
                msg(p, "Energy: " + bar(en) + " " + en + "/" + MAX_ENERGY + "  (" + (int) (power(en) * 100) + "% power)", NamedTextColor.GOLD);
                msg(p, "Passive - " + pl.passiveName + ": " + pl.passiveDesc, NamedTextColor.AQUA);
                for (int i = 1; i <= 2; i++)
                    msg(p, "Ability " + i + " - " + pl.abilityName(i) + ": " + pl.abilityDesc(i) + " (" + pl.baseCooldown(i) + "s base cooldown)", NamedTextColor.WHITE);
            }
            case "config" -> {
                if (!(s instanceof Player p)) { msg(s, "Players only.", NamedTextColor.RED); return; }
                if (!p.hasPermission("solarsmp.admin")) { msg(p, "No permission.", NamedTextColor.RED); return; }
                configGui.open(p);
            }
            case "give" -> {
                if (!s.hasPermission("solarsmp.admin")) { msg(s, "No permission.", NamedTextColor.RED); return; }
                if (a.length < 3) { msg(s, "Usage: /solarsmp give <player> <core|reroller> [amount]", NamedTextColor.RED); return; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { msg(s, "Player not online.", NamedTextColor.RED); return; }
                ItemStack item = a[2].equalsIgnoreCase("core") ? SolarItems.energyCore()
                        : a[2].equalsIgnoreCase("reroller") ? SolarItems.reroller() : null;
                if (item == null) { msg(s, "Item must be core or reroller.", NamedTextColor.RED); return; }
                int amt = 1;
                if (a.length > 3) try { amt = Math.max(1, Math.min(64, Integer.parseInt(a[3]))); } catch (NumberFormatException ignored) { }
                item.setAmount(amt);
                t.getInventory().addItem(item).values().forEach(r -> t.getWorld().dropItemNaturally(t.getLocation(), r));
                msg(s, "Gave " + amt + "x " + a[2] + " to " + t.getName() + ".", NamedTextColor.GREEN);
            }
            default -> msg(s, "Usage: /solarsmp <ability <1|2> | recipes | info>", NamedTextColor.YELLOW);
        }
    }

    private static String names() {
        return String.join(", ", Arrays.stream(Planet.values()).map(p -> p.display).toList());
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        String last = a[a.length - 1].toLowerCase();
        switch (cmd.getName().toLowerCase()) {
            case "setsolar" -> {
                if (a.length == 1) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                else if (a.length == 2) for (Planet p : Planet.values()) out.add(p.display);
            }
            case "setenergy" -> {
                if (a.length == 1) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                else if (a.length == 2) out.addAll(List.of("0", "1", "2", "3", "4", "5"));
            }
            case "solarsmp" -> {
                if (a.length == 1) { out.addAll(List.of("ability", "recipes", "info")); if (s.hasPermission("solarsmp.admin")) { out.add("give"); out.add("config"); } }
                else if (a.length == 2 && a[0].equalsIgnoreCase("ability")) out.addAll(List.of("1", "2"));
                else if (a[0].equalsIgnoreCase("give") && s.hasPermission("solarsmp.admin")) {
                    if (a.length == 2) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                    else if (a.length == 3) out.addAll(List.of("core", "reroller"));
                }
            }
        }
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }
}
