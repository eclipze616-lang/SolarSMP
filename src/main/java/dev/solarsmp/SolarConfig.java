package dev.solarsmp;

import org.bukkit.configuration.file.FileConfiguration;

/** Thin wrapper around config.yml. Edited live through the /solarsmp config GUI. */
public final class SolarConfig {
    private final SolarSMP plugin;

    public SolarConfig(SolarSMP plugin) {
        this.plugin = plugin;
        FileConfiguration c = plugin.getConfig();
        for (Planet p : Planet.values())
            for (int s = 1; s <= 2; s++) {
                c.addDefault(key(p, s, "cooldown"), (double) p.baseCooldown(s));
                c.addDefault(key(p, s, "damage"), 1.0);
            }
        c.addDefault("global.cooldown-multiplier", 1.0);
        c.addDefault("global.damage-multiplier", 1.0);
        c.addDefault("global.lose-energy-on-death", true);
        c.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private static String key(Planet p, int slot, String field) {
        return "abilities." + p.name() + "." + slot + "." + field;
    }

    private FileConfiguration c() { return plugin.getConfig(); }

    public double cooldown(Planet p, int slot) { return c().getDouble(key(p, slot, "cooldown")); }
    public double damage(Planet p, int slot) { return c().getDouble(key(p, slot, "damage")); }
    public void setCooldown(Planet p, int slot, double v) { c().set(key(p, slot, "cooldown"), v); plugin.saveConfig(); }
    public void setDamage(Planet p, int slot, double v) { c().set(key(p, slot, "damage"), v); plugin.saveConfig(); }

    public double globalCooldown() { return c().getDouble("global.cooldown-multiplier"); }
    public double globalDamage() { return c().getDouble("global.damage-multiplier"); }
    public boolean loseEnergyOnDeath() { return c().getBoolean("global.lose-energy-on-death"); }
    public void setGlobalCooldown(double v) { c().set("global.cooldown-multiplier", v); plugin.saveConfig(); }
    public void setGlobalDamage(double v) { c().set("global.damage-multiplier", v); plugin.saveConfig(); }
    public void setLoseEnergyOnDeath(boolean v) { c().set("global.lose-energy-on-death", v); plugin.saveConfig(); }

    public void resetAll() {
        for (Planet p : Planet.values())
            for (int s = 1; s <= 2; s++) {
                c().set(key(p, s, "cooldown"), (double) p.baseCooldown(s));
                c().set(key(p, s, "damage"), 1.0);
            }
        c().set("global.cooldown-multiplier", 1.0);
        c().set("global.damage-multiplier", 1.0);
        c().set("global.lose-energy-on-death", true);
        plugin.saveConfig();
    }
}
