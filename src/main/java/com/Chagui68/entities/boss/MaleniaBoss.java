package com.Chagui68.entities.boss;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.utils.DisplaySculpt.Piece;
import com.Chagui68.utils.MscBossBar;
import com.Chagui68.utils.MscEntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
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
import org.bukkit.util.Transformation;
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
 * Malenia, Blade of Miquella: the {@link MaleniaModel} body fighting with the {@link MaleniaRig} attacks.
 *
 * <p>An invisible armour stand carries her hitbox, health and boss bar. Her block displays stand at an
 * anchor and are placed by a carry matrix ({@link MaleniaRig#bones(MaleniaRig.Move, float, Matrix4f)}),
 * so walking and turning move her hundreds of pieces by their interpolated transformation instead of
 * teleporting each one; the anchor catches up with her when she strays {@link #REANCHOR_DISTANCE} from it.
 * Her displays are never saved: after a restart or a chunk reload the stand is dressed again.</p>
 *
 * <p>She picks an attack by distance: close, the three-slash combo or the whirlwind; mid range, the
 * lunging thrust; far, the leaping slam; below half health, the Waterfowl Dance now and then. During an
 * attack the stand follows her root, her blade hurts whoever it passes through (once per swing), the
 * slashes round her hurt everyone near, a landing shakes the ground, and every hit she lands heals her.</p>
 */
public class MaleniaBoss implements Listener {

    public static final String TAG = "MSC_Malenia";
    private static final String BAR_TITLE = ChatColor.RED + "Malenia, Blade of Miquella";
    /** How far the stand may wander from her displays before they are moved to it. */
    static final double REANCHOR_DISTANCE = 6.0;
    private static final double SLASH_RADIUS = 2.6;
    private static final double RING_RADIUS = 4.0;
    private static final MaleniaRig.Move IDLE = MaleniaRig.move("idle");

    private final MultiverseCreatures plugin;
    private final Random random = new Random();
    private final Map<UUID, Instance> active = new HashMap<>();
    private final List<Piece> pieces = MaleniaModel.pieces();
    private final int[] owners = MaleniaRig.owners(pieces);
    private BukkitTask ticker;

    private final double health;
    private final double maxDamagePerHit;
    private final double hitboxScale;
    private final double aggroRange;
    private final double moveSpeed;
    private final double bladeDamage;
    private final double slashDamage;
    private final double ringDamage;
    private final double lifesteal;
    private final int attackCooldownTicks;
    private final int waterfowlCooldownTicks;

    public MaleniaBoss(MultiverseCreatures plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig();
        String p = "entities.malenia.";
        health = config.getDouble(p + "health", 1500.0);
        maxDamagePerHit = config.getDouble(p + "max-damage-per-hit", 10.0);
        hitboxScale = MscEntityUtils.clampHitboxScale(config.getDouble(p + "hitbox-scale", 1.3));
        aggroRange = config.getDouble(p + "aggro-range", 30.0);
        moveSpeed = config.getDouble(p + "move-speed", 0.3);
        bladeDamage = config.getDouble(p + "blade-damage", 12.0);
        slashDamage = config.getDouble(p + "slash-damage", 4.0);
        ringDamage = config.getDouble(p + "ring-damage", 10.0);
        lifesteal = config.getDouble(p + "lifesteal", 0.5);
        attackCooldownTicks = config.getInt(p + "attack-cooldown-ticks", 20);
        waterfowlCooldownTicks = config.getInt(p + "waterfowl-cooldown-ticks", 400);
        if (!plugin.isEnabled("entities.malenia")) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        reloadExisting();
        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                for (Instance inst : new ArrayList<>(active.values())) tick(inst);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Stops the tick loop and takes her bodies and bars down; the plugin calls this from {@code onDisable}. */
    public void stopTasks() {
        if (ticker != null) ticker.cancel();
        ticker = null;
        active.values().forEach(this::cleanup);
        active.clear();
    }

    private void reloadExisting() {
        for (World world : Bukkit.getWorlds()) {
            // Displays left by a reload: the stands below get a fresh body.
            for (BlockDisplay display : world.getEntitiesByClass(BlockDisplay.class)) {
                if (display.getScoreboardTags().contains(MaleniaModel.TAG)) display.remove();
            }
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (!stand.getScoreboardTags().contains(TAG)) continue;
                if (!stand.getPersistentDataContainer().has(MscEntityUtils.KEY_VIRTUAL_MAX_HEALTH, PersistentDataType.DOUBLE)) {
                    MscEntityUtils.initVirtualHealth(stand, health);
                }
                adopt(stand);
            }
        }
    }

    public boolean trySpawn(Location location) {
        if (!plugin.isEnabled("entities.malenia") || location.getWorld() == null) return false;
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
            s.addScoreboardTag(TAG);
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
        inst.bossBar = MscBossBar.create(BAR_TITLE, BarColor.RED, BarStyle.SEGMENTED_10);
        MscBossBar.showInWorld(inst.bossBar, stand.getWorld());
        active.put(stand.getUniqueId(), inst);
        return inst;
    }

    /** A new body at the stand, replacing whatever is left of the old one. */
    private void dress(Instance inst) {
        inst.body.forEach(BlockDisplay::remove);
        inst.anchor = inst.stand.getLocation();
        inst.anchor.setPitch(0f);
        inst.body = MaleniaModel.spawn(pieces, inst.anchor, inst.anchor.getYaw());
        inst.resting = false;
    }

    private void cleanup(Instance inst) {
        inst.body.forEach(BlockDisplay::remove);
        inst.body = List.of();
        if (inst.bossBar != null) inst.bossBar.removeAll();
        inst.bossBar = null;
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
        if (BossDespawn.abandoned(stand)) {
            cleanup(inst);
            active.remove(stand.getUniqueId());
            stand.remove();
            return;
        }
        // Not saved with the world: a chunk unload takes the displays and leaves the stand.
        if (inst.body.isEmpty() || !inst.body.get(0).isValid()) dress(inst);
        inst.ticks++;
        if (inst.cooldown > 0) inst.cooldown--;
        if (inst.waterfowlCooldown > 0) inst.waterfowlCooldown--;
        if (inst.move != null) attack(inst);
        else think(inst);

        if (inst.bossBar != null) {
            inst.bossBar.setProgress(MscEntityUtils.calculateVirtualProgress(
                    MscEntityUtils.getVirtualHealth(stand), MscEntityUtils.getVirtualMaxHealth(stand)));
            if (inst.ticks % 20 == 0) MscBossBar.showInWorld(inst.bossBar, stand.getWorld());
        }
    }

    /** Between attacks: turn to the nearest player, start an attack in reach or walk towards them, sway. */
    private void think(Instance inst) {
        Location at = inst.stand.getLocation();
        at.setPitch(0f);
        Player target = BossArena.findNearestPlayer(at, aggroRange);
        if (target != null) {
            Vector to = target.getLocation().toVector().subtract(at.toVector()).setY(0);
            double dist = to.length();
            if (dist > 1e-3) {
                at.setDirection(to);
                at.setPitch(0f);
            }
            MaleniaRig.Move attack = inst.cooldown <= 0 ? choose(inst, dist) : null;
            if (attack != null) {
                inst.stand.teleport(at);
                inst.move = attack;
                inst.moveTick = 0;
                inst.base = at.clone();
                inst.struck.clear();
                return;
            }
            if (dist > 2.5) BossArena.walk(at, to.normalize().multiply(Math.min(moveSpeed, dist - 2.5)), true);
        }
        BossArena.settle(at);
        inst.stand.teleport(at);
        if (target == null) {
            // Nobody to fight: one last pose at rest, then her pieces send nothing at all.
            if (!inst.resting) show(inst, IDLE, 0, at, 4);
            inst.resting = true;
            return;
        }
        inst.resting = false;
        inst.idleTick++;
        // Walking and swaying are slow and even, so every 4 ticks with the game interpolating between
        // looks the same as every 2 and sends half the packets; only attacks need every 2.
        if (inst.idleTick % 4 == 0) show(inst, IDLE, inst.idleTick % IDLE.ticks(), at, 4);
    }

    /** The attack for a player {@code dist} blocks away, or null when none reaches. */
    private MaleniaRig.Move choose(Instance inst, double dist) {
        double hp = MscEntityUtils.calculateVirtualProgress(
                MscEntityUtils.getVirtualHealth(inst.stand), MscEntityUtils.getVirtualMaxHealth(inst.stand));
        if (hp < .5 && inst.waterfowlCooldown <= 0 && dist <= 6) {
            inst.waterfowlCooldown = waterfowlCooldownTicks;
            return MaleniaRig.move("waterfowl");
        }
        if (dist <= 3.5) return MaleniaRig.move(random.nextBoolean() ? "combo" : "whirlwind");
        if (dist <= 6.5) return MaleniaRig.move("thrust");
        if (dist <= 10) return MaleniaRig.move("leap");
        return null;
    }

    /** One tick of the attack under way: the stand follows her root, her blows land, and every two ticks she is posed. */
    private void attack(Instance inst) {
        MaleniaRig.Move move = inst.move;
        int t = ++inst.moveTick;
        float[] travel = MaleniaRig.sample(move.travel(), t);
        Location to = MaleniaRig.toWorld(inst.base, inst.base.getYaw(), new Vector3f(travel[0], travel[1], travel[2]));
        if (!to.getBlock().isSolid() && !to.clone().add(0, 1, 0).getBlock().isSolid()) inst.stand.teleport(to);
        // Half ticks too: in the Waterfowl Dance her blade turns a quarter circle a tick.
        strike(inst, move, t - .5f);
        strike(inst, move, t);
        if (t % 2 == 0 || t >= move.ticks()) {
            Matrix4f carry = carry(inst.anchor, inst.base);
            send(inst, MaleniaRig.pose(move, t, pieces, owners, carry), 2);
            MaleniaRig.effects(inst.anchor, inst.anchor.getYaw(), move, t, MaleniaRig.bones(move, t, carry), 2);
        }
        if (t < move.ticks()) return;
        inst.move = null;
        inst.cooldown = attackCooldownTicks;
        inst.idleTick = 0;
        Location end = inst.stand.getLocation();
        end.setYaw(inst.base.getYaw());
        end.setPitch(0f);
        BossArena.settle(end);
        inst.stand.teleport(end);
    }

    /** Her blows at {@code t}: the blade on whoever it passes through, the slashes and the landing on whoever is near. */
    private void strike(Instance inst, MaleniaRig.Move move, float t) {
        List<Player> near = BossArena.getValidPlayersNear(inst.stand.getLocation(), 64);
        if (near.isEmpty()) return;
        Matrix4f[] world = MaleniaRig.bones(move, t);
        Location base = inst.base;
        for (int i = 0; i < move.trails().size(); i++) {
            int[] trail = move.trails().get(i);
            if (t < trail[0] || t > trail[1]) continue;
            for (int k = 0; k <= 4; k++) {
                Vector3f point = world[MaleniaRig.Bone.HAND_R.ordinal()].transformPosition(MaleniaModel.bladePoint(k / 4f));
                Vector at = MaleniaRig.toWorld(base, base.getYaw(), point).toVector();
                for (Player p : near) {
                    if (p.getBoundingBox().expand(.3).contains(at) && inst.struck.add(i + ":" + p.getUniqueId())) {
                        hurt(inst, p, bladeDamage);
                    }
                }
            }
        }
        if (t != (int) t) return;
        Location root = MaleniaRig.toWorld(base, base.getYaw(),
                world[MaleniaRig.Bone.ROOT.ordinal()].transformPosition(new Vector3f(0, 1.3f, 0)));
        for (int[] range : move.slashes()) {
            if (t < range[0] || t > range[1] || (int) t % 4 != 0) continue;
            for (Player p : near) {
                if (p.getLocation().add(0, 1, 0).distanceSquared(root) <= SLASH_RADIUS * SLASH_RADIUS) hurt(inst, p, slashDamage);
            }
        }
        for (int ring : move.rings()) {
            if (ring != t) continue;
            Location ground = root.clone().subtract(0, 1.25, 0);
            for (Player p : near) {
                Location l = p.getLocation();
                if (Math.abs(l.getY() - ground.getY()) > 2 || Math.hypot(l.getX() - ground.getX(), l.getZ() - ground.getZ()) > RING_RADIUS) continue;
                hurt(inst, p, ringDamage);
                BossArena.launchPlayer(p, .7);
            }
        }
    }

    /** Her hit on a player, which heals her by {@code lifesteal} of it. */
    private void hurt(Instance inst, Player p, double damage) {
        p.damage(damage, inst.stand);
        ArmorStand stand = inst.stand;
        MscEntityUtils.setVirtualHealth(stand, MscEntityUtils.getVirtualHealth(stand) + damage * lifesteal);
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 8, .3, .4, .3, 0,
                new Particle.DustOptions(Color.fromRGB(200, 20, 30), 1.2f));
    }

    /** Poses her body standing at {@code at}, moving her displays to it first once it has strayed too far. */
    private void show(Instance inst, MaleniaRig.Move move, float tick, Location at, int ticks) {
        if (at.distanceSquared(inst.anchor) > REANCHOR_DISTANCE * REANCHOR_DISTANCE) {
            inst.anchor = at.clone();
            inst.body.forEach(d -> d.teleport(inst.anchor));
            ticks = 0; // the teleport is instant, so the pose must be too
        }
        send(inst, MaleniaRig.pose(move, tick, pieces, owners, carry(inst.anchor, at)), ticks);
    }

    /** Sends every piece its pose, reached over {@code ticks}. */
    private static void send(Instance inst, List<Transformation> posed, int ticks) {
        for (int i = 0; i < inst.body.size(); i++) MaleniaModel.animate(inst.body.get(i), posed.get(i), ticks);
    }

    /**
     * The carry ({@link MaleniaRig#bones(MaleniaRig.Move, float, Matrix4f)}) that draws a body standing
     * at {@code at} with displays at {@code anchor}: the offset in the anchor's frame, then the turn
     * between their headings.
     */
    static Matrix4f carry(Location anchor, Location at) {
        Vector3f local = new Vector3f((float) (at.getX() - anchor.getX()), (float) (at.getY() - anchor.getY()),
                (float) (at.getZ() - anchor.getZ())).rotateY((float) Math.toRadians(anchor.getYaw()));
        return new Matrix4f().translation(local).rotateY((float) Math.toRadians(anchor.getYaw() - at.getYaw()));
    }

    // =====================================================================================
    // Events
    // =====================================================================================

    /** Only players hurt her, through the virtual health, at most {@code max-damage-per-hit} a hit. */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand) || !stand.getScoreboardTags().contains(TAG)) return;
        event.setCancelled(true);
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;
        Player player = byEntity.getDamager() instanceof Player p ? p
                : byEntity.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p ? p : null;
        if (player == null) return;
        double damage = Math.max(1.0, event.getFinalDamage());
        if (maxDamagePerHit > 0) damage = Math.min(damage, maxDamagePerHit);
        MscEntityUtils.setVirtualHealth(stand, MscEntityUtils.getVirtualHealth(stand) - damage);
        Location chest = stand.getLocation().add(0, 1.5, 0);
        stand.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, chest, 6, .3, .5, .3, .1);
        stand.getWorld().playSound(chest, Sound.ENTITY_PLAYER_ATTACK_CRIT, .8f, 1.2f);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand) || !stand.getScoreboardTags().contains(TAG)) return;
        event.getDrops().clear();
        event.setDroppedExp(500);
        Instance inst = active.remove(stand.getUniqueId());
        if (inst != null) cleanup(inst);
        Location at = stand.getLocation().add(0, 1.2, 0);
        stand.getWorld().playSound(at, Sound.ENTITY_WITHER_DEATH, 1f, 1.4f);
        stand.getWorld().spawnParticle(Particle.DUST, at, 80, .8, 1.2, .8, 0, new Particle.DustOptions(Color.fromRGB(200, 20, 30), 2f));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!(event.getDamageSource().getCausingEntity() instanceof ArmorStand stand)) return;
        if (!stand.getScoreboardTags().contains(TAG)) return;
        List<String> messages = plugin.getConfig().getStringList("entities.malenia.death-messages");
        if (messages.isEmpty()) return;
        String raw = messages.get(random.nextInt(messages.size()));
        event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', raw.replace("%player%", event.getEntity().getName())));
    }

    private static final class Instance {
        final ArmorStand stand;
        List<BlockDisplay> body = List.of();
        /** Where her displays stand, facing their yaw. */
        Location anchor;
        /** The attack under way, or null. */
        MaleniaRig.Move move;
        /** Where the attack under way started, facing her heading then. */
        Location base;
        int ticks, moveTick, idleTick, cooldown, waterfowlCooldown;
        /** No one to fight and her rest pose sent: nothing is posed until a player comes. */
        boolean resting;
        /** Swing and player pairs the blade has already hit in this attack. */
        final Set<String> struck = new HashSet<>();
        BossBar bossBar;

        Instance(ArmorStand stand) {
            this.stand = stand;
        }
    }
}
