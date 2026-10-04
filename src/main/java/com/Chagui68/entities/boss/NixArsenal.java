package com.Chagui68.entities.boss;

import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Timeline;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
 * Plays NIX's ten {@link NixAbility arsenal attacks}, each a {@link Timeline} the boss ticks once
 * per server tick. Every blow is telegraphed before it lands.
 */
final class NixArsenal {

    private NixArsenal() {
    }

    /** Starts {@code ability}; {@code power} multiplies every hit it deals. */
    static ArsenalKit.Running start(NixAbility ability, ArsenalKit.Host host, Player target, double power) {
        ArsenalKit.Cast c = new ArsenalKit.Cast(host, target, power, ability.label);
        Timeline t = switch (ability) {
            case HEADSMANS_TOSS -> headsmansToss(c);
            case BLOOD_POOL -> bloodPool(c);
            case IRON_MAIDEN -> ironMaiden(c);
            case NOOSE -> noose(c);
            case MARCH -> march(c);
            case PILLORY -> pillory(c);
            case CRIMSON_RAIN -> crimsonRain(c);
            case SEVERING_CROSS -> severingCross(c);
            case LAST_RITES -> lastRites(c);
            case CHAIN_WHIRL -> chainWhirl(c);
            case GRAND_GUILLOTINE -> grandGuillotine(c);
            case BLOOD_MOON -> bloodMoon(c);
            case EXECUTION_DAY -> executionDay(c);
        };
        t.hold(ability.channel);
        return new ArsenalKit.Running(ability.name(), ability.gesture, ability.channel, t);
    }

    /** The axe hurled at the target: it spins out, then comes back to his hand. */
    private static Timeline headsmansToss(ArsenalKit.Cast c) {
        final int flight = 12;
        Vector[] path = new Vector[2];
        Vector[] last = new Vector[1];
        Set<UUID> struckOut = new HashSet<>();
        Set<UUID> struckBack = new HashSet<>();
        Timeline t = new Timeline();
        t.span(0, 12, (tick, p) -> {
            c.face(c.aim());
            if (tick % 3 == 0) c.fx.gather(c.hand(), 1.4, 3, Palette.BLOOD, 6);
        });
        t.at(12, () -> {
            path[0] = c.hand();
            path[1] = c.aim().add(new Vector(0, 1.0, 0));
            last[0] = path[0].clone();
            c.fx.sound(path[0], Sfx.TRIDENT_THROW, 1.8f, 0.6f);
        });
        t.span(12, 12 + flight * 2, (tick, p) -> {
            boolean out = tick < flight;
            double f = out ? (tick + 1) / (double) flight : (tick - flight + 1) / (double) flight;
            Vector from = out ? path[0] : path[1];
            Vector to = out ? path[1] : c.hand();
            Vector at = from.clone().add(to.clone().subtract(from).multiply(f));
            drawAxe(c, at, ArsenalKit.towards(from, to), tick * 0.9);
            if (tick % 3 == 0) c.fx.sound(at, Sfx.PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
            Area area = Area.segment(last[0], at, 1.2);
            last[0] = at;
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                if (!(out ? struckOut : struckBack).add(pl.getUniqueId())) continue;
                c.hit(pl, 14);
                c.afflict(pl, PotionEffectType.WITHER, 40, 0);
                c.fx.impact(pl.getLocation().toVector().add(new Vector(0, 1, 0)), Palette.BLOOD, 1.0);
            }
        });
        t.at(12 + flight * 2, () -> c.fx.sound(c.hand(), Sfx.ANVIL_PLACE, 0.8f, 1.6f));
        return t;
    }

    /** A spinning axe in particles: the haft and the blade's edge. */
    private static void drawAxe(ArsenalKit.Cast c, Vector at, Vector heading, double spin) {
        Vector up = Shapes.UP;
        Vector tip = Shapes.onCircle(at, 0.8, spin, heading, up);
        Vector butt = Shapes.onCircle(at, 0.8, spin + Math.PI, heading, up);
        c.fx.line(butt, tip, 0.2, c.fx.dust(Palette.ASH, 1.1f));
        c.fx.draw(Shapes.arc(tip, 0.45, spin - 1.2, spin + 1.2, 7, heading, up), c.fx.dust(Palette.BLOOD, 1.3f));
        c.fx.particle(Particle.CRIT).at(tip);
    }

