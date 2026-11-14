package dev.solarsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Abilities {
    private Abilities() {}

    private static final Random R = new Random();
    private static final float PI = (float) Math.PI;

    /** @return true if the ability was actually cast (so the cooldown starts). */
    public static boolean cast(Player p, Planet pl, int slot, int energy, double dm) {
        double pw = SolarSMP.power(energy);
        return switch (pl) {
            case MERCURY -> slot == 1 ? dash(p, pw, dm) : blink(p, pw, dm);
            case VENUS -> slot == 1 ? acidRain(p, pw, dm) : greenhouse(p, pw, dm);
            case EARTH -> slot == 1 ? earthfall(p, pw, dm) : gaiaBarrier(p, pw, dm);
            case MARS -> slot == 1 ? meteor(p, pw, dm) : rage(p, pw, dm);
            case JUPITER -> slot == 1 ? redStorm(p, pw, dm) : gravityCrush(p, pw, dm);
            case SATURN -> slot == 1 ? ringSlash(p, pw, dm) : ringPrison(p, pw, dm);
            case URANUS -> slot == 1 ? iceBreath(p, pw, dm) : axialFlip(p, pw, dm);
            case NEPTUNE -> slot == 1 ? tidalWave(p, pw, dm) : maelstrom(p, pw, dm);
        };
    }

    // ───────────────────────── helpers ─────────────────────────

    private static List<LivingEntity> enemies(Player p, Location c, double r) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : c.getWorld().getNearbyLivingEntities(c, r)) {
            if (e.equals(p) || e instanceof ArmorStand || e.isDead()) continue;
            if (e instanceof Player o && o.getGameMode() == GameMode.SPECTATOR) continue;
            list.add(e);
        }
        return list;
    }

    private static Vector flat(Vector v) {
        Vector f = v.clone().setY(0);
        if (f.lengthSquared() < 1e-4) return new Vector(1, 0, 0);
        return f.normalize();
    }

    private static Vector away(Location from, Location to) {
        Vector v = to.toVector().subtract(from.toVector()).setY(0);
        if (v.lengthSquared() < 1e-4) return new Vector(R.nextDouble() - .5, 0, R.nextDouble() - .5).normalize();
        return v.normalize();
    }

    private static void hurt(Player p, LivingEntity e, double dmg, double dm) {
        e.setNoDamageTicks(0);
        e.damage(Math.max(0, dmg * dm), p);
    }

    private static void fling(Entity e, Vector dir, double h, double up) {
        e.setVelocity(dir.clone().multiply(h).setY(up));
    }

    private static void effect(LivingEntity e, PotionEffectType t, int ticks, int amp) {
        e.addPotionEffect(new PotionEffect(t, ticks, amp));
    }

    private static void dust(World w, Location l, int n, double spread, Color c, float size) {
        w.spawnParticle(Particle.DUST, l, n, spread, spread, spread, 0, new Particle.DustOptions(c, size));
    }

    private static Location ground(Location l, Location ref) {
        Location g = l.clone();
        int gy = g.getWorld().getHighestBlockYAt(g);
        if (Math.abs(gy + 1 - ref.getY()) <= 6) g.setY(gy + 1); else g.setY(ref.getY());
        return g;
    }

    // ───────────────────────── MERCURY ─────────────────────────

    private static boolean dash(Player p, double pw, double dm) {
        World w = p.getWorld();
        Vector dir = p.getEyeLocation().getDirection();
        p.setVelocity(dir.clone().setY(dir.getY() * 0.35).normalize().multiply(1.1 + 1.4 * pw));
        w.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.8f);
        Set<UUID> hit = new HashSet<>();
        Fx.repeat(1, 12, t -> {
            Location l = p.getLocation().add(0, 1, 0);
            w.spawnParticle(Particle.END_ROD, l, 6, .2, .3, .2, .02);
            w.spawnParticle(Particle.CRIT, l, 8, .3, .4, .3, .1);
            var d = Fx.cube(l, Material.IRON_BLOCK, 0.7f, new Quaternionf());
            Fx.animateLater(d, Fx.centered(0.05f, new Quaternionf(), 0, 0, 0), 6);
            Fx.remove(d, 10);
            for (LivingEntity e : enemies(p, l, 1.8))
                if (hit.add(e.getUniqueId())) { hurt(p, e, 3 + 5 * pw, dm); fling(e, dir, 0.8, 0.3); }
        }, null);
        return true;
    }

    private static boolean blink(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        double dist = 5 + 10 * pw;
        RayTraceResult r = w.rayTraceBlocks(eye, dir, dist, FluidCollisionMode.NEVER, true);
        Vector target = r != null
                ? r.getHitPosition().clone().subtract(dir.clone().multiply(0.7))
                : eye.toVector().add(dir.clone().multiply(dist));
        Location dest = target.toLocation(w);
        dest.setYaw(eye.getYaw()); dest.setPitch(eye.getPitch());
        dest.subtract(0, 1.62, 0);
        if (dest.getBlock().getType().isSolid()) dest.add(0, 1, 0);
        for (int i = 0; i <= dist * 3; i++)
            w.spawnParticle(Particle.END_ROD, eye.clone().add(dir.clone().multiply(i / 3.0)), 1, 0, 0, 0, 0);
        Fx.ring(p.getLocation().add(0, .2, 0), Material.LIGHT_GRAY_CONCRETE, 2.5, 8, 12, .3f, .1);
        w.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.6f);
        p.teleport(dest);
        w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.9f);
        Fx.ring(dest.clone().add(0, .2, 0), Material.IRON_BLOCK, 3 + 2 * pw, 8, 16, .35f, .1);
        Fx.pulse(dest.clone().add(0, 1, 0), Material.LIGHT_GRAY_STAINED_GLASS, 3f, 4, 2, new Quaternionf());
        for (LivingEntity e : enemies(p, dest, 3 + 2 * pw)) {
            hurt(p, e, 2 + 4 * pw, dm);
            effect(e, PotionEffectType.SLOWNESS, 60, 1);
        }
        return true;
    }

    // ───────────────────────── VENUS ─────────────────────────

    private static boolean acidRain(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location c = p.getLocation();
        double rad = 5 + 4 * pw;
        var cloud = Fx.box(c.clone().add(0, 9, 0), Material.GREEN_STAINED_GLASS, (float) (rad * 2), 0.7f, (float) (rad * 2), new Quaternionf());
        var cloud2 = Fx.box(c.clone().add(0, 9.5, 0), Material.GRAY_STAINED_GLASS, (float) (rad * 1.3), 0.5f, (float) (rad * 1.3), new Quaternionf());
        Fx.remove(cloud, 112); Fx.remove(cloud2, 112);
        w.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.6f);
        Fx.repeat(2, 50, t -> {
            for (int i = 0; i < 4; i++) {
                double a = R.nextDouble() * Math.PI * 2, rr = rad * Math.sqrt(R.nextDouble());
                Location l = c.clone().add(Math.cos(a) * rr, 8.5, Math.sin(a) * rr);
                var d = Fx.cube(l, Material.LIME_STAINED_GLASS, 0.25f, new Quaternionf());
                Fx.animateLater(d, Fx.centered(0.25f, new Quaternionf(), 0, -8.5f, 0), 14);
                Fx.remove(d, 18);
                dust(w, c.clone().add(Math.cos(a) * rr, 0.2, Math.sin(a) * rr), 3, .3, Color.LIME, 1.2f);
            }
            if (t % 5 == 0)
                for (LivingEntity e : enemies(p, c, rad)) {
                    hurt(p, e, 1 + 2 * pw, dm);
                    effect(e, PotionEffectType.POISON, 60, pw > 0.6 ? 1 : 0);
                }
        }, null);
        return true;
    }

    private static boolean greenhouse(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location c = p.getLocation();
        double rad = 4 + 5 * pw;
        Fx.pulse(c.clone().add(0, 1, 0), Material.MAGMA_BLOCK, (float) (rad * 1.8), 8, 8, new Quaternionf());
        Fx.ring(c.clone().add(0, .3, 0), Material.ORANGE_CONCRETE, rad, 10, 24, .5f, .3);
        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
        w.playSound(c, Sound.ITEM_FIRECHARGE_USE, 1f, 0.5f);
        w.spawnParticle(Particle.FLAME, c.clone().add(0, 1, 0), 120, rad / 3, .8, rad / 3, 0.08);
        w.spawnParticle(Particle.LAVA, c.clone().add(0, 1, 0), 40, rad / 3, .5, rad / 3, 0);
        for (LivingEntity e : enemies(p, c, rad)) {
            hurt(p, e, 4 + 7 * pw, dm);
            e.setFireTicks((int) (60 + 100 * pw));
            fling(e, away(c, e.getLocation()), 1.1, 0.6);
        }
        return true;
    }

    // ───────────────────────── EARTH ─────────────────────────

    private static boolean earthfall(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location c = p.getLocation();
        Location t = ground(c.clone().add(flat(p.getEyeLocation().getDirection()).multiply(7)), c);
        float size = (float) (3.5 + 3 * pw);
        double rad = 4 + 3 * pw;
        Quaternionf q = new Quaternionf();
        var rock = Fx.cube(t.clone().add(0, 16, 0), Material.GRASS_BLOCK, size, q);
        var core = Fx.cube(t.clone().add(0, 16, 0), Material.STONE, size * 0.8f, q);
        Quaternionf spin = new Quaternionf().rotateX(2.5f).rotateZ(1.2f);
        Fx.animateLater(rock, Fx.centered(size, spin, 0, -16 + size / 2f, 0), 14);
        Fx.animateLater(core, Fx.centered(size * 0.8f, spin, 0, -16 + size / 2f, 0), 14);
        w.playSound(t, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);
        Fx.repeat(2, 8, i -> {
            for (int k = 0; k < 24; k++) {
                double a = Math.PI * 2 * k / 24;
                dust(w, t.clone().add(Math.cos(a) * rad, 0.2, Math.sin(a) * rad), 1, 0, Color.ORANGE, 1.4f);
            }
        }, null);
        Fx.later(16, () -> {
            w.playSound(t, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
            w.playSound(t, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, t, 2);
            w.spawnParticle(Particle.BLOCK, t.clone().add(0, 1, 0), 150, rad / 2, .6, rad / 2, 0.2, Material.DIRT.createBlockData());
            Fx.ring(t.clone().add(0, .3, 0), Material.COBBLESTONE, rad, 12, 18, .6f, .5);
            Fx.ring(t.clone().add(0, .3, 0), Material.DIRT, rad * 0.6, 10, 12, .5f, 1.2);
            for (LivingEntity e : enemies(p, t, rad)) {
                hurt(p, e, 5 + 9 * pw, dm);
                fling(e, away(t, e.getLocation()), 0.6, 0.9 + 0.5 * pw);
            }
            Fx.later(6, () -> {
                Fx.animate(rock, Fx.centered(0.01f, spin, 0, -16 + size / 2f, 0), 8);
                Fx.animate(core, Fx.centered(0.01f, spin, 0, -16 + size / 2f, 0), 8);
            });
            Fx.remove(rock, 18); Fx.remove(core, 18);
        });
        return true;
    }

    private static boolean gaiaBarrier(Player p, double pw, double dm) {
        int dur = (int) ((6 + 8 * pw) * 20);
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, dur, (int) Math.round(pw * 3)));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, dur, pw > 0.6 ? 1 : 0));
        World w = p.getWorld();
        w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
        Material[] ms = {Material.STONE, Material.MOSSY_COBBLESTONE, Material.DIRT, Material.COBBLESTONE, Material.DEEPSLATE, Material.GRASS_BLOCK};
        BlockDisplay[] ds = new BlockDisplay[ms.length];
        for (int i = 0; i < ms.length; i++) {
            ds[i] = Fx.cube(p.getLocation(), ms[i], 0.8f, new Quaternionf());
            ds[i].setTeleportDuration(2);
        }
        Fx.repeat(1, dur, t -> {
            if (!p.isOnline() || p.isDead()) return;
            Location base = p.getLocation();
            for (int i = 0; i < ds.length; i++) {
                double a = t * 0.15 + i * Math.PI * 2 / ds.length;
                Fx.tp(ds[i], base.clone().add(Math.cos(a) * 1.9, 1 + Math.sin(t * 0.1 + i) * 0.4, Math.sin(a) * 1.9));
                if (t % 20 == 0)
                    Fx.animate(ds[i], Fx.centered(0.8f, new Quaternionf().rotateXYZ(R.nextFloat() * PI, R.nextFloat() * PI, R.nextFloat() * PI), 0, 0, 0), 20);
            }
            if (t % 10 == 0)
                for (LivingEntity e : enemies(p, base.clone().add(0, 1, 0), 2.6)) {
                    hurt(p, e, 1 + pw, dm);
                    fling(e, away(base, e.getLocation()), 0.7, 0.3);
                    w.playSound(e.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.7f);
                }
        }, () -> {
            for (BlockDisplay d : ds) {
                w.spawnParticle(Particle.BLOCK, d.getLocation(), 12, .3, .3, .3, 0.05, Material.STONE.createBlockData());
                d.remove();
            }
        });
        return true;
    }

    // ───────────────────────── MARS ─────────────────────────

    private static boolean meteor(Player p, double pw, double dm) {
        World w = p.getWorld();
        Vector dir = p.getEyeLocation().getDirection().normalize();
        Location loc = p.getEyeLocation().add(dir.clone().multiply(1.5));
        var core = Fx.cube(loc, Material.MAGMA_BLOCK, 1.6f, new Quaternionf().rotateXYZ(.6f, .4f, .2f));
        var shell = Fx.cube(loc, Material.RED_NETHER_BRICKS, 1.1f, new Quaternionf().rotateXYZ(.2f, .9f, .5f));
        core.setTeleportDuration(1); shell.setTeleportDuration(1);
        w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
        AtomicBoolean done = new AtomicBoolean(false);
        Runnable explode = () -> {
            if (!done.compareAndSet(false, true)) return;
            Location at = loc.clone();
            core.remove(); shell.remove();
            double rad = 3.5 + 3 * pw;
            w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.7f);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
            w.spawnParticle(Particle.FLAME, at, 80, rad / 3, rad / 3, rad / 3, 0.15);
            w.spawnParticle(Particle.LAVA, at, 30, rad / 3, rad / 3, rad / 3, 0);
            Fx.pulse(at, Material.ORANGE_STAINED_GLASS, (float) (rad * 1.6), 4, 3, new Quaternionf());
            Fx.ring(at, Material.MAGMA_BLOCK, rad, 10, 16, .5f, .3);
            for (LivingEntity e : enemies(p, at, rad)) {
                hurt(p, e, 6 + 10 * pw, dm);
                e.setFireTicks(100);
                fling(e, away(at, e.getLocation()), 0.9, 0.6);
            }
        };
        Fx.repeat(1, 40, t -> {
            if (done.get()) return;
            loc.add(dir.clone().multiply(0.9));
            Fx.tp(core, loc); Fx.tp(shell, loc);
            w.spawnParticle(Particle.FLAME, loc, 6, .3, .3, .3, 0.02);
            w.spawnParticle(Particle.LARGE_SMOKE, loc, 3, .3, .3, .3, 0.01);
            if (loc.getBlock().getType().isSolid() || !enemies(p, loc, 1.6).isEmpty()) explode.run();
        }, explode);
        return true;
    }

    private static boolean rage(Player p, double pw, double dm) {
        int dur = (int) ((6 + 8 * pw) * 20);
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, dur, pw > 0.7 ? 1 : 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, pw > 0.7 ? 1 : 0));
        SolarSMP.RAGING.put(p.getUniqueId(), System.currentTimeMillis() + dur * 50L);
        World w = p.getWorld();
        Location c = p.getLocation();
        w.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.7f);
        Fx.ring(c.clone().add(0, .2, 0), Material.RED_CONCRETE, 3.5, 10, 20, .4f, .2);
        Fx.pulse(c.clone().add(0, 1, 0), Material.RED_STAINED_GLASS, 3.5f, 5, 3, new Quaternionf());
        Fx.repeat(4, dur / 4, t -> {
            if (!p.isOnline()) return;
            dust(w, p.getLocation().add(0, 1, 0), 8, .5, Color.RED, 1.5f);
            w.spawnParticle(Particle.FLAME, p.getLocation().add(0, .2, 0), 3, .3, .1, .3, 0.01);
        }, null);
        return true;
    }

    // ───────────────────────── JUPITER ─────────────────────────

    private static boolean redStorm(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location c = p.getLocation();
        double rad = 4 + 6 * pw;
        int strikes = 4 + (int) (8 * pw);
        var cloud = Fx.box(c.clone().add(0, 14, 0), Material.ORANGE_TERRACOTTA, (float) (rad * 2.4), 1f, (float) (rad * 2.4), new Quaternionf());
        var eye = Fx.box(c.clone().add(0, 14.6, 0), Material.RED_TERRACOTTA, (float) (rad * 1.0), 0.8f, (float) (rad * 1.0), new Quaternionf());
        int total = strikes * 5 + 15;
        Fx.remove(cloud, total + 10); Fx.remove(eye, total + 10);
        for (int s = 0; s <= total; s += 10) {
            float ang = s / 10f * 0.6f;
            Fx.later(s + 2, () -> {
                Fx.animate(cloud, Fx.centered((float) (rad * 2.4), 1f, (float) (rad * 2.4), new Quaternionf().rotateY(ang), 0, 0, 0), 10);
                Fx.animate(eye, Fx.centered((float) (rad), 0.8f, (float) (rad), new Quaternionf().rotateY(-ang * 1.5f), 0, 0, 0), 10);
            });
        }
        w.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
        Fx.repeat(5, strikes, t -> {
            double a = R.nextDouble() * Math.PI * 2, rr = 2 + (rad - 2) * Math.sqrt(R.nextDouble());
            Location at = ground(c.clone().add(Math.cos(a) * rr, 0, Math.sin(a) * rr), c);
            w.strikeLightningEffect(at);
            w.spawnParticle(Particle.ELECTRIC_SPARK, at, 40, 1, 1, 1, 0.3);
            Fx.ring(at.clone().add(0, .2, 0), Material.YELLOW_CONCRETE, 2.5, 6, 8, .25f, .3);
            for (LivingEntity e : enemies(p, at, 3.2)) {
                hurt(p, e, 5 + 8 * pw, dm);
                e.setFireTicks(40);
            }
        }, null);
        return true;
    }

    private static boolean gravityCrush(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location center = p.getLocation().add(flat(p.getEyeLocation().getDirection()).multiply(6)).add(0, 1.5, 0);
        double rad = 8 + 4 * pw;
        Quaternionf q = new Quaternionf();
        var hole = Fx.cube(center, Material.BLACK_CONCRETE, 0.5f, q);
        Fx.animateLater(hole, Fx.centered(2.2f, q, 0, 0, 0), 10);
        int n = 12;
        BlockDisplay[] ring = new BlockDisplay[n];
        for (int i = 0; i < n; i++) {
            ring[i] = Fx.cube(center, i % 2 == 0 ? Material.CRYING_OBSIDIAN : Material.PURPLE_STAINED_GLASS, 0.45f, q);
            ring[i].setTeleportDuration(1);
        }
        w.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.5f);
        Fx.repeat(1, 50, t -> {
            if (t % 8 == 0) Fx.animate(hole, Fx.centered(2.2f, new Quaternionf().rotateXYZ(t * .2f, t * .3f, t * .1f), 0, 0, 0), 8);
            for (int i = 0; i < n; i++) {
                double a = t * 0.3 + i * Math.PI * 2 / n;
                Fx.tp(ring[i], center.clone().add(Math.cos(a) * 3, Math.sin(a * 0.5) * 0.6, Math.sin(a) * 3));
            }
            w.spawnParticle(Particle.PORTAL, center, 25, 2.5, 1.2, 2.5, 0.5);
            w.spawnParticle(Particle.SMOKE, center, 6, 1.2, .8, 1.2, 0.02);
            for (LivingEntity e : enemies(p, center, rad)) {
                Vector pull = center.toVector().subtract(e.getLocation().toVector().add(new Vector(0, 1, 0)));
                if (pull.lengthSquared() < 1e-3) continue;
                e.setVelocity(e.getVelocity().multiply(0.5).add(pull.normalize().multiply(0.3 + 0.25 * pw)));
                if (t % 10 == 0) hurt(p, e, 1.5 + 2 * pw, dm);
            }
        }, () -> {
            w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, center, 2);
            Fx.pulse(center, Material.PURPLE_STAINED_GLASS, 8f, 3, 2, q);
            Fx.ring(center, Material.OBSIDIAN, 6, 8, 18, .5f, 0);
            for (LivingEntity e : enemies(p, center, 5)) {
                hurt(p, e, 5 + 8 * pw, dm);
                fling(e, away(center, e.getLocation()), 1.2, 0.7);
            }
            hole.remove();
            for (BlockDisplay d : ring) d.remove();
        });
        return true;
    }

    // ───────────────────────── SATURN ─────────────────────────

    private static boolean ringSlash(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location c = p.getLocation().add(0, 0.8, 0);
        double max = 8 + 8 * pw;
        int count = 32, ticks = 14;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Quaternionf q = new Quaternionf().rotateY((float) a);
            var d = Fx.box(c, i % 2 == 0 ? Material.GOLD_BLOCK : Material.HONEY_BLOCK, 0.9f, 0.18f, 0.5f, q);
            Fx.animateLater(d, Fx.centered(0.9f, 0.18f, 0.5f, q, (float) (Math.cos(a) * max), 0, (float) (Math.sin(a) * max)), ticks);
            Fx.later(ticks + 2, () -> Fx.animate(d, Fx.centered(0.02f, 0.02f, 0.02f, q, 0, 0, 0), 4));
            Fx.remove(d, ticks + 8);
        }
        w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
        w.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        Set<UUID> hit = new HashSet<>();
        Fx.repeat(1, ticks + 2, t -> {
            double r = max * Math.max(0, t - 1) / ticks;
            for (int k = 0; k < 40; k++) {
                double a = Math.PI * 2 * k / 40;
                dust(w, c.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r), 1, 0, Color.fromRGB(230, 190, 90), 1.2f);
            }
            for (LivingEntity e : enemies(p, c, max + 2)) {
                Location el = e.getLocation();
                double d = Math.hypot(el.getX() - c.getX(), el.getZ() - c.getZ());
                if (Math.abs(d - r) <= 1.8 && Math.abs(el.getY() - c.getY()) < 2.5 && hit.add(e.getUniqueId())) {
                    hurt(p, e, 4 + 7 * pw, dm);
                    fling(e, away(c, el), 0.9 + 0.5 * pw, 0.4);
                }
            }
        }, null);
        return true;
    }

    private static boolean ringPrison(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        RayTraceResult r = w.rayTraceEntities(eye, eye.getDirection(), 25, 1.2,
                e -> e instanceof LivingEntity && !e.equals(p) && !(e instanceof ArmorStand));
        if (r == null || !(r.getHitEntity() instanceof LivingEntity target)) {
            p.sendActionBar(Component.text("No target in sight", NamedTextColor.RED));
            return false;
        }
        int dur = (int) ((3 + 4 * pw) * 20);
        effect(target, PotionEffectType.SLOWNESS, dur, 6);
        effect(target, PotionEffectType.JUMP_BOOST, dur, 128);
        effect(target, PotionEffectType.GLOWING, dur, 0);
        w.playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.8f);
        int n = 10;
        BlockDisplay[][] rings = new BlockDisplay[2][n];
        for (int k = 0; k < 2; k++)
            for (int i = 0; i < n; i++) {
                rings[k][i] = Fx.cube(target.getLocation(), (i % 2 == 0) ? Material.GOLD_BLOCK : Material.HONEY_BLOCK, 0.35f, new Quaternionf());
                rings[k][i].setTeleportDuration(1);
            }
        Fx.repeat(1, dur, t -> {
            Location base = target.getLocation().add(0, target.getHeight() / 2, 0);
            double rad = target.getWidth() + 0.9;
            for (int k = 0; k < 2; k++) {
                double tilt = k == 0 ? 0 : Math.toRadians(55);
                for (int i = 0; i < n; i++) {
                    double a = (k == 0 ? 1 : -1) * t * 0.25 + i * Math.PI * 2 / n;
                    double x = Math.cos(a) * rad, zf = Math.sin(a) * rad;
                    Fx.tp(rings[k][i], base.clone().add(x, zf * Math.sin(tilt), zf * Math.cos(tilt)));
                }
            }
            if (t % 20 == 10 && !target.isDead()) hurt(p, target, 1 + 1.5 * pw, dm);
        }, () -> {
            w.spawnParticle(Particle.FIREWORK, target.getLocation().add(0, 1, 0), 30, .5, .8, .5, 0.1);
            for (BlockDisplay[] row : rings) for (BlockDisplay d : row) d.remove();
        });
        return true;
    }

    // ───────────────────────── URANUS ─────────────────────────

    private static boolean iceBreath(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        double range = 7 + 6 * pw;
        w.playSound(eye, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.6f);
        Fx.repeat(1, 24, t -> {
            Location o = p.getEyeLocation().add(dir.clone().multiply(0.8));
            for (int i = 0; i < 3; i++) {
                Vector v = dir.clone().add(new Vector(R.nextDouble() - .5, R.nextDouble() - .5, R.nextDouble() - .5).multiply(0.45)).normalize().multiply(range);
                Quaternionf q = new Quaternionf().rotateXYZ(R.nextFloat() * PI, R.nextFloat() * PI, R.nextFloat() * PI);
                var d = Fx.cube(o, i == 0 ? Material.BLUE_ICE : Material.PACKED_ICE, 0.4f, q);
                Fx.animateLater(d, Fx.centered(0.15f, q, (float) v.getX(), (float) v.getY(), (float) v.getZ()), 6);
                Fx.remove(d, 9);
            }
            for (int i = 0; i < 14; i++) {
                double dist = R.nextDouble() * range;
                Vector spread = new Vector(R.nextDouble() - .5, R.nextDouble() - .5, R.nextDouble() - .5).multiply(dist * 0.4);
                w.spawnParticle(Particle.SNOWFLAKE, o.clone().add(dir.clone().multiply(dist)).add(spread), 1, 0, 0, 0, 0);
            }
            if (t % 3 == 0)
                for (LivingEntity e : enemies(p, o, range + 1)) {
                    Vector to = e.getLocation().add(0, 1, 0).toVector().subtract(o.toVector());
                    if (to.length() <= range + 1 && to.normalize().dot(dir) > 0.8) {
                        hurt(p, e, 1 + 1.5 * pw, dm);
                        effect(e, PotionEffectType.SLOWNESS, 60, pw > 0.6 ? 2 : 1);
                        e.setFreezeTicks(Math.max(e.getFreezeTicks(), 160));
                    }
                }
        }, null);
        return true;
    }

    private static boolean axialFlip(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location c = p.getLocation().add(0, 1, 0);
        double rad = 6 + 4 * pw;
        Quaternionf tilt = new Quaternionf().rotateZ((float) Math.toRadians(98));
        Fx.pulse(c, Material.LIGHT_BLUE_STAINED_GLASS, (float) (rad * 1.5), 8, 6, tilt);
        Fx.ring(c.clone().add(0, -.5, 0), Material.BLUE_ICE, rad, 10, 24, .4f, .5);
        w.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.4f);
        w.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
        w.spawnParticle(Particle.SNOWFLAKE, c, 150, rad / 3, rad / 3, rad / 3, 0.15);
        for (LivingEntity e : enemies(p, c, rad)) {
            Location l = e.getLocation();
            l.setYaw(l.getYaw() + 180);
            e.teleport(l);
            hurt(p, e, 3 + 3 * pw, dm);
            e.setVelocity(new Vector(0, 0.9 + 0.3 * pw, 0));
            effect(e, PotionEffectType.LEVITATION, 20, 0);
            e.setFreezeTicks(100);
        }
        return true;
    }

    // ───────────────────────── NEPTUNE ─────────────────────────

    private static boolean tidalWave(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location c = p.getLocation();
        Vector dir = flat(p.getEyeLocation().getDirection());
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        int range = (int) (14 + 10 * pw);
        Location start = c.clone().add(dir.clone().multiply(2));
        w.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5f, 0.6f);
        w.playSound(c, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.5f, 0.6f);
        Material[] layers = {Material.BLUE_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS, Material.BLUE_ICE};
        for (int col = -3; col <= 3; col++)
            for (int row = 0; row < 3; row++) {
                Location l = start.clone().add(right.clone().multiply(col * 1.3)).add(0, 0.8 + row * 1.2, 0);
                Quaternionf q = new Quaternionf();
                var d = Fx.cube(l, layers[row], 1.5f, q);
                Fx.animateLater(d, Fx.centered(1.5f, q, (float) (dir.getX() * range), (row == 2 ? 0.4f : 0f), (float) (dir.getZ() * range)), range);
                Fx.remove(d, range + 6);
            }
        Set<UUID> hit = new HashSet<>();
        Fx.repeat(1, range, t -> {
            Location mid = start.clone().add(dir.clone().multiply(Math.max(0, t - 1))).add(0, 1.2, 0);
            for (int col = -3; col <= 3; col++)
                w.spawnParticle(Particle.SPLASH, mid.clone().add(right.clone().multiply(col * 1.3)), 6, .4, .8, .4, 0.1);
            for (LivingEntity e : enemies(p, mid, 4.5)) {
                Vector to = e.getLocation().toVector().subtract(mid.toVector());
                if (Math.abs(to.dot(right)) <= 5 && hit.add(e.getUniqueId())) {
                    hurt(p, e, 4 + 6 * pw, dm);
                    e.setVelocity(dir.clone().multiply(1.4).setY(0.5));
                    effect(e, PotionEffectType.SLOWNESS, 40, 1);
                }
            }
        }, null);
        return true;
    }

    private static boolean maelstrom(Player p, double pw, double dm) {
        World w = p.getWorld();
        Location center = ground(p.getLocation().add(flat(p.getEyeLocation().getDirection()).multiply(6)), p.getLocation());
        double rad = 6 + 4 * pw;
        int n = 14;
        Material[] ms = {Material.PRISMARINE, Material.DARK_PRISMARINE, Material.CYAN_STAINED_GLASS, Material.BLUE_ICE};
        BlockDisplay[] ds = new BlockDisplay[n];
        for (int i = 0; i < n; i++) {
            ds[i] = Fx.cube(center, ms[i % ms.length], 0.6f, new Quaternionf());
            ds[i].setTeleportDuration(1);
        }
        w.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.7f);
        Fx.repeat(1, 50, t -> {
            for (int i = 0; i < n; i++) {
                double frac = (double) i / n;
                double a = t * 0.35 + i * 0.45;
                double rr = 1 + 4 * (1 - frac);
                Fx.tp(ds[i], center.clone().add(Math.cos(a) * rr, 0.3 + frac * 5, Math.sin(a) * rr));
            }
            for (int i = 0; i < 12; i++) {
                double frac = R.nextDouble(), a = t * 0.35 + frac * 12;
                w.spawnParticle(i % 2 == 0 ? Particle.SPLASH : Particle.BUBBLE_POP,
                        center.clone().add(Math.cos(a) * (1 + 4 * (1 - frac)), frac * 5, Math.sin(a) * (1 + 4 * (1 - frac))), 2, .1, .1, .1, 0.02);
            }
            for (LivingEntity e : enemies(p, center, rad)) {
                Vector in = center.toVector().subtract(e.getLocation().toVector()).setY(0);
                if (in.lengthSquared() < 1e-3) continue;
                in.normalize();
                Vector tangent = new Vector(-in.getZ(), 0, in.getX());
                e.setVelocity(tangent.multiply(0.4).add(in.multiply(0.2)).setY(0.08));
                if (t % 10 == 0) hurt(p, e, 1.5 + 2 * pw, dm);
            }
        }, () -> {
            w.playSound(center, Sound.ENTITY_GENERIC_SPLASH, 2f, 0.5f);
            w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
            Fx.ring(center.clone().add(0, .2, 0), Material.LIGHT_BLUE_STAINED_GLASS, 4, 8, 16, .5f, 4);
            w.spawnParticle(Particle.SPLASH, center.clone().add(0, 2, 0), 200, 1.5, 3, 1.5, 0.3);
            for (LivingEntity e : enemies(p, center, 4)) {
                hurt(p, e, 3 + 4 * pw, dm);
                e.setVelocity(new Vector(0, 1.2 + 0.6 * pw, 0));
            }
            for (BlockDisplay d : ds) d.remove();
        });
        return true;
    }
}
