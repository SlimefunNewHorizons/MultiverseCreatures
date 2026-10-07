package com.Chagui68.entities.boss.witherstorm;

import com.Chagui68.entities.boss.BossArena;
import com.Chagui68.entities.boss.fx.ParticleBudget;
import com.Chagui68.utils.MscBossBar;
import com.Chagui68.utils.MscEntityUtils;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameRules;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One living Wither Storm: its anchor, its body, its heads and everything it is doing this tick.
 *
 * <p>The storm hangs off an invisible marker armour stand (the anchor) that carries its saved state —
 * form, health, how much it has consumed — and every display of its body as passengers. The fight
 * follows the mod. It hunts every living thing round it, players first. The hunchbacks suck lumps of
 * the ground round them into their mass, and from the second form on a head can lock a tractor beam
 * on its victim: the beam lights up thin, pulls at the mod's slow, steady speed, and goes dark again,
 * and since the head turns slower than a player runs, stepping out of it sideways gets away. Whatever
 * reaches a mouth is bitten (a player) or swallowed (anything else), and everything swallowed makes
 * it grow through five forms. Projectiles injure the heads, the big forms play dead once, and the
 * storm's big attacks and its summons live in {@link WitherStormSpecials}.</p>
 */
final class WitherStorm {

    enum State { FORMING, FIGHTING, EVOLVING, PLAYING_DEAD, DYING }

    /** Where a head's beam is in its cycle. */
    enum Beam { OFF, CHARGING, PULLING, RESTING }

    /** One head's look, mouth, beam and injuries. */
    static final class HeadState {
        final WitherStormModel.Gaze gaze;
        float yaw, pitch, jaw, roll;
        int roarTicks, biteTicks, shakeTicks;
        int nextRoar, nextSkull;
        int injured, injureCooldown, hits, required;
        LivingEntity target;
        Beam beam = Beam.OFF;
        int beamTicks;
        double beamLength;
        /** The length the beam is drawn with: the ground ray eased, so torn ground does not make it bob. */
        double shownLength;
        /** Last frame's points in the world: the pivot it turns on, its middle and its mouth. */
        Vector pivot, centre, mouth;
        float width;

        HeadState(WitherStormModel.Gaze gaze) {
            this.gaze = gaze;
        }

        boolean pulling() {
            return beam == Beam.PULLING;
        }
    }

    /** A flaming wither skull in flight. */
    static final class FlamingSkull {
        ItemDisplay display;
        Vector pos;
        Vector dir;
        double speed;
        float power;
        float size;
        boolean blue;
        int life;
    }

    /** A block torn from the ground, flying into a mouth (or into the mass, for the hunchbacks). */
    private static final class Debris {
        BlockDisplay display;
        Vector pos;
        int head;
        int life;
        double speed;
        float spin;
        float size;
    }

    static final NamespacedKeys KEYS = new NamespacedKeys();

    /** The anchor's saved state. */
    static final class NamespacedKeys {
        final org.bukkit.NamespacedKey form = new org.bukkit.NamespacedKey("multiversecreatures", "witherstorm_form");
        final org.bukkit.NamespacedKey consumed = new org.bukkit.NamespacedKey("multiversecreatures", "witherstorm_consumed");
        final org.bukkit.NamespacedKey playedDead = new org.bukkit.NamespacedKey("multiversecreatures", "witherstorm_played_dead");
    }

    private static final float DEG = (float) (Math.PI / 180.0);
    /** Ticks between two animation frames; the client slides the boxes over the frame. */
    static final int FRAME = 3;
    private static final int EVOLVE_TICKS = 90;
    private static final int DYING_TICKS = 120;
    /** Ticks between two lumps sucked in by a hunchback. */
    private static final int SUCTION_INTERVAL = 60;
    private static final int MAX_DEBRIS = 24;
    /** A beam only lights up once its head points this close to its victim, in degrees. */
    private static final double LOCK_ANGLE = 12;
    /** A holding head ignores a victim this far off its aim, in degrees, and the beam stays put. */
    private static final float HOLD_SLACK = 3f;
    /** The drawn beam ignores the ground ray moving less than this, in blocks. */
    private static final double LENGTH_SLACK = 1.0;
    private static final Set<Material> NEVER_TORN = EnumSet.of(Material.BEDROCK, Material.BARRIER,
            Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.NETHER_PORTAL, Material.END_GATEWAY,
            Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.SPAWNER, Material.TRIAL_SPAWNER, Material.VAULT,
            Material.REINFORCED_DEEPSLATE, Material.LIGHT);
    static final Particle.DustOptions BEAM = new Particle.DustOptions(Color.fromRGB(0xA35BFF), 1.6f);
    static final Particle.DustOptions SICK = new Particle.DustOptions(Color.fromRGB(0x5B1A8C), 1.2f);
    static final Particle.DustOptions VOID = new Particle.DustOptions(Color.fromRGB(0x150A22), 2.2f);

    final WitherStormBoss boss;
    final ArmorStand anchor;
    final String ownerTag;
    private final WitherStormSpecials specials;

    WitherStormForm form;
    private WitherStormModel model;
    private WitherStormBody body;
    private WitherStormBody oldBody;
    private WitherStormForm evolvingTo;
    private HeadState[] heads;
    private Matrix4f[] frame;

    State state;
    private int stateTicks;
    private int age;
    private float tentacleClock;
    private float yaw;
    private float sentYaw;
    private float bodyPitch;
    private float growth = 1f;
    private float brokenJaw;
    private final Vector velocity = new Vector();
    private final double orbit = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;

    double health;
    double maxHealth;
    int consumed;
    boolean playedDead;
    private int exposedTicks;

    private BossBar bar;
    private Interaction bodyBox;
    private final Map<Integer, Interaction> headBoxes = new HashMap<>();
    private float[] restBounds;

    private final List<FlamingSkull> skulls = new ArrayList<>();
    private final List<Debris> debris = new ArrayList<>();
    private List<LivingEntity> targets = new ArrayList<>();
    private final Map<UUID, Integer> biteCooldown = new HashMap<>();
    private final Map<UUID, Integer> tentacleCooldown = new HashMap<>();
    private final Map<UUID, Integer> escaping = new HashMap<>();
    private final Map<UUID, Integer> exposure = new HashMap<>();
    private final List<UUID> sickened = new ArrayList<>();
    private final List<UUID> phantoms = new ArrayList<>();

    WitherStorm(WitherStormBoss boss, ArmorStand anchor, WitherStormForm form, State state) {
        this.boss = boss;
        this.anchor = anchor;
        this.ownerTag = WitherStormBoss.OWNER_PREFIX + anchor.getUniqueId().toString().replace("-", "");
        this.form = form;
        this.state = state;
        this.yaw = anchor.getLocation().getYaw();
        this.sentYaw = yaw;
        this.specials = new WitherStormSpecials(this);
        if (state == State.FORMING) {
            growth = 0.35f;
        }
        wear(form);
    }

    // ------------------------------------------------------------------ body

    /** Puts on a form: its model, its heads and a fresh body built round the anchor. */
    private void wear(WitherStormForm next) {
        this.form = next;
        this.model = next.model(settings().highDetail());
        List<WitherStormModel.Gaze> gazes = model.gazes();
        HeadState[] previous = heads;
        heads = new HeadState[gazes.size()];
        for (int i = 0; i < gazes.size(); i++) {
            HeadState h = new HeadState(gazes.get(i));
            h.nextRoar = age + 100 + ThreadLocalRandom.current().nextInt(200) + i * 40;
            h.nextSkull = age + 40 + i * 15;
            h.required = rollRequiredHits();
            if (previous != null && i < previous.length) {
                h.yaw = previous[i].yaw;
                h.pitch = previous[i].pitch;
                h.target = previous[i].target;
            }
            heads[i] = h;
        }
        restBounds = null;
        body = new WitherStormBody(model);
        frame = model.partMatrices(pose(), size());
        body.spawn(anchor, model.place(frame), ownerTag, next.isColossal() ? 6f : 2.5f);
        body.turn(yaw);
        sentYaw = yaw;
        locateHeads();
    }

    float size() {
        return (float) boss.settings().form(form).scale() * growth;
    }

    /** Size for distances: never less than the mod's own. */
    double reach() {
        return Math.max(1.0, size());
    }

    private WitherStormModel.Pose pose() {
        WitherStormModel.Pose pose = new WitherStormModel.Pose();
        pose.ticks = state == State.PLAYING_DEAD || state == State.DYING ? 0 : age;
        pose.tentacleTicks = tentacleClock;
        pose.bodyPitch = bodyPitch;
        pose.brokenJaw = brokenJaw;
        if (heads != null) {
            for (HeadState h : heads) {
                int i = h.gaze.index();
                pose.headYaw[i] = h.yaw;
                pose.headPitch[i] = h.pitch;
                pose.mouth[i] = h.jaw;
                pose.roll[i] = h.roll;
            }
        }
        return pose;
    }