    /** A pool of blood spreads around him: it slows and withers, and every pulse feeds him. */
    private static Timeline bloodPool(ArsenalKit.Cast c) {
        final double radius = 5;
        Vector[] center = new Vector[1];
        Timeline t = new Timeline();
        t.at(0, () -> {
            center[0] = c.ground(c.feet());
            c.fx.sound(center[0], Sfx.WARDEN_HEARTBEAT, 2f, 0.5f);
        });
        t.span(0, 104, (tick, p) -> {
            double r = Math.min(radius, 1 + (radius - 1) * tick / 24.0);
            Vector floor = center[0].clone().add(new Vector(0, 0.12, 0));
            if (tick % 2 == 0) {
                c.fx.disc(floor, r, 0.9, c.fx.dust(Palette.BLOOD, 1.2f).sometimes(0.45));
                c.fx.ring(floor, r, 0.6, tick * 0.1, c.fx.dust(Palette.mix(Palette.BLOOD, Palette.ASH, 0.4), 1.0f));
            }
            if (tick < 20 || tick % 10 != 0) return;
            c.fx.sound(center[0], Sfx.BLOCK_LAVA_POP, 1.0f, 0.5f);
            Area area = Area.cylinder(center[0], r, 1, 2.5);
            int fed = 0;
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 3);
                c.afflict(pl, PotionEffectType.SLOWNESS, 30, 1);
                c.afflict(pl, PotionEffectType.WITHER, 40, 0);
                c.fx.line(pl.getLocation().toVector().add(new Vector(0, 1, 0)), c.chest(), 0.5, c.fx.dust(Palette.BLOOD, 0.9f));
                fed++;
            }
            if (fed > 0) c.host.heal(6.0 * fed);
        });
        return t;
    }

    /** A ring of spikes around the target closes in; whoever is still inside when it shuts is impaled. */
    private static Timeline ironMaiden(ArsenalKit.Cast c) {
        final double open = 3.5;
        final double shut = 0.6;
        Vector[] center = new Vector[1];
        Map<UUID, Integer> scraped = new HashMap<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            center[0] = c.ground(c.aim());
            c.fx.sound(center[0], Sfx.IRON_DOOR_CLOSE, 1.4f, 0.5f);
        });
        t.span(0, 14, (tick, p) -> {
            c.face(center[0]);
            if (tick % 2 == 0) ArsenalKit.warnCircle(c.fx, center[0], open, p);
        });
        t.span(14, 38, (tick, p) -> {
            int now = 14 + tick;
            double r = open - (open - shut) * p;
            if (tick % 2 == 0) {
                for (int k = 0; k < 12; k++) {
                    Vector base = Shapes.onCircle(center[0], r, k * Math.PI / 6, Shapes.FLAT_U, Shapes.FLAT_V);
                    c.fx.line(base, base.clone().add(new Vector(0, 1.8, 0)), 0.3, c.fx.dust(Palette.STONE, 1.2f));
                    c.fx.particle(Particle.CRIT).at(base.clone().add(new Vector(0, 1.9, 0)));
                }
                ArsenalKit.warnCircle(c.fx, center[0], r, p);
            }
            if (tick % 6 == 0) c.fx.sound(center[0], Sfx.CHAIN_BREAK, 1.0f, 0.6f + (float) p);
            for (Player pl : c.players()) {
                Vector at = pl.getLocation().toVector();
                if (Math.abs(ArsenalKit.flatDistance(at, center[0]) - r) > 0.5 || Math.abs(at.getY() - center[0].getY()) > 2) continue;
                Integer last = scraped.get(pl.getUniqueId());
                if (last != null && now - last < 10) continue;
                scraped.put(pl.getUniqueId(), now);
                c.hit(pl, 4);
            }
        });
        t.at(38, () -> {
            c.fx.impact(center[0].clone().add(new Vector(0, 1, 0)), Palette.BLOOD, 1.8);
            c.fx.sound(center[0], Sfx.ANVIL_LAND, 1.6f, 0.6f);
            c.fx.sound(center[0], Sfx.BONE_BREAK, 2f, 0.6f);
            Area area = Area.cylinder(center[0], 1.6, 1, 3);
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 18);
                c.afflict(pl, PotionEffectType.SLOWNESS, 60, 2);
            }
        });
        t.hold(44);
        return t;
    }

    /** A noose hunts the target for a moment; if it catches, it hoists them up and drops them. */
    private static Timeline noose(ArsenalKit.Cast c) {
        Vector[] loop = new Vector[1];
        boolean[] caught = new boolean[1];
        Timeline t = new Timeline();
        t.at(0, () -> {
            loop[0] = c.ground(c.aim());
            c.fx.sound(loop[0], Sfx.ZOMBIE_WOODEN_DOOR, 1.2f, 0.5f);
        });
        t.span(0, 16, (tick, p) -> {
            c.face(loop[0]);
            if (c.targetHere()) {
                Vector want = c.target.getLocation().toVector().setY(loop[0].getY());
                Vector step = want.subtract(loop[0]);
                if (step.length() > 0.22) step.normalize().multiply(0.22);
                loop[0] = c.ground(loop[0].clone().add(step));
            }
            drawNoose(c, loop[0].clone().add(new Vector(0, 2.0, 0)));
            if (tick % 2 == 0) ArsenalKit.warnCircle(c.fx, loop[0], 1.3, p);
        });
        t.at(16, () -> {
            if (!c.targetHere()) return;
            Vector at = c.target.getLocation().toVector();
            if (ArsenalKit.flatDistance(at, loop[0]) > 1.3 || Math.abs(at.getY() - loop[0].getY()) > 2) {
                c.fx.sound(loop[0], Sfx.CHAIN_BREAK, 1.2f, 1.4f);
                return;
            }
            caught[0] = true;
            c.hit(c.target, 4);
            c.afflict(c.target, PotionEffectType.LEVITATION, 20, 2);
            c.fx.sound(at, Sfx.IRON_DOOR_CLOSE, 1.6f, 0.8f);
        });
        t.span(16, 36, (tick, p) -> {
            if (caught[0] && c.targetHere()) drawNoose(c, c.target.getLocation().toVector().add(new Vector(0, 1.6, 0)));
        });
        t.at(36, () -> {
            if (!caught[0] || !c.targetHere()) return;
            c.target.removePotionEffect(PotionEffectType.LEVITATION);
            c.target.setVelocity(new Vector(0, -1.6, 0));
            c.hit(c.target, 10);
            c.afflict(c.target, PotionEffectType.SLOWNESS, 60, 1);
            c.fx.sound(c.target.getLocation().toVector(), Sfx.BONE_BREAK, 2f, 0.6f);
        });
        t.hold(42);
        return t;
    }

    /** The rope from high above down to its loop at {@code neck}. */
    private static void drawNoose(ArsenalKit.Cast c, Vector neck) {
        c.fx.line(neck.clone().add(new Vector(0, 0.4, 0)), neck.clone().add(new Vector(0, 6, 0)), 0.4, c.fx.dust(Palette.ASH, 1.0f));
        c.fx.draw(Shapes.circle(neck, 0.4, 10, Shapes.FLAT_U, Shapes.FLAT_V, 0), c.fx.dust(Palette.mix(Palette.ASH, Palette.BLOOD, 0.4), 1.1f));
    }

    /** Three heavy marching steps towards the target; every landing is a shockwave. */
    private static Timeline march(ArsenalKit.Cast c) {
        final double radius = 3.2;
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.RAVAGER_ROAR, 1.6f, 0.7f));
        for (int k = 0; k < 3; k++) {
            int step = 8 + 16 * k;
            t.span(step - 8, step, (tick, p) -> {
                if (tick % 2 != 0) return;
                Vector landing = c.ground(c.feet().add(ArsenalKit.towards(c.feet(), c.aim()).multiply(3)));
                ArsenalKit.warnCircle(c.fx, landing, radius, p);
            });
            t.span(step, step + 6, (tick, p) -> {
                Location loc = c.stand.getLocation();
                Vector dir = ArsenalKit.towards(loc.toVector(), c.aim());
                BossArena.walk(loc, dir.clone().multiply(0.5), true);
                loc.setDirection(dir);
                c.stand.teleport(loc);
                c.fx.cloud(Particle.CLOUD, c.feet(), 2, 0.4, 0.02);
            });
            t.at(step + 6, () -> {
                Vector center = c.feet();
                c.fx.flatBurst(center, Particle.CLOUD, 24, 0.4);
                c.fx.crumble(Material.STONE, 20, radius * 0.4).at(center.clone().add(new Vector(0, 0.3, 0)));
                c.fx.ring(center.clone().add(new Vector(0, 0.2, 0)), radius, 0.5, 0, c.fx.dust(Palette.ASH, 1.6f));
                c.fx.sound(center, Sfx.MACE_SMASH_GROUND, 1.8f, 0.6f);
                Area area = Area.cylinder(center, radius, 1.5, 3);
                for (Player pl : c.players()) {
                    if (!area.contains(pl.getLocation().toVector())) continue;
                    c.hit(pl, 10);
                    c.push(pl, ArsenalKit.towards(center, pl.getLocation().toVector()).multiply(0.5).setY(0.4));
                }
            });
        }
        t.hold(56);
        return t;
    }

    /** Wooden stocks over up to three players: whoever has not stepped out is locked in place. */
    private static Timeline pillory(ArsenalKit.Cast c) {
        final double radius = 1.4;
        List<Vector> spots = new ArrayList<>();
        List<Player> locked = new ArrayList<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            for (Player p : c.players()) {
                if (spots.size() >= 3) break;
                if (p.getLocation().distanceSquared(c.stand.getLocation()) > 22 * 22) continue;
                spots.add(c.ground(p.getLocation().toVector()));
            }
            c.fx.sound(c.feet(), Sfx.ZOMBIE_WOODEN_DOOR, 1.4f, 0.7f);
        });
        t.span(0, 16, (tick, p) -> {
            c.face(c.aim());
            if (tick % 2 != 0) return;
            for (Vector spot : spots) {
                ArsenalKit.warnCircle(c.fx, spot, radius, p);
                drawStocks(c, spot.clone().add(new Vector(0, 1.5, 0)), ArsenalKit.towards(spot, c.feet()));
            }
        });
        t.at(16, () -> {
            for (Vector spot : spots) {
                for (Player pl : c.players()) {
                    Vector at = pl.getLocation().toVector();
                    if (ArsenalKit.flatDistance(at, spot) > radius || Math.abs(at.getY() - spot.getY()) > 2 || locked.contains(pl)) continue;
                    locked.add(pl);
                    c.hit(pl, 6);
                    c.afflict(pl, PotionEffectType.SLOWNESS, 60, 5);
                    c.afflict(pl, PotionEffectType.WEAKNESS, 60, 1);
                    c.fx.sound(at, Sfx.ANVIL_LAND, 1.2f, 1.2f);
                }
            }
        });
        t.span(16, 76, (tick, p) -> {
            if (tick % 2 != 0) return;
            for (Player pl : locked) {
                if (!ArsenalKit.valid(pl)) continue;
                Vector at = pl.getLocation().toVector();
                drawStocks(c, at.clone().add(new Vector(0, 1.5, 0)), Shapes.flat(pl.getLocation().getDirection()));
            }
        });
        t.hold(78);
        return t;
    }

    /** A plank with a neck hole, held up by two posts. */
    private static void drawStocks(ArsenalKit.Cast c, Vector neck, Vector facing) {
        Vector side = ArsenalKit.rotateFlat(facing, Math.PI / 2);
        Vector floor = neck.clone().subtract(new Vector(0, 1.5, 0));
        c.fx.line(neck.clone().add(side.clone().multiply(0.9)), neck.clone().subtract(side.clone().multiply(0.9)), 0.2,
                c.fx.dust(Palette.mix(Palette.ASH, Palette.MOLTEN, 0.3), 1.3f));
        for (int s = -1; s <= 1; s += 2) {
            Vector post = floor.clone().add(side.clone().multiply(0.9 * s));
            c.fx.line(post, post.clone().add(new Vector(0, 1.7, 0)), 0.35, c.fx.dust(Palette.ASH, 1.2f));
        }
    }

    /** One drop of Crimson Rain, warned from tick {@code born}. */
    private record Drop(Vector at, int born) {
    }

    /** Blood falls from the sky in telegraphed drops around the players. */
    private static Timeline crimsonRain(ArsenalKit.Cast c) {
        final int warn = 12;
        final int fall = 3;
        List<Drop> drops = new ArrayList<>();
        Timeline t = new Timeline();
        t.at(0, () -> c.fx.sound(c.feet(), Sfx.ELDER_GUARDIAN_CURSE, 1.0f, 0.5f));
        t.span(0, 80, (tick, p) -> {
            if (tick >= 8 && tick < 60 && tick % 3 == 0) {
                List<Player> players = c.players();
                Vector spot;
                if (!players.isEmpty() && c.random.nextInt(100) < 60) {
                    Player aim = players.get(c.random.nextInt(players.size()));
                    spot = aim.getLocation().toVector().add(new Vector(c.random.nextGaussian() * 1.8, 0, c.random.nextGaussian() * 1.8));
                } else {
                    spot = c.feet().add(Shapes.heading(c.random.nextDouble() * Math.PI * 2).multiply(2 + c.random.nextDouble() * 8));
                }
                drops.add(new Drop(c.ground(spot), tick));
            }
            if (tick < 24 && tick % 3 == 0) c.fx.gather(c.chest().add(new Vector(0, 2, 0)), 2.0, 3, Palette.BLOOD, 6);
            for (Drop d : drops) {
                int age = tick - d.born();
                if (age < 0 || age > warn + fall) continue;
                if (age < warn) {
                    if (age % 2 == 0) ArsenalKit.warnCircle(c.fx, d.at(), 1.3, age / (double) warn);
                    continue;
                }
                double f = (age - warn + 1) / (double) fall;
                Vector bottom = d.at().clone().add(new Vector(0, 12 * (1 - f), 0));
                c.fx.line(bottom, bottom.clone().add(new Vector(0, 2, 0)), 0.3, c.fx.dust(Palette.BLOOD, 1.6f));
                if (age < warn + fall) continue;
                c.fx.flatBurst(d.at().clone().add(new Vector(0, 0.2, 0)), Particle.DAMAGE_INDICATOR, 6, 0.2);
                c.fx.dust(Palette.BLOOD, 2f, 0.5, 8).at(d.at().clone().add(new Vector(0, 0.3, 0)));
                c.fx.sound(d.at(), Sfx.BLOCK_LAVA_POP, 1.0f, 0.8f);
                Area area = Area.cylinder(d.at(), 1.3, 1, 3);
                for (Player pl : c.players()) {
                    if (!area.contains(pl.getLocation().toVector())) continue;
                    c.hit(pl, 6);
                    c.afflict(pl, PotionEffectType.WITHER, 40, 0);
                }
            }
        });
        return t;
    }

    /** Two blades cross in an X over the target. */
    private static Timeline severingCross(ArsenalKit.Cast c) {
        final double half = 6;
        List<Vector[]> lines = new ArrayList<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            Vector center = c.ground(c.aim());
            Vector dir = ArsenalKit.towards(c.feet(), center);
            for (double turn : new double[]{Math.PI / 4, -Math.PI / 4}) {
                Vector d = ArsenalKit.rotateFlat(dir, turn).multiply(half);
                lines.add(new Vector[]{center.clone().subtract(d), center.clone().add(d)});
            }
            c.fx.sound(center, Sfx.PLAYER_ATTACK_SWEEP, 1.4f, 0.5f);
        });
        t.span(0, 24, (tick, p) -> {
            c.face(c.aim());
            if (tick % 2 != 0) return;
            for (Vector[] line : lines) ArsenalKit.warnLine(c.fx, line[0], line[1], 2.2, p);
        });
        t.at(26, () -> {
            Area area = null;
            for (Vector[] line : lines) {
                Vector lift = new Vector(0, 0.9, 0);
                c.fx.line(line[0].clone().add(lift), line[1].clone().add(lift), 0.25,
                        c.fx.fade(Palette.BLOOD, Palette.ASH, 1.8f).and(c.fx.particle(Particle.SWEEP_ATTACK).sometimes(0.15)));
                Area one = Area.segment(line[0], line[1], 1.1);
                area = area == null ? one : area.or(one);
                c.fx.sound(line[1], Sfx.PLAYER_ATTACK_STRONG, 1.6f, 0.6f);
            }
            if (area == null) return;
            for (Player pl : c.players()) {
                if (!area.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 16);
                c.afflict(pl, PotionEffectType.WITHER, 60, 1);
            }
        });
        t.hold(32);
        return t;
    }

    /** A bell tolls three times; then the wounded are executed and the rest only bleed. */
    private static Timeline lastRites(ArsenalKit.Cast c) {
        final double range = 22;
        final int[] tolls = {6, 20, 34};
        Timeline t = new Timeline();
        for (int toll : tolls) {
            t.at(toll, () -> {
                c.fx.sound(c.feet(), Sfx.BELL, 2f, 0.5f);
                c.fx.sound(c.feet(), Sfx.BELL_RESONATE, 1.4f, 0.6f);
            });
            t.span(toll, toll + 10, (tick, p) -> {
                if (tick % 2 == 0) c.fx.ring(c.feet().add(new Vector(0, 0.2, 0)), 1 + range * p, 1.4, tick, c.fx.dust(Palette.VOID_DEEP, 1.4f));
            });
        }
        t.span(0, 40, (tick, p) -> {
            if (tick % 4 != 0) return;
            for (Player pl : c.players()) {
                if (pl.getLocation().distanceSquared(c.stand.getLocation()) > range * range || !wounded(pl)) continue;
                c.fx.ring(pl.getLocation().toVector().add(new Vector(0, 2.3, 0)), 0.35, 0.2, tick * 0.3, c.fx.dust(Palette.BLOOD, 1.2f));
            }
        });
        t.at(40, () -> {
            c.fx.sound(c.feet(), Sfx.WITHER_AMBIENT, 2f, 0.5f);
            for (Player pl : c.players()) {
                if (pl.getLocation().distanceSquared(c.stand.getLocation()) > range * range) continue;
                if (wounded(pl)) {
                    c.hit(pl, 15);
                    c.afflict(pl, PotionEffectType.WITHER, 60, 1);
                    c.fx.impact(pl.getLocation().toVector().add(new Vector(0, 1, 0)), Palette.BLOOD, 1.2);
                } else {
                    c.hit(pl, 6);
                }
                c.afflict(pl, PotionEffectType.DARKNESS, 40, 0);
            }
        });
        t.hold(50);
        return t;
    }

    /** Whether a player is at half health or below: the ones Last Rites executes. */
    private static boolean wounded(Player p) {
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max != null ? max.getValue() : 20.0;
        return p.getHealth() <= cap * 0.5;
    }

    /** Two chains swung around him twice; each catch drags the player in. */
    private static Timeline chainWhirl(ArsenalKit.Cast c) {
        final double length = 7;
        final int revolution = 18;
        Vector[] center = new Vector[1];
        double[] base = new double[1];
        Map<UUID, Integer> lastHit = new HashMap<>();
        Timeline t = new Timeline();
        t.at(0, () -> {
            center[0] = c.feet();
            Vector dir = c.facing();
            base[0] = Math.atan2(dir.getZ(), dir.getX());
            c.fx.sound(center[0], Sfx.CHAIN_BREAK, 1.6f, 0.5f);
        });
        t.span(0, 12, (tick, p) -> {
            if (tick % 2 == 0) ArsenalKit.warnCircle(c.fx, center[0], length, p);
        });
        t.span(12, 12 + revolution * 2, (tick, p) -> {
            int now = 12 + tick;
            double angle = base[0] + tick * 2 * Math.PI / revolution;
            Vector hub = center[0].clone().add(new Vector(0, 1.2, 0));
            Vector low = center[0].clone().add(new Vector(0, 0.5, 0));
            for (int k = 0; k < 2; k++) {
                Vector heading = Shapes.heading(angle + k * Math.PI);
                Vector tip = hub.clone().add(heading.clone().multiply(length));
                c.fx.line(hub, tip, 0.5, c.fx.dust(tick % 2 == 0 ? Palette.STONE : Palette.ASH, 1.2f));
                c.fx.particle(Particle.CRIT, 3, 0.1, 0.05).at(tip);
                Area area = Area.segment(low, low.clone().add(heading.multiply(length)), 1.0);
                for (Player pl : c.players()) {
                    if (!area.contains(pl.getLocation().toVector())) continue;
                    Integer last = lastHit.get(pl.getUniqueId());
                    if (last != null && now - last < revolution - 2) continue;
                    lastHit.put(pl.getUniqueId(), now);
                    c.hit(pl, 8);
                    c.push(pl, ArsenalKit.towards(pl.getLocation().toVector(), center[0]).multiply(0.8).setY(0.3));
                }
            }
            if (tick % 4 == 0) c.fx.sound(center[0], Sfx.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f + (float) p * 0.4f);
        });
        t.hold(52);
        return t;
    }

    // ------------------------------------------------------------------ destructive

    /**
     * Grand Guillotine: a guillotine sixteen blocks wide rises across the target, its blade hanging
     * twelve blocks up and shaking for three seconds, then it drops along the whole length of the
     * frame. Step out to the sides.
     */
    private static Timeline grandGuillotine(ArsenalKit.Cast c) {
        final int rise = 20;
        final int drop = 80;
        final int fall = 6;
        final double half = 8;
        final double top = 12;
        Vector center = c.ground(c.aim());
        Vector along = ArsenalKit.rotateFlat(ArsenalKit.towards(c.feet(), center), Math.PI / 2);
        Vector a = center.clone().subtract(along.clone().multiply(half));
        Vector b = center.clone().add(along.clone().multiply(half));
        Timeline t = new Timeline();
        t.at(0, () -> {
            c.fx.sound(center, Sfx.ZOMBIE_WOODEN_DOOR, 3f, 0.4f);
            ArsenalKit.hud(c.world, center, 50, "⚖ GRAND GUILLOTINE", "Get off the line before the blade falls");
        });
        t.span(0, drop, (tick, p) -> {
            double up = Math.min(1, tick / (double) rise);
            Fx.Brush wood = c.fx.dust(Palette.ASH, 1.6f);
            for (Vector post : new Vector[]{a, b}) c.fx.line(post, post.clone().add(new Vector(0, top * up + 1, 0)), 0.4, wood);
            c.fx.line(a.clone().add(new Vector(0, top * up + 1, 0)), b.clone().add(new Vector(0, top * up + 1, 0)), 0.4, c.fx.dust(Palette.BLOOD, 1.6f));
            double shake = tick > rise ? Math.sin(tick * 1.7) * 0.15 : 0;
            Vector bladeAt = new Vector(0, top * up + shake, 0);
            c.fx.line(a.clone().add(bladeAt), b.clone().add(bladeAt).add(new Vector(0, -0.8, 0)), 0.25, c.fx.dust(Palette.ICE, 1.8f));
            if (tick % 2 == 0) ArsenalKit.warnLine(c.fx, a, b, 3.2, p);
            if (tick % 16 == 0) c.fx.sound(center, Sfx.WARDEN_HEARTBEAT, 2.5f, 0.5f + (float) p);
            if (tick == drop - 3) c.fx.sound(center, Sfx.CHAIN_BREAK, 3f, 0.5f);
        });
        t.span(drop, drop + fall, (tick, p) -> {
            double y = top * (1 - (tick + 1) / (double) fall);
            c.fx.line(a.clone().add(new Vector(0, y, 0)), b.clone().add(new Vector(0, y - 0.8, 0)), 0.2,
                    c.fx.dust(Palette.ICE, 2.2f).and(c.fx.particle(org.bukkit.Particle.CRIT).sometimes(0.3)));
        });
        t.at(drop + fall, () -> {
            for (double d = 0; d <= half * 2; d += 1.5) {
                Vector at = a.clone().add(along.clone().multiply(d)).add(new Vector(0, 0.4, 0));
                c.fx.crumble(Material.REDSTONE_BLOCK, 10, 0.5).at(at);
                c.fx.dust(Palette.BLOOD, 2.2f, 0.4, 4).at(at);
            }
            c.fx.impact(center.clone().add(new Vector(0, 0.6, 0)), Palette.BLOOD, 3);
            c.fx.sound(center, Sfx.ANVIL_LAND, 3f, 0.4f);
            c.fx.sound(center, Sfx.EXPLODE, 2.5f, 0.6f);
            Area line = Area.segment(a, b, 1.9);
            for (Player pl : c.players()) {
                if (!line.contains(pl.getLocation().toVector())) continue;
                c.hit(pl, 26);
                c.afflict(pl, PotionEffectType.WITHER, 80, 1);
                c.push(pl, new Vector(0, 0.6, 0));
            }
        });
        t.hold(drop + fall + 16);
        return t;
    }

    /**
     * Blood Moon: a red moon climbs over the arena for three seconds while the light dies, then three
     * waves of blood roll out from Nix across twenty-two blocks — low enough to jump, one after another.
     */
    private static Timeline bloodMoon(ArsenalKit.Cast c) {
        final int rise = 60;
        final double reach = 22;
        final int[] waves = {70, 86, 102};
        final int travel = 20;
        Vector center = c.ground(c.feet());
        Timeline t = new Timeline();
        t.at(0, () -> {
            c.fx.sound(center, Sfx.WITHER_SPAWN, 2f, 0.4f);
            ArsenalKit.hud(c.world, center, 50, "🌑 BLOOD MOON", "Jump the three waves");
            for (Player pl : c.players()) c.afflict(pl, PotionEffectType.DARKNESS, 140, 0);
        });
        t.span(0, waves[2] + travel, (tick, p) -> {
            double up = Math.min(1, tick / (double) rise);
            Vector moon = center.clone().add(new Vector(0, 6 + 16 * up, 0));
            c.fx.draw(Shapes.sphere(moon, 1 + 3 * up, (int) (30 + 60 * up)), c.fx.dust(Palette.BLOOD, 2.6f));
            if (tick % 3 == 0) c.fx.cloud(org.bukkit.Particle.DAMAGE_INDICATOR, moon, 2, 2, 0);
            if (tick < rise && tick % 2 == 0) c.fx.ring(center.clone().add(new Vector(0, 0.2, 0)), reach, 1.2, tick * 0.02,
                    c.fx.dust(Palette.mix(Palette.WARNING, Palette.WARNING_HOT, up), 1.4f));
            if (tick % 20 == 0) c.fx.sound(center, Sfx.WARDEN_HEARTBEAT, 3f, 0.4f + (float) up * 0.4f);
        });
        for (int start : waves) {
            Set<UUID> struck = new HashSet<>();
            t.at(start, () -> c.fx.sound(center, Sfx.RAVAGER_ROAR, 2f, 0.6f));
            t.span(start, start + travel, (tick, p) -> {
                double r = 1 + (reach - 1) * p;
                Vector floor = center.clone().add(new Vector(0, 0.3, 0));
                c.fx.ring(floor, r, 0.5, tick, c.fx.dust(Palette.BLOOD, 2.2f).and(c.fx.particle(org.bukkit.Particle.DAMAGE_INDICATOR).sometimes(0.1)));
                c.fx.ring(floor.clone().add(new Vector(0, 0.6, 0)), r, 0.9, -tick, c.fx.dust(Palette.mix(Palette.BLOOD, Palette.ASH, 0.4), 1.6f));
                Area band = Area.ring(center, r - 1.0, r + 1.0, 1.0);
                for (Player pl : c.players()) {
                    if (!band.contains(pl.getLocation().toVector()) || !struck.add(pl.getUniqueId())) continue;
                    c.hit(pl, 14);
                    c.afflict(pl, PotionEffectType.WITHER, 60, 1);
                    c.push(pl, Shapes.flat(pl.getLocation().toVector().subtract(center)).multiply(0.8).setY(0.5));
                }
            });
        }
        return t;
    }

    /**
     * Execution Day: eight giant axes rise in a ring around the arena, spin, and sweep in along their
     * spokes to the centre — then a second ring, turned half a spoke, does it again. Stand between
     * the spokes and move between the waves.
     */
    private static Timeline executionDay(ArsenalKit.Cast c) {
        final int axes = 8;
        final double ring = 16;
        final int warn = 50;
        final int sweep = 26;
        Vector center = c.ground(c.feet());
        Timeline t = new Timeline();
        t.at(0, () -> {
            c.fx.sound(center, Sfx.BELL_RESONATE, 3f, 0.4f);
            ArsenalKit.hud(c.world, center, 50, "🪓 EXECUTION DAY", "Stand between the spokes");
        });
        for (int wave = 0; wave < 2; wave++) {
            double offset = wave * Math.PI / axes;
            int start = wave * (warn + sweep - 14);
            Set<UUID> struck = new HashSet<>();
            t.span(start, start + warn, (tick, p) -> {
                if (tick % 2 != 0) return;
                for (int k = 0; k < axes; k++) {
                    Vector spoke = Shapes.heading(offset + 2 * Math.PI * k / axes);
                    Vector out = center.clone().add(spoke.clone().multiply(ring));
                    ArsenalKit.warnLine(c.fx, out, center, 2.6, p);
                    drawAxe(c, out.clone().add(new Vector(0, 2.5 * p + 0.5, 0)), spoke.clone().multiply(-1), tick * 0.4, 2.4);
                }
                if (tick % 10 == 0) c.fx.sound(center, Sfx.CHAIN_BREAK, 2f, 0.5f + (float) p);
            });
            t.span(start + warn, start + warn + sweep, (tick, p) -> {
                for (int k = 0; k < axes; k++) {
                    Vector spoke = Shapes.heading(offset + 2 * Math.PI * k / axes);
                    double before = ring * (1 - tick / (double) sweep);
                    double after = ring * (1 - (tick + 1) / (double) sweep);
                    Vector at = center.clone().add(spoke.clone().multiply(after)).add(new Vector(0, 1.5, 0));
                    drawAxe(c, at, spoke.clone().multiply(-1), tick * 1.1, 2.4);
                    if (tick % 3 == 0) c.fx.sound(at, Sfx.PLAYER_ATTACK_SWEEP, 1.4f, 0.6f);
                    Area path = Area.segment(center.clone().add(spoke.clone().multiply(before)), center.clone().add(spoke.clone().multiply(after)), 1.6);
                    for (Player pl : c.players()) {
                        if (!path.contains(pl.getLocation().toVector()) || !struck.add(pl.getUniqueId())) continue;
                        c.hit(pl, 18);
                        c.afflict(pl, PotionEffectType.SLOWNESS, 50, 2);
                        c.push(pl, ArsenalKit.rotateFlat(spoke, Math.PI / 2).multiply(0.9).setY(0.5));
                    }
                }
            });
        }
        t.at(2 * (warn + sweep) - 14, () -> {
            c.fx.impact(center.clone().add(new Vector(0, 1, 0)), Palette.BLOOD, 3);
            c.fx.sound(center, Sfx.MACE_SMASH_GROUND, 3f, 0.5f);
        });
        t.hold(2 * (warn + sweep) - 4);
        return t;
    }

    /** A spinning axe {@code size} times the size of the thrown one. */
    private static void drawAxe(ArsenalKit.Cast c, Vector at, Vector heading, double spin, double size) {
        Vector up = Shapes.UP;
        Vector tip = Shapes.onCircle(at, 0.8 * size, spin, heading, up);
        Vector butt = Shapes.onCircle(at, 0.8 * size, spin + Math.PI, heading, up);
        c.fx.line(butt, tip, 0.25, c.fx.dust(Palette.ASH, 1.4f));
        c.fx.draw(Shapes.arc(tip, 0.45 * size, spin - 1.2, spin + 1.2, 12, heading, up), c.fx.dust(Palette.BLOOD, 1.6f));
        c.fx.particle(org.bukkit.Particle.CRIT).at(tip);
    }
}
