package com.Chagui68.entities.boss.witherstorm;

import com.Chagui68.entities.boss.fx.ParticleBudget;
import com.Chagui68.utils.MscEntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What makes the storm feel like the end of the world: its big attacks, the things it calls to its
 * side, and the thunder that follows the big forms around.
 *
 * <p>One big attack at a time, one every {@code specials.cooldown-ticks}, never the same one twice in
 * a row, each announced so it can be read and dodged:</p>
 * <ul>
 *     <li><b>Cataclysmic Roar</b> — every head roars at once and a shockwave rolls out over the ground,
 *     darkening the sky of everyone it reaches. Jump it.</li>
 *     <li><b>Skull Barrage</b> — every head spits a fan of flaming skulls.</li>
 *     <li><b>Debris Rain</b> — the ground it has eaten comes back down: rocks fall on marked circles.</li>
 *     <li><b>Abyssal Eruption</b> — tentacles of the mass burst out of the ground under its victims.</li>
 *     <li><b>Singularity</b> (Destroyer and Devourer) — it holds still and drags the whole field into
 *     itself, then the core bursts.</li>
 * </ul>
 * <p>Its summons — a Sickened Horde, the Withered Symbiont of the mod, a phantom swarm — come on their
 * own cooldown, up to {@code summons.max} at once, and all of them fall with it.</p>
 */
final class WitherStormSpecials {

    enum Kind { CATACLYSMIC_ROAR, SKULL_BARRAGE, DEBRIS_RAIN, ABYSSAL_ERUPTION, SINGULARITY }

    enum Summon { HORDE, SYMBIONT, PHANTOM_SWARM }

    /** Tag on everything the storm summons. */
    static final String SUMMON_TAG = "MSC_WitherStormSummon";
    static final String SYMBIONT_TAG = "MSC_WitherStormSymbiont";

    private static final Particle.DustOptions WARNING = new Particle.DustOptions(Color.fromRGB(0xD12A5C), 1.6f);
    private static final Particle.DustOptions WAVE = new Particle.DustOptions(Color.fromRGB(0x8F3BFF), 2.4f);
    private static final Material[] ROCKS = {Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BLACKSTONE,
            Material.COBBLED_DEEPSLATE, Material.BLACK_CONCRETE};

    /** A rock of the Debris Rain: marked on the ground first, then falling onto the mark. */
    private static final class Rock {
        Vector ground;
        int warning;
        BlockDisplay display;
        double y;
        double speed;
        float spin;
    }

    /** A tentacle of the Abyssal Eruption, bursting out of the ground. */
    private static final class Eruption {
        Vector ground;
        int age;
        final List<BlockDisplay> segments = new ArrayList<>();
        final Set<UUID> struck = new HashSet<>();
    }

    private final WitherStorm storm;
    private Kind running;
    private Kind last;
    private int ticks;
    private int nextSpecial = 300;
    private int nextSummon = 400;
    private int nextThunder = 200;
    private int clock;
    private Vector origin;
    private double radius;
    private final Set<UUID> struck = new HashSet<>();
    private final List<Rock> rocks = new ArrayList<>();
    private final List<Eruption> eruptions = new ArrayList<>();
    private final List<UUID> summons = new ArrayList<>();

    WitherStormSpecials(WitherStorm storm) {
        this.storm = storm;
    }

    /** How fast the storm may fly while an attack plays: it holds still for the Singularity. */
    double speedFactor() {
        if (running == Kind.SINGULARITY) return 0.1;
        if (running == Kind.CATACLYSMIC_ROAR) return 0.3;
        return 1.0;
    }

    void tick() {
        clock++;
        WitherStormSettings s = storm.settings();
        presence();
        tickSummons();
        if (running != null) {
            ticks++;
            if (!play()) {
                last = running;
                running = null;
                struck.clear();
                nextSpecial = clock + s.specials().cooldownTicks() + ThreadLocalRandom.current().nextInt(200);
            }
        } else if (s.specials().enabled() && clock >= nextSpecial && !storm.targets().isEmpty()) {
            start(pick());
        }
        tickRocks();
        tickEruptions();
        if (s.summons().enabled() && clock >= nextSummon && !storm.targets().isEmpty()) {
            nextSummon = clock + s.summons().cooldownTicks() + ThreadLocalRandom.current().nextInt(200);
            summon();
        }
    }

    // ------------------------------------------------------------------ picking