    /**
     * A point of the display space in the world. The displays are drawn at the yaw last sent to
     * them, so that is the yaw used: the world point is where the player sees the box.
     */
    Vector toWorld(Vector3f local) {
        Vector3f v = new Quaternionf().rotateY(-sentYaw * DEG).transform(new Vector3f(local));
        Location a = anchor.getLocation();
        return new Vector(a.getX() + v.x, a.getY() + v.y, a.getZ() + v.z);
    }

    /** A world direction in the display space (yaw taken out). */
    private Vector3f toLocal(Vector direction) {
        return new Quaternionf().rotateY(sentYaw * DEG).transform(new Vector3f((float) direction.getX(),
                (float) direction.getY(), (float) direction.getZ()));
    }

    private void locateHeads() {
        for (HeadState h : heads) {
            Matrix4f m = frame[h.gaze.part()];
            if (m == null) continue;
            WitherStormModel.Gaze g = h.gaze;
            h.pivot = toWorld(WitherStormModel.point(m, 0, 0, 0));
            h.centre = toWorld(WitherStormModel.point(m, g.centre().x, g.centre().y, g.centre().z));
            h.mouth = toWorld(WitherStormModel.point(m, g.mouth().x, g.mouth().y, g.mouth().z));
            h.width = WitherStormModel.width(m, g);
        }
    }

    // ------------------------------------------------------------------ what the specials use

    World world() {
        return anchor.getWorld();
    }

    HeadState[] heads() {
        return heads;
    }

    List<LivingEntity> targets() {
        return targets;
    }

    WitherStormSettings settings() {
        return boss.settings();
    }

    /** The middle of the mass in the world. */
    Vector massCentre() {
        float[] b = restBounds();
        return toWorld(new Vector3f((b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2));
    }

    /** The mass's height in blocks. */
    double massHeight() {
        float[] b = restBounds();
        return b[4] - b[1];
    }

    /** Hurts a player with true damage (scaled to their gear like every boss's) or a mob plainly. */
    void hit(LivingEntity victim, double amount, String source) {
        if (victim instanceof Player p) {
            boss.dealToPlayer(this, p, amount, source);
        } else if (victim.isValid()) {
            MscEntityUtils.damageBy(anchor, victim, amount);
        }
    }

    // ------------------------------------------------------------------ the tick

    /** @return false once the storm is gone and should be forgotten */
    boolean tick() {
        if (!anchor.isValid() || anchor.isDead()) {
            discard();
            return false;
        }
        age++;
        stateTicks++;
        tickCooldowns();
        if (age % 5 == 0 || targets.isEmpty()) {
            targets = findTargets();
        }
        switch (state) {
            case FORMING -> tickForming();
            case FIGHTING -> tickFighting();
            case EVOLVING -> tickEvolving();
            case PLAYING_DEAD -> tickPlayingDead();
            case DYING -> {
                if (!tickDying()) return false;
            }
        }
        if (state != State.DYING) {
            animateHeads();
        }
        tentacleClock += state == State.PLAYING_DEAD || state == State.DYING ? 0.2f : 1f + (float) velocity.length() * 4f;
        if (age % FRAME == 0) {
            drawFrame();
        }
        // Every tick, not every frame: the beam is stepped along its arc between the body's frames.
        drawBeams(age % FRAME == 0);
        tickSkulls();
        if (age % 2 == 0) {
            tickDebris();
        }
        if (age % 4 == 0 && state != State.DYING) {
            syncHitboxes();
        }
        if (age % 20 == 0) {
            refreshBar();
            save();
        }
        return true;
    }

    private void drawFrame() {
        if (age % WitherStormBody.TURN_TICKS == 0 && Math.abs(wrap(yaw - sentYaw)) > 2f) {
            body.turn(yaw);
            if (oldBody != null) oldBody.turn(yaw);
            sentYaw = yaw;
        }
        frame = model.partMatrices(pose(), size());
        body.update(model.place(frame), FRAME);
        locateHeads();
        if (state == State.FIGHTING) {
            tentacleStrikes();
        }
    }

    private void tickCooldowns() {
        decrement(biteCooldown);
        decrement(tentacleCooldown);
        decrement(escaping);
        if (exposedTicks > 0) exposedTicks--;
        for (HeadState h : heads) {
            if (h.injured > 0) h.injured--;
            if (h.injureCooldown > 0) h.injureCooldown--;
            if (h.roarTicks > 0) h.roarTicks--;
            if (h.biteTicks > 0) h.biteTicks--;
            if (h.shakeTicks > 0) h.shakeTicks--;
        }
    }

    private static void decrement(Map<UUID, Integer> map) {
        Iterator<Map.Entry<UUID, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            if (e.getValue() <= 1) it.remove();
            else e.setValue(e.getValue() - 1);
        }
    }

    /**
     * Everything it hunts, nearest first: players, then (with {@code attack-mobs}) every other living
     * thing round it, as the mod's storm goes after anything that moves. A player who injured a head
     * is left alone for a while, the mod's escape time.
     */
    private List<LivingEntity> findTargets() {
        double range = settings().aggroRange() * reach() * (form.isColossal() ? 1.5 : 1.0);
        Location at = anchor.getLocation();
        List<LivingEntity> out = new ArrayList<>();
        for (Player p : BossArena.getValidPlayersNear(at, range * range)) {
            if (!escaping.containsKey(p.getUniqueId())) out.add(p);
        }
        out.sort((a, b) -> Double.compare(a.getLocation().distanceSquared(at), b.getLocation().distanceSquared(at)));
        if (settings().attackMobs() && state == State.FIGHTING) {
            List<LivingEntity> mobs = new ArrayList<>();
            double r = Math.min(range, 48 * reach());
            for (Entity e : world().getNearbyEntities(at, r, r, r)) {
                if (e instanceof LivingEntity living && !(e instanceof Player) && pullable(e)
                        && !escaping.containsKey(e.getUniqueId())) {
                    mobs.add(living);
                }
            }
            mobs.sort((a, b) -> Double.compare(a.getLocation().distanceSquared(at), b.getLocation().distanceSquared(at)));
            out.addAll(mobs.subList(0, Math.min(24, mobs.size())));
        }
        return out;
    }

    private boolean alive(LivingEntity e) {
        if (e == null || !e.isValid() || e.isDead() || !e.getWorld().equals(world())) return false;
        if (escaping.containsKey(e.getUniqueId())) return false;
        return !(e instanceof Player p) || BossArena.isValidTarget(p);
    }

    // ------------------------------------------------------------------ forming

