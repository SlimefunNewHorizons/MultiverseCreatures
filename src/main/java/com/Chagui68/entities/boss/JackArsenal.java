package com.Chagui68.entities.boss;

import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Timeline;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Plays JackStar's twenty {@link JackAbility arsenal attacks}. Each one is a {@link Timeline}: the
 * boss ticks it once per server tick, and every blow is telegraphed on the floor before it lands.
 */
final class JackArsenal {

    private static final Color CODE = JackStarBoss.CODE;
    private static final Color CODE_DIM = JackStarBoss.CODE_DIM;
    private static final Color CYAN = JackStarBoss.GLITCH_CYAN;
    private static final Color MAGENTA = JackStarBoss.GLITCH_MAGENTA;

    private JackArsenal() {
    }

    /** Starts {@code ability}; {@code power} multiplies every hit it deals. */
    static ArsenalKit.Running start(JackAbility ability, ArsenalKit.Host host, Player target, double power) {
        ArsenalKit.Cast c = new ArsenalKit.Cast(host, target, power, ability.label);
        Timeline t = switch (ability) {
            case PING_FLOOD -> pingFlood(c);
            case NULL_POINTER -> nullPointer(c);
            case SYNTAX_ERROR -> syntaxError(c);
            case PACKET_SNIFFER -> packetSniffer(c);
            case MEMORY_LEAK -> memoryLeak(c);
            case INFINITE_LOOP -> infiniteLoop(c);
            case ROLLBACK -> rollback(c);
            case FIREWALL_RING -> firewallRing(c);
            case THREAD_SPIKES -> threadSpikes(c);
            case DDOS -> ddos(c);
            case ENCRYPTION_LOCK -> encryptionLock(c);
            case PORT_SCAN -> portScan(c);
            case SEGFAULT -> segfault(c);
            case OVERCLOCK -> overclock(c);
            case RECURSIVE_CLONE -> recursiveClone(c);
            case BLUE_SCREEN -> blueScreen(c);
            case RM_RF -> rmRf(c);
            case ROOT_ACCESS -> rootAccess(c);
            case ZERO_DAY -> zeroDay(c);
            case SYSTEM_CRASH -> systemCrash(c);
            case KERNEL_NUKE -> kernelNuke(c);
            case DISK_FORMAT -> diskFormat(c);
            case SUDO_LASER -> sudoLaser(c);
        };
        t.hold(ability.channel);
        return new ArsenalKit.Running(ability.name(), ability.gesture, ability.channel, t);
    }

    // ------------------------------------------------------------------ phase 1