    private Set<Kind> available() {
        WitherStormForm form = storm.form;
        Set<Kind> kinds = EnumSet.of(Kind.CATACLYSMIC_ROAR);
        if (form.ordinal() >= WitherStormForm.GROWING.ordinal()) kinds.add(Kind.SKULL_BARRAGE);
        if (form.ordinal() >= WitherStormForm.PREGNANT.ordinal()) {
            kinds.add(Kind.DEBRIS_RAIN);
            kinds.add(Kind.ABYSSAL_ERUPTION);
        }
        if (form.isColossal()) kinds.add(Kind.SINGULARITY);
        return kinds;
    }

    private Kind pick() {
        List<Kind> kinds = new ArrayList<>(available());
        if (kinds.size() > 1) kinds.remove(last);
        return kinds.get(ThreadLocalRandom.current().nextInt(kinds.size()));
    }

    private void start(Kind kind) {
        running = kind;
        ticks = 0;
        struck.clear();
        origin = null;
    }

    /** Ends whatever is playing and clears its props; the summons stay. */
    void cancel() {
        running = null;
        struck.clear();
        for (Rock r : rocks) {
            if (r.display != null) r.display.remove();
        }
        rocks.clear();
        for (Eruption e : eruptions) {
            e.segments.forEach(Entity::remove);
        }
        eruptions.clear();
    }

