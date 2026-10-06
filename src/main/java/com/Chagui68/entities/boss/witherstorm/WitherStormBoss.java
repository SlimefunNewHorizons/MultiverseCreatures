package com.Chagui68.entities.boss.witherstorm;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.boss.BossDespawn;
import com.Chagui68.entities.boss.TrueDamage;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.Chagui68.utils.MscEntityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Wither Storm of Cracker's Wither Storm Mod, drawn with block displays and fought in the
 * overworld.
 *
 * <p>It is summoned with the Wither's own structure: four soul sand (or soul soil) and three wither
 * skeleton skulls, built on top of the configured core block (crying obsidian by default). The Wither
 * that structure would have made is swallowed and the storm forms in its place; built anywhere else
 * the structure still makes a plain Wither. {@code /msc spawn witherstorm} summons it too, and
 * {@code witherstorm2}..{@code witherstorm5} skip straight to a later form.</p>
 *
 * <p>This class keeps the storms alive across ticks, reloads and restarts, and routes what happens to
 * them: hits on their hitboxes, projectiles, their anchors dying. What a storm does lives in
 * {@link WitherStorm}; how it looks, in {@link WitherStormModel} and {@link WitherStormBody}.</p>
 */
public final class WitherStormBoss implements Listener {

    /** The anchor of every storm. */
    public static final String TAG = "MSC_WitherStorm";
    static final String OWNER_PREFIX = "MSC_WitherStormOwner_";
    static final String HITBOX_TAG = "MSC_WitherStormHitbox";
    static final String DEBRIS_TAG = "MSC_WitherStormDebris";
    static final String SKULL_TAG = "MSC_WitherStormSkull";
    static final String PROJECTILE_TAG = "MSC_WitherStormProjectile";
    static final String SICKENED_TAG = "MSC_WitherStormSickened";
    static final String PHANTOM_TAG = "MSC_WitherStormPhantom";

    private record Hitbox(WitherStorm storm, int head) {
    }

    private final MultiverseCreatures plugin;
    private WitherStormSettings settings;
    private final Map<UUID, WitherStorm> storms = new HashMap<>();
    private final Map<UUID, Hitbox> hitboxes = new HashMap<>();
    /**
     * The loop that ticks every storm. Held so {@link #stopTasks()} can end it: a second start would
     * otherwise leave two loops ticking the same storms.
     */
    private BukkitTask ticker;

