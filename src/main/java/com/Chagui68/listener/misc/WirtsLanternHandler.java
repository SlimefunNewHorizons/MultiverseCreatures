package com.Chagui68.listener.misc;

import com.Chagui68.items.misc.WirtsLantern;
import com.Chagui68.ritual.BossDimensionManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wirt's Lantern, held in either hand: hostile mobs are pushed away and stop targeting the holder,
 * mobs cannot hurt them once the lantern has been held for {@code arm-ticks}, and the holder cannot
 * hurt anything while holding it nor for {@code disarm-ticks} after putting it away, so swapping to
 * a weapon between hits gives no immunity. Inside the Boss Dimension it does nothing.
 */
public class WirtsLanternHandler implements Listener {

    /** Potion effects that hurt over time; instant damage lands as an ordinary damage event. */
    private static final Set<PotionEffectType> DAMAGING_EFFECTS = Set.of(PotionEffectType.POISON, PotionEffectType.WITHER);

    private final Plugin plugin;
    private final Random random = new Random();
    /** When each player last took the lantern into a hand, while it is still there. */
    private final Map<UUID, Long> heldSince = new ConcurrentHashMap<>();
    /** When each player last had the lantern in a hand. */
    private final Map<UUID, Long> lastHeld = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final boolean repelEnabled;
    private final boolean nightVisionEnabled;
    private final boolean targetProtection;
    private final boolean mobProtection;
    private final long armMillis;
    private final long disarmMillis;
    private final double repelRadius;
    private final int repelIntervalTicks;
    private final double particleChance;
    private final double horizontalForce;
    private final double verticalForce;
    /** The repel/aura loop; held so the plugin can end it on shutdown. */
    private BukkitTask ticker;

    public WirtsLanternHandler(Plugin plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig();
        String p = "items.wirts-lantern.";
        enabled = config.getBoolean(p + "enabled", true);
        repelEnabled = config.getBoolean(p + "mob-repel.enabled", true);
        nightVisionEnabled = config.getBoolean(p + "night-vision.enabled", true);
        targetProtection = config.getBoolean(p + "target-protection.enabled", true);
        mobProtection = config.getBoolean(p + "mob-protection.enabled", true);
        armMillis = Math.max(0, config.getInt(p + "arm-ticks", 30)) * 50L;
        disarmMillis = Math.max(0, config.getInt(p + "disarm-ticks", 60)) * 50L;
        repelRadius = config.getDouble(p + "mob-repel.radius", 12.0);
        repelIntervalTicks = Math.max(1, config.getInt(p + "mob-repel.interval-ticks", 20));
        particleChance = config.getDouble(p + "mob-repel.particle-chance", 0.3);
        horizontalForce = config.getDouble(p + "mob-repel.horizontal-force", 0.6);
        verticalForce = config.getDouble(p + "mob-repel.vertical-force", 0.2);
        startRepelTask();
    }