    /** Everything goes with the storm, its summons too. */
    void dismiss() {
        cancel();
        for (UUID id : summons) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) {
                Location at = e.getLocation();
                ParticleBudget.spawn(at.getWorld(), Particle.SOUL, at.getX(), at.getY() + 1, at.getZ(), 12, 0.3, 0.6, 0.3, 0.02, null);
                e.remove();
            }
        }
        summons.clear();
    }

    /** @return false when the attack is over */
    private boolean play() {
        return switch (running) {
            case CATACLYSMIC_ROAR -> roar();
            case SKULL_BARRAGE -> barrage();
            case DEBRIS_RAIN -> debrisRain();
            case ABYSSAL_ERUPTION -> eruption();
            case SINGULARITY -> singularity();
        };
    }

    private World world() {
        return storm.world();
    }

    private Vector groundUnderStorm() {
        Vector centre = storm.massCentre();
        double y = storm.groundY(centre, storm.anchor.getLocation().getY() - 4);
        return new Vector(centre.getX(), y, centre.getZ());
    }

    private List<LivingEntity> victims(int max) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : storm.targets()) {
            if (e.isValid() && !e.isDead()) out.add(e);
            if (out.size() >= max) break;
        }
        return out;
    }

    private void announce(String text) {
        Location at = storm.anchor.getLocation();
        for (Player p : world().getPlayers()) {
            if (p.getLocation().distanceSquared(at) <= 160 * 160) {
                p.sendActionBar(Component.text(text, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
            }
        }
    }

    // ------------------------------------------------------------------ Cataclysmic Roar

    private boolean roar() {
        int spread = 34;
        if (ticks == 1) {
            origin = groundUnderStorm();
            radius = (14 + 6 * storm.form.ordinal()) * storm.reach() * (storm.form.isColossal() ? 1.6 : 1.0);
            for (WitherStorm.HeadState h : storm.heads()) storm.roar(h, false);
            Location at = origin.toLocation(world());
            world().playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 12f, 0.4f);
            world().playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10f, 0.5f);
            world().playSound(at, Sound.ENTITY_WARDEN_SONIC_BOOM, 8f, 0.5f);
            for (Player p : world().getPlayers()) {
                if (p.getLocation().distanceSquared(at) <= radius * radius * 1.5) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 70, 0, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 90, 0, false, false));
                }
            }
            announce("The Wither Storm roars: jump the shockwave!");
        }
        if (ticks >= 4 && ticks <= spread && origin != null) {
            double r = radius * (ticks - 3) / (spread - 3);
            if (ticks % 2 == 0) {
                int points = (int) Math.min(56, Math.max(12, r * 1.6));
                for (int i = 0; i < points; i++) {
                    double a = Math.PI * 2 * i / points;
                    ParticleBudget.spawn(world(), Particle.DUST, origin.getX() + Math.cos(a) * r, origin.getY() + 0.3,
                            origin.getZ() + Math.sin(a) * r, 1, 0.1, 0.2, 0.1, 0, WAVE);
                }
                if (ticks % 6 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.PI * 2 * i / 6 + ticks;
                        ParticleBudget.spawn(world(), Particle.EXPLOSION, origin.getX() + Math.cos(a) * r, origin.getY() + 0.5,
                                origin.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0, null);
                    }
                }
            }
            double reach = Math.max(r + 2, 3);
            for (Entity e : world().getNearbyEntities(origin.toLocation(world()), reach, 6, reach)) {
                if (!(e instanceof LivingEntity living) || !storm.pullable(e) || struck.contains(e.getUniqueId())) continue;
                Vector rel = e.getLocation().toVector().subtract(origin);
                double flat = Math.hypot(rel.getX(), rel.getZ());
                // The wave rolls along the ground: anyone in the air when it passes is spared.
                if (flat < r - 2 || flat > r + 1 || Math.abs(rel.getY()) > 2.5 || !e.isOnGround()) continue;
                struck.add(e.getUniqueId());
                storm.hit(living, storm.settings().specials().roarDamage(), "Cataclysmic Roar");
                Vector push = rel.setY(0);
                if (push.lengthSquared() < 1.0e-4) push = new Vector(1, 0, 0);
                e.setVelocity(push.normalize().multiply(1.5).setY(0.7));
            }
        }
        return ticks < 50;
    }

    // ------------------------------------------------------------------ Skull Barrage

    private boolean barrage() {
        if (ticks == 1) {
            announce("The Wither Storm opens every jaw...");
            world().playSound(storm.anchor.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 8f, 0.4f);
        }
        if (ticks <= 45 && ticks % 5 == 0) {
            List<LivingEntity> victims = victims(6);
            int i = 0;
            for (WitherStorm.HeadState h : storm.heads()) {
                if (!h.gaze.jawed() && h.gaze.index() != 0) continue;
                h.roarTicks = Math.max(h.roarTicks, 10);
                LivingEntity target = victims.isEmpty() ? null : victims.get((i++ + ticks / 5) % victims.size());
                storm.shootFlamingSkull(h, target, false, 12);
            }
        }
        return ticks < 60;
    }

    // ------------------------------------------------------------------ Debris Rain

    private boolean debrisRain() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (ticks == 1) {
            announce("The ground it ate is coming back down!");
            world().playSound(storm.anchor.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 8f, 0.5f);
            Vector under = groundUnderStorm();
            ParticleBudget.spawn(world(), Particle.BLOCK, under.getX(), under.getY() + 1, under.getZ(), 40,
                    4, 2, 4, 0, Material.BLACKSTONE.createBlockData());
            for (LivingEntity victim : victims(6)) {
                int count = victim instanceof Player ? 4 : 2;
                for (int i = 0; i < count; i++) {
                    Rock rock = new Rock();
                    Vector at = victim.getLocation().toVector().add(new Vector(r.nextDouble(-4, 4), 0, r.nextDouble(-4, 4)));
                    if (i == 0) at = victim.getLocation().toVector();
                    at.setY(storm.groundY(at, victim.getLocation().getY()));
                    rock.ground = at;
                    rock.warning = 30 + i * 6;
                    rocks.add(rock);
                }
            }
        }
        return ticks < 30 || !rocks.isEmpty();
    }

    private void tickRocks() {
        Iterator<Rock> it = rocks.iterator();
        while (it.hasNext()) {
            Rock rock = it.next();
            if (rock.warning > 0) {
                rock.warning--;
                if (rock.warning % 4 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double a = Math.PI * 2 * i / 12;
                        ParticleBudget.spawn(world(), Particle.DUST, rock.ground.getX() + Math.cos(a) * 2.2, rock.ground.getY() + 0.15,
                                rock.ground.getZ() + Math.sin(a) * 2.2, 1, 0, 0, 0, 0, WARNING);
                    }
                }
                if (rock.warning == 0) {
                    rock.y = rock.ground.getY() + 22;
                    rock.speed = 0.4;
                    Material material = ROCKS[ThreadLocalRandom.current().nextInt(ROCKS.length)];
                    Location spawn = new Location(world(), rock.ground.getX(), rock.y, rock.ground.getZ());
                    rock.display = world().spawn(spawn, BlockDisplay.class, d -> {
                        d.setPersistent(false);
                        d.addScoreboardTag(WitherStormBoss.DEBRIS_TAG);
                        d.addScoreboardTag(storm.ownerTag);
                        d.setBlock(material.createBlockData());
                        d.setTeleportDuration(2);
                        d.setShadowRadius(0f);
                        d.setTransformation(new Transformation(new Vector3f(-0.9f), new Quaternionf(), new Vector3f(1.8f), new Quaternionf()));
                    });
                }
                continue;
            }
            if (rock.display == null || !rock.display.isValid()) {
                it.remove();
                continue;
            }
            rock.speed = Math.min(2.2, rock.speed + 0.15);
            rock.y -= rock.speed;
            if (rock.y <= rock.ground.getY() + 0.5) {
                land(rock);
                rock.display.remove();
                it.remove();
                continue;
            }
            if ((clock & 1) == 0) {
                rock.spin += 0.5f;
                rock.display.teleport(new Location(world(), rock.ground.getX(), rock.y, rock.ground.getZ()));
                Quaternionf q = new Quaternionf().rotationXYZ(rock.spin, rock.spin * 0.6f, 0);
                rock.display.setInterpolationDelay(0);
                rock.display.setInterpolationDuration(2);
                rock.display.setTransformation(new Transformation(q.transform(new Vector3f(-0.9f)), q, new Vector3f(1.8f), new Quaternionf()));
            }
        }
    }

    private void land(Rock rock) {
        Location at = rock.ground.toLocation(world()).add(0, 0.5, 0);
        ParticleBudget.spawn(world(), Particle.EXPLOSION, at.getX(), at.getY(), at.getZ(), 1, 0, 0, 0, 0, null);
        ParticleBudget.spawn(world(), Particle.BLOCK, at.getX(), at.getY(), at.getZ(), 20, 1, 0.4, 1, 0,
                rock.display.getBlock());
        world().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.7f);
        for (Entity e : world().getNearbyEntities(at, 2.6, 2.6, 2.6)) {
            if (!(e instanceof LivingEntity living) || !storm.pullable(e)) continue;
            storm.hit(living, storm.settings().specials().debrisDamage(), "Debris Rain");
            Vector push = e.getLocation().toVector().subtract(rock.ground).setY(0);
            if (push.lengthSquared() < 1.0e-4) push = new Vector(0, 0, 1);
            e.setVelocity(push.normalize().multiply(0.8).setY(0.5));
        }
    }

    // ------------------------------------------------------------------ Abyssal Eruption

    private boolean eruption() {
        if (ticks == 1) {
            announce("The ground splits beneath you...");
            for (LivingEntity victim : victims(4)) {
                Eruption e = new Eruption();
                Vector at = victim.getLocation().toVector();
                at.setY(storm.groundY(at, victim.getLocation().getY()));
                e.ground = at;
                eruptions.add(e);
            }
        }
        return ticks < 20 || !eruptions.isEmpty();
    }

    private void tickEruptions() {
        Iterator<Eruption> it = eruptions.iterator();
        double scale = Math.min(2.5, storm.reach() * (storm.form.isColossal() ? 1.6 : 1.0));
        float width = (float) (1.4 * scale);
        float segment = (float) (1.6 * scale);
        while (it.hasNext()) {
            Eruption e = it.next();
            e.age++;
            Location at = e.ground.toLocation(world());
            if (e.age < 30) {
                if (e.age % 3 == 0) {
                    ParticleBudget.spawn(world(), Particle.REVERSE_PORTAL, at.getX(), at.getY() + 0.2, at.getZ(), 10, width * 0.6, 0.1, width * 0.6, 0.05, null);
                    ParticleBudget.spawn(world(), Particle.BLOCK, at.getX(), at.getY() + 0.1, at.getZ(), 6, width * 0.6, 0.1, width * 0.6, 0,
                            at.clone().add(0, -0.5, 0).getBlock().getBlockData());
                }
                if (e.age % 10 == 0) world().playSound(at, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.5f, 0.5f);
                continue;
            }
            if (e.age == 30) {
                for (int i = 0; i < 4; i++) {
                    Material material = i % 2 == 0 ? Material.OBSIDIAN : Material.BLACK_CONCRETE;
                    BlockDisplay d = world().spawn(at, BlockDisplay.class, piece -> {
                        piece.setPersistent(false);
                        piece.addScoreboardTag(WitherStormBoss.DEBRIS_TAG);
                        piece.addScoreboardTag(storm.ownerTag);
                        piece.setBlock(material.createBlockData());
                        piece.setShadowRadius(0f);
                        piece.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.001f), new Quaternionf()));
                    });
                    e.segments.add(d);
                }
                world().playSound(at, Sound.ENTITY_EVOKER_FANGS_ATTACK, 3f, 0.4f);
                world().playSound(at, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2f, 0.4f);
            }
            float lean = e.age < 66 ? (float) Math.sin(e.age * 0.25) * 0.18f : 0;
            float grown = e.age >= 64 ? 0.001f : 1f;
            if (e.age == 31 || (e.age > 31 && e.age % 5 == 0) || e.age == 64) {
                Quaternionf q = new Quaternionf().rotationXYZ(lean, 0, lean * 0.6f);
                for (int i = 0; i < e.segments.size(); i++) {
                    float w = width * (1 - i * 0.18f) * grown;
                    Vector3f corner = q.transform(new Vector3f(-w / 2, i * segment * grown, -w / 2));
                    BlockDisplay piece = e.segments.get(i);
                    piece.setInterpolationDelay(0);
                    piece.setInterpolationDuration(e.age == 31 ? 4 : 5);
                    piece.setTransformation(new Transformation(corner, new Quaternionf(q), new Vector3f(w, segment * grown, w), new Quaternionf()));
                }
            }
            if (e.age >= 31 && e.age <= 40) {
                for (Entity hit : world().getNearbyEntities(at, width + 0.6, segment * 4, width + 0.6)) {
                    if (!(hit instanceof LivingEntity living) || !storm.pullable(hit) || !e.struck.add(hit.getUniqueId())) continue;
                    if (hit.getLocation().getY() < at.getY() - 1) continue;
                    storm.hit(living, storm.settings().specials().eruptionDamage(), "Abyssal Eruption");
                    hit.setVelocity(new Vector(0, 1.3, 0));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0, false, true));
                }
            }
            if (e.age >= 72) {
                e.segments.forEach(Entity::remove);
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ Singularity

    private boolean singularity() {
        int pullFor = 80;
        if (ticks == 1) {
            origin = storm.massCentre();
            radius = 48 * storm.reach();
            announce("The Wither Storm is collapsing everything into itself!");
            Location at = origin.toLocation(world());
            world().playSound(at, Sound.BLOCK_PORTAL_TRIGGER, 10f, 0.5f);
            world().playSound(at, Sound.BLOCK_BEACON_POWER_SELECT, 10f, 0.4f);
        }
        if (origin == null) return false;
        Location at = origin.toLocation(world());
        if (ticks <= pullFor) {
            for (Entity e : world().getNearbyEntities(at, radius, radius, radius)) {
                if (!(e instanceof LivingEntity) || !storm.pullable(e)) continue;
                Vector to = origin.clone().subtract(e.getLocation().toVector());
                double d = to.length();
                if (d > radius || d < 1) continue;
                Vector v = e.getVelocity().add(to.multiply(0.09 / d));
                if (v.length() > 0.9) v.normalize().multiply(0.9);
                e.setVelocity(v);
                e.setFallDistance(0);
            }
            if (ticks % 2 == 0) {
                double r = radius * (1 - (ticks % 20) / 20.0) * 0.5;
                for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16 + ticks * 0.2;
                    ParticleBudget.spawn(world(), Particle.DUST, origin.getX() + Math.cos(a) * r, origin.getY() + Math.sin(a * 2) * 3,
                            origin.getZ() + Math.sin(a) * r, 1, 0.2, 0.2, 0.2, 0, i % 2 == 0 ? WitherStorm.VOID : WAVE);
                }
                ParticleBudget.spawn(world(), Particle.REVERSE_PORTAL, origin.getX(), origin.getY(), origin.getZ(), 20, 3, 3, 3, 0.4, null);
            }
            if (ticks % 20 == 0) world().playSound(at, Sound.ENTITY_WARDEN_HEARTBEAT, 10f, 0.5f);
        }
        if (ticks == pullFor) {
            world().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 12f, 0.4f);
            world().playSound(at, Sound.ENTITY_WITHER_SPAWN, 12f, 0.6f);
            ParticleBudget.spawn(world(), Particle.EXPLOSION_EMITTER, at.getX(), at.getY(), at.getZ(), 4, 3, 3, 3, 0, null);
            double burst = 14 * storm.reach();
            for (Entity e : world().getNearbyEntities(at, radius, radius, radius)) {
                if (!(e instanceof LivingEntity living) || !storm.pullable(e)) continue;
                Vector out = e.getLocation().toVector().subtract(origin);
                double d = out.length();
                if (d > radius) continue;
                if (out.lengthSquared() < 1.0e-4) out = new Vector(0, 1, 0);
                if (d <= burst) storm.hit(living, storm.settings().specials().singularityDamage(), "Singularity");
                e.setVelocity(out.normalize().multiply(d <= burst ? 2.4 : 1.0).setY(0.8));
            }
        }
        return ticks < pullFor + 20;
    }

    // ------------------------------------------------------------------ presence

    /** The weather that follows it: thunder and lightning round the big forms, a growl round the small. */
    private void presence() {
        if (clock < nextThunder) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location at = storm.anchor.getLocation();
        if (storm.form.isColossal()) {
            nextThunder = clock + 120 + r.nextInt(200);
            Vector strike = at.toVector().add(new Vector(r.nextDouble(-40, 40), 0, r.nextDouble(-40, 40)));
            strike.setY(storm.groundY(strike, at.getY() - 30));
            world().strikeLightningEffect(strike.toLocation(world()));
        } else {
            nextThunder = clock + 200 + r.nextInt(200);
            world().playSound(at, Sound.ENTITY_WITHER_AMBIENT, 2f, 0.4f);
        }
    }

    // ------------------------------------------------------------------ summons

    private int summonsAlive() {
        summons.removeIf(id -> {
            Entity e = Bukkit.getEntity(id);
            return e == null || !e.isValid() || e.isDead();
        });
        return summons.size();
    }

    private void summon() {
        WitherStormSettings.Summons config = storm.settings().summons();
        if (summonsAlive() >= config.max()) return;
        List<Summon> kinds = new ArrayList<>();
        kinds.add(Summon.HORDE);
        if (storm.form.ordinal() >= WitherStormForm.PREGNANT.ordinal() && !symbiontAlive()) kinds.add(Summon.SYMBIONT);
        if (storm.form.isColossal()) kinds.add(Summon.PHANTOM_SWARM);
        List<LivingEntity> victims = victims(4);
        if (victims.isEmpty()) return;
        LivingEntity target = victims.get(ThreadLocalRandom.current().nextInt(victims.size()));
        switch (kinds.get(ThreadLocalRandom.current().nextInt(kinds.size()))) {
            case HORDE -> horde(target, config);
            case SYMBIONT -> symbiont(target, config);
            case PHANTOM_SWARM -> phantomSwarm(target);
        }
    }

    private boolean symbiontAlive() {
        for (UUID id : summons) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.getScoreboardTags().contains(SYMBIONT_TAG)) return true;
        }
        return false;
    }

    private Location around(LivingEntity target, double min, double max) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double a = r.nextDouble(Math.PI * 2), d = r.nextDouble(min, max);
        Vector at = target.getLocation().toVector().add(new Vector(Math.cos(a) * d, 0, Math.sin(a) * d));
        at.setY(storm.groundY(at, target.getLocation().getY()));
        return at.toLocation(world());
    }

    private void rift(Location at) {
        ParticleBudget.spawn(world(), Particle.REVERSE_PORTAL, at.getX(), at.getY() + 1, at.getZ(), 30, 0.5, 1, 0.5, 0.1, null);
        ParticleBudget.spawn(world(), Particle.DUST, at.getX(), at.getY() + 1, at.getZ(), 15, 0.4, 0.8, 0.4, 0, WitherStorm.SICK);
        world().playSound(at, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
    }

    /** The Sickened Horde: mobs the storm has turned, crawling out of purple rifts round a victim. */
    private void horde(LivingEntity target, WitherStormSettings.Summons config) {
        EntityType[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.VINDICATOR,
                EntityType.HUSK, EntityType.STRAY};
        int size = storm.form.isColossal() ? config.hordeSize() + 2 : config.hordeSize();
        announce("The Sickened answer the storm's call.");
        for (int i = 0; i < size && summonsAlive() < config.max(); i++) {
            Location at = around(target, 4, 9);
            EntityType type = types[ThreadLocalRandom.current().nextInt(types.length)];
            Entity raw = world().spawnEntity(at, type);
            if (!(raw instanceof Mob mob)) {
                raw.remove();
                continue;
            }
            storm.sicken(mob, SUMMON_TAG);
            mob.setPersistent(false);
            mob.setRemoveWhenFarAway(true);
            mob.setTarget(target);
            rift(at);
            summons.add(mob.getUniqueId());
        }
    }

    /**
     * The Withered Symbiont, the mod's own minion: a towering wither skeleton in the storm's colours
     * that keeps spitting wither skulls at whoever the storm is after.
     */
    private void symbiont(LivingEntity target, WitherStormSettings.Summons config) {
        Location at = around(target, 6, 10);
        world().strikeLightningEffect(at);
        rift(at);
        WitherSkeleton symbiont = world().spawn(at, WitherSkeleton.class, s -> {
            s.addScoreboardTag(SUMMON_TAG);
            s.addScoreboardTag(SYMBIONT_TAG);
            s.customName(Component.text("Withered Symbiont", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
            s.setCustomNameVisible(true);
            s.setPersistent(false);
            MscEntityUtils.setMaxHealthAndHeal(s, config.symbiontHealth());
            MscEntityUtils.setAttribute(s, Attribute.SCALE, 1.5);
            MscEntityUtils.setAttribute(s, Attribute.ATTACK_DAMAGE, 12);
            MscEntityUtils.setAttribute(s, Attribute.MOVEMENT_SPEED, 0.3);
            MscEntityUtils.setAttribute(s, Attribute.KNOCKBACK_RESISTANCE, 0.8);
            EntityEquipment gear = s.getEquipment();
            if (gear != null) {
                gear.setChestplate(dyed(Material.LEATHER_CHESTPLATE));
                gear.setLeggings(dyed(Material.LEATHER_LEGGINGS));
                gear.setBoots(dyed(Material.LEATHER_BOOTS));
                gear.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
                gear.setChestplateDropChance(0f);
                gear.setLeggingsDropChance(0f);
                gear.setBootsDropChance(0f);
                gear.setItemInMainHandDropChance(0f);
            }
            s.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        });
        symbiont.setTarget(target);
        summons.add(symbiont.getUniqueId());
        world().playSound(at, Sound.ENTITY_WITHER_SPAWN, 3f, 1.2f);
        announce("The Withered Symbiont has risen!");
    }

    private static ItemStack dyed(Material material) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.fromRGB(0x2B0A3D));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void phantomSwarm(LivingEntity target) {
        announce("Phantoms pour out of the storm!");
        Location base = target.getLocation().add(0, 14, 0);
        for (int i = 0; i < 3; i++) {
            Location at = base.clone().add(ThreadLocalRandom.current().nextDouble(-6, 6), i * 2, ThreadLocalRandom.current().nextDouble(-6, 6));
            Phantom phantom = world().spawn(at, Phantom.class, p -> {
                p.addScoreboardTag(SUMMON_TAG);
                p.customName(Component.text("Sickened Phantom", NamedTextColor.DARK_PURPLE));
                p.setShouldBurnInDay(false);
                p.setSize(3);
                p.setPersistent(false);
            });
            phantom.setTarget(target);
            summons.add(phantom.getUniqueId());
        }
        world().playSound(base, Sound.ENTITY_PHANTOM_SWOOP, 4f, 0.5f);
    }

    /** The Symbiont's wither skulls, the summons' trail, and their victims kept on the storm's. */
    private void tickSummons() {
        if (clock % 10 != 0 || summons.isEmpty()) return;
        for (UUID id : summons) {
            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof Mob mob) || !mob.isValid()) continue;
            if (mob.getTarget() == null || !mob.getTarget().isValid()) {
                List<LivingEntity> victims = victims(1);
                if (!victims.isEmpty()) mob.setTarget(victims.get(0));
            }
            if (e.getScoreboardTags().contains(SYMBIONT_TAG) && clock % 50 == 0 && mob.getTarget() != null) {
                Location eye = mob.getEyeLocation();
                Vector dir = mob.getTarget().getEyeLocation().toVector().subtract(eye.toVector()).normalize();
                world().spawn(eye.add(dir), WitherSkull.class, skull -> {
                    skull.setShooter(mob);
                    skull.setDirection(dir);
                    skull.addScoreboardTag(WitherStormBoss.PROJECTILE_TAG);
                });
                world().playSound(eye, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.8f);
            }
            if (clock % 20 == 0) {
                Location at = mob.getLocation();
                ParticleBudget.spawn(world(), Particle.DUST, at.getX(), at.getY() + 1, at.getZ(), 3, 0.3, 0.6, 0.3, 0, WitherStorm.SICK);
            }
        }
    }
}
