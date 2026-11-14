package dev.solarsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.concurrent.ThreadLocalRandom;

public enum Planet {
    MERCURY("Mercury", NamedTextColor.GRAY, Material.IRON_INGOT,
            "Swift Orbit", "Speed I (Energy 1+), Speed II (Energy 4+).",
            "Solar Dash", "Rocket forward leaving a silver afterimage trail, damaging everything you pass through.", 22,
            "Quicksilver Blink", "Teleport forward in a flash. The arrival shockwave damages and slows nearby foes.", 35),
    VENUS("Venus", NamedTextColor.YELLOW, Material.BLAZE_POWDER,
            "Greenhouse Skin", "Fire Resistance (Energy 1+).",
            "Acid Rain", "Summon a sulfuric storm cloud that rains poison on everyone beneath it.", 30,
            "Runaway Greenhouse", "Unleash a blast of crushing heat that burns and launches everything around you.", 40),
    EARTH("Earth", NamedTextColor.GREEN, Material.GRASS_BLOCK,
            "Gaia's Grit", "Haste I (1+), Resistance I (3+), Resistance II (5).",
            "Earthfall", "Call a chunk of the planet down from the sky to slam into the ground ahead.", 32,
            "Gaia Barrier", "Orbiting rock shields give you absorption and resistance and smash back attackers.", 45),
    MARS("Mars", NamedTextColor.RED, Material.RED_SAND,
            "Red Fury", "Below half health: Strength I (1+), Strength II (4+).",
            "Olympus Meteor", "Hurl a blazing meteor that detonates on impact.", 25,
            "Warlord's Rage", "Enter a war trance: Strength, Speed, and your hits set foes ablaze.", 45),
    JUPITER("Jupiter", NamedTextColor.GOLD, Material.ORANGE_TERRACOTTA,
            "Gas Giant Mass", "No fall damage (1+), +2 hearts (4+).",
            "Great Red Storm", "Conjure the Great Red Spot overhead and rain lightning on the area.", 38,
            "Gravity Crush", "Create a gravity well that drags enemies in, then implodes.", 45),
    SATURN("Saturn", NamedTextColor.YELLOW, Material.HONEY_BLOCK,
            "Ringed Aegis", "Take 5% less damage per Energy (up to 25%).",
            "Ring Slash", "Release an expanding ring of debris that cuts and flings everything it passes.", 26,
            "Ring Prison", "Trap the enemy you're looking at inside spinning rings, immobilizing them.", 40),
    URANUS("Uranus", NamedTextColor.AQUA, Material.PACKED_ICE,
            "Sideways Spin", "Jump Boost I (1+), II (4+). Immune to freezing (1+).",
            "Ice Giant's Breath", "Exhale a cone of freezing ice shards that chills and slows.", 24,
            "Axial Flip", "Tilt the world: nearby enemies are flipped around and flung upward.", 40),
    NEPTUNE("Neptune", NamedTextColor.BLUE, Material.LAPIS_BLOCK,
            "Deep Blue", "Water Breathing + Dolphin's Grace (1+), Conduit Power (4+).",
            "Tidal Wave", "Send a wall of water crashing forward, sweeping enemies away.", 28,
            "Maelstrom", "Whip up a violent whirlpool that spins enemies in, then erupts in a geyser.", 42);

    public final String display;
    public final NamedTextColor color;
    public final Material icon;
    public final String passiveName, passiveDesc;
    public final String ab1Name, ab1Desc, ab2Name, ab2Desc;
    public final int ab1Cd, ab2Cd;

    Planet(String display, NamedTextColor color, Material icon,
           String passiveName, String passiveDesc,
           String ab1Name, String ab1Desc, int ab1Cd,
           String ab2Name, String ab2Desc, int ab2Cd) {
        this.display = display; this.color = color; this.icon = icon;
        this.passiveName = passiveName; this.passiveDesc = passiveDesc;
        this.ab1Name = ab1Name; this.ab1Desc = ab1Desc; this.ab1Cd = ab1Cd;
        this.ab2Name = ab2Name; this.ab2Desc = ab2Desc; this.ab2Cd = ab2Cd;
    }

    public Component component() { return Component.text(display, color); }
    public String abilityName(int slot) { return slot == 1 ? ab1Name : ab2Name; }
    public String abilityDesc(int slot) { return slot == 1 ? ab1Desc : ab2Desc; }
    public int baseCooldown(int slot) { return slot == 1 ? ab1Cd : ab2Cd; }

    public static Planet parse(String s) {
        for (Planet p : values()) if (p.name().equalsIgnoreCase(s)) return p;
        return null;
    }

    public static Planet random(Planet exclude) {
        Planet[] all = values();
        Planet p;
        do { p = all[ThreadLocalRandom.current().nextInt(all.length)]; } while (p == exclude);
        return p;
    }
}