    private void startRepelTask() {
        if (ticker != null) ticker.cancel();
        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                // Every online player, so a lantern put in a hand from the inventory or by /give counts too.
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!track(p)) continue;
                    if (nightVisionEnabled) applyNightVision(p);
                    if (repelEnabled || targetProtection) repelNearbyEntities(p);
                }
            }
        }.runTaskTimer(plugin, 0L, repelIntervalTicks);
    }

    /** Stops the repel loop; the plugin calls this from its own {@code onDisable}. */
    public void stopTasks() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    /** Lantern in either hand, outside the Boss Dimension: inside it the lantern does nothing at all. */
    private boolean hasLantern(Player p) {
        if (!enabled || inBossDimension(p)) return false;
        return isLantern(p.getInventory().getItemInMainHand()) || isLantern(p.getInventory().getItemInOffHand());
    }

    private static boolean inBossDimension(Entity entity) {
        return entity.getWorld().getName().equals(BossDimensionManager.WORLD_NAME);
    }

    private boolean isLantern(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(WirtsLantern.WIRTS_LANTERN_KEY, PersistentDataType.INTEGER);
    }

    /** Refreshes when {@code p} took and last had the lantern; true while it is in a hand. */
    private boolean track(Player p) {
        long now = System.currentTimeMillis();
        UUID id = p.getUniqueId();
        if (hasLantern(p)) {
            heldSince.putIfAbsent(id, now);
            lastHeld.put(id, now);
            return true;
        }
        if (heldSince.remove(id) != null) lastHeld.put(id, now);
        return false;
    }

    /** Mobs cannot hurt {@code p}: the lantern has been in their hand for at least {@code arm-ticks}. */
    private boolean shielded(Player p) {
        if (!mobProtection || !track(p)) return false;
        Long since = heldSince.get(p.getUniqueId());
        return since != null && System.currentTimeMillis() - since >= armMillis;
    }

    /** {@code p} deals no damage: the lantern is in their hand or left it less than {@code disarm-ticks} ago. */
    private boolean pacified(Player p) {
        if (inBossDimension(p)) return false;
        if (track(p)) return true;
        Long last = lastHeld.get(p.getUniqueId());
        return last != null && System.currentTimeMillis() - last < disarmMillis;
    }

    private void repelNearbyEntities(Player p) {
        Location center = p.getLocation();
        for (Entity entity : p.getWorld().getNearbyEntities(center, repelRadius, repelRadius, repelRadius)) {
            // Only hostile mobs: villagers, pets, armor stands and the holder's own mount stay put.
            if (!(entity instanceof Enemy) || !(entity instanceof LivingEntity living)) continue;
            if (entity.equals(p.getVehicle()) || entity.getPassengers().contains(p)) continue;

            if (repelEnabled) {
                Vector direction = living.getLocation().toVector().subtract(center.toVector());
                if (direction.lengthSquared() > 0) {
                    Vector away = direction.normalize().multiply(horizontalForce);
                    away.setY(Math.min(away.getY() + verticalForce, 0.6));
                    living.setVelocity(living.getVelocity().add(away));
                }
                if (random.nextDouble() < particleChance) {
                    p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, living.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.01);
                }
            }

            if (targetProtection && living instanceof Mob mob && mob.getTarget() == p) {
                mob.setTarget(null);
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent e) {
        if (!targetProtection || !(e.getTarget() instanceof Player p)) return;
        if (e.getEntity() instanceof Enemy && track(p)) e.setCancelled(true);
    }

    /** Only mobs are kept off: players, the void, falls, fire, drowning, hunger and /kill still hurt. */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Entity cause = e.getDamageSource().getCausingEntity();
        if (cause == null && e instanceof EntityDamageByEntityEvent byEntity) cause = byEntity.getDamager();
        if (cause instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) cause = shooter;
        if (!(cause instanceof LivingEntity) || owner(cause) != null) return;
        if (shielded(p)) e.setCancelled(true);
    }

    /** A holder deals no damage to anything, players included, by any means that traces back to them. */
    @EventHandler
    public void onHolderDealsDamage(EntityDamageEvent e) {
        Player holder = owner(e.getDamageSource().getCausingEntity());
        if (holder == null && e instanceof EntityDamageByEntityEvent byEntity) holder = owner(byEntity.getDamager());
        if (holder == null || holder.equals(e.getEntity()) || !pacified(holder)) return;
        e.setCancelled(true);
    }

    /** Fire from a holder's flame arrows, fire aspect or fireballs. */
    @EventHandler
    public void onHolderIgnitesEntity(EntityCombustByEntityEvent e) {
        Player holder = owner(e.getCombuster());
        if (holder != null && !holder.equals(e.getEntity()) && pacified(holder)) e.setCancelled(true);
    }

    /** Poison and wither from a holder's splash potions: instant damage already goes through the damage event. */
    @EventHandler
    public void onHolderSplash(PotionSplashEvent e) {
        if (!(e.getPotion().getShooter() instanceof Player holder) || !pacified(holder)) return;
        if (e.getPotion().getEffects().stream().noneMatch(effect -> DAMAGING_EFFECTS.contains(effect.getType()))) return;
        for (LivingEntity affected : e.getAffectedEntities()) {
            if (!affected.equals(holder)) e.setIntensity(affected, 0);
        }
    }

    /** The same for a holder's lingering clouds. */
    @EventHandler
    public void onHolderCloud(AreaEffectCloudApplyEvent e) {
        if (!(e.getEntity().getSource() instanceof Player holder) || !pacified(holder)) return;
        AreaEffectCloud cloud = e.getEntity();
        boolean damaging = cloud.getCustomEffects().stream().anyMatch(effect -> DAMAGING_EFFECTS.contains(effect.getType()))
                || (cloud.getBasePotionType() != null && cloud.getBasePotionType().getPotionEffects().stream()
                        .anyMatch(effect -> DAMAGING_EFFECTS.contains(effect.getType())));
        if (damaging) e.getAffectedEntities().removeIf(affected -> !affected.equals(holder));
    }

    /** No lava poured while holding it. */
    @EventHandler
    public void onHolderPoursLava(PlayerBucketEmptyEvent e) {
        if (e.getBucket() == Material.LAVA_BUCKET && pacified(e.getPlayer())) e.setCancelled(true);
    }

    /** No fire lit while holding it. */
    @EventHandler
    public void onHolderLightsFire(BlockIgniteEvent e) {
        if (e.getPlayer() != null && pacified(e.getPlayer())) e.setCancelled(true);
    }

    /** The player behind a damage source: themselves, their projectile, TNT, potion cloud or tamed pet. */
    private Player owner(Entity source) {
        if (source instanceof Player p) return p;
        if (source instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        if (source instanceof TNTPrimed tnt) return owner(tnt.getSource());
        if (source instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player p) return p;
        if (source instanceof Tameable pet && pet.getOwner() instanceof Player p) return p;
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        heldSince.remove(e.getPlayer().getUniqueId());
        lastHeld.remove(e.getPlayer().getUniqueId());
    }

    /** It can never be placed or hung on a stand, anywhere: a placed lantern would lose what makes it Wirt's. */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || !isLantern(item)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        ItemStack item = e.getPlayerItem();
        if (item == null || !isLantern(item)) return;
        e.setCancelled(true);
    }

    private void applyNightVision(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, false, false), false);
    }
}