    private void tickForming() {
        int total = settings().formingTicks();
        float t = Math.min(1f, stateTicks / (float) total);
        float before = growth;
        growth = 0.35f + 0.65f * t;
        if (growth != before) restBounds = null;
        health = Math.max(1, maxHealth * t);
        Location at = anchor.getLocation().add(0, 2.5 * size(), 0);
        World world = at.getWorld();
        // The vortex the mod draws round a storm being born: dark matter spiralling in.
        if (age % 2 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = (age * 0.35) + i * Math.PI / 3;
                double r = 6 * (1 - t) + 1.5;
                ParticleBudget.spawn(world, Particle.DUST, at.getX() + Math.cos(a) * r, at.getY() + Math.sin(age * 0.2 + i) * 1.5,
                        at.getZ() + Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0, i % 2 == 0 ? VOID : BEAM);
            }
        }
        if (age % 6 == 0) {
            ParticleBudget.spawn(world, Particle.REVERSE_PORTAL, at.getX(), at.getY(), at.getZ(), 20, 2, 2, 2, 0.15, null);
        }
        if (stateTicks == 1) {
            world.strikeLightningEffect(anchor.getLocation());
            world.playSound(at, Sound.ENTITY_WITHER_AMBIENT, 4f, 0.4f);
        }
        if (stateTicks % 40 == 0) {
            world.playSound(at, Sound.BLOCK_BEACON_AMBIENT, 3f, 0.5f);
            world.playSound(at, Sound.ENTITY_WARDEN_HEARTBEAT, 3f, 0.6f);
        }
        if (stateTicks >= total) {
            growth = 1f;
            restBounds = null;
            health = maxHealth;
            world.createExplosion(at, settings().spawnExplosion(), false, griefAllowed(), anchor);
            for (Player p : world.getPlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.55f);
            }
            for (HeadState h : heads) roar(h, false);
            enter(State.FIGHTING);
        }
    }

    // ------------------------------------------------------------------ fighting

    private void tickFighting() {
        assignTargets();
        LivingEntity main = targets.isEmpty() ? null : targets.get(0);
        move(main, true);
        for (HeadState h : heads) {
            tickBeam(h);
            tickRoar(h);
            tickPlainSkull(h);
        }
        if (!targets.isEmpty() && form.suctionClusters() > 0 && age % SUCTION_INTERVAL == 0) {
            suck();
        }
        if (age % 20 == 0) {
            sicken();
        }
        if (form.isColossal() && age % 200 == 0) {
            summonPhantoms(main);
        }
        specials.tick();
        WitherStormForm next = form.next();
        if (next != null && consumed >= settings().form(form).evolveAt()) {
            startEvolution(next);
        }
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
        refreshBar();
    }

    /** Spreads the heads over its victims: the middle one on the nearest, the others on the next. */
    private void assignTargets() {
        boolean stale = age % 40 == 0;
        for (int i = 0; i < heads.length; i++) {
            HeadState h = heads[i];
            if (!stale && alive(h.target)) continue;
            // A head mid-beam keeps its victim until the beam is done with it.
            if (h.beam == Beam.PULLING && alive(h.target)) continue;
            LivingEntity next = targets.isEmpty() ? null : targets.get(i % targets.size());
            if (next != h.target && h.beam == Beam.CHARGING) {
                h.beam = Beam.OFF;
            }
            h.target = next;
        }
    }

    /** Flies after the main victim at its form's distance and height, turning its body slowly. */
    private void move(LivingEntity target, boolean chase) {
        Location at = anchor.getLocation();
        Vector pos = at.toVector();
        Vector desired = pos.clone();
        float wantedYaw = yaw;
        if (target != null && chase) {
            Vector tp = target.getLocation().toVector();
            Vector away = pos.clone().subtract(tp).setY(0);
            if (away.lengthSquared() < 1.0e-4) away = new Vector(1, 0, 0);
            away.normalize();
            if (form.isColossal()) {
                away.rotateAroundY(0.004 * orbit);
            }
            desired = tp.clone().add(away.multiply(form.keepDistance() * reach()));
            Vector look = tp.clone().subtract(pos);
            wantedYaw = (float) Math.toDegrees(Math.atan2(-look.getX(), look.getZ()));
        }
        double ground = groundY(desired, at.getY() - flyHeight());
        desired.setY(ground + flyHeight() + Math.sin(age / 30.0) * (form.isColossal() ? 1.8 : 0.5));
        Vector step = desired.subtract(pos);
        double max = form.moveSpeed() * specials.speedFactor();
        if (step.length() > max) step.normalize().multiply(max);
        velocity.multiply(0.85).add(step.multiply(0.15));
        float turn = (float) form.turnRate();
        yaw += Math.max(-turn, Math.min(turn, wrap(wantedYaw - yaw)));
        yaw = wrap(yaw);
        if (form.isColossal()) {
            float wantedPitch = (float) Math.max(-15, Math.min(15, -velocity.getY() * 60));
            bodyPitch += (wantedPitch - bodyPitch) * 0.05f;
        }
        teleportTo(pos.add(velocity));
    }

    private double flyHeight() {
        return settings().form(form).flyHeight() * reach();
    }

    private void teleportTo(Vector to) {
        Location next = new Location(anchor.getWorld(), to.getX(), to.getY(), to.getZ(), yaw, 0f);
        anchor.teleport(next, TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }

    /** The ground under a point, or {@code fallback} when that column is not loaded. */
    double groundY(Vector at, double fallback) {
        World world = anchor.getWorld();
        int x = at.getBlockX();
        int z = at.getBlockZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return fallback;
        return world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
    }

    // ------------------------------------------------------------------ heads

    /** Turns every head towards what it is doing and moves its jaw, the way the mod's heads do. */
    private void animateHeads() {
        float free = form.isColossal() ? 2.5f : 5f;
        for (HeadState h : heads) {
            float wantedYaw;
            float wantedPitch;
            if (state == State.PLAYING_DEAD) {
                wantedYaw = h.gaze.index() == 1 ? 25 : h.gaze.index() == 2 ? -25 : 0;
                wantedPitch = 55;
            } else if (h.injured > 0) {
                wantedYaw = h.yaw + (float) Math.sin(age * 0.4 + h.gaze.index()) * 4;
                wantedPitch = 35;
            } else if (alive(h.target) && h.pivot != null) {
                Vector3f d = toLocal(h.target.getEyeLocation().toVector().subtract(h.pivot));
                wantedYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
                wantedPitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
            } else {
                wantedYaw = (float) Math.sin((age + h.gaze.index() * 40) / 60.0) * 40;
                wantedPitch = 20 + (float) Math.sin(age / 50.0 + h.gaze.index()) * 15;
            }
            float reach = h.gaze.jawed() ? 80 : 60;
            wantedYaw = Math.max(-reach, Math.min(reach, wrap(wantedYaw)));
            wantedPitch = Math.max(-60, Math.min(80, wantedPitch));
            // With its beam on a head drags round slower than a sprint: running across the beam escapes it.
            float step = h.beam == Beam.CHARGING || h.beam == Beam.PULLING ? (float) form.beamAimRate() : free;
            float dYaw = wrap(wantedYaw - h.yaw);
            float dPitch = wantedPitch - h.pitch;
            if (h.beam == Beam.PULLING) {
                // Holding: a victim bobbing inside the beam does not swing it; one leaving it is followed smoothly.
                dYaw = hold(dYaw);
                dPitch = hold(dPitch);
            }
            h.yaw += Math.max(-step, Math.min(step, dYaw));
            h.pitch += Math.max(-step, Math.min(step, dPitch));

            if (state == State.PLAYING_DEAD) {
                h.jaw += (1.2f - h.jaw) * 0.1f;
            } else if (h.roarTicks > 0 && h.biteTicks == 0) {
                h.jaw = Math.min(2f, h.jaw + (1 - h.jaw) * 0.15f + 0.04f);
            } else if (h.biteTicks > 0) {
                h.jaw = Math.min(1.4f, h.jaw + (1 - h.jaw) * 0.16f + 0.1f);
            } else if (h.injured > 0 || h.beam == Beam.PULLING) {
                h.jaw += ((h.injured > 0 ? 0.8f : 0.6f) - h.jaw) * 0.1f;
            } else {
                h.jaw = Math.max(0f, h.jaw - h.jaw * 0.16f - 0.02f);
            }
            float shake = h.shakeTicks / 20f;
            h.roll = shake <= 0 ? 0 : (float) (Math.sin(shake * Math.PI) * Math.sin(shake * Math.PI * 12) * 0.05 * Math.PI);
        }
        float wantedJaw = state == State.PLAYING_DEAD ? 1.5f : 0f;
        brokenJaw += (wantedJaw - brokenJaw) * 0.2f;
    }

    private static float hold(float error) {
        float past = Math.abs(error) - HOLD_SLACK;
        return past <= 0 ? 0 : Math.signum(error) * past * 0.3f;
    }

    private void tickRoar(HeadState h) {
        if (!form.hasBeam(h.gaze.index()) && h.gaze.index() != 0) return;
        if (h.injured > 0 || age < h.nextRoar) return;
        WitherStormSettings s = settings();
        h.nextRoar = age + s.roarMin() + ThreadLocalRandom.current().nextInt(Math.max(1, s.roarMax() - s.roarMin()));
        roar(h, false);
        if (alive(h.target) || form.isColossal()) {
            shootFlamingSkull(h, alive(h.target) ? h.target : null, false, 0);
        }
    }

    /** The roar: the jaw thrown open for two seconds and a sound that carries for a long way. */
    void roar(HeadState h, boolean hurt) {
        h.roarTicks = 40;
        Location at = h.mouth != null ? h.mouth.toLocation(world()) : anchor.getLocation();
        float volume = (float) (4 + size() * (form.isColossal() ? 6 : 2));
        World world = world();
        if (hurt) {
            world.playSound(at, Sound.ENTITY_WITHER_HURT, volume, 0.45f);
            world.playSound(at, Sound.ENTITY_RAVAGER_HURT, volume, 0.5f);
        } else {
            world.playSound(at, Sound.ENTITY_WITHER_AMBIENT, volume, form.isColossal() ? 0.35f : 0.55f);
            world.playSound(at, Sound.ENTITY_WARDEN_ROAR, volume, form.isColossal() ? 0.5f : 0.8f);
        }
        double radius = 10 * reach() * (form.isColossal() ? 3 : 1);
        for (Player p : BossArena.getValidPlayersNear(at, radius * radius)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, true));
        }
    }

    /** The plain wither skulls the hunchbacks keep spitting at their victims, as the Wither does. */
    private void tickPlainSkull(HeadState h) {
        int interval = form.skullInterval();
        if (interval <= 0 || !alive(h.target) || h.injured > 0 || h.pulling() || age < h.nextSkull || h.mouth == null) {
            return;
        }
        h.nextSkull = age + interval + ThreadLocalRandom.current().nextInt(interval);
        Vector from = h.mouth.clone();
        Vector dir = h.target.getEyeLocation().toVector().subtract(from).normalize();
        boolean charged = ThreadLocalRandom.current().nextDouble() < 0.1;
        world().spawn(from.toLocation(world()), WitherSkull.class, skull -> {
            skull.setShooter(anchor);
            skull.setCharged(charged);
            skull.setDirection(dir);
            skull.addScoreboardTag(WitherStormBoss.PROJECTILE_TAG);
        });
        world().playSound(from.toLocation(world()), Sound.ENTITY_WITHER_SHOOT, 2f, 0.7f);
    }

    // ------------------------------------------------------------------ tractor beam

    /**
     * A head's beam cycle. It only lights up once the head points straight at a victim it can see;
     * it charges thin and harmless for a moment, then pulls for a few seconds and goes dark to rest.
     */
    private void tickBeam(HeadState h) {
        WitherStormSettings.Beam config = settings().beam();
        if (!form.hasBeam(h.gaze.index()) || h.mouth == null || h.centre == null) {
            h.beam = Beam.OFF;
            return;
        }
        if (h.injured > 0 && h.beam != Beam.RESTING) {
            rest(h, config.restTicks());
            return;
        }
        h.beamTicks--;
        switch (h.beam) {
            case OFF -> {
                if (alive(h.target) && aimedAt(h, h.target) && canSee(h, h.target)) {
                    h.beam = Beam.CHARGING;
                    h.beamTicks = config.chargeTicks();
                    world().playSound(h.mouth.toLocation(world()), Sound.BLOCK_BEACON_ACTIVATE, 3f, 0.5f);
                }
            }
            case CHARGING -> {
                if (!alive(h.target)) rest(h, config.restTicks() / 2);
                else if (h.beamTicks <= 0) {
                    h.beam = Beam.PULLING;
                    h.beamTicks = config.holdTicks();
                }
            }
            case PULLING -> {
                if (!alive(h.target) || h.beamTicks <= 0) rest(h, config.restTicks());
                else pull(h);
            }
            case RESTING -> {
                if (h.beamTicks <= 0) h.beam = Beam.OFF;
            }
        }
    }

    private void rest(HeadState h, int ticks) {
        h.beam = Beam.RESTING;
        h.beamTicks = ticks + ThreadLocalRandom.current().nextInt(40);
    }

    private static Vector forward(HeadState h) {
        Vector dir = h.mouth.clone().subtract(h.centre);
        return dir.lengthSquared() < 1.0e-6 ? new Vector(0, 0, 1) : dir.normalize();
    }

    private boolean aimedAt(HeadState h, LivingEntity target) {
        Vector to = target.getEyeLocation().toVector().subtract(h.mouth);
        double range = beamRange();
        if (to.lengthSquared() > range * range) return false;
        return Math.toDegrees(forward(h).angle(to)) < LOCK_ANGLE;
    }

    private boolean canSee(HeadState h, LivingEntity target) {
        Vector to = target.getEyeLocation().toVector().subtract(h.mouth);
        double distance = to.length();
        if (distance < 0.5) return true;
        RayTraceResult blocked = world().rayTraceBlocks(h.mouth.toLocation(world()), to.normalize(), distance,
                FluidCollisionMode.NEVER, true);
        return blocked == null;
    }

    private double beamRange() {
        return settings().form(form).beamRange() * reach();
    }

    private double beamEndRadius() {
        return form.beamEndRadius() * reach();
    }

    /**
     * Pulls whatever is inside the beam at the mod's steady speed. The hunchbacks' beam only holds
     * the victim it locked onto; from the Destroyer on it swallows anything that wanders into it.
     */
    private void pull(HeadState h) {
        World world = world();
        Vector origin = h.mouth.clone();
        Vector dir = forward(h);
        double range = beamRange();
        RayTraceResult ground = world.rayTraceBlocks(origin.toLocation(world), dir, range, FluidCollisionMode.NEVER, true);
        double length = ground != null ? ground.getHitPosition().distance(origin) : range;
        h.beamLength = length;
        double endRadius = beamEndRadius();
        if (age % 10 == 0) {
            world.playSound(origin.toLocation(world), Sound.BLOCK_BEACON_AMBIENT, form.isColossal() ? 3f : 1.5f, 0.5f);
        }

        Vector end = origin.clone().add(dir.clone().multiply(length));
        org.bukkit.util.BoundingBox box = org.bukkit.util.BoundingBox.of(origin, end).expand(endRadius + 1);
        double bite = form.biteRadius() * reach();
        double speed = settings().beam().pullSpeed();
        for (Entity e : world.getNearbyEntities(box)) {
            if (!pullable(e) || escaping.containsKey(e.getUniqueId())) continue;
            if (!form.isColossal() && e instanceof LivingEntity && e != h.target) continue;
            Vector at = e.getBoundingBox().getCenter();
            Vector rel = at.clone().subtract(origin);
            double t = rel.dot(dir);
            if (t < -bite || t > length + 1) continue;
            double radius = 0.6 + (endRadius - 0.6) * Math.max(0, t) / Math.max(1, length);
            if (rel.clone().subtract(dir.clone().multiply(t)).length() > radius) continue;
            double dist = rel.length();
            if (dist <= bite) {
                bite(h, e);
                continue;
            }
            // The mod's pull: a steady speed straight at the mouth, slower within the last block.
            Vector delta = rel.multiply(-1 / dist).multiply(speed * Math.max(0.1, Math.min(1.0, dist)));
            e.setVelocity(e instanceof Player ? delta : delta.multiply(1.5));
            e.setFallDistance(0);
        }

        if (ground != null && ground.getHitBlock() != null
                && age % form.clusterInterval() == h.gaze.index() % form.clusterInterval()) {
            tear(h.gaze.index(), ground.getHitBlock(), form.clusterRadius(), form.isColossal() ? 6 : 3);
        }
    }

    /** Draws every head's beam as it stands this frame: thin while charging, a full cone while pulling. */
    private void drawBeams(boolean fresh) {
        for (HeadState h : heads) {
            int i = h.gaze.index();
            boolean on = state == State.FIGHTING && (h.beam == Beam.CHARGING || h.beam == Beam.PULLING);
            if (!on || frame[h.gaze.part()] == null) {
                body.hideBeam(i);
                h.shownLength = 0;
                continue;
            }
            Matrix4f m = frame[h.gaze.part()];
            WitherStormModel.Gaze g = h.gaze;
            Vector3f mouth = WitherStormModel.point(m, g.mouth().x, g.mouth().y, g.mouth().z);
            Vector3f centre = WitherStormModel.point(m, g.centre().x, g.centre().y, g.centre().z);
            Vector3f dir = new Vector3f(mouth).sub(centre);
            if (dir.lengthSquared() < 1.0e-6f) continue;
            double length = steadyLength(h, h.beam == Beam.PULLING && h.beamLength > 0 ? h.beamLength : beamRange(), fresh);
            float start = Math.max(0.15f, h.width * 0.18f);
            float endWidth = h.beam == Beam.PULLING ? (float) beamEndRadius() * 2f : start * 1.5f;
            body.beam(i, mouth, dir, (float) length, start, endWidth, sentYaw, FRAME, fresh);
            if (h.beam == Beam.PULLING && age % 6 == 0) {
                Vector p = h.mouth.clone().add(forward(h).multiply(length * ThreadLocalRandom.current().nextDouble()));
                ParticleBudget.spawn(world(), Particle.REVERSE_PORTAL, p.getX(), p.getY(), p.getZ(), 4, 0.5, 0.5, 0.5, 0.02, null);
            }
        }
    }

    /**
     * The ground ray under a sweeping head, and under the ground the beam itself tears up, jumps by whole
     * blocks from one tick to the next. Drawn raw, the far end of a beam pointing down bobbed up and down,
     * so the drawn length only moves on frames, ignores small changes and eases towards big ones.
     */
    private static double steadyLength(HeadState h, double length, boolean fresh) {
        if (h.shownLength <= 0) {
            h.shownLength = length;
        } else if (fresh) {
            double gap = length - h.shownLength;
            if (Math.abs(gap) > LENGTH_SLACK) {
                h.shownLength += (gap - Math.signum(gap) * LENGTH_SLACK) * (gap < 0 ? 0.5 : 0.3);
            }
        }
        return h.shownLength;
    }

    /** Living things, dropped items and falling blocks; never anything of a boss. */
    boolean pullable(Entity e) {
        if (!e.isValid() || e instanceof ArmorStand || e instanceof Display || e instanceof Interaction
                || e instanceof Boss) return false;
        for (String tag : e.getScoreboardTags()) {
            if (tag.startsWith("MSC_")) return false;
        }
        if (e instanceof Player p) return BossArena.isValidTarget(p);
        return e instanceof LivingEntity || e instanceof Item || e instanceof FallingBlock;
    }

    /** Something reached a mouth: a player is bitten and spat out, anything else is swallowed. */
    private void bite(HeadState h, Entity e) {
        World world = world();
        h.biteTicks = 10;
        if (e instanceof Player p) {
            if (biteCooldown.containsKey(p.getUniqueId())) return;
            biteCooldown.put(p.getUniqueId(), 60);
            boss.dealToPlayer(this, p, settings().biteDamage(), "Bite");
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1, false, true));
            Vector out = p.getLocation().toVector().subtract(h.centre).setY(0);
            if (out.lengthSquared() < 1.0e-4) out = new Vector(0, 0, 1);
            p.setVelocity(out.normalize().multiply(1.4).setY(0.6));
            world.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 2f, 0.5f);
            world.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.6f);
            consume(settings().pointsPlayer());
            // The head lets go after a bite: the beam rests before it can lock on again.
            rest(h, settings().beam().restTicks());
            return;
        }
        if (e instanceof Item item) {
            consume(Math.max(1, item.getItemStack().getAmount() / 16) * settings().pointsItem());
            e.remove();
        } else if (e instanceof FallingBlock) {
            consume(settings().pointsBlock());
            e.remove();
        } else if (e instanceof LivingEntity living) {
            if (living.customName() != null || (living instanceof Tameable tame && tame.isTamed())) {
                // A pet or a named mob is not food: it is thrown back out instead.
                e.setVelocity(e.getLocation().toVector().subtract(h.centre).normalize().multiply(1.2).setY(0.5));
                escaping.put(e.getUniqueId(), 200);
                return;
            }
            consume(settings().pointsMob());
            Location at = living.getLocation();
            ParticleBudget.spawn(world, Particle.DUST, at.getX(), at.getY() + 0.5, at.getZ(), 12, 0.4, 0.4, 0.4, 0, SICK);
            e.remove();
            if (e == h.target) rest(h, settings().beam().restTicks() / 2);
        }
        world.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.5f, 0.5f);
    }

    void consume(int points) {
        consumed += Math.max(0, points);
    }

    // ------------------------------------------------------------------ eating the ground

    /**
     * The hunchbacks' feeding, the mod's hunchback cluster source: lumps of the ground round the
     * storm torn loose and sucked into its mass, more of them every form (3, 9 and 18 at a time).
     */
    private void suck() {
        double radius = 10 + 6 * form.ordinal() * reach();
        Location at = anchor.getLocation();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < form.suctionClusters(); i++) {
            double a = r.nextDouble(Math.PI * 2);
            double d = 3 + r.nextDouble(radius);
            int x = (int) Math.floor(at.getX() + Math.cos(a) * d);
            int z = (int) Math.floor(at.getZ() + Math.sin(a) * d);
            if (!world().isChunkLoaded(x >> 4, z >> 4)) continue;
            Block top = world().getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (Math.abs(top.getY() - at.getY()) > 24) continue;
            tear(-1, top, 1, i < 4 ? 2 : 1);
        }
        if (r.nextInt(3) == 0) {
            world().playSound(at, Sound.BLOCK_GRAVEL_BREAK, 2f, 0.6f);
        }
    }

    /**
     * Tears a lump of ground out round {@code hit} and sends up to {@code visuals} of its blocks
     * flying into a mouth (or into the mass for {@code head} -1). With griefing off the ground stays
     * and the storm still eats the lump.
     */
    private void tear(int head, Block hit, int radius, int visuals) {
        List<Block> lump = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius + 1) continue;
                    Block b = hit.getRelative(dx, dy, dz);
                    if (tearable(b)) lump.add(b);
                }
            }
        }
        if (lump.isEmpty()) return;
        Collections.shuffle(lump);
        int taken = 0;
        boolean grief = griefAllowed();
        for (Block b : lump) {
            if (taken >= 4 + 4 * radius) break;
            BlockData data = b.getBlockData();
            if (grief) {
                EntityChangeBlockEvent event = new EntityChangeBlockEvent(anchor, b, Material.AIR.createBlockData());
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) continue;
                b.setType(Material.AIR);
            }
            taken++;
            if (debris.size() < MAX_DEBRIS && taken <= visuals) {
                launchDebris(head, b.getLocation().add(0.5, 0.5, 0.5).toVector(), data);
            }
        }
        consume(taken * settings().pointsBlock());
        Location at = hit.getLocation().add(0.5, 0.5, 0.5);
        ParticleBudget.spawn(hit.getWorld(), Particle.BLOCK, at.getX(), at.getY(), at.getZ(), 8, radius, radius, radius, 0, hit.getBlockData());
    }

    private boolean tearable(Block b) {
        Material type = b.getType();
        if (type.isAir() || !type.isBlock() || b.isLiquid() || NEVER_TORN.contains(type)) return false;
        float hardness = type.getHardness();
        if (hardness < 0 || hardness >= 50) return false;
        // Only exposed blocks, as in the mod: the storm peels the ground, it does not bore into it.
        if (!b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.NORTH).isPassable()
                && !b.getRelative(BlockFace.SOUTH).isPassable() && !b.getRelative(BlockFace.EAST).isPassable()
                && !b.getRelative(BlockFace.WEST).isPassable()) return false;
        // A block with an inventory or other data of its own would lose it; the mod leaves those too.
        return !(b.getState(false) instanceof TileState);
    }

    boolean griefAllowed() {
        if (!settings().griefBlocks()) return false;
        Boolean rule = world().getGameRuleValue(GameRules.MOB_GRIEFING);
        return rule == null || rule;
    }

    private void launchDebris(int head, Vector at, BlockData data) {
        World world = world();
        Debris d = new Debris();
        d.pos = at.clone();
        d.head = head;
        d.speed = 0.3;
        d.size = 0.9f;
        d.display = world.spawn(at.toLocation(world), BlockDisplay.class, display -> {
            display.setPersistent(false);
            display.addScoreboardTag(WitherStormBoss.DEBRIS_TAG);
            display.addScoreboardTag(ownerTag);
            display.setBlock(data);
            display.setTeleportDuration(2);
            display.setShadowRadius(0f);
            display.setTransformation(new Transformation(new Vector3f(-0.45f), new Quaternionf(),
                    new Vector3f(0.9f), new Quaternionf()));
        });
        debris.add(d);
    }

    /** Moves the torn blocks two ticks at a time; the client slides them over the two ticks. */
    private void tickDebris() {
        Iterator<Debris> it = debris.iterator();
        Vector mass = debris.isEmpty() ? null : massCentre();
        while (it.hasNext()) {
            Debris d = it.next();
            HeadState h = d.head >= 0 ? head(d.head) : null;
            Vector goal = h != null ? h.mouth : mass;
            d.life += 2;
            if (d.display == null || !d.display.isValid() || goal == null || d.life > 160) {
                if (d.display != null) d.display.remove();
                it.remove();
                continue;
            }
            Vector to = goal.clone().subtract(d.pos);
            double dist = to.length();
            if (dist < Math.max(1.2, (h != null ? h.width : massHeight()) * 0.3)) {
                d.display.remove();
                it.remove();
                continue;
            }
            d.speed = Math.min(2.4, d.speed * 1.1 + 0.05);
            d.pos.add(to.multiply(Math.min(dist, d.speed) / dist));
            d.display.teleport(d.pos.toLocation(world()));
            if (d.life % 6 == 0) {
                d.spin += 0.9f;
                float s = d.size * (float) Math.min(1.0, dist / 6.0 + 0.3);
                d.display.setInterpolationDelay(0);
                d.display.setInterpolationDuration(6);
                Quaternionf q = new Quaternionf().rotationXYZ(d.spin, d.spin * 0.7f, d.spin * 0.3f);
                d.display.setTransformation(new Transformation(q.transform(new Vector3f(-s / 2)), q,
                        new Vector3f(s), new Quaternionf()));
            }
        }
    }

    HeadState head(int index) {
        for (HeadState h : heads) {
            if (h.gaze.index() == index) return h;
        }
        return null;
    }

    // ------------------------------------------------------------------ flaming skulls

    /**
     * The skull the mod spits with every roar; a blue one from a head that has just been hurt.
     * {@code spread} turns it off its aim by up to that many degrees, for a barrage.
     */
    void shootFlamingSkull(HeadState h, LivingEntity target, boolean blue, double spread) {
        if (h.mouth == null || h.centre == null) return;
        World world = world();
        Vector from = h.mouth.clone();
        Vector dir = target != null ? target.getEyeLocation().toVector().subtract(from) : forward(h);
        if (dir.lengthSquared() < 1.0e-6) return;
        dir.normalize();
        if (spread > 0) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            dir.rotateAroundY(Math.toRadians(r.nextDouble(-spread, spread)));
            dir.add(new Vector(0, Math.toRadians(r.nextDouble(-spread, spread) / 2), 0)).normalize();
        }
        FlamingSkull s = new FlamingSkull();
        s.pos = from;
        s.dir = dir;
        s.blue = blue;
        s.power = form.flamingSkullPower() + (blue ? 1f : 0f);
        s.size = Math.max(0.9f, h.width * 0.6f);
        s.speed = form.isColossal() ? 1.5 : 1.0;
        Quaternionf facing = new Quaternionf().rotationTo(new Vector3f(0, 0, -1),
                new Vector3f((float) dir.getX(), (float) dir.getY(), (float) dir.getZ()));
        float k = s.size * 2f;
        s.display = world.spawn(from.toLocation(world), ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(WitherStormBoss.SKULL_TAG);
            d.addScoreboardTag(ownerTag);
            d.setItemStack(new ItemStack(Material.WITHER_SKELETON_SKULL));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTeleportDuration(1);
            d.setGlowing(true);
            d.setGlowColorOverride(blue ? Color.fromRGB(0x3FD7FF) : Color.fromRGB(0xFF7A1A));
            d.setTransformation(new Transformation(facing.transform(new Vector3f(0, k * 0.25f, 0)), facing,
                    new Vector3f(k), new Quaternionf()));
        });
        skulls.add(s);
        world.playSound(from.toLocation(world), Sound.ENTITY_BLAZE_SHOOT, 3f, 0.5f);
        world.playSound(from.toLocation(world), Sound.ENTITY_WITHER_SHOOT, 3f, 0.6f);
    }

    private void tickSkulls() {
        World world = world();
        Iterator<FlamingSkull> it = skulls.iterator();
        while (it.hasNext()) {
            FlamingSkull s = it.next();
            if (s.display == null || !s.display.isValid()) {
                it.remove();
                continue;
            }
            Location from = s.pos.toLocation(world);
            Vector hitAt = null;
            if (++s.life > 140) {
                hitAt = s.pos.clone();
            } else {
                RayTraceResult block = world.rayTraceBlocks(from, s.dir, s.speed, FluidCollisionMode.NEVER, true);
                RayTraceResult entity = world.rayTraceEntities(from, s.dir, s.speed, s.size * 0.5,
                        e -> e instanceof LivingEntity && pullable(e));
                if (entity != null) hitAt = entity.getHitPosition();
                else if (block != null) hitAt = block.getHitPosition();
            }
            if (hitAt != null) {
                explodeSkull(s, hitAt);
                it.remove();
                continue;
            }
            s.pos.add(s.dir.clone().multiply(s.speed));
            s.display.teleport(s.pos.toLocation(world));
            if (s.life % 2 == 0) {
                ParticleBudget.spawn(world, s.blue ? Particle.SOUL_FIRE_FLAME : Particle.FLAME, s.pos.getX(), s.pos.getY(),
                        s.pos.getZ(), 4, s.size * 0.2, s.size * 0.2, s.size * 0.2, 0.01, null);
                ParticleBudget.spawn(world, Particle.LARGE_SMOKE, s.pos.getX(), s.pos.getY(), s.pos.getZ(), 1,
                        0.1, 0.1, 0.1, 0.01, null);
            }
        }
    }

    private void explodeSkull(FlamingSkull s, Vector at) {
        World world = world();
        Location loc = at.toLocation(world);
        s.display.remove();
        boolean grief = griefAllowed();
        world.createExplosion(loc, s.power, grief && !s.blue, grief, anchor);
        if (s.blue) {
            ParticleBudget.spawn(world, Particle.SOUL, loc.getX(), loc.getY(), loc.getZ(), 30, 1.5, 1.5, 1.5, 0.05, null);
            for (Entity e : world.getNearbyEntities(loc, 5, 5, 5)) {
                if (e instanceof LivingEntity living && pullable(e)) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1, false, true));
                }
            }
        }
    }

    // ------------------------------------------------------------------ tentacles

    private void tentacleStrikes() {
        if (model.tentacles().isEmpty() || form.tentacleReach() <= 0) return;
        double reach = form.tentacleReach() * reach();
        float[] b = restBounds();
        double span = Math.max(b[3] - b[0], b[5] - b[2]) + reach * 4 + 20;
        Location at = anchor.getLocation();
        List<LivingEntity> near = new ArrayList<>();
        for (LivingEntity e : targets) {
            if (alive(e) && e.getLocation().distanceSquared(at) <= span * span) near.add(e);
        }
        if (near.isEmpty()) return;
        for (WitherStormModel.Tentacle t : model.tentacles()) {
            Vector3f local = model.tip(frame, t);
            if (local == null) continue;
            Vector tip = toWorld(local);
            for (LivingEntity e : near) {
                if (tentacleCooldown.containsKey(e.getUniqueId())) continue;
                Vector centre = e.getBoundingBox().getCenter();
                if (centre.distanceSquared(tip) > reach * reach) continue;
                tentacleCooldown.put(e.getUniqueId(), 20);
                hit(e, settings().tentacleDamage(), "Tentacle");
                Vector push = centre.clone().subtract(tip);
                if (push.lengthSquared() < 1.0e-4) push = new Vector(0, 1, 0);
                e.setVelocity(push.normalize().multiply(1.3).setY(0.5));
                e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));
                world().playSound(e.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2f, 0.5f);
            }
        }
    }

    // ------------------------------------------------------------------ sickness

    /**
     * The wither sickness: players near the storm grow hungry, then weak, then wither; hostile mobs
     * near it are sickened and fight for it.
     */
    private void sicken() {
        WitherStormSettings s = settings();
        if (!s.sickness()) return;
        double radius = form.sicknessRadius() * reach();
        Location at = anchor.getLocation();
        Set<UUID> inside = new java.util.HashSet<>();
        for (Player p : BossArena.getValidPlayersNear(at, radius * radius)) {
            inside.add(p.getUniqueId());
            int seconds = exposure.merge(p.getUniqueId(), 1, Integer::sum);
            int stage = seconds >= 80 ? 3 : seconds >= 40 ? 2 : seconds >= 20 ? 1 : 0;
            if (stage >= 1) p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, stage - 1, false, false));
            if (stage >= 2) p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, false));
            if (stage >= 3) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 0, false, false));
            }
            if (seconds == 20 || seconds == 40 || seconds == 80) {
                p.sendActionBar(Component.text(seconds == 80 ? "The Wither Sickness is consuming you..."
                        : "You feel the Wither Sickness...", NamedTextColor.DARK_PURPLE));
            }
        }
        exposure.replaceAll((id, sec) -> inside.contains(id) ? sec : sec - 2);
        exposure.values().removeIf(sec -> sec <= 0);

        if (!s.sickenMobs()) return;
        sickened.removeIf(id -> {
            Entity e = Bukkit.getEntity(id);
            return e == null || !e.isValid();
        });
        if (sickened.size() >= s.maxSickened()) return;
        double mobs = radius * 0.75;
        for (Entity e : world().getNearbyEntities(at, mobs, mobs, mobs)) {
            if (!(e instanceof Monster mob) || e instanceof Phantom || sickened.size() >= s.maxSickened()) continue;
            if (ThreadLocalRandom.current().nextDouble() > 0.2 || !pullable(e)) continue;
            sicken(mob, WitherStormBoss.SICKENED_TAG);
            sickened.add(mob.getUniqueId());
        }
    }

    /** Turns a mob into one of the storm's: purple name, more health, Strength and Speed, on a victim. */
    void sicken(Mob mob, String tag) {
        mob.addScoreboardTag(tag);
        String type = mob.getType().name().toLowerCase().replace('_', ' ');
        mob.customName(Component.text("Sickened " + Character.toUpperCase(type.charAt(0)) + type.substring(1),
                NamedTextColor.DARK_PURPLE));
        if (mob.getAttribute(Attribute.MAX_HEALTH) != null) {
            double max = mob.getAttribute(Attribute.MAX_HEALTH).getBaseValue() * 1.5;
            MscEntityUtils.setAttribute(mob, Attribute.MAX_HEALTH, max);
            mob.setHealth(Math.min(max, mob.getAttribute(Attribute.MAX_HEALTH).getValue()));
        }
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, false, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, true));
        Player nearest = BossArena.findNearestPlayer(mob.getLocation(), 48);
        if (nearest != null) mob.setTarget(nearest);
        Location at = mob.getEyeLocation();
        ParticleBudget.spawn(mob.getWorld(), Particle.DUST, at.getX(), at.getY(), at.getZ(), 15, 0.3, 0.5, 0.3, 0, SICK);
    }

    /** The phantoms that circle the big forms and dive at whoever they see. */
    private void summonPhantoms(LivingEntity target) {
        phantoms.removeIf(id -> {
            Entity e = Bukkit.getEntity(id);
            return e == null || !e.isValid();
        });
        Location at = anchor.getLocation();
        for (UUID id : phantoms) {
            if (Bukkit.getEntity(id) instanceof Phantom p) p.setAnchorLocation(at);
        }
        int max = settings().maxPhantoms();
        if (phantoms.size() >= max) return;
        World world = world();
        Location spawn = at.clone().add(ThreadLocalRandom.current().nextDouble(-8, 8), 4, ThreadLocalRandom.current().nextDouble(-8, 8));
        Phantom phantom = world.spawn(spawn, Phantom.class, p -> {
            p.addScoreboardTag(WitherStormBoss.PHANTOM_TAG);
            p.customName(Component.text("Sickened Phantom", NamedTextColor.DARK_PURPLE));
            p.setShouldBurnInDay(false);
            p.setSize(2);
            p.setAnchorLocation(at);
            p.setPersistent(false);
        });
        if (target != null) phantom.setTarget(target);
        phantoms.add(phantom.getUniqueId());
    }

    // ------------------------------------------------------------------ evolving

    private void startEvolution(WitherStormForm next) {
        evolvingTo = next;
        specials.cancel();
        enter(State.EVOLVING);
        for (HeadState h : heads) roar(h, false);
        World world = world();
        Location at = anchor.getLocation().add(0, 3 * size(), 0);
        world.playSound(at, Sound.ENTITY_WITHER_SPAWN, 8f, 0.4f);
        world.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 8f, 0.5f);
        for (Player p : BossArena.getValidPlayersNear(at, 160 * 160)) {
            p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "The Wither Storm is evolving into "
                    + next.displayName() + "!");
        }
    }

    private void tickEvolving() {
        move(null, false);
        Location at = anchor.getLocation().add(0, 4 * size(), 0);
        World world = world();
        if (age % 4 == 0) {
            ParticleBudget.spawn(world, Particle.REVERSE_PORTAL, at.getX(), at.getY(), at.getZ(), 30, 4 * size(), 4 * size(), 4 * size(), 0.2, null);
            ParticleBudget.spawn(world, Particle.DUST, at.getX(), at.getY(), at.getZ(), 10, 3 * size(), 3 * size(), 3 * size(), 0, VOID);
        }
        if (stateTicks == 20) {
            oldBody = body;
            oldBody.shrink(25);
            double before = maxHealth;
            maxHealth = boss.maxHealthFor(evolvingTo);
            health = Math.min(maxHealth, health + (maxHealth - before));
            growth = 0.15f;
            removeHitboxes();
            wear(evolvingTo);
        }
        if (stateTicks > 20 && stateTicks <= 75 && stateTicks % 5 == 0) {
            growth = Math.min(1f, 0.15f + 0.85f * (stateTicks - 20) / 55f);
            // The whole body grows in one step every five ticks, slid over those five ticks.
            restBounds = null;
            frame = model.partMatrices(pose(), size());
            body.update(model.place(frame), 5);
        }
        if (stateTicks == 50 && oldBody != null) {
            oldBody.remove();
            oldBody = null;
        }
        if (stateTicks >= EVOLVE_TICKS) {
            growth = 1f;
            restBounds = null;
            anchor.getPersistentDataContainer().set(KEYS.form, PersistentDataType.STRING, form.key());
            world.playSound(at, Sound.ENTITY_WITHER_SPAWN, 10f, 0.5f);
            for (HeadState h : heads) roar(h, false);
            enter(State.FIGHTING);
        }
    }

    // ------------------------------------------------------------------ playing dead

    private void startPlayingDead() {
        playedDead = true;
        anchor.getPersistentDataContainer().set(KEYS.playedDead, PersistentDataType.BYTE, (byte) 1);
        specials.cancel();
        enter(State.PLAYING_DEAD);
        world().playSound(anchor.getLocation(), Sound.ENTITY_WITHER_DEATH, 10f, 0.5f);
        for (HeadState h : heads) {
            h.beam = Beam.OFF;
            h.roarTicks = 0;
        }
    }

    private void tickPlayingDead() {
        Location at = anchor.getLocation();
        double ground = groundY(at.toVector(), at.getY() - 1);
        Vector pos = at.toVector();
        if (pos.getY() > ground + 1) {
            velocity.setX(velocity.getX() * 0.9).setZ(velocity.getZ() * 0.9).setY(Math.max(-0.9, velocity.getY() - 0.05));
            teleportTo(pos.add(velocity));
        } else {
            velocity.zero();
        }
        bodyPitch += (-12 - bodyPitch) * 0.05f;
        if (stateTicks >= settings().playDeadTicks()) {
            revive();
        }
    }

    /** It was never dead: it rises with every head roaring and a blast that throws everything back. */
    private void revive() {
        World world = world();
        Location at = anchor.getLocation().add(0, 3 * size(), 0);
        world.playSound(at, Sound.ENTITY_WITHER_SPAWN, 10f, 0.45f);
        world.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 10f, 0.4f);
        double radius = 24 * reach();
        for (Entity e : world.getNearbyEntities(at, radius, radius, radius)) {
            if (!(e instanceof LivingEntity living) || !pullable(e)) continue;
            hit(living, settings().reviveDamage(), "Revival");
            Vector push = e.getLocation().toVector().subtract(at.toVector()).setY(0);
            if (push.lengthSquared() < 1.0e-4) push = new Vector(1, 0, 0);
            e.setVelocity(push.normalize().multiply(2.2).setY(0.9));
        }
        ParticleBudget.spawn(world, Particle.EXPLOSION_EMITTER, at.getX(), at.getY(), at.getZ(), 3, 4, 2, 4, 0, null);
        health = Math.min(maxHealth, health + maxHealth * 0.1);
        for (HeadState h : heads) roar(h, false);
        enter(State.FIGHTING);
    }

    // ------------------------------------------------------------------ dying

    private void startDying() {
        specials.cancel();
        enter(State.DYING);
        removeHitboxes();
        for (HeadState h : heads) {
            h.beam = Beam.OFF;
            body.hideBeam(h.gaze.index());
        }
        Location at = anchor.getLocation().add(0, 3 * size(), 0);
        world().playSound(at, Sound.ENTITY_WITHER_DEATH, 12f, 0.45f);
    }

    /** @return false when the storm is gone */
    private boolean tickDying() {
        World world = world();
        Vector centre = massCentre();
        double reach = Math.max(4, massHeight() * 0.8);
        // The rays of light the mod's dying storm breaks apart in, more of them every tick.
        int rays = 1 + stateTicks / 16;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < rays; i++) {
            Vector dir = new Vector(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize();
            for (int k = 1; k <= 5; k++) {
                Vector p = centre.clone().add(dir.clone().multiply(reach * k / 5.0));
                ParticleBudget.spawn(world, Particle.END_ROD, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0, null);
            }
        }
        List<Display> standing = body.standing();
        int fall = Math.max(1, standing.size() / 70);
        for (int i = 0; i < fall && !standing.isEmpty(); i++) {
            Display d = standing.remove(r.nextInt(standing.size()));
            Vector p = toWorld(body.centreOf(d));
            ParticleBudget.spawn(world, Particle.EXPLOSION, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0, null);
            d.remove();
        }
        if (stateTicks % 20 == 0) {
            world.playSound(centre.toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 6f, 0.5f);
        }
        if (stateTicks == DYING_TICKS / 2) {
            world.playSound(centre.toLocation(world), Sound.ENTITY_ENDER_DRAGON_DEATH, 12f, 0.6f);
        }
        if (stateTicks < DYING_TICKS) {
            return true;
        }
        Location loc = centre.toLocation(world);
        ParticleBudget.spawn(world, Particle.EXPLOSION_EMITTER, loc.getX(), loc.getY(), loc.getZ(), 6, reach / 3, reach / 3, reach / 3, 0, null);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 12f, 0.4f);
        Location drop = new Location(world, loc.getX(), groundY(loc.toVector(), loc.getY()) + 1, loc.getZ());
        ItemStack stars = new ItemStack(Material.NETHER_STAR, Math.max(1, form.netherStars()));
        world.dropItemNaturally(drop, stars);
        world.spawn(drop, ExperienceOrb.class).setExperience(500 * (form.ordinal() + 1));
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 200 * 200) {
                p.sendTitle(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "WITHER STORM",
                        ChatColor.GRAY + "The storm has been destroyed.", 10, 80, 20);
                cure(p);
            }
        }
        cureSickened();
        specials.dismiss();
        discard();
        anchor.remove();
        return false;
    }

    private void cure(Player p) {
        if (exposure.remove(p.getUniqueId()) != null) {
            p.removePotionEffect(PotionEffectType.HUNGER);
            p.removePotionEffect(PotionEffectType.WEAKNESS);
            p.removePotionEffect(PotionEffectType.WITHER);
            p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        }
    }

    private void cureSickened() {
        for (UUID id : sickened) {
            if (Bukkit.getEntity(id) instanceof Monster mob) {
                mob.removeScoreboardTag(WitherStormBoss.SICKENED_TAG);
                mob.customName(null);
                mob.removePotionEffect(PotionEffectType.STRENGTH);
                mob.removePotionEffect(PotionEffectType.SPEED);
            }
        }
        sickened.clear();
        for (UUID id : phantoms) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        phantoms.clear();
    }

    // ------------------------------------------------------------------ being hurt

    /**
     * A hit on one of the storm's hitboxes.
     *
     * @param head       the head index hit, or -1 for the mass
     * @param projectile whether a projectile landed it; only projectiles injure a head
     */
    void hurt(double amount, int head, boolean projectile, Player attacker) {
        if (state == State.FORMING || state == State.EVOLVING || state == State.DYING || amount <= 0) {
            if (state == State.FORMING || state == State.EVOLVING) shieldSpark(head);
            return;
        }
        HeadState h = head >= 0 ? head(head) : null;
        double factor = h == null ? form.bodyDamageMultiplier() : (h.injured > 0 ? 1.5 : 1.0);
        if (exposedTicks > 0) factor *= settings().exposedMultiplier();
        if (state == State.PLAYING_DEAD) factor *= 1.5;
        double dealt = Math.min(amount * factor, settings().maxDamagePerHit());
        health = Math.max(0, health - dealt);
        hitEffect(h);
        if (h != null) {
            h.shakeTicks = 20;
            if (projectile && h.injured <= 0 && h.injureCooldown <= 0) {
                h.hits++;
                h.injureCooldown = 8;
                if (h.hits >= h.required) injure(h, attacker);
                else h.roarTicks = Math.max(h.roarTicks, 20);
            }
        }
        if (health <= 0) {
            if (state == State.PLAYING_DEAD || playedDead || !form.isColossal() || settings().playDeadThreshold() <= 0) {
                startDying();
            } else {
                health = 1;
                startPlayingDead();
            }
        } else if (form.isColossal() && !playedDead && state == State.FIGHTING
                && health / maxHealth <= settings().playDeadThreshold()) {
            startPlayingDead();
        }
        if (age % 4 == 0) refreshBar();
    }

    private void injure(HeadState h, Player attacker) {
        h.injured = settings().injuryTicks();
        h.hits = 0;
        h.required = rollRequiredHits();
        h.injureCooldown = 40;
        rest(h, settings().beam().restTicks());
        roar(h, true);
        shootFlamingSkull(h, attacker, true, 0);
        if (attacker != null) {
            // The mod's escape time: the player who wounded a head is let go and left alone a while.
            escaping.put(attacker.getUniqueId(), settings().escapeTicks());
            for (HeadState other : heads) {
                if (other.target == attacker) {
                    other.target = null;
                    if (other.beam != Beam.OFF) rest(other, settings().beam().restTicks());
                }
            }
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            attacker.sendActionBar(Component.text("You wounded a head: the storm lets you go for now.",
                    NamedTextColor.LIGHT_PURPLE));
        }
        boolean allDown = true;
        for (HeadState other : heads) {
            if (other.injured <= 0) allDown = false;
        }
        if (allDown) {
            exposedTicks = settings().exposedTicks();
            for (Player p : BossArena.getValidPlayersNear(anchor.getLocation(), 160 * 160)) {
                p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Every head is down: the Wither Storm is exposed!");
            }
        }
    }

    private int rollRequiredHits() {
        int base = form.injuryHits();
        return base + ThreadLocalRandom.current().nextInt(base + 1);
    }

    private void hitEffect(HeadState h) {
        Vector at = h != null && h.centre != null ? h.centre : anchor.getLocation().toVector().add(new Vector(0, 2 * size(), 0));
        World world = world();
        ParticleBudget.spawn(world, Particle.DAMAGE_INDICATOR, at.getX(), at.getY(), at.getZ(), 6, 0.6, 0.6, 0.6, 0.1, null);
        if (ThreadLocalRandom.current().nextInt(3) == 0) {
            world.playSound(at.toLocation(world), Sound.ENTITY_WITHER_HURT, 1.5f, 0.5f);
        }
    }

    private void shieldSpark(int head) {
        HeadState h = head >= 0 ? head(head) : null;
        Vector at = h != null && h.centre != null ? h.centre : anchor.getLocation().toVector().add(new Vector(0, 2, 0));
        ParticleBudget.spawn(world(), Particle.ENCHANTED_HIT, at.getX(), at.getY(), at.getZ(), 8, 0.5, 0.5, 0.5, 0.1, null);
    }

    // ------------------------------------------------------------------ hitboxes

    /** The mass's box at rest, cached per form and size, as {minX, minY, minZ, maxX, maxY, maxZ}. */
    private float[] restBounds() {
        if (restBounds == null) {
            float[] mass = model.bounds(new WitherStormModel.Pose(), size(), WitherStormModel.Kind.MASS);
            float[] base = model.bounds(new WitherStormModel.Pose(), size(), WitherStormModel.Kind.BASE);
            restBounds = union(mass, base);
        }
        return restBounds;
    }

    private static float[] union(float[] a, float[] b) {
        if (b[0] > b[3]) return a;
        if (a[0] > a[3]) return b;
        return new float[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                Math.max(a[3], b[3]), Math.max(a[4], b[4]), Math.max(a[5], b[5])};
    }

    private void syncHitboxes() {
        float[] b = restBounds();
        World world = world();
        Vector3f centre = new Vector3f((b[0] + b[3]) / 2, b[1], (b[2] + b[5]) / 2);
        float width = Math.max(1f, Math.min(b[3] - b[0], b[5] - b[2]) * 0.85f);
        float height = Math.max(1f, (b[4] - b[1]) * 0.9f);
        bodyBox = place(bodyBox, toWorld(centre), width, height, -1, world);
        for (HeadState h : heads) {
            if (h.centre == null) continue;
            float w = Math.max(0.8f, h.width);
            Vector bottom = h.centre.clone().subtract(new Vector(0, w / 2, 0));
            headBoxes.put(h.gaze.index(), place(headBoxes.get(h.gaze.index()), bottom, w, w, h.gaze.index(), world));
        }
    }

    private Interaction place(Interaction box, Vector bottom, float width, float height, int head, World world) {
        Location at = bottom.toLocation(world);
        if (!world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) return box;
        if (box == null || !box.isValid()) {
            box = world.spawn(at, Interaction.class, i -> {
                i.setPersistent(false);
                i.addScoreboardTag(WitherStormBoss.HITBOX_TAG);
                i.addScoreboardTag(ownerTag);
                i.setResponsive(true);
            });
            boss.registerHitbox(box.getUniqueId(), this, head);
        } else if (box.getLocation().toVector().distanceSquared(bottom) > 0.04) {
            box.teleport(at);
        }
        if (Math.abs(box.getInteractionWidth() - width) > 0.05f) box.setInteractionWidth(width);
        if (Math.abs(box.getInteractionHeight() - height) > 0.05f) box.setInteractionHeight(height);
        return box;
    }

    private void removeHitboxes() {
        if (bodyBox != null) {
            boss.unregisterHitbox(bodyBox.getUniqueId());
            bodyBox.remove();
            bodyBox = null;
        }
        for (Interaction box : headBoxes.values()) {
            if (box != null) {
                boss.unregisterHitbox(box.getUniqueId());
                box.remove();
            }
        }
        headBoxes.clear();
    }

    // ------------------------------------------------------------------ bar, saving, removal

    private void refreshBar() {
        if (bar == null) {
            bar = MscBossBar.create(title(), BarColor.PURPLE, BarStyle.SEGMENTED_10, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
        }
        bar.setTitle(title());
        bar.setProgress(MscEntityUtils.calculateVirtualProgress(health, maxHealth));
        MscBossBar.showNear(bar, anchor.getLocation(), settings().aggroRange() * Math.max(1.5, size()) + 32);
    }

    private String title() {
        String name = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "WITHER STORM" + ChatColor.DARK_GRAY + " - "
                + ChatColor.LIGHT_PURPLE + form.displayName();
        return switch (state) {
            case FORMING -> name + ChatColor.GRAY + " (forming)";
            case EVOLVING -> name + ChatColor.GRAY + " (evolving)";
            case PLAYING_DEAD -> name + ChatColor.GRAY + " (...)";
            case DYING -> name + ChatColor.GRAY + " (dying)";
            default -> exposedTicks > 0 ? name + ChatColor.RED + " (exposed)" : name;
        };
    }

    void save() {
        var pdc = anchor.getPersistentDataContainer();
        pdc.set(KEYS.form, PersistentDataType.STRING, (state == State.EVOLVING && evolvingTo != null ? evolvingTo : form).key());
        pdc.set(KEYS.consumed, PersistentDataType.INTEGER, consumed);
        pdc.set(MscEntityUtils.KEY_VIRTUAL_HEALTH, PersistentDataType.DOUBLE, Math.max(1, health));
        pdc.set(MscEntityUtils.KEY_VIRTUAL_MAX_HEALTH, PersistentDataType.DOUBLE, maxHealth);
    }

    /** Takes down everything the storm put in the world except the anchor, which carries its save. */
    void discard() {
        if (body != null) body.remove();
        if (oldBody != null) oldBody.remove();
        removeHitboxes();
        for (FlamingSkull s : skulls) {
            if (s.display != null) s.display.remove();
        }
        skulls.clear();
        for (Debris d : debris) {
            if (d.display != null) d.display.remove();
        }
        debris.clear();
        specials.cancel();
        if (bar != null) {
            bar.removeAll();
            bar = null;
        }
    }

    /** Removes the storm for good, with its summons and the mobs it sickened: nobody was fighting it. */
    void leave() {
        cureSickened();
        specials.dismiss();
        discard();
        anchor.remove();
    }

    static float wrap(float degrees) {
        float d = degrees % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}