    /** Six data packets fired in a stream at the target. */
    private static Timeline pingFlood(ArsenalKit.Cast c) {
        List<ArsenalKit.Shot> shots = new ArrayList<>();
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.BEACON_POWER, 1.4f, 1.9f));
        t.span(0, 10, (tick, p) -> {
            c.face(c.aim());
            if (tick % 2 == 0) c.fx.gather(c.hand(), 1.4, 3, CYAN, 5);
        });
        t.every(10, 40, 5, tick -> {
            c.face(c.aim());
            Vector from = c.hand();
            Vector to = c.targetHere() ? c.target.getLocation().toVector().add(new Vector(0, 1.0, 0))
                    : from.clone().add(c.facing().multiply(10));
            Vector v = to.subtract(from);
            if (v.lengthSquared() < 0.01) v = c.facing();
            shots.add(new ArsenalKit.Shot(from, v.normalize().multiply(1.1), 26));
            c.fx.sound(from, Sfx.NOTE_HAT, 1.6f, 1.8f);
        });
        t.span(10, 68, (tick, p) -> ArsenalKit.fly(shots, c.world, c.players(), 1.0,
                s -> {
                    c.fx.dust(CYAN, 1.3f).at(s.at);
                    c.fx.dust(Palette.ICE, 0.8f).at(s.at.clone().subtract(s.velocity.clone().multiply(0.5)));
                },
                (s, victim) -> {
                    c.hit(victim, 4);
                    c.afflict(victim, PotionEffectType.GLOWING, 40, 0);
                    c.fx.cloud(Particle.ELECTRIC_SPARK, s.at, 8, 0.3, 0.05);
                    c.fx.sound(s.at, Sfx.AMETHYST_BREAK, 1.2f, 1.8f);
                }));
        return t;
    }

    /** A circle under the target that collapses into nothing 1.5 s later. */
    private static Timeline nullPointer(ArsenalKit.Cast c) {
        final double radius = 3.0;
        Vector[] mark = new Vector[1];
        Timeline t = new Timeline();
        t.at(0, () -> {
            mark[0] = c.ground(c.aim());
            c.fx.sound(mark[0], Sfx.ELDER_GUARDIAN_CURSE, 1.0f, 1.6f);
        });
        t.span(0, 30, (tick, p) -> {
            c.face(mark[0]);
            if (tick % 2 == 0) ArsenalKit.warnCircle(c.fx, mark[0], radius, p);
            if (tick % 3 == 0) {
                c.fx.draw(Shapes.helix(mark[0], radius * (1 - p) + 0.3, 2.5, 1, 12, tick * 0.3),
                        c.fx.dust(Palette.mix(Palette.VOID, Palette.VOID_DEEP, p), 1.3f));
            }
            if (tick % 10 == 0) c.fx.sound(mark[0], Sfx.WARDEN_HEARTBEAT, 1.2f, 1.2f + (float) p);
        });
        t.at(30, () -> {
            Vector mid = mark[0].clone().add(new Vector(0, 0.8, 0));
            c.fx.impact(mid, Palette.VOID, 1.8);
            c.fx.flatBurst(mark[0], Particle.PORTAL, 30, 0.6);
            c.fx.sound(mark[0], Sfx.END_PORTAL_SPAWN, 0.8f, 1.8f);
            c.fx.sound(mark[0], Sfx.EXPLODE, 1.2f, 1.4f);
            Area area = Area.cylinder(mark[0], radius, 1.5, 3);
            for (Player p : c.players()) {
                if (!area.contains(p.getLocation().toVector())) continue;
                c.hit(p, 12);
                c.afflict(p, PotionEffectType.SLOWNESS, 60, 1);
                c.push(p, ArsenalKit.towards(p.getLocation().toVector(), mark[0]).multiply(0.5).setY(0.2));
            }
        });
        t.hold(36);
        return t;
    }

    /** A cone of corrupted glyphs in front of him: nausea and weakness for whoever reads it. */
    private static Timeline syntaxError(ArsenalKit.Cast c) {
        final double half = 0.6;
        final double length = 8;
        Vector[] apex = new Vector[1];
        Vector[] dir = new Vector[1];
        Timeline t = new Timeline();
        t.at(0, () -> {
            apex[0] = c.feet();
            dir[0] = ArsenalKit.towards(apex[0], c.aim());
            c.face(c.aim());
            c.fx.sound(apex[0], Sfx.NOTE_BASEDRUM, 1.5f, 0.6f);
        });
        t.span(0, 18, (tick, p) -> {
            if (tick % 2 == 0) ArsenalKit.warnCone(c.fx, apex[0], dir[0], half, length, p);
        });
        t.at(18, () -> {
            double base = Math.atan2(dir[0].getZ(), dir[0].getX());
            for (int i = 0; i < 40; i++) {
                double a = base + (c.random.nextDouble() * 2 - 1) * half;
                double d = 1 + c.random.nextDouble() * (length - 1);
                Vector at = apex[0].clone().add(Shapes.heading(a).multiply(d)).add(new Vector(0, 0.4 + c.random.nextDouble() * 1.6, 0));
                c.fx.dust(i % 3 == 0 ? Palette.WARNING_HOT : Palette.WARNING, 1.5f).at(at);
                if (i % 4 == 0) c.fx.cloud(Particle.CRIT, at, 2, 0.2, 0.1);
            }
            c.fx.sound(apex[0], Sfx.GLASS_BREAK, 1.6f, 0.7f);
            c.fx.sound(apex[0], Sfx.SHIELD_BREAK, 1.4f, 1.2f);
            Area area = Area.cone(apex[0], dir[0], half, length, 3);
            for (Player p : c.players()) {
                if (!area.contains(p.getLocation().toVector())) continue;
                c.hit(p, 10);
                c.afflict(p, PotionEffectType.NAUSEA, 80, 0);
                c.afflict(p, PotionEffectType.WEAKNESS, 60, 0);
                c.push(p, dir[0].clone().multiply(0.8).setY(0.3));
            }
        });
        t.hold(24);
        return t;
    }

    /** A scanning beam sweeps a wide arc in front of him and tags everyone it crosses. */
    private static Timeline packetSniffer(ArsenalKit.Cast c) {
        final double length = 10;
        final double half = 1.2;
        Vector[] origin = new Vector[1];
        Vector[] dir = new Vector[1];
        Set<UUID> struck = new HashSet<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            origin[0] = c.feet();
            dir[0] = ArsenalKit.towards(origin[0], c.aim());
            c.face(c.aim());
            c.fx.sound(origin[0], Sfx.BEACON_ACTIVATE, 1.4f, 1.7f);
        });
        t.span(0, 12, (tick, p) -> {
            if (tick % 2 == 0) ArsenalKit.warnCone(c.fx, origin[0], dir[0], half, length, p);
        });
        t.span(12, 44, (tick, p) -> {
            double base = Math.atan2(dir[0].getZ(), dir[0].getX());
            Vector heading = Shapes.heading(base - half + 2 * half * p);
            Vector from = origin[0].clone().add(new Vector(0, 1.2 * c.host.scale(), 0));
            c.fx.beam(from, from.clone().add(heading.clone().multiply(length)), CYAN, CODE, 0.3);
            if (tick % 4 == 0) c.fx.sound(from, Sfx.NOTE_HAT, 1.0f, 1.2f + (float) p);
            Vector low = origin[0].clone().add(new Vector(0, 0.5, 0));
            Area area = Area.segment(low, low.clone().add(heading.multiply(length)), 1.0);
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector()) || !struck.add(pl.getUniqueId())) continue;
                c.hit(pl, 8);
                c.afflict(pl, PotionEffectType.GLOWING, 100, 0);
                c.fx.cloud(Particle.ELECTRIC_SPARK, pl.getLocation().toVector().add(new Vector(0, 1, 0)), 10, 0.3, 0.05);
            }
        });
        t.hold(48);
        return t;
    }

    // ------------------------------------------------------------------ phase 2

    /** Pools of leaked memory under the players that grow and drain them while they stand in them. */
    private static Timeline memoryLeak(ArsenalKit.Cast c) {
        List<Vector> pools = new ArrayList<>();
        Timeline t = new Timeline();
        t.span(0, 8, (tick, p) -> {
            if (tick % 2 == 0) c.fx.gather(c.chest().add(new Vector(0, 1.6, 0)), 1.8, 4, Palette.PLAGUE, 6);
        });
        t.at(8, () -> {
            List<Player> players = new ArrayList<>(c.players());
            players.sort((a, b) -> Double.compare(a.getLocation().distanceSquared(c.stand.getLocation()),
                    b.getLocation().distanceSquared(c.stand.getLocation())));
            for (Player p : players) {
                if (pools.size() >= 3) break;
                pools.add(c.ground(p.getLocation().toVector()));
            }
            while (pools.size() < 2) {
                Vector aim = c.aim().add(new Vector(c.random.nextGaussian() * 3, 0, c.random.nextGaussian() * 3));
                pools.add(c.ground(aim));
            }
            for (Vector pool : pools) c.fx.sound(pool, Sfx.BLOCK_LAVA_POP, 1.4f, 0.6f);
        });
        t.span(8, 110, (tick, p) -> {
            double radius = 1.6 + 1.4 * p;
            for (Vector pool : pools) {
                Vector floor = pool.clone().add(new Vector(0, 0.12, 0));
                if (tick % 2 == 0) {
                    c.fx.disc(floor, radius, 0.9, c.fx.dust(Palette.PLAGUE, 1.1f).sometimes(0.45));
                    c.fx.ring(floor, radius, 0.6, tick * 0.1, c.fx.dust(CODE, 1.0f));
                }
                if (tick % 9 == 0) c.fx.sound(pool, Sfx.BLOCK_LAVA_POP, 0.6f, 1.4f);
                if (tick % 10 != 0) continue;
                Area area = Area.cylinder(pool, radius, 1, 2.5);
                for (Player pl : c.players()) {
                    if (!area.contains(pl.getLocation().toVector())) continue;
                    c.hit(pl, 2.5);
                    c.afflict(pl, PotionEffectType.HUNGER, 60, 1);
                    c.afflict(pl, PotionEffectType.SLOWNESS, 30, 0);
                }
            }
        });
        return t;
    }

    /** A ring of code around the target: crossing it hurts, and it implodes on whoever stays inside. */
    private static Timeline infiniteLoop(ArsenalKit.Cast c) {
        final double radius = 4.0;
        Vector[] center = new Vector[1];
        Map<UUID, Integer> lastHit = new HashMap<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            center[0] = c.ground(c.aim());
            c.fx.sound(center[0], Sfx.CONDUIT_ACTIVATE, 1.4f, 1.4f);
        });
        t.span(0, 20, (tick, p) -> {
            c.face(center[0]);
            if (tick % 2 == 0) {
                ArsenalKit.warnCircle(c.fx, center[0], radius, p);
                c.fx.ring(center[0].clone().add(new Vector(0, 0.6, 0)), radius, 0.7, tick * 0.2, c.fx.dust(CODE_DIM, 1.0f));
            }
        });
        t.span(20, 70, (tick, p) -> {
            int now = 20 + tick;
            for (double h : new double[]{0.3, 1.2}) {
                c.fx.ring(center[0].clone().add(new Vector(0, h, 0)), radius, 0.6, now * (h > 1 ? -0.15 : 0.15),
                        c.fx.dust(tick % 4 < 2 ? CODE : CYAN, 1.2f));
            }
            if (tick % 8 == 0) c.fx.sound(center[0], Sfx.NOTE_HAT, 1.0f, 0.8f + (float) p);
            for (Player pl : c.players()) {
                Vector at = pl.getLocation().toVector();
                double dy = at.getY() - center[0].getY();
                double flat = ArsenalKit.flatDistance(at, center[0]);
                if (Math.abs(flat - radius) > 0.7 || dy < -1 || dy > 2.5) continue;
                Integer last = lastHit.get(pl.getUniqueId());
                if (last != null && now - last < 10) continue;
                lastHit.put(pl.getUniqueId(), now);
                c.hit(pl, 4);
                c.push(pl, ArsenalKit.towards(at, center[0]).multiply(0.6).setY(0.15));
                c.fx.cloud(Particle.ELECTRIC_SPARK, at.clone().add(new Vector(0, 1, 0)), 8, 0.3, 0.05);
            }
        });
        t.at(70, () -> {
            c.fx.impact(center[0].clone().add(new Vector(0, 1, 0)), CODE, 2.2);
            c.fx.sound(center[0], Sfx.BEACON_DEACTIVATE, 1.8f, 1.4f);
            for (Player pl : c.players()) {
                Vector at = pl.getLocation().toVector();
                double dy = at.getY() - center[0].getY();
                if (ArsenalKit.flatDistance(at, center[0]) > radius - 0.5 || dy < -1 || dy > 3) continue;
                c.hit(pl, 10);
                c.push(pl, new Vector(0, 0.7, 0));
            }
        });
        t.hold(74);
        return t;
    }

    /** Saves where everyone stands, then rewinds them back there 2.5 s later. */
    private static Timeline rollback(ArsenalKit.Cast c) {
        Map<UUID, Location> saves = new HashMap<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            for (Player p : c.players()) {
                if (p.getLocation().distanceSquared(c.stand.getLocation()) > 18 * 18) continue;
                saves.put(p.getUniqueId(), p.getLocation().clone());
            }
            c.fx.sound(c.feet(), Sfx.BEACON_ACTIVATE, 1.5f, 1.2f);
        });
        t.span(0, 50, (tick, p) -> {
            c.face(c.aim());
            for (Location save : saves.values()) {
                Vector at = save.toVector();
                if (tick % 3 == 0) {
                    c.fx.ring(at.clone().add(new Vector(0, 0.1, 0)), 0.6, 0.3, tick * 0.3, c.fx.dust(CYAN, 1.0f));
                    ArsenalKit.drawFigure(c.fx, at, Shapes.flat(save.getDirection()), Palette.mix(CYAN, Palette.WARNING, p), 1.8);
                }
                if (tick % 10 == 0) c.fx.sound(at, Sfx.NOTE_HAT, 1.0f, 1.0f + (float) p);
            }
        });
        t.at(50, () -> {
            for (Map.Entry<UUID, Location> save : saves.entrySet()) {
                Player p = c.stand.getServer().getPlayer(save.getKey());
                if (!ArsenalKit.valid(p) || !p.getWorld().equals(c.world)) continue;
                Location back = save.getValue().clone();
                if (back.distanceSquared(p.getLocation()) > 30 * 30) continue;
                back.setYaw(p.getLocation().getYaw());
                back.setPitch(p.getLocation().getPitch());
                c.fx.cloud(Particle.PORTAL, p.getLocation().toVector().add(new Vector(0, 1, 0)), 20, 0.4, 0.2);
                p.teleport(back);
                c.hit(p, 6);
                c.afflict(p, PotionEffectType.NAUSEA, 60, 0);
                c.fx.sound(back.toVector(), Sfx.ENDERMAN_TELEPORT, 1.2f, 0.7f);
            }
        });
        t.hold(54);
        return t;
    }

    /** A wall of soul fire racing outwards from him: jump it or burn. */
    private static Timeline firewallRing(ArsenalKit.Cast c) {
        Vector[] center = new Vector[1];
        Set<UUID> struck = new HashSet<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            center[0] = c.feet();
            c.fx.sound(center[0], Sfx.BLAZE_AMBIENT, 1.6f, 0.7f);
        });
        t.span(0, 16, (tick, p) -> {
            if (tick % 2 == 0) {
                ArsenalKit.warnCircle(c.fx, center[0], 2.0, p);
                c.fx.ring(center[0].clone().add(new Vector(0, 0.15, 0)), 12, 1.4, 0, c.fx.dust(Palette.WARNING, 1.0f));
            }
            c.fx.cloud(Particle.SOUL_FIRE_FLAME, center[0].clone().add(new Vector(0, 0.3, 0)), 3, 1.0, 0.02);
        });
        t.span(16, 44, (tick, p) -> {
            double r = 1 + tick * 0.43;
            Vector base = center[0].clone().add(new Vector(0, 0.3, 0));
            c.fx.ring(base, r, 0.5, tick * 0.1, c.fx.particle(Particle.SOUL_FIRE_FLAME));
            c.fx.ring(base.clone().add(new Vector(0, 0.6, 0)), r, 0.9, tick * 0.1, c.fx.dust(Palette.EMBER, 1.3f));
            if (tick % 4 == 0) c.fx.sound(center[0], Sfx.FIRECHARGE, 1.2f, 0.8f + (float) p * 0.4f);
            Area area = Area.ring(center[0], r - 0.7, r + 0.7, 1.2);
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector()) || !struck.add(pl.getUniqueId())) continue;
                c.hit(pl, 10);
                pl.setFireTicks(Math.max(pl.getFireTicks(), 60));
            }
        });
        t.hold(48);
        return t;
    }

    // ------------------------------------------------------------------ phase 3

    /** Three lines of spikes erupt one block at a time from his feet towards the target. */
    private static Timeline threadSpikes(ArsenalKit.Cast c) {
        final double length = 14;
        Vector[] origin = new Vector[1];
        List<Vector> dirs = new ArrayList<>();
        Set<UUID> struck = new HashSet<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            origin[0] = c.feet();
            Vector dir = ArsenalKit.towards(origin[0], c.aim());
            dirs.add(dir);
            dirs.add(ArsenalKit.rotateFlat(dir, 0.45));
            dirs.add(ArsenalKit.rotateFlat(dir, -0.45));
            c.face(c.aim());
            c.fx.sound(origin[0], Sfx.EVOKER_PREPARE_SUMMON, 1.4f, 1.6f);
        });
        t.span(0, 20, (tick, p) -> {
            if (tick % 2 != 0) return;
            for (Vector dir : dirs) ArsenalKit.warnLine(c.fx, origin[0], origin[0].clone().add(dir.clone().multiply(length)), 2.0, p);
        });
        t.span(20, 34, (tick, p) -> {
            for (int i = 0; i < dirs.size(); i++) {
                Vector point = c.ground(origin[0].clone().add(dirs.get(i).clone().multiply(tick + 1)));
                c.fx.line(point, point.clone().add(new Vector(0, 2.2, 0)), 0.25,
                        c.fx.dust(i % 2 == 0 ? MAGENTA : CYAN, 1.4f).and(c.fx.particle(Particle.CRIT).sometimes(0.3)));
                c.fx.crumble(Material.DEEPSLATE, 4, 0.4).at(point.clone().add(new Vector(0, 0.2, 0)));
                for (Player pl : c.players()) {
                    Vector at = pl.getLocation().toVector();
                    double dy = at.getY() - point.getY();
                    if (ArsenalKit.flatDistance(at, point) > 1.3 || dy < -1 || dy > 2.5) continue;
                    if (!struck.add(pl.getUniqueId())) continue;
                    c.hit(pl, 12);
                    c.push(pl, new Vector(0, 0.9, 0));
                }
            }
            if (tick % 2 == 0) c.fx.sound(origin[0], Sfx.POINTED_DRIPSTONE_LAND, 1.5f, 0.8f + tick * 0.05f);
        });
        t.hold(38);
        return t;
    }

    /** One request of a DDoS: a bolt landing on {@code at}, warned from tick {@code born}. */
    private record Request(Vector at, int born) {
    }

    /** A flood of requests: bolts rain down around the players for two and a half seconds. */
    private static Timeline ddos(ArsenalKit.Cast c) {
        final int warn = 12;
        final int fall = 3;
        List<Request> requests = new ArrayList<>();
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.BEACON_POWER, 1.6f, 0.8f));
        t.span(0, 80, (tick, p) -> {
            if (tick >= 8 && tick < 60 && tick % 4 == 0) {
                List<Player> players = c.players();
                Vector spot;
                if (!players.isEmpty() && c.random.nextInt(100) < 70) {
                    Player aim = players.get(c.random.nextInt(players.size()));
                    spot = aim.getLocation().toVector().add(new Vector(c.random.nextGaussian() * 2, 0, c.random.nextGaussian() * 2));
                } else {
                    spot = c.aim().add(new Vector(c.random.nextGaussian() * 4, 0, c.random.nextGaussian() * 4));
                }
                requests.add(new Request(c.ground(spot), tick));
            }
            if (tick < 24 && tick % 3 == 0) c.fx.gather(c.chest().add(new Vector(0, 2, 0)), 2.0, 3, MAGENTA, 6);
            for (Request r : requests) {
                int age = tick - r.born();
                if (age < 0 || age > warn + fall) continue;
                if (age < warn) {
                    if (age % 2 == 0) ArsenalKit.warnCircle(c.fx, r.at(), 1.6, age / (double) warn);
                    continue;
                }
                double f = (age - warn + 1) / (double) fall;
                Vector bottom = r.at().clone().add(new Vector(0, 14 * (1 - f), 0));
                c.fx.line(bottom, bottom.clone().add(new Vector(0, 3, 0)), 0.3, c.fx.dust(MAGENTA, 1.5f));
                if (age < warn + fall) continue;
                c.fx.impact(r.at().clone().add(new Vector(0, 0.5, 0)), MAGENTA, 1.1);
                c.fx.sound(r.at(), Sfx.FIRECHARGE, 1.0f, 1.5f);
                Area area = Area.cylinder(r.at(), 1.6, 1, 3);
                for (Player pl : c.players()) {
                    if (!area.contains(pl.getLocation().toVector())) continue;
                    c.hit(pl, 5);
                    c.push(pl, ArsenalKit.towards(r.at(), pl.getLocation().toVector()).multiply(0.4).setY(0.25));
                }
            }
        });
        return t;
    }

    /** Chains the target in place under a padlock, then the lock bursts. */
    private static Timeline encryptionLock(ArsenalKit.Cast c) {
        boolean[] locked = new boolean[1];
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.TRIDENT_THROW, 1.4f, 0.7f));
        t.span(0, 10, (tick, p) -> {
            if (!c.targetHere()) return;
            c.face(c.aim());
            Vector chest = c.target.getLocation().toVector().add(new Vector(0, 1.0, 0));
            Vector from = c.hand();
            Vector reach = from.clone().add(chest.subtract(from).multiply(p));
            c.fx.line(from, reach, 0.35, c.fx.dust(CYAN, 1.1f).and(c.fx.particle(Particle.CRIT).sometimes(0.2)));
        });
        t.at(10, () -> {
            if (!c.targetHere() || c.target.getLocation().distanceSquared(c.stand.getLocation()) > 20 * 20) return;
            locked[0] = true;
            c.afflict(c.target, PotionEffectType.SLOWNESS, 50, 4);
            c.afflict(c.target, PotionEffectType.MINING_FATIGUE, 50, 1);
            c.afflict(c.target, PotionEffectType.WEAKNESS, 50, 1);
            c.fx.sound(c.target.getLocation().toVector(), Sfx.IRON_DOOR_CLOSE, 1.6f, 0.7f);
        });
        t.span(10, 60, (tick, p) -> {
            if (!locked[0] || !c.targetHere()) return;
            Vector feet = c.target.getLocation().toVector();
            if (tick % 2 == 0) {
                Vector lock = feet.clone().add(new Vector(0, 2.6, 0));
                Vector side = ArsenalKit.rotateFlat(ArsenalKit.towards(feet, c.feet()), Math.PI / 2);
                c.fx.draw(Shapes.circle(lock.clone().add(new Vector(0, 0.35, 0)), 0.25, 10, side, Shapes.UP, 0), c.fx.dust(Palette.STONE, 1.0f));
                c.fx.line(lock.clone().add(side.clone().multiply(0.35)), lock.clone().subtract(side.clone().multiply(0.35)), 0.12,
                        c.fx.dust(Palette.GOLD, 1.3f));
                c.fx.line(lock.clone().add(side.clone().multiply(0.35)).add(new Vector(0, -0.4, 0)),
                        lock.clone().subtract(side.clone().multiply(0.35)).add(new Vector(0, -0.4, 0)), 0.12, c.fx.dust(Palette.GOLD, 1.3f));
            }
            c.fx.ring(feet.clone().add(new Vector(0, 0.2 + (tick % 20) * 0.09, 0)), 1.0, 0.4, tick * 0.3, c.fx.dust(CODE, 0.9f));
            if (tick % 10 == 0) c.fx.sound(feet, Sfx.NOTE_HAT, 1.0f, 0.6f + (float) p);
        });
        t.at(60, () -> {
            if (!locked[0] || !c.targetHere()) return;
            Vector chest = c.target.getLocation().toVector().add(new Vector(0, 1, 0));
            c.fx.impact(chest, CYAN, 1.6);
            c.fx.sound(chest, Sfx.GLASS_BREAK, 1.6f, 0.9f);
            for (Player pl : c.players()) {
                if (pl.getLocation().toVector().distanceSquared(c.target.getLocation().toVector()) > 2.5 * 2.5) continue;
                c.hit(pl, 10);
                c.push(pl, new Vector(0, 0.6, 0));
            }
        });
        t.hold(64);
        return t;
    }

    /** Four laser lines in a cross, slowly turning a quarter circle around him. */
    private static Timeline portScan(ArsenalKit.Cast c) {
        final double length = 12;
        Vector[] center = new Vector[1];
        double[] base = new double[1];
        Map<UUID, Integer> lastHit = new HashMap<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            center[0] = c.feet();
            Vector dir = ArsenalKit.towards(center[0], c.aim());
            base[0] = Math.atan2(dir.getZ(), dir.getX()) + Math.PI / 4;
            c.fx.sound(center[0], Sfx.BEACON_ACTIVATE, 1.6f, 0.9f);
        });
        t.span(0, 14, (tick, p) -> {
            if (tick % 2 != 0) return;
            for (int k = 0; k < 4; k++) {
                Vector end = center[0].clone().add(Shapes.heading(base[0] + k * Math.PI / 2).multiply(length));
                ArsenalKit.warnLine(c.fx, center[0], end, 1.6, p);
            }
        });
        t.span(14, 56, (tick, p) -> {
            int now = 14 + tick;
            double angle = base[0] + Math.PI / 2 * p;
            Vector from = center[0].clone().add(new Vector(0, 1.0, 0));
            Vector low = center[0].clone().add(new Vector(0, 0.5, 0));
            for (int k = 0; k < 4; k++) {
                Vector heading = Shapes.heading(angle + k * Math.PI / 2);
                c.fx.beam(from, from.clone().add(heading.clone().multiply(length)), MAGENTA, Palette.WARNING, 0.25);
                Area area = Area.segment(low, low.clone().add(heading.multiply(length)), 0.9);
                for (Player pl : c.players()) {
                    if (!area.contains(pl.getLocation().toVector())) continue;
                    Integer last = lastHit.get(pl.getUniqueId());
                    if (last != null && now - last < 20) continue;
                    lastHit.put(pl.getUniqueId(), now);
                    c.hit(pl, 6);
                    c.afflict(pl, PotionEffectType.GLOWING, 60, 0);
                }
            }
            if (tick % 6 == 0) c.fx.sound(center[0], Sfx.BEACON_POWER, 0.9f, 0.6f + (float) p);
        });
        t.hold(60);
        return t;
    }

    // ------------------------------------------------------------------ phase 4

    /** The floor cracks from him to the target, then erupts along the whole crack. */
    private static Timeline segfault(ArsenalKit.Cast c) {
        Vector[] line = new Vector[2];
        Timeline t = new Timeline();
        t.at(0, () -> {
            Vector a = c.feet();
            Vector dir = ArsenalKit.towards(a, c.aim());
            double length = Math.max(10, Math.min(20, ArsenalKit.flatDistance(a, c.aim()) + 4));
            line[0] = a;
            line[1] = a.clone().add(dir.multiply(length));
            c.face(c.aim());
            c.fx.sound(a, Sfx.WARDEN_SONIC_CHARGE, 1.4f, 1.3f);
        });
        t.span(0, 26, (tick, p) -> {
            if (tick % 2 == 0) ArsenalKit.warnLine(c.fx, line[0], line[1], 2.6, p);
            Vector crack = line[0].clone().add(line[1].clone().subtract(line[0]).multiply(p));
            c.fx.crumble(Material.DEEPSLATE, 6, 0.4).at(c.ground(crack).add(new Vector(0, 0.2, 0)));
        });
        t.at(26, () -> {
            Vector dir = line[1].clone().subtract(line[0]);
            double length = dir.length();
            dir.normalize();
            for (double d = 0; d <= length; d += 1.0) {
                Vector point = c.ground(line[0].clone().add(dir.clone().multiply(d)));
                c.fx.line(point, point.clone().add(new Vector(0, 1.8, 0)), 0.3, c.fx.dust(((int) d) % 2 == 0 ? MAGENTA : CYAN, 1.5f));
                c.fx.crumble(Material.DEEPSLATE, 6, 0.5).at(point.clone().add(new Vector(0, 0.3, 0)));
            }
            c.fx.sound(line[0], Sfx.EXPLODE, 1.6f, 0.8f);
            c.fx.sound(line[1], Sfx.WITHER_BREAK_BLOCK, 1.4f, 1.2f);
            Area area = Area.segment(line[0], line[1], 1.4);
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 16);
                c.push(pl, new Vector(0, 1.1, 0));
            }
        });
        t.hold(34);
        return t;
    }

    /** He overclocks: faster for five seconds, wrapped in a heat aura that burns whoever stays close. */
    private static Timeline overclock(ArsenalKit.Cast c) {
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.BLAZE_AMBIENT, 1.5f, 1.4f));
        t.span(0, 16, (tick, p) -> {
            c.fx.ring(c.feet().add(new Vector(0, 0.2 + p * 1.6, 0)), 1.2, 0.4, tick * 0.4, c.fx.dust(Palette.MOLTEN, 1.2f));
        });
        t.at(16, () -> {
            c.host.empower(100);
            c.fx.sound(c.feet(), Sfx.RESPAWN_ANCHOR_CHARGE, 1.6f, 1.6f);
            c.fx.flash(c.chest(), Palette.MOLTEN);
        });
        t.span(16, 116, (tick, p) -> {
            Vector feet = c.feet();
            if (tick % 2 == 0) {
                c.fx.ring(feet.clone().add(new Vector(0, 0.2, 0)), 3.2, 0.7, tick * 0.4,
                        c.fx.dust(Palette.MOLTEN, 1.1f).and(c.fx.particle(Particle.FLAME).sometimes(0.3)));
            }
            if (tick % 10 != 0) return;
            Area area = Area.cylinder(feet, 3.2, 1, 3);
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 2.5);
                pl.setFireTicks(Math.max(pl.getFireTicks(), 30));
            }
        });
        return t;
    }

    /** Three holographic copies of him surround the target and dash through it one after another. */
    private static Timeline recursiveClone(ArsenalKit.Cast c) {
        final int dash = 6;
        List<Vector[]> lanes = new ArrayList<>();
        List<Set<UUID>> struck = new ArrayList<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            Vector center = c.aim();
            double start = c.random.nextDouble() * Math.PI * 2;
            for (int k = 0; k < 3; k++) {
                Vector from = c.ground(center.clone().add(Shapes.heading(start + k * 2 * Math.PI / 3).multiply(6)));
                Vector dir = ArsenalKit.towards(from, center);
                lanes.add(new Vector[]{from, c.ground(center.clone().add(dir.multiply(6)))});
                struck.add(new HashSet<>());
            }
            c.fx.sound(center, Sfx.ILLUSIONER_MIRROR, 1.6f, 1.0f);
        });
        t.span(0, 30, (tick, p) -> {
            if (tick % 2 != 0) return;
            for (int k = 0; k < lanes.size(); k++) {
                Vector[] lane = lanes.get(k);
                ArsenalKit.drawFigure(c.fx, lane[0], ArsenalKit.towards(lane[0], lane[1]), k % 2 == 0 ? CYAN : MAGENTA, 2.0);
                if (tick >= 10) ArsenalKit.warnLine(c.fx, lane[0], lane[1], 1.6, (tick - 10) / 20.0);
            }
        });
        for (int k = 0; k < 3; k++) {
            final int index = k;
            int from = 30 + 8 * k;
            t.span(from, from + dash, (tick, p) -> {
                if (index >= lanes.size()) return;
                Vector[] lane = lanes.get(index);
                Vector dir = lane[1].clone().subtract(lane[0]);
                Vector before = lane[0].clone().add(dir.clone().multiply(tick / (double) dash));
                Vector after = lane[0].clone().add(dir.clone().multiply((tick + 1) / (double) dash));
                Color color = index % 2 == 0 ? CYAN : MAGENTA;
                ArsenalKit.drawFigure(c.fx, after, ArsenalKit.towards(lane[0], lane[1]), color, 2.0);
                c.fx.line(before.clone().add(new Vector(0, 1, 0)), after.clone().add(new Vector(0, 1, 0)), 0.3, c.fx.dust(color, 1.4f));
                if (tick == 0) c.fx.sound(before, Sfx.BREEZE_WIND_BURST, 1.4f, 1.4f);
                Area area = Area.segment(before, after, 1.1);
                for (Player pl : c.players()) {
                    if (!area.contains(pl.getLocation().toVector()) || !struck.get(index).add(pl.getUniqueId())) continue;
                    c.hit(pl, 9);
                    c.push(pl, ArsenalKit.towards(lane[0], lane[1]).multiply(0.6).setY(0.3));
                }
            });
        }
        t.hold(56);
        return t;
    }

    /** A blue screen opens behind him: whoever is looking at him when it flashes is blinded. */
    private static Timeline blueScreen(ArsenalKit.Cast c) {
        final double range = 14;
        Timeline t = new Timeline();
        t.at(0, () -> {
            c.fx.sound(c.feet(), Sfx.BEACON_DEACTIVATE, 1.6f, 0.6f);
            for (Player p : c.players()) {
                if (p.getLocation().distanceSquared(c.stand.getLocation()) > (range + 4) * (range + 4)) continue;
                p.sendActionBar(ChatColor.BLUE + "" + ChatColor.BOLD + "⚠ Look away from JackStar's screen!");
            }
        });
        t.span(0, 30, (tick, p) -> {
            c.face(c.aim());
            if (tick % 2 != 0) return;
            float s = c.host.scale();
            Vector facing = c.facing();
            Vector right = ArsenalKit.rotateFlat(facing, Math.PI / 2);
            Vector center = c.feet().add(new Vector(0, 2.8 * s, 0)).subtract(facing.clone().multiply(0.6 * s));
            double half = (0.6 + 1.2 * p) * s;
            Fx.Brush fill = c.fx.dust(Palette.STORM, 1.2f).sometimes(0.6);
            for (double u = -half; u <= half; u += 0.4) {
                for (double v = -half * 0.6; v <= half * 0.6; v += 0.4) {
                    fill.at(center.clone().add(right.clone().multiply(u)).add(new Vector(0, v, 0)));
                }
            }
            Fx.Brush edge = c.fx.dust(Palette.ICE, 1.1f);
            Vector corner = new Vector(0, half * 0.6, 0);
            Vector across = right.clone().multiply(half);
            c.fx.line(center.clone().add(corner).subtract(across), center.clone().add(corner).add(across), 0.3, edge);
            c.fx.line(center.clone().subtract(corner).subtract(across), center.clone().subtract(corner).add(across), 0.3, edge);
            if (tick % 10 == 0) c.fx.sound(center, Sfx.BEACON_POWER, 1.0f, 0.5f + (float) p);
        });
        t.at(30, () -> {
            Vector eye = c.chest().add(new Vector(0, 0.5, 0));
            c.fx.flash(eye, Palette.STORM);
            c.fx.burst(eye, Particle.END_ROD, 30, 0.4);
            c.fx.sound(eye, Sfx.WARDEN_SONIC_BOOM, 1.2f, 1.8f);
            for (Player pl : c.players()) {
                if (pl.getLocation().distanceSquared(c.stand.getLocation()) > range * range) continue;
                Vector toBoss = eye.clone().subtract(pl.getEyeLocation().toVector());
                if (toBoss.lengthSquared() < 1e-6) continue;
                if (pl.getEyeLocation().getDirection().dot(toBoss.normalize()) < 0.5 || !pl.hasLineOfSight(c.stand)) continue;
                c.hit(pl, 8);
                c.afflict(pl, PotionEffectType.BLINDNESS, 60, 0);
                c.afflict(pl, PotionEffectType.SLOWNESS, 40, 1);
            }
        });
        t.hold(36);
        return t;
    }

    // ------------------------------------------------------------------ phase 5

    /** Everything within seven blocks of him is deleted after a long, loud warning. */
    private static Timeline rmRf(ArsenalKit.Cast c) {
        final double radius = 7;
        Vector[] center = new Vector[1];
        Timeline t = new Timeline();
        t.at(0, () -> {
            center[0] = c.feet();
            c.fx.sound(center[0], Sfx.WARDEN_ROAR, 1.6f, 1.3f);
        });
        t.span(0, 44, (tick, p) -> {
            if (tick % 2 == 0) ArsenalKit.warnCircle(c.fx, center[0], radius, p);
            if (tick % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = c.random.nextDouble() * Math.PI * 2;
                    double r = Math.sqrt(c.random.nextDouble()) * radius;
                    c.fx.moving(Particle.SMOKE, center[0].clone().add(Shapes.heading(a).multiply(r)).add(new Vector(0, 0.2, 0)),
                            new Vector(0, 0.15, 0));
                }
            }
            if (tick % 10 == 0) c.fx.sound(center[0], Sfx.WARDEN_HEARTBEAT, 2f, 0.8f + (float) p);
        });
        t.at(44, () -> {
            c.fx.disc(center[0].clone().add(new Vector(0, 0.3, 0)), radius, 0.9, c.fx.dust(Palette.WARNING, 2.0f));
            c.fx.flatBurst(center[0], Particle.LARGE_SMOKE, 60, 0.5);
            c.fx.impact(center[0].clone().add(new Vector(0, 1, 0)), Palette.WARNING, 3);
            c.fx.sound(center[0], Sfx.WARDEN_SONIC_BOOM, 2f, 0.6f);
            c.fx.sound(center[0], Sfx.EXPLODE, 2f, 0.6f);
            Area area = Area.cylinder(center[0], radius, 2, 4);
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 22);
                c.afflict(pl, PotionEffectType.DARKNESS, 80, 0);
            }
        });
        t.hold(50);
        return t;
    }

    /** A vortex drags everyone towards him, then he detonates. */
    private static Timeline rootAccess(ArsenalKit.Cast c) {
        final double pull = 20;
        final double blast = 5;
        Vector[] center = new Vector[1];
        Timeline t = new Timeline();
        t.at(0, () -> {
            center[0] = c.feet();
            c.fx.sound(center[0], Sfx.CONDUIT_ACTIVATE, 2f, 0.5f);
        });
        t.span(0, 40, (tick, p) -> {
            if (tick % 2 == 0) {
                double r = blast + 1 + (pull - blast) * (1 - (tick % 16) / 16.0);
                c.fx.ring(center[0].clone().add(new Vector(0, 0.3, 0)), r, 1.2, tick * 0.2, c.fx.dust(Palette.VOID, 1.2f));
                ArsenalKit.warnCircle(c.fx, center[0], blast, tick / 40.0);
            }
            if (tick >= 36) return;
            for (Player pl : c.players()) {
                Vector at = pl.getLocation().toVector();
                if (at.distanceSquared(center[0]) > pull * pull || ArsenalKit.flatDistance(at, center[0]) < 1.5) continue;
                c.push(pl, ArsenalKit.towards(at, center[0]).multiply(0.1));
                if (tick % 4 == 0) {
                    c.fx.line(at.clone().add(new Vector(0, 1, 0)), center[0].clone().add(new Vector(0, 1, 0)), 1.2,
                            c.fx.dust(CYAN, 0.8f).sometimes(0.5));
                }
            }
            if (tick % 8 == 0) c.fx.sound(center[0], Sfx.SOUL_ESCAPE, 1.6f, 0.6f);
        });
        t.at(40, () -> {
            c.fx.impact(center[0].clone().add(new Vector(0, 1, 0)), MAGENTA, 2.5);
            c.fx.sound(center[0], Sfx.EXPLODE, 2f, 0.7f);
            Area area = Area.cylinder(center[0], blast, 1.5, 3);
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 14);
                c.push(pl, ArsenalKit.towards(center[0], pl.getLocation().toVector()).multiply(1.2).setY(0.5));
            }
        });
        t.hold(46);
        return t;
    }

    /** He glitches out of sight and reappears behind the target with a blade already swinging. */
    private static Timeline zeroDay(ArsenalKit.Cast c) {
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.ENDERMAN_TELEPORT, 1.2f, 0.5f));
        t.span(0, 14, (tick, p) -> {
            c.fx.cloud(Particle.ELECTRIC_SPARK, c.chest(), 4, 0.5, 0.05);
            c.fx.dust(tick % 4 < 2 ? CYAN : MAGENTA, 1.4f, 0.5, 3).at(c.chest());
            if (c.targetHere() && tick % 2 == 0) ArsenalKit.warnCircle(c.fx, behind(c), 1.2, p);
        });
        t.at(14, () -> {
            if (!c.targetHere()) return;
            Location dest = behind(c).toLocation(c.world);
            dest.setDirection(c.target.getLocation().toVector().subtract(dest.toVector()).setY(0));
            BossArena.settle(dest);
            c.fx.cloud(Particle.PORTAL, c.chest(), 20, 0.4, 0.2);
            c.stand.teleport(dest);
            c.fx.sound(dest.toVector(), Sfx.ENDERMAN_TELEPORT, 1.4f, 1.6f);
        });
        t.span(14, 18, (tick, p) -> c.fx.cloud(Particle.ELECTRIC_SPARK, c.hand(), 3, 0.2, 0.05));
        t.at(18, () -> {
            Vector apex = c.feet();
            Vector dir = ArsenalKit.towards(apex, c.aim());
            Vector[] axes = {dir, ArsenalKit.rotateFlat(dir, Math.PI / 2)};
            c.fx.crescent(apex.clone().add(new Vector(0, 1.1, 0)), 1.6, 3.2, -1.2, 1.2, axes[0], axes[1],
                    c.fx.dust(MAGENTA, 1.5f), c.fx.dust(CYAN, 1.0f).sometimes(0.5));
            c.fx.sound(apex, Sfx.PLAYER_ATTACK_CRIT, 1.6f, 0.8f);
            c.fx.sound(apex, Sfx.PLAYER_ATTACK_SWEEP, 1.6f, 1.2f);
            Area area = Area.cone(apex, dir, 0.9, 3.6, 3);
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 16);
                c.afflict(pl, PotionEffectType.SLOWNESS, 40, 1);
            }
        });
        t.hold(26);
        return t;
    }

    /** One and a half blocks behind the target, where Zero Day lands. */
    private static Vector behind(ArsenalKit.Cast c) {
        Vector look = Shapes.flat(c.target.getLocation().getDirection());
        return c.ground(c.target.getLocation().toVector().subtract(look.multiply(1.5)));
    }

    /** One pillar of corrupted data, warned {@code warn} ticks before it erupts. */
    private record Column(Vector at, int born) {
    }

    /** The arena crashes: two waves of corrupted pillars erupt around every player. */
    private static Timeline systemCrash(ArsenalKit.Cast c) {
        final int warn = 20;
        List<Column> columns = new ArrayList<>();
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.WITHER_SPAWN, 1.0f, 1.6f));
        t.span(0, 60, (tick, p) -> {
            if (tick == 8 || tick == 32) {
                int count = 0;
                for (Player pl : c.players()) {
                    if (count++ >= 6) break;
                    Vector at = pl.getLocation().toVector();
                    columns.add(new Column(c.ground(at), tick));
                    for (int i = 0; i < 3; i++) {
                        Vector off = Shapes.heading(c.random.nextDouble() * Math.PI * 2).multiply(1.5 + c.random.nextDouble() * 2);
                        columns.add(new Column(c.ground(at.clone().add(off)), tick));
                    }
                }
                c.fx.sound(c.feet(), Sfx.GLASS_BREAK, 1.4f, 0.5f);
            }
            for (Column col : columns) {
                int age = tick - col.born();
                if (age < 0 || age > warn + 6) continue;
                if (age < warn) {
                    if (age % 2 == 0) ArsenalKit.warnCircle(c.fx, col.at(), 1.3, age / (double) warn);
                    continue;
                }
                Fx.Brush glitch = c.fx.dust(age % 2 == 0 ? MAGENTA : CYAN, 1.4f);
                c.fx.line(col.at(), col.at().clone().add(new Vector(0, 6, 0)), age == warn ? 0.25 : 0.7,
                        age == warn ? glitch : glitch.sometimes(0.4));
                if (age != warn) continue;
                c.fx.crumble(Material.OBSIDIAN, 10, 0.4).at(col.at().clone().add(new Vector(0, 0.3, 0)));
                c.fx.sound(col.at(), Sfx.AMETHYST_BREAK, 1.2f, 0.6f);
                Area area = Area.cylinder(col.at(), 1.3, 1, 3);
                for (Player pl : c.players()) {
                    if (!area.contains(pl.getLocation().toVector())) continue;
                    c.hit(pl, 12);
                    c.afflict(pl, PotionEffectType.NAUSEA, 40, 0);
                }
            }
        });
        return t;
    }

    // ------------------------------------------------------------------ destructive

    /**
     * Kernel Nuke: a targeting grid locks onto the target while a countdown flashes on every screen,
     * code rains into the zone, beams converge from orbit and a dome eleven blocks wide goes off.
     */
    private static Timeline kernelNuke(ArsenalKit.Cast c) {
        final int lock = 80;
        final int impact = 100;
        final double radius = 11;
        Vector[] aim = {c.ground(c.aim())};
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.BEACON_ACTIVATE, 3f, 0.5f));
        t.span(0, impact, (tick, p) -> {
            if (tick < lock && c.targetHere()) aim[0] = c.ground(aim[0].clone().add(c.target.getLocation().toVector().subtract(aim[0]).multiply(0.15)));
            if (tick % 20 == 0 && tick < lock) {
                ArsenalKit.hud(c.world, aim[0], 60, "☢ KERNEL NUKE", "Launch in " + ((lock - tick) / 20) + "... get out of the grid");
                c.fx.sound(aim[0], Sfx.NOTE_BASEDRUM, 3f, 0.5f + tick / 160f);
            }
            if (tick % 2 == 0) ArsenalKit.reticle(c.fx, aim[0], radius, Math.min(1, tick / (double) lock), MAGENTA);
            if (tick % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    Vector drop = aim[0].clone().add(new Vector((c.random.nextDouble() * 2 - 1) * radius, 6 + c.random.nextDouble() * 10,
                            (c.random.nextDouble() * 2 - 1) * radius));
                    c.fx.dust(c.random.nextBoolean() ? CODE : CODE_DIM, 1.4f).at(drop);
                }
            }
            c.fx.line(c.hand(), c.hand().add(new Vector(0, 40 * Math.min(1, p * 4), 0)), 1.0, c.fx.dust(MAGENTA, 1.2f).sometimes(0.5));
            if (tick >= lock) {
                double q = (tick - lock) / (double) (impact - lock);
                for (int i = 0; i < 10; i++) {
                    Vector top = aim[0].clone().add(Shapes.heading(2 * Math.PI * i / 10 + q * 2).multiply(radius * 1.5 * (1 - q)))
                            .add(new Vector(0, 50, 0));
                    c.fx.line(top, aim[0], 1.4, c.fx.dust(MAGENTA, 1.4f).sometimes(0.7));
                }
            }
        });
        t.at(lock, () -> {
            ArsenalKit.hud(c.world, aim[0], 60, "IMPACT CONFIRMED", "LOCK ON");
            // Scheduled at the lock so the blast lands where the grid stopped.
            Vector at = aim[0].clone();
            ArsenalKit.dome(t, c.fx, impact, 16, at, radius, MAGENTA, () -> {
                for (Player pl : c.players()) {
                    Vector feet = pl.getLocation().toVector();
                    if (ArsenalKit.flatDistance(feet, at) > radius || Math.abs(feet.getY() - at.getY()) > radius) continue;
                    c.hit(pl, 24);
                    c.afflict(pl, PotionEffectType.BLINDNESS, 40, 0);
                    c.push(pl, new Vector(0, 1.0, 0));
                }
            });
        });
        t.hold(impact + 46);
        return t;
    }

    /**
     * Disk Format: the arena around him becomes a grid of sectors that a progress bar wipes row by
     * row; when it reaches 100% every sector is erased except the few that glow green.
     */
    private static Timeline diskFormat(ArsenalKit.Cast c) {
        final int cells = 7;
        final double cell = 4;
        final int safeCount = 6;
        final int wipe = 92;
        Vector center = c.ground(c.feet());
        double half = cells * cell / 2;
        boolean[] safe = new boolean[cells * cells];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < cells * cells; i++) order.add(i);
        java.util.Collections.shuffle(order, c.random);
        int marked = 0;
        for (int index : order) {
            if (marked >= safeCount) break;
            if (index == (cells * cells) / 2) continue;
            safe[index] = true;
            marked++;
        }
        Timeline t = new Timeline();
        t.at(0, () -> {
            c.fx.sound(center, Sfx.BEACON_POWER, 3f, 0.5f);
            ArsenalKit.hud(c.world, center, 50, "💾 FORMAT C:", "Stand in a green sector");
        });
        t.span(0, wipe, (tick, p) -> {
            Vector floor = center.clone().add(new Vector(-half, 0.2, -half));
            if (tick % 4 == 0) {
                Fx.Brush grid = c.fx.dust(CODE_DIM, 1.0f);
                for (int i = 0; i <= cells; i++) {
                    c.fx.line(floor.clone().add(new Vector(i * cell, 0, 0)), floor.clone().add(new Vector(i * cell, 0, cells * cell)), 1.0, grid);
                    c.fx.line(floor.clone().add(new Vector(0, 0, i * cell)), floor.clone().add(new Vector(cells * cell, 0, i * cell)), 1.0, grid);
                }
            }
            int formatted = (int) (cells * cells * Math.min(1, tick / (double) (wipe - 10)));
            for (int i = 0; i < cells * cells; i++) {
                Vector corner = floor.clone().add(new Vector((i % cells) * cell, 0, (i / cells) * cell));
                Vector mid = corner.clone().add(new Vector(cell / 2, 0, cell / 2));
                if (safe[i]) {
                    if (tick % 2 == 0) c.fx.ring(mid, cell / 2 - 0.3, 0.6, tick * 0.1, c.fx.dust(Palette.PLAGUE, 1.4f));
                } else if (i < formatted && tick % 3 == 0) {
                    c.fx.dust(Palette.mix(Palette.WARNING, Palette.WARNING_HOT, p), 1.3f, cell / 3, 3).at(mid);
                }
            }
            if (tick % 10 == 0) c.fx.sound(center, Sfx.NOTE_HAT, 1.5f, 0.6f + (float) p);
        });
        t.at(wipe, () -> {
            Vector floor = center.clone().add(new Vector(-half, 0, -half));
            for (int i = 0; i < cells * cells; i++) {
                if (safe[i]) continue;
                Vector mid = floor.clone().add(new Vector((i % cells) * cell + cell / 2, 0.6, (i / cells) * cell + cell / 2));
                c.fx.dust(i % 2 == 0 ? MAGENTA : CYAN, 2.2f, cell / 3, 6).at(mid);
                if (i % 4 == 0) c.fx.cloud(org.bukkit.Particle.EXPLOSION, mid, 1, 0.5, 0);
            }
            c.fx.sound(center, Sfx.EXPLODE, 3f, 0.6f);
            c.fx.sound(center, Sfx.GLASS_BREAK, 3f, 0.5f);
            for (Player pl : c.players()) {
                Vector at = pl.getLocation().toVector().subtract(floor);
                if (at.getX() < 0 || at.getZ() < 0 || at.getX() >= cells * cell || at.getZ() >= cells * cell) continue;
                if (Math.abs(pl.getLocation().getY() - center.getY()) > 4) continue;
                int index = (int) (at.getZ() / cell) * cells + (int) (at.getX() / cell);
                if (safe[index]) continue;
                c.hit(pl, 22);
                c.afflict(pl, PotionEffectType.NAUSEA, 80, 0);
                c.push(pl, new Vector(0, 0.8, 0));
            }
        });
        t.hold(wipe + 26);
        return t;
    }

    /**
     * sudo laser: he charges a beam for three and a half seconds, then sweeps it once all the way
     * around himself, thirty blocks long and too tall to jump. The only shelter is right at his feet.
     */
    private static Timeline sudoLaser(ArsenalKit.Cast c) {
        final int charge = 70;
        final int sweep = 30;
        final double inner = 3;
        final double outer = 30;
        Vector center = c.ground(c.feet());
        double[] base = {0};
        java.util.Set<UUID> struck = new HashSet<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            Vector dir = ArsenalKit.towards(center, c.aim());
            base[0] = Math.atan2(dir.getZ(), dir.getX());
            c.fx.sound(center, Sfx.WARDEN_SONIC_CHARGE, 3f, 0.6f);
            ArsenalKit.hud(c.world, center, 40, "⚡ sudo laser", "Get close to him, the beam starts three blocks out");
        });
        t.span(0, charge, (tick, p) -> {
            Vector floor = center.clone().add(new Vector(0, 0.2, 0));
            if (tick % 2 == 0) {
                c.fx.ring(floor, inner, 0.5, tick * 0.2, c.fx.dust(Palette.PLAGUE, 1.6f));
                c.fx.ring(floor, outer, 1.2, -tick * 0.02, c.fx.dust(Palette.mix(Palette.WARNING, Palette.WARNING_HOT, p), 1.4f));
            }
            if (tick % 2 == 0) c.fx.gather(c.chest(), 6, 5, MAGENTA, 10);
            c.fx.draw(Shapes.sphere(c.chest(), 0.4 + 1.2 * p, 20), c.fx.dust(MAGENTA, 1.8f));
            if (tick % 14 == 0) c.fx.sound(center, Sfx.BEACON_POWER, 2f, 0.6f + (float) p);
        });
        t.span(charge, charge + sweep, (tick, p) -> {
            double angle = base[0] + 2 * Math.PI * p;
            Vector heading = Shapes.heading(angle);
            for (double h : new double[]{0.6, 1.6, 2.6}) {
                Vector a = center.clone().add(heading.clone().multiply(inner)).add(new Vector(0, h, 0));
                c.fx.beam(a, a.clone().add(heading.clone().multiply(outer - inner)), Palette.WARNING_HOT, MAGENTA, 0.5);
            }
            if (tick % 3 == 0) c.fx.sound(center, Sfx.WARDEN_SONIC_BOOM, 1.2f, 1.6f);
            Vector a = center.clone().add(heading.clone().multiply(inner));
            Vector b = center.clone().add(heading.clone().multiply(outer));
            for (Player pl : c.players()) {
                Vector feet = pl.getLocation().toVector();
                if (ArsenalKit.flatDistance(feet, center) < inner || feet.getY() - center.getY() > 3.5) continue;
                if (com.Chagui68.entities.boss.fx.Area.segment(a, b, 1.6).contains(feet) && struck.add(pl.getUniqueId())) {
                    c.hit(pl, 22);
                    pl.setFireTicks(Math.max(pl.getFireTicks(), 80));
                    c.push(pl, heading.clone().multiply(1.2).setY(0.5));
                }
            }
        });
        t.hold(charge + sweep + 4);
        return t;
    }
}
