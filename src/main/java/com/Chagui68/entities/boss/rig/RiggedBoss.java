package com.Chagui68.entities.boss.rig;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.boss.BossArena;
import com.Chagui68.entities.boss.BossDespawn;
import com.Chagui68.utils.MscBossBar;
import com.Chagui68.utils.MscEntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * A boss drawn with a {@link RigForm}: a body of block displays on a skeleton, fighting with
 * keyframed moves.
 *
 * <p>An invisible armour stand carries the hitbox, the health and the boss bar. The displays stand
 * at an anchor and are placed by a carry matrix ({@link Rig#bones(Rig.Move, float, Matrix4f)}), so
 * walking and turning move the pieces by their interpolated transformation instead of teleporting
 * each one; the anchor catches up when the stand strays {@link #REANCHOR_DISTANCE} from it. The
 * displays are never saved: after a restart or a chunk reload the stand is dressed again, in the
 * phase it was in.</p>
 *
 * <p>Between attacks the boss turns to its target at a limited rate, runs to it from afar, walks
 * the last stretch, circles it while its next attack is not ready, backs off when crowded and may
 * dodge a blow. During an attack its feet walk the root's travel over the ground (walls stop it,
 * ledges drop it) and it keeps turning to its target until the blow starts. Its weapon hurts whoever
 * it passes through (once per swing), slashes hurt everyone near, landings shake the ground, and
 * cues are left to the boss.</p>
 */
public abstract class RiggedBoss implements Listener {

    /** What a boss is like when its config says nothing. */
    public record Defaults(double health, double maxDamagePerHit, double hitboxScale, double aggroRange, double moveSpeed,
                           int attackCooldownTicks) {
    }

    /** How far the stand may wander from its displays before they are moved to it. */
    public static final double REANCHOR_DISTANCE = 6.0;
    /** Degrees a tick the boss turns to its target between attacks, and while winding one up. */
    private static final float TURN_RATE = 14f, AIM_RATE = 9f;
    private static final NamespacedKey KEY_PHASE = new NamespacedKey("multiversecreatures", "rig_phase");

    protected final MultiverseCreatures plugin;
    protected final Random random = new Random();
    private final String key, tag;
    private final Map<UUID, Instance> active = new HashMap<>();
    private BukkitTask ticker;

    protected final double health, maxDamagePerHit, hitboxScale, aggroRange, moveSpeed;
    protected final int attackCooldownTicks;

    protected RiggedBoss(MultiverseCreatures plugin, String key, String tag, Defaults defaults) {
        this.plugin = plugin;
        this.key = key;
        this.tag = tag;
        health = number("health", defaults.health());
        maxDamagePerHit = number("max-damage-per-hit", defaults.maxDamagePerHit());
        hitboxScale = MscEntityUtils.clampHitboxScale(number("hitbox-scale", defaults.hitboxScale()));
        aggroRange = number("aggro-range", defaults.aggroRange());
        moveSpeed = number("move-speed", defaults.moveSpeed());
        attackCooldownTicks = (int) number("attack-cooldown-ticks", defaults.attackCooldownTicks());
    }

    /** A number from this boss's section of the config. */
    protected final double number(String name, double fallback) {
        return plugin.getConfig().getDouble("entities." + key + "." + name, fallback);
    }

    // =====================================================================================
    // What a boss is
    // =====================================================================================

    /** Every look of the boss, the first being the one it spawns in. */
    public abstract List<RigForm> forms();

    /** The attack for a target {@code dist} blocks away, or null when none is ready or reaches. */
    protected abstract Rig.Move choose(Instance inst, Player target, double dist);

    protected abstract double weaponDamage(Instance inst);

    /** The slashes round the body, every four ticks while they last. */
    protected double slashDamage(Instance inst) {
        return 0;
    }

    /** A landing's shockwave. */
    protected double ringDamage(Instance inst) {
        return 0;
    }

    protected RigForm form(Instance inst) {
        List<RigForm> forms = forms();
        return forms.get(Math.min(inst.phase, forms.size() - 1));
    }

    protected String title(Instance inst) {
        return ChatColor.RED + form(inst).name();
    }

    /** The distance the boss likes to keep from its target between attacks. */
    protected double reach(Instance inst) {
        return 2.5;
    }

    /** How often a blow taken between attacks makes the boss dodge, when its form has a {@code dodge} move. */
    protected double dodgeChance(Instance inst) {
        return .25;
    }

    /** A cue of the move under way. */
    protected void cue(Instance inst, Rig.Move move, String name) {
    }

    /** After the boss lands a hit of {@code damage} on {@code player}. */
    protected void onHit(Instance inst, Player player, double damage) {
    }

    /** Every tick the boss is loaded, before it thinks or attacks. */
    protected void everyTick(Instance inst) {
    }

    /** A blow that would kill: true when the boss lives on in another phase instead. */
    protected boolean survives(Instance inst) {
        return false;
    }

    /** How fast the boss runs, in blocks a tick; it walks at half of it. */
    protected double speed(Instance inst) {
        return moveSpeed;
    }

    /** A boss has left the world, whatever the reason: slain, despawned, or the plugin stopping. */
    protected void onGone(Instance inst) {
    }

    /** The boss is dead at {@code at}. */
    protected void onSlain(Location at) {
        at.getWorld().playSound(at, Sound.ENTITY_WITHER_DEATH, 1f, 1.4f);
    }

    // =====================================================================================
    // Life cycle
    // =====================================================================================

    /** Starts the boss when its config enables it; the last line of a boss's constructor. */
    protected final void start() {
        if (!plugin.isEnabled("entities." + key)) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        reloadExisting();
        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                for (Instance inst : new ArrayList<>(active.values())) tick(inst);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Stops the tick loop and takes the bodies and bars down; the plugin calls this from {@code onDisable}. */
    public void stopTasks() {
        if (ticker != null) ticker.cancel();
        ticker = null;
        active.values().forEach(this::cleanup);
        active.clear();
    }

    public String key() {
        return key;
    }

    public String tag() {
        return tag;
    }

    /** The tag on every display and prop of this boss, so a reload can sweep what was left behind. */
    public String bodyTag() {
        return tag + "_Body";
    }

    private void reloadExisting() {
        for (World world : Bukkit.getWorlds()) {
            // Displays left by a reload: the stands below get a fresh body.
            for (BlockDisplay display : world.getEntitiesByClass(BlockDisplay.class)) {
                if (display.getScoreboardTags().contains(bodyTag())) display.remove();
            }
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (!stand.getScoreboardTags().contains(tag)) continue;
                if (!stand.getPersistentDataContainer().has(MscEntityUtils.KEY_VIRTUAL_MAX_HEALTH, PersistentDataType.DOUBLE)) {
                    MscEntityUtils.initVirtualHealth(stand, health);
                }
                adopt(stand);
            }
        }
    }

    /** Whether this boss is fighting in {@code world}; rituals and the Boss Dimension's rules ask. */
    public boolean isBossActiveIn(World world) {
        for (Instance inst : active.values()) {
            if (inst.stand.isValid() && inst.stand.getWorld().equals(world)) return true;
        }
        return false;
    }

    public boolean trySpawn(Location location) {
        if (!plugin.isEnabled("entities." + key) || location.getWorld() == null) return false;
        Location at = location.clone();
        at.setPitch(0f);
        BossArena.settle(at);
        ArmorStand stand = at.getWorld().spawn(at, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setBasePlate(false);
            s.setArms(false);
            s.setGravity(false);
            s.setSilent(true);
            s.setCanPickupItems(false);
            s.setMaximumNoDamageTicks(0);
            s.addScoreboardTag(tag);
        });
        MscEntityUtils.applyAmbientPersistence(plugin, stand);
        MscEntityUtils.initVirtualHealth(stand, health);
        AttributeInstance scale = stand.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(hitboxScale);
        dress(adopt(stand));
        at.getWorld().playSound(at, Sound.ITEM_TRIDENT_RETURN, 1.5f, .6f);
        return true;
    }

    private Instance adopt(ArmorStand stand) {
        Instance inst = new Instance(stand);
        inst.phase = stand.getPersistentDataContainer().getOrDefault(KEY_PHASE, PersistentDataType.INTEGER, 0);
        inst.bossBar = MscBossBar.create(title(inst), BarColor.RED, BarStyle.SEGMENTED_10);
        MscBossBar.showInWorld(inst.bossBar, stand.getWorld());
        active.put(stand.getUniqueId(), inst);
        return inst;
    }

    /** A new body at the stand in the form of its phase, replacing whatever is left of the old one. */
    protected final void dress(Instance inst) {
        if (inst.body != null) inst.body.remove();
        inst.anchor = inst.stand.getLocation();
        inst.anchor.setPitch(0f);
        inst.body = RigBody.spawn(form(inst).pieces(), inst.anchor, inst.anchor.getYaw(), bodyTag());
        inst.resting = false;
    }

    /** Takes the boss's body out of sight, or brings it back; a body dressed anew is always in sight. */
    protected final void hide(Instance inst, boolean hidden) {
        if (inst.body != null) inst.body.hide(hidden);
    }

    /** Moves the boss to {@code phase}: remembered on the stand, shown on the bar, drawn by {@link #dress}. */
    protected final void setPhase(Instance inst, int phase) {
        inst.phase = phase;
        inst.stand.getPersistentDataContainer().set(KEY_PHASE, PersistentDataType.INTEGER, phase);
        if (inst.bossBar != null) inst.bossBar.setTitle(title(inst));
    }

    private void cleanup(Instance inst) {
        if (inst.body != null) inst.body.remove();
        inst.body = null;
        inst.hazards.forEach(Hazard::end);
        inst.hazards.clear();
        if (inst.bossBar != null) inst.bossBar.removeAll();
        inst.bossBar = null;
        onGone(inst);
    }

    // =====================================================================================
    // Every tick
    // =====================================================================================

    private void tick(Instance inst) {
        ArmorStand stand = inst.stand;
        if (stand.isDead() || !stand.isValid()) {
            cleanup(inst);
            active.remove(stand.getUniqueId());
            return;
        }
        if (!stand.getLocation().isChunkLoaded()) return;
        // Not while it changes phase: a change may send everyone running from it.
        if (!inst.busy && BossDespawn.abandoned(stand)) {
            cleanup(inst);
            active.remove(stand.getUniqueId());
            stand.remove();
            return;
        }
        // Not saved with the world: a chunk unload takes the displays and leaves the stand.
        if (inst.body == null || !inst.body.valid()) dress(inst);
        inst.ticks++;
        if (inst.cooldown > 0) inst.cooldown--;
        // A copy: a hazard may leave another behind it.
        for (Hazard hazard : new ArrayList<>(inst.hazards)) {
            if (hazard.tick()) continue;
            hazard.end();
            inst.hazards.remove(hazard);
        }
        everyTick(inst);
        if (inst.move != null) attack(inst);
        else think(inst);

        if (inst.bossBar != null) {
            inst.bossBar.setProgress(MscEntityUtils.calculateVirtualProgress(
                    MscEntityUtils.getVirtualHealth(stand), MscEntityUtils.getVirtualMaxHealth(stand)));
            if (inst.ticks % 20 == 0) MscBossBar.showInWorld(inst.bossBar, stand.getWorld());
        }
    }

    /** Between attacks: turn to the nearest player, start an attack in reach, or close in, circle or back off. */
    private void think(Instance inst) {
        Location at = inst.stand.getLocation();
        at.setPitch(0f);
        RigForm form = form(inst);
        Rig.Move idle = form.move("idle");
        Player target = BossArena.findNearestPlayer(at, aggroRange);
        if (target == null) {
            BossArena.settle(at);
            inst.stand.teleport(at);
            // Nobody to fight: one last pose at rest, then the pieces send nothing at all.
            if (!inst.resting) show(inst, idle, 0, at, 4);
            inst.resting = true;
            return;
        }
        Vector to = target.getLocation().toVector().subtract(at.toVector()).setY(0);
        double dist = to.length();
        Vector toward = dist > 1e-3 ? to.multiply(1 / dist) : null;
        Vector step = toward == null ? null : steer(inst, at, toward, dist);
        boolean facing = toward == null || faces(inst, wrap(yawOf(toward) - at.getYaw()));
        Rig.Move attack = inst.cooldown <= 0 && facing ? choose(inst, target, dist) : null;
        if (attack != null) {
            inst.stand.teleport(at);
            begin(inst, attack);
            return;
        }
        Rig.Move loop = idle;
        if (toward != null) {
            if (step != null && BossArena.walk(at, step, true)) {
                boolean running = step.length() > speed(inst) * .75;
                loop = running && form.move("run") != null ? form.move("run") : form.move("walk") != null ? form.move("walk") : idle;
            } else if (step != null) {
                inst.strafe = -inst.strafe; // a wall: circle the other way
            }
        }
        BossArena.settle(at);
        inst.stand.teleport(at);
        inst.resting = false;
        if (loop != inst.loop) {
            inst.loop = loop;
            inst.loopTick = 0;
        }
        inst.loopTick++;
        // Standing and walking are slow and even, so every 4 ticks with the game interpolating between
        // looks the same as every 2 and sends half the packets; only running and attacks need every 2.
        int every = loop.id().equals("run") ? 2 : 4;
        if (inst.loopTick % every == 0) show(inst, loop, inst.loopTick % loop.ticks(), at, every);
    }

    /**
     * Between attacks, with a target {@code dist} blocks off in the direction {@code toward}: turns
     * {@code at} the way the boss wants to face and gives its step for this tick, or null to stand.
     * A boss on foot turns to its target and closes, circles or backs off ({@link #stride}).
     */
    protected Vector steer(Instance inst, Location at, Vector toward, double dist) {
        at.setYaw(turn(at.getYaw(), yawOf(toward), TURN_RATE));
        return stride(inst, toward.clone(), dist);
    }

    /** Whether the boss may begin an attack with its target {@code off} degrees from where it faces (to its right when positive). */
    protected boolean faces(Instance inst, float off) {
        return Math.abs(off) < 40;
    }

    /**
     * The step towards, round or away from a target {@code dist} blocks off in the direction {@code
     * toward}: a run from afar, a walk for the last stretch, a slow circle inside reach while the
     * next attack is not ready, a step back when the target is on top of the boss. Null to stand.
     */
    private Vector stride(Instance inst, Vector toward, double dist) {
        double reach = reach(inst), speed = speed(inst);
        if (dist > reach + 7) return toward.multiply(speed);
        if (dist > reach) return toward.multiply(Math.min(speed * .5, dist - reach));
        if (dist < reach * .5) return toward.multiply(-speed * .35);
        if (inst.cooldown <= 0) return null;
        if (--inst.strafeFor <= 0) {
            inst.strafeFor = 30 + random.nextInt(40);
            if (random.nextBoolean()) inst.strafe = -inst.strafe;
        }
        return new Vector(-toward.getZ(), 0, toward.getX()).multiply(inst.strafe * speed * .3);
    }

    /** Starts {@code move} where the boss stands, facing the way it faces. */
    protected final void begin(Instance inst, Rig.Move move) {
        Location at = inst.stand.getLocation();
        at.setPitch(0f);
        // Mid-move the stand is lifted by the move's travel: start again from the ground.
        if (inst.feet != null && inst.move != null) at.setY(inst.feet.getY());
        inst.move = move;
        inst.moveTick = 0;
        inst.base = at.clone();
        inst.feet = at.clone();
        inst.struck.clear();
    }

    /** One tick of the move under way: the stand follows the root, blows land, cues fire, and every two ticks the body is posed. */
    private void attack(Instance inst) {
        Rig.Move move = inst.move;
        RigForm form = form(inst);
        int t = ++inst.moveTick;
        float[] was = Rig.sample(move.travel(), t - 1), travel = Rig.sample(move.travel(), t);
        if (t <= move.aim()) aim(inst, was);
        follow(inst, was, travel);
        inst.stand.teleport(inst.feet.clone().add(0, travel[1], 0));
        // Half ticks too: a fast swing covers a quarter circle a tick.
        strike(inst, form, move, t - .5f);
        strike(inst, form, move, t);
        for (Rig.Cue cue : move.cues()) {
            if (cue.tick() == t) cue(inst, move, cue.name());
        }
        // A cue may have ended the move or begun another (a phase change).
        if (inst.move != move) return;
        form = form(inst);
        if (t % 2 == 0 || t >= move.ticks()) {
            Matrix4f carry = carry(inst.anchor, inst.base);
            inst.body.send(form.pose(move, t, carry), 2);
            form.effects(inst.anchor, inst.anchor.getYaw(), move, t, form.rig().bones(move, t, carry), 2);
        }
        if (t < move.ticks()) return;
        inst.move = null;
        inst.busy = false;
        inst.cooldown = attackCooldownTicks;
        inst.loop = null;
        Location end = inst.feet.clone();
        BossArena.settle(end);
        inst.stand.teleport(end);
    }

    /** While a blow is wound up the boss keeps turning to its target, about its feet. */
    private void aim(Instance inst, float[] travel) {
        Player target = BossArena.findNearestPlayer(inst.feet, aggroRange);
        if (target == null) return;
        Vector to = target.getLocation().toVector().subtract(inst.feet.toVector()).setY(0);
        if (to.lengthSquared() < 1e-6) return;
        rebase(inst, turn(inst.base.getYaw(), yawOf(to), AIM_RATE), travel);
    }

    /** Turns the move under way to face {@code yaw} about the feet, which stay where they are. */
    private void rebase(Instance inst, float yaw, float[] travel) {
        Location origin = inst.feet.clone();
        origin.setX(0);
        origin.setZ(0);
        Location offset = Rig.toWorld(origin, yaw, new Vector3f(travel[0], 0, travel[2]));
        inst.base = inst.feet.clone().subtract(offset.getX(), 0, offset.getZ());
        inst.base.setYaw(yaw);
        inst.feet.setYaw(yaw);
    }

    /** Puts the boss at {@code to}, facing its yaw, mid-move or not: its displays go with it at once. */
    protected final void relocate(Instance inst, Location to) {
        to.setPitch(0f);
        BossArena.settle(to);
        if (inst.move != null) {
            float[] travel = Rig.sample(inst.move.travel(), inst.moveTick);
            inst.feet = to.clone();
            rebase(inst, to.getYaw(), travel);
            inst.stand.teleport(to.clone().add(0, travel[1], 0));
        } else {
            inst.stand.teleport(to);
        }
        inst.anchor = to.clone();
        inst.body.moveTo(inst.anchor);
    }

    /**
     * Walks the feet along the root's travel from {@code was} to {@code now}, half a block at a time:
     * up steps, down ledges, stopped by walls. The base follows, so the body is drawn over its feet and
     * never slides through blocks or hangs in the air. The travel's height stays a lift above the feet.
     */
    private void follow(Instance inst, float[] was, float[] now) {
        float yaw = inst.base.getYaw();
        Vector from = Rig.toWorld(inst.base, yaw, new Vector3f(was[0], 0, was[2])).toVector();
        Location reached = Rig.toWorld(inst.base, yaw, new Vector3f(now[0], 0, now[2]));
        Vector step = reached.toVector().subtract(from).setY(0);
        int parts = (int) Math.ceil(step.length() / .5);
        for (int i = 0; i < parts; i++) {
            if (!BossArena.walk(inst.feet, step.clone().multiply(1.0 / parts), true)) break;
        }
        BossArena.settle(inst.feet);
        Vector offset = reached.toVector().subtract(inst.base.toVector()).setY(0);
        inst.base = inst.feet.clone().subtract(offset);
    }

    /** The blows at {@code t}: the weapon on whoever it passes through, the slashes and the landing on whoever is near. */
    private void strike(Instance inst, RigForm form, Rig.Move move, float t) {
        if (move.trails().isEmpty() && move.slashes().isEmpty() && move.rings().isEmpty()) return;
        List<Player> near = BossArena.getValidPlayersNear(inst.stand.getLocation(), 64);
        if (near.isEmpty()) return;
        Matrix4f[] world = form.rig().bones(move, t);
        Location base = inst.base;
        for (int i = 0; i < move.trails().size(); i++) {
            int[] trail = move.trails().get(i);
            if (t < trail[0] || t > trail[1]) continue;
            for (int k = 0; k <= 4; k++) {
                Vector3f point = world[form.weaponBone()].transformPosition(form.weaponPoint(k / 4f));
                Vector at = Rig.toWorld(base, base.getYaw(), point).toVector();
                for (Player p : near) {
                    if (p.getBoundingBox().expand(.3).contains(at) && inst.struck.add(i + ":" + p.getUniqueId())) {
                        hurt(inst, p, weaponDamage(inst));
                    }
                }
            }
        }
        if (t != (int) t) return;
        Location root = Rig.toWorld(base, base.getYaw(), world[0].transformPosition(new Vector3f(0, form.middle(), 0)));
        double slash = slashRadius(inst);
        for (int[] range : move.slashes()) {
            if (t < range[0] || t > range[1] || (int) t % 4 != 0) continue;
            for (Player p : near) {
                if (p.getLocation().add(0, 1, 0).distanceSquared(root) <= slash * slash) hurt(inst, p, slashDamage(inst));
            }
        }
        for (int ring : move.rings()) {
            if (ring != t) continue;
            double radius = ringRadius(inst);
            for (Player p : near) {
                Location l = p.getLocation();
                if (Math.abs(l.getY() - inst.feet.getY()) > 2 || Math.hypot(l.getX() - root.getX(), l.getZ() - root.getZ()) > radius) continue;
                hurt(inst, p, ringDamage(inst));
                BossArena.launchPlayer(p, .7);
            }
        }
    }

    protected double slashRadius(Instance inst) {
        return 2.6;
    }

    protected double ringRadius(Instance inst) {
        return 4.0;
    }

    /** The boss's hit on a player. */
    public final void hurt(Instance inst, Player p, double damage) {
        if (damage <= 0) return;
        p.damage(damage, inst.stand);
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 8, .3, .4, .3, 0,
                new Particle.DustOptions(form(inst).trail(), 1.2f));
        onHit(inst, p, damage);
    }

    /** The players a hazard of this boss can reach round {@code center}. */
    public final List<Player> playersNear(Location center, double radius) {
        return BossArena.getValidPlayersNear(center, radius * radius);
    }

    public final void launch(Player p, double up) {
        BossArena.launchPlayer(p, up);
    }

    /** The player the boss is fighting: the nearest in its aggro range, or null. */
    protected final Player target(Instance inst) {
        return BossArena.findNearestPlayer(inst.stand.getLocation(), aggroRange);
    }

    /** The way the boss faces, level. */
    protected final Vector facing(Instance inst) {
        double yaw = Math.toRadians((inst.move != null ? inst.base : inst.stand.getLocation()).getYaw());
        return new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /** The ground under the boss, whatever its move has lifted it to. */
    protected final Location ground(Instance inst) {
        return (inst.move != null ? inst.feet : inst.stand.getLocation()).clone();
    }

    /** A warned burst on the ground at {@code center} ({@link Hazard.Blast}) in this boss's colour. */
    protected final void blast(Instance inst, Location center, int fuse, double radius, double damage) {
        BossArena.settle(center);
        add(inst, new Hazard.Blast(this, inst, center, null, fuse, radius, damage, .5, form(inst).trail(), null, 0));
    }

    /** Leaves a timed effect in the fight; it ends with the boss. */
    protected final void add(Instance inst, Hazard hazard) {
        inst.hazards.add(hazard);
    }

    /** The share of its health the boss has left. */
    protected final double healthLeft(Instance inst) {
        return MscEntityUtils.calculateVirtualProgress(
                MscEntityUtils.getVirtualHealth(inst.stand), MscEntityUtils.getVirtualMaxHealth(inst.stand));
    }

    /** Poses the body standing at {@code at}, moving the displays to it first once it has strayed too far. */
    private void show(Instance inst, Rig.Move move, float tick, Location at, int ticks) {
        if (at.distanceSquared(inst.anchor) > REANCHOR_DISTANCE * REANCHOR_DISTANCE) {
            inst.anchor = at.clone();
            inst.body.moveTo(inst.anchor);
            ticks = 0; // the teleport is instant, so the pose must be too
        }
        inst.body.send(form(inst).pose(move, tick, carry(inst.anchor, at)), ticks);
    }

    /**
     * The carry ({@link Rig#bones(Rig.Move, float, Matrix4f)}) that draws a body standing at {@code at}
     * with displays at {@code anchor}: the offset in the anchor's frame, then the turn between their
     * headings.
     */
    public static Matrix4f carry(Location anchor, Location at) {
        Vector3f local = new Vector3f((float) (at.getX() - anchor.getX()), (float) (at.getY() - anchor.getY()),
                (float) (at.getZ() - anchor.getZ())).rotateY((float) Math.toRadians(anchor.getYaw()));
        return new Matrix4f().translation(local).rotateY((float) Math.toRadians(anchor.getYaw() - at.getYaw()));
    }

    /** The yaw that faces along {@code direction}. */
    protected static float yawOf(Vector direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
    }

    /** {@code degrees} as a turn between -180 and 180. */
    protected static float wrap(float degrees) {
        return (float) Math.IEEEremainder(degrees, 360);
    }

    /** {@code from} turned towards {@code to} by at most {@code rate} degrees, the short way round. */
    protected static float turn(float from, float to, float rate) {
        float left = wrap(to - from);
        return from + Math.max(-rate, Math.min(rate, left));
    }

    // =====================================================================================
    // Events
    // =====================================================================================

    /**
     * Only players hurt the boss, through the virtual health, at most {@code max-damage-per-hit} a hit.
     * Not {@code ignoreCancelled}: the server hands an invisible stand's damage over already cancelled.
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand) || !stand.getScoreboardTags().contains(tag)) return;
        event.setCancelled(true);
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;
        Player player = byEntity.getDamager() instanceof Player p ? p
                : byEntity.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p ? p : null;
        Instance inst = active.get(stand.getUniqueId());
        if (player == null || inst != null && inst.busy) return;
        double damage = Math.max(1.0, event.getFinalDamage());
        if (maxDamagePerHit > 0) damage = Math.min(damage, maxDamagePerHit);
        Location chest = stand.getLocation().add(0, 1.5, 0);
        stand.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, chest, 6, .3, .5, .3, .1);
        stand.getWorld().playSound(chest, Sound.ENTITY_PLAYER_ATTACK_CRIT, .8f, 1.2f);
        double left = MscEntityUtils.getVirtualHealth(stand) - damage;
        if (left <= 0 && inst != null && survives(inst)) return;
        MscEntityUtils.setVirtualHealth(stand, left);
        if (left <= 0 || inst == null || inst.move != null || inst.ticks < inst.dodgeReady) return;
        Rig.Move dodge = form(inst).move("dodge");
        if (dodge != null && random.nextDouble() < dodgeChance(inst)) {
            inst.dodgeReady = inst.ticks + 80;
            begin(inst, dodge);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand) || !stand.getScoreboardTags().contains(tag)) return;
        event.getDrops().clear();
        event.setDroppedExp(500);
        Instance inst = active.remove(stand.getUniqueId());
        Location at = stand.getLocation().add(0, 1.2, 0);
        if (inst != null) {
            stand.getWorld().spawnParticle(Particle.DUST, at, 80, .8, 1.2, .8, 0, new Particle.DustOptions(form(inst).trail(), 2f));
            cleanup(inst);
        }
        onSlain(at);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!(event.getDamageSource().getCausingEntity() instanceof ArmorStand stand)) return;
        if (!stand.getScoreboardTags().contains(tag)) return;
        List<String> messages = plugin.getConfig().getStringList("entities." + key + ".death-messages");
        if (messages.isEmpty()) return;
        String raw = messages.get(random.nextInt(messages.size()));
        event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', raw.replace("%player%", event.getEntity().getName())));
    }

    /** One boss in the world. */
    public static final class Instance {
        public final ArmorStand stand;
        RigBody body;
        /** Where the displays stand, facing their yaw. */
        Location anchor;
        /** The move under way, or null. */
        public Rig.Move move;
        /** Where the move under way started, facing the boss's heading; shifted as walls and ledges move its feet. */
        public Location base;
        /** The feet on the ground during a move, without the move's lift. */
        public Location feet;
        /** The loop played between attacks. */
        Rig.Move loop;
        public int ticks, moveTick, cooldown, phase;
        int loopTick, strafe = 1, strafeFor, dodgeReady;
        /** No one to fight and the rest pose sent: nothing is posed until a player comes. */
        boolean resting;
        /** Changing phase: no blow lands on the boss until the move under way ends. */
        public boolean busy;
        /** Swing and player pairs the weapon has already hit in this move. */
        final Set<String> struck = new HashSet<>();
        final List<Hazard> hazards = new ArrayList<>();
        private final Map<String, Integer> readyAt = new HashMap<>();
        BossBar bossBar;

        Instance(ArmorStand stand) {
            this.stand = stand;
        }

        /** Whether the ability called {@code what} is off its cooldown. */
        public boolean ready(String what) {
            return ticks >= readyAt.getOrDefault(what, 0);
        }

        /** Puts the ability called {@code what} on a cooldown of {@code forTicks}. */
        public void delay(String what, int forTicks) {
            readyAt.put(what, ticks + forTicks);
        }
    }
}
