package dev.solarsmp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;

/** Visual-effect helpers built on block display entities (no resource pack needed). */
public final class Fx {
    private Fx() {}

    private static SolarSMP plugin;
    private static final Set<Entity> LIVE = new HashSet<>();

    public static void init(SolarSMP p) { plugin = p; }

    public static void clearAll() {
        for (Entity e : LIVE) e.remove();
        LIVE.clear();
    }

    public static void later(long ticks, Runnable r) {
        Bukkit.getScheduler().runTaskLater(plugin, r, ticks);
    }

    public static void repeat(int period, int total, IntConsumer body, Runnable end) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= total) { cancel(); if (end != null) end.run(); return; }
                body.accept(t++);
            }
        }.runTaskTimer(plugin, 0, period);
    }

    /** Transformation that keeps a scaled/rotated box centred on the entity, plus an offset. */
    public static Transformation centered(float sx, float sy, float sz, Quaternionf rot, float ox, float oy, float oz) {
        Vector3f c = new Vector3f(sx / 2f, sy / 2f, sz / 2f);
        new Quaternionf(rot).transform(c);
        return new Transformation(new Vector3f(ox - c.x, oy - c.y, oz - c.z),
                new Quaternionf(rot), new Vector3f(sx, sy, sz), new Quaternionf());
    }

    public static Transformation centered(float s, Quaternionf rot, float ox, float oy, float oz) {
        return centered(s, s, s, rot, ox, oy, oz);
    }

    public static BlockDisplay box(Location at, Material m, float sx, float sy, float sz, Quaternionf rot) {
        Location l = at.clone();
        l.setYaw(0); l.setPitch(0);
        BlockDisplay d = l.getWorld().spawn(l, BlockDisplay.class, e -> {
            e.setBlock(m.createBlockData());
            e.setPersistent(false);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setTransformation(centered(sx, sy, sz, rot, 0, 0, 0));
        });
        LIVE.add(d);
        return d;
    }

    public static BlockDisplay cube(Location at, Material m, float size, Quaternionf rot) {
        return box(at, m, size, size, size, rot);
    }

    public static void animate(BlockDisplay d, Transformation t, int ticks) {
        if (!d.isValid()) return;
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(ticks);
        d.setTransformation(t);
    }

    /** Animate after the client has received the initial state (needs a 2-tick gap). */
    public static void animateLater(BlockDisplay d, Transformation t, int ticks) {
        later(2, () -> animate(d, t, ticks));
    }

    public static void remove(Entity e, long ticks) {
        later(ticks, () -> { e.remove(); LIVE.remove(e); });
    }

    public static void tp(Entity e, Location l) {
        Location c = l.clone();
        c.setYaw(0); c.setPitch(0);
        e.teleport(c);
    }

    /** Ring of small cubes bursting outward. */
    public static void ring(Location c, Material m, double radius, int ticks, int count, float size, double yOff) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Quaternionf q = new Quaternionf().rotateY((float) a);
            BlockDisplay d = cube(c, m, size, q);
            animateLater(d, centered(size, q, (float) (Math.cos(a) * radius), (float) yOff, (float) (Math.sin(a) * radius)), ticks);
            later(ticks + 2, () -> animate(d, centered(0.02f, q, 0, 0, 0), 5));
            remove(d, ticks + 9);
        }
    }

    /** Two overlapping cubes (one rotated 45°) that swell and vanish: a cheap "sphere". */
    public static void pulse(Location c, Material m, float endSize, int grow, int hold, Quaternionf tilt) {
        for (int k = 0; k < 2; k++) {
            Quaternionf q = new Quaternionf(tilt).rotateY((float) Math.toRadians(45 * k));
            BlockDisplay d = cube(c, m, 0.3f, q);
            animateLater(d, centered(endSize, q, 0, 0, 0), grow);
            later(2 + grow + hold, () -> animate(d, centered(0.01f, q, 0, 0, 0), 6));
            remove(d, 2 + grow + hold + 9);
        }
    }
}