    public WitherStormBoss(MultiverseCreatures plugin) {
        this.plugin = plugin;
        reloadConfig();
        if (!plugin.isEnabled("entities.wither-storm")) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        sweepStrays();
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (stand.getScoreboardTags().contains(TAG)) adopt(stand);
            }
        }
        startTicker();
    }

    public void reloadConfig() {
        settings = WitherStormSettings.load(plugin.getConfig());
        infinityWeaponMultiplier = plugin.getConfig().getDouble("boss-balance.infinity-weapon-damage-multiplier", 0.5);
    }

    WitherStormSettings settings() {
        return settings;
    }

    MultiverseCreatures plugin() {
        return plugin;
    }

    private void startTicker() {
        if (ticker != null) ticker.cancel();
        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                for (WitherStorm storm : new ArrayList<>(storms.values())) {
                    if (!storm.anchor.getWorld().isChunkLoaded(storm.anchor.getLocation().getBlockX() >> 4,
                            storm.anchor.getLocation().getBlockZ() >> 4)) {
                        continue;
                    }
                    if (BossDespawn.abandoned(storm.anchor, storm.form.isColossal())) {
                        storm.leave();
                        storms.remove(storm.anchor.getUniqueId());
                        continue;
                    }
                    if (!storm.tick()) {
                        storms.remove(storm.anchor.getUniqueId());
                    } else {
                        catchProjectiles(storm);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Stops the tick loop and takes every body down; the anchors keep the storms for the next start. */
    public void stopTasks() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        for (WitherStorm storm : storms.values()) {
            storm.save();
            storm.discard();
        }
        storms.clear();
        hitboxes.clear();
    }

    // ------------------------------------------------------------------ spawning

    /** Summons the first form, born out of its vortex. */
    public boolean trySpawn(Location location) {
        return summon(location, WitherStormForm.HUNCHBACK, true);
    }

    /** Summons a storm already grown into {@code form}; the first form is still born out of its vortex. */
    public boolean trySpawn(Location location, WitherStormForm form) {
        return summon(location, form, form == WitherStormForm.HUNCHBACK);
    }

    private boolean summon(Location location, WitherStormForm form, boolean forming) {
        if (!settings.enabled() || location.getWorld() == null) return false;
        if (countIn(location.getWorld()) >= settings.maxPerWorld()) return false;
        Location at = location.clone();
        at.setPitch(0f);
        ArmorStand anchor = location.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setMarker(true);
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setBasePlate(false);
            stand.setCanPickupItems(false);
            stand.setPersistent(true);
            stand.setRemoveWhenFarAway(false);
            stand.addScoreboardTag(TAG);
            stand.getPersistentDataContainer().set(WitherStorm.KEYS.form, PersistentDataType.STRING, form.key());
        });
        WitherStorm storm = new WitherStorm(this, anchor, form,
                forming ? WitherStorm.State.FORMING : WitherStorm.State.FIGHTING);
        storm.maxHealth = maxHealthFor(form);
        storm.health = forming ? 1 : storm.maxHealth;
        storm.save();
        storms.put(anchor.getUniqueId(), storm);
        return true;
    }

    double maxHealthFor(WitherStormForm form) {
        return settings.health() * settings.form(form).healthMultiplier();
    }

    /** Takes over a storm whose anchor survived a restart or a reload, and builds its body again. */
    private void adopt(ArmorStand anchor) {
        WitherStorm known = storms.get(anchor.getUniqueId());
        if (known != null) {
            // A chunk that unloaded and loaded again hands back a new entity for the same anchor: the
            // storm still holding the old one could never move it again.
            if (known.anchor.isValid()) return;
            known.discard();
            storms.remove(anchor.getUniqueId());
        }
        PersistentDataContainer pdc = anchor.getPersistentDataContainer();
        WitherStormForm form = WitherStormForm.byKey(pdc.getOrDefault(WitherStorm.KEYS.form, PersistentDataType.STRING, "hunchback"));
        if (form == null) form = WitherStormForm.HUNCHBACK;
        for (Entity passenger : new ArrayList<>(anchor.getPassengers())) {
            if (passenger.getScoreboardTags().contains(WitherStormBody.PART_TAG)) {
                anchor.removePassenger(passenger);
                passenger.remove();
            }
        }
        WitherStorm storm = new WitherStorm(this, anchor, form, WitherStorm.State.FIGHTING);
        storm.maxHealth = maxHealthFor(form);
        storm.health = Math.max(1, Math.min(storm.maxHealth,
                pdc.getOrDefault(MscEntityUtils.KEY_VIRTUAL_HEALTH, PersistentDataType.DOUBLE, storm.maxHealth)));
        storm.consumed = pdc.getOrDefault(WitherStorm.KEYS.consumed, PersistentDataType.INTEGER, 0);
        storm.playedDead = pdc.has(WitherStorm.KEYS.playedDead, PersistentDataType.BYTE);
        storms.put(anchor.getUniqueId(), storm);
    }

    /** Removes body parts, hitboxes and props a stopped run left in loaded worlds. */
    private void sweepStrays() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (!(e instanceof Display) && !(e instanceof Interaction)) continue;
                var tags = e.getScoreboardTags();
                if (tags.contains(WitherStormBody.PART_TAG) || tags.contains(HITBOX_TAG)
                        || tags.contains(DEBRIS_TAG) || tags.contains(SKULL_TAG)) {
                    e.remove();
                }
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof ArmorStand stand && stand.getScoreboardTags().contains(TAG)) adopt(stand);
        }
    }

    public boolean isBossActive() {
        return !storms.isEmpty();
    }

    public boolean isBossActiveIn(World world) {
        return world != null && countIn(world) > 0;
    }

    private int countIn(World world) {
        int n = 0;
        for (WitherStorm storm : storms.values()) {
            if (storm.anchor.isValid() && world.equals(storm.anchor.getWorld())) n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ the summoning

    /**
     * The Wither's structure built on the core block: the Wither it would have made is swallowed, the
     * core with it, and a Wither Storm begins to form where it stood.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWitherBuilt(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER
                || event.getEntityType() != EntityType.WITHER) return;
        if (!settings.enabled() || !settings.summonEnabled()) return;
        Location at = event.getLocation();
        // Vanilla clears the structure before the Wither is added, so the stem's foot is air by now and
        // the core is the block right under it.
        Block core = at.getBlock().getRelative(BlockFace.DOWN);
        if (!isAltarCore(core.getType(), settings.coreBlock())) return;
        if (countIn(at.getWorld()) >= settings.maxPerWorld()) return;
        event.setCancelled(true);
        core.setType(Material.AIR);
        Location spawn = core.getLocation().add(0.5, 0, 0.5);
        Player nearest = com.Chagui68.entities.boss.BossArena.findNearestPlayer(spawn, 64);
        if (nearest != null) {
            org.bukkit.util.Vector look = nearest.getLocation().toVector().subtract(spawn.toVector());
            spawn.setYaw((float) Math.toDegrees(Math.atan2(-look.getX(), look.getZ())));
        }
        if (summon(spawn, WitherStormForm.HUNCHBACK, true)) {
            for (Player p : com.Chagui68.entities.boss.BossArena.getValidPlayersNear(spawn, 96 * 96)) {
                p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC
                        + "The Wither twists and swells... something far worse is being born.");
            }
        }
    }

    /** Whether the block under a built Wither makes it a Wither Storm. */
    static boolean isAltarCore(Material under, Material core) {
        return under != null && under == core;
    }

    // ------------------------------------------------------------------ hitboxes and hits

    void registerHitbox(UUID box, WitherStorm storm, int head) {
        hitboxes.put(box, new Hitbox(storm, head));
    }

    void unregisterHitbox(UUID box) {
        hitboxes.remove(box);
    }

    /** A swing at a hitbox: the storm takes what the swing would have dealt. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSwing(PrePlayerAttackEntityEvent event) {
        Hitbox hit = hitboxes.get(event.getAttacked().getUniqueId());
        if (hit == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        double damage = meleeDamage(player);
        player.resetCooldown();
        player.getWorld().playSound(player.getLocation(), damage > 6 ? Sound.ENTITY_PLAYER_ATTACK_STRONG
                : Sound.ENTITY_PLAYER_ATTACK_WEAK, 1f, 1f);
        hit.storm().hurt(damage, hit.head(), false, player);
    }

    /** The engine's own damage event on a hitbox is not used: the swing above already counted. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHitboxDamaged(EntityDamageByEntityEvent event) {
        if (hitboxes.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity target = event.getHitEntity();
        if (target == null) return;
        Hitbox hit = hitboxes.get(target.getUniqueId());
        if (hit == null) return;
        event.setCancelled(true);
        Projectile projectile = event.getEntity();
        if (isOwn(projectile)) return;
        land(hit.storm(), hit.head(), projectile);
    }

    /**
     * The mod checks every projectile inside a head's box each tick, so a shot that the engine lets
     * pass through a hitbox still lands; the bodies are swept here the same way.
     */
    private void catchProjectiles(WitherStorm storm) {
        List<Map.Entry<UUID, Hitbox>> mine = new ArrayList<>();
        for (Map.Entry<UUID, Hitbox> e : hitboxes.entrySet()) {
            if (e.getValue().storm() == storm) mine.add(e);
        }
        for (Map.Entry<UUID, Hitbox> e : mine) {
            Entity box = Bukkit.getEntity(e.getKey());
            if (!(box instanceof Interaction interaction) || !box.isValid()) continue;
            for (Entity near : box.getWorld().getNearbyEntities(interaction.getBoundingBox(),
                    x -> x instanceof Projectile && x.isValid())) {
                Projectile projectile = (Projectile) near;
                if (isOwn(projectile)) continue;
                land(storm, e.getValue().head(), projectile);
            }
        }
    }

    private boolean isOwn(Projectile projectile) {
        if (projectile.getScoreboardTags().contains(PROJECTILE_TAG)) return true;
        return projectile.getShooter() instanceof ArmorStand stand && stand.getScoreboardTags().contains(TAG);
    }

    private void land(WitherStorm storm, int head, Projectile projectile) {
        Player shooter = projectile.getShooter() instanceof Player p ? p : null;
        double damage = projectileDamage(projectile);
        // The mod lets a thrown trident keep flying; everything else is swallowed by the storm.
        if (projectile instanceof Trident trident) {
            trident.setVelocity(trident.getVelocity().multiply(-0.1));
        } else {
            projectile.remove();
        }
        storm.hurt(damage, head, true, shooter);
    }

    static double projectileDamage(Projectile projectile) {
        if (projectile instanceof Trident) return 8.0;
        if (projectile instanceof AbstractArrow arrow) {
            double damage = Math.ceil(arrow.getDamage() * arrow.getVelocity().length());
            if (arrow.isCritical()) damage += ThreadLocalRandom.current().nextInt((int) damage / 2 + 2);
            return Math.max(1.0, damage);
        }
        return 1.0;
    }

    /**
     * What a swing deals, worked out the way the engine would: the attack damage attribute (weapon,
     * Strength and all), the attack cooldown, Sharpness, and Smite, which counts since the storm is
     * undead like any Wither.
     */
    static double meleeDamage(Player player) {
        AttributeInstance attack = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double base = attack == null ? 1.0 : attack.getValue();
        float cooldown = player.getAttackCooldown();
        double scaled = base * (0.2 + cooldown * cooldown * 0.8);
        ItemStack hand = player.getInventory().getItemInMainHand();
        int sharpness = hand.getEnchantmentLevel(Enchantment.SHARPNESS);
        int smite = hand.getEnchantmentLevel(Enchantment.SMITE);
        double bonus = (sharpness > 0 ? 0.5 * sharpness + 0.5 : 0) + 2.5 * smite;
        boolean critical = cooldown > 0.9f && player.getFallDistance() > 0 && !player.isSprinting()
                && !player.isInWater() && !player.hasPotionEffect(PotionEffectType.BLINDNESS)
                && player.getVelocity().getY() < 0;
        if (critical) {
            scaled *= 1.5;
            player.getWorld().spawnParticle(Particle.CRIT, player.getEyeLocation(), 10, 0.3, 0.3, 0.3, 0.1);
        }
        double damage = scaled + bonus * cooldown;
        // The Infinity sword deals the same share to the storm as to every other boss.
        if (com.Chagui68.integration.SlimefunArmorAdaptation.isInfinitySingularityWeapon(hand)) {
            damage *= Math.max(0, infinityWeaponMultiplier);
        }
        return damage;
    }

    /** {@code boss-balance.infinity-weapon-damage-multiplier}, read with the rest of the config. */
    private static double infinityWeaponMultiplier = 0.5;

    /**
     * A hit the storm lands on a player: true damage, like every other boss of the plugin, scaled to
     * what the player has invested ({@code boss-balance.adaptive-damage}) inside {@link TrueDamage}.
     */
    void dealToPlayer(WitherStorm storm, Player player, double amount, String source) {
        TrueDamage.apply(player, storm.anchor, amount, settings.pierce(), settings.maxDamageDealt());
    }

    /**
     * The adaptive damage every boss has, on the storm's hits that are not its own true damage: its
     * explosions, its wither skulls and whatever its summons and sickened mobs land. Those are plain
     * damage, so armour still counts in the factor, like Kinger's and Garou's.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStormHitsPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID) return;
        Entity damager = event.getDamager();
        Entity source = damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter
                ? shooter : damager;
        if (!isStormSide(source) && !isStormSide(damager)) return;
        event.setDamage(event.getDamage() * com.Chagui68.entities.boss.BossDamageScaling.factor(victim, false));
    }

    private static boolean isStormSide(Entity e) {
        if (e == null) return false;
        var tags = e.getScoreboardTags();
        return tags.contains(TAG) || tags.contains(PROJECTILE_TAG) || tags.contains(SICKENED_TAG)
                || tags.contains(PHANTOM_TAG) || tags.contains(WitherStormSpecials.SUMMON_TAG);
    }

    // ------------------------------------------------------------------ deaths

    /** An anchor killed from outside the fight (a command): the storm goes without its death scene. */
    @EventHandler
    public void onAnchorDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand) || !stand.getScoreboardTags().contains(TAG)) return;
        event.getDrops().clear();
        WitherStorm storm = storms.remove(stand.getUniqueId());
        if (storm != null) storm.discard();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!(event.getDamageSource().getCausingEntity() instanceof ArmorStand stand)) return;
        if (!stand.getScoreboardTags().contains(TAG)) return;
        List<String> messages = settings.deathMessages();
        if (messages.isEmpty()) return;
        String raw = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', raw.replace("%player%", event.getEntity().getName())));
    }
}
