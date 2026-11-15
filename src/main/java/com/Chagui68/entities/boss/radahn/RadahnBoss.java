package com.Chagui68.entities.boss.radahn;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.boss.BossArena;
import com.Chagui68.entities.boss.TrueDamage;
import com.Chagui68.entities.boss.rig.Rig;
import com.Chagui68.entities.boss.rig.RigForm;
import com.Chagui68.entities.boss.rig.RiggedBoss;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Starscourge Radahn: a warlord who fights from the saddle and never reins in for long.
 *
 * <p>He gallops at whoever he is fighting along a curve that brings them past his sword hand, cuts
 * as he rides by, runs on and wheels round for the next pass. From further off he puts his swords on
 * his back and takes his greatbow: one great arrow that takes half a player's health, a fan of
 * arrows at one of them, or a rain of arrows that turn after their marks. Now and then the blades
 * cross over his head and a hole opens there that drags everyone to him, before they come down.</p>
 *
 * <p>At half health he leaps into the sky and is gone, and comes back down as a star: everyone
 * still inside the ring on the ground when it lands dies.</p>
 */
public class RadahnBoss extends RiggedBoss {

    public static final String TAG = "MSC_Radahn";
    private static final List<RigForm> FORMS = List.of(RadahnModel.FORM);

    /** Degrees a tick the horse turns at the gallop: what makes his way to a target a curve. */
    static final float WHEEL = 7f;
    /** How far to its left of a target he rides, so it passes on his sword hand. */
    static final double ASIDE = 2.2;
    /**
     * How far up his path a target must be when he begins a cut for the blade to meet it as he rides
     * by: the sweep from {@code CUT} down to {@code SWEEP_FROM}, the quick cut from there down to
     * {@code SLASH_FROM}.
     */
    static final double CUT = 8.0, SWEEP_FROM = 7.0, SLASH_FROM = 5.0;
    /** How far the hole over his head reaches. */
    private static final double HORIZON = 16;
    /** No armour stands between a player and the star. */
    private static final double STARFALL = 1e9;

    private final double swordDamage, slamDamage, pullStrength, bowRange, shotShare, rainShare, volleyDamage, craterRadius;
    private final int rainArrows, volleyArrows;

    public RadahnBoss(MultiverseCreatures plugin) {
        super(plugin, "radahn", TAG, new Defaults(2200.0, 14.0, 2.0, 48.0, 0.42, 22));
        swordDamage = number("sword-damage", 14.0);
        slamDamage = number("slam-damage", 10.0);
        pullStrength = number("pull-strength", 0.2);
        bowRange = number("bow-range", 40.0);
        shotShare = number("shot-share", 0.5);
        rainShare = number("rain-share", 0.15);
        rainArrows = (int) number("rain-arrows", 3);
        volleyDamage = number("volley-damage", 8.0);
        volleyArrows = (int) number("volley-arrows", 5);
        craterRadius = number("cataclysm-radius", 20.0);
        start();
    }

    @Override
    public List<RigForm> forms() {
        return FORMS;
    }

    // =====================================================================================
    // Riding
    // =====================================================================================

    /**
     * He never walks and never stands: the horse runs the way it faces, and only turns so fast. He
     * rides at a point beside his target, runs on past it, and wheels round for the next pass.
     */
    @Override
    protected Vector steer(Instance inst, Location at, Vector toward, double dist) {
        if (inst.ready("wheel")) {
            float off = wrap(yawOf(toward) - at.getYaw());
            // Just past it: he rides on a way before he wheels, or he would circle his target for ever.
            if (dist < 5 && Math.abs(off) > 100) inst.delay("wheel", 14);
            else at.setYaw(turn(at.getYaw(), yawOf(passing(toward, dist)), WHEEL));
        }
        double yaw = Math.toRadians(at.getYaw());
        return new Vector(-Math.sin(yaw), 0, Math.cos(yaw)).multiply(speed(inst));
    }

    /**
     * The way to ride at a target {@code dist} blocks off along {@code toward}: at a point to its
     * left, so that it passes on his right, where his sword is. Never further aside than half the
     * way there: level with the target that point would be abeam, and he would turn away from it.
     */
    static Vector passing(Vector toward, double dist) {
        double aside = Math.min(ASIDE, dist * .5);
        return new Vector(toward.getX() * dist + toward.getZ() * aside, 0, toward.getZ() * dist - toward.getX() * aside);
    }

    /** He starts a move wherever his target is: he turns to it as he winds up, and a cut is asked for only where it can land. */
    @Override
    protected boolean faces(Instance inst, float off) {
        return true;
    }

    @Override
    protected Rig.Move choose(Instance inst, Player target, double dist) {
        if (dist <= CUT) {
            Location at = inst.stand.getLocation();
            float off = wrap(yawOf(target.getLocation().toVector().subtract(at.toVector()).setY(0)) - at.getYaw());
            // A cut lands some blocks up his path (RadahnBossTest measures where): a target nearer than
            // that, or not ahead of him, he rides on past and comes round for again.
            if (Math.abs(off) > 40 || dist < SLASH_FROM) return null;
            // From furthest off the sweep, on some passes and not others; nearer, the quick cut.
            if (dist > SWEEP_FROM) return random.nextInt(3) == 0 ? RadahnModel.FORM.move("sweep") : null;
            return RadahnModel.FORM.move("slash");
        }
        if (dist <= 13 && inst.ready("pull")) return after(inst, "pull", 300);
        if (dist >= 9 && dist <= bowRange) {
            if (inst.ready("volley")) return after(inst, "volley", 100);
            if (inst.ready("shot")) return after(inst, "shot", 240);
            if (inst.ready("rain")) return after(inst, "rain", 360);
        }
        return null;
    }

    private Rig.Move after(Instance inst, String id, int cooldown) {
        inst.delay(id, cooldown);
        return RadahnModel.FORM.move(id);
    }

    @Override
    protected double weaponDamage(Instance inst) {
        return swordDamage;
    }

    @Override
    protected double ringDamage(Instance inst) {
        return slamDamage;
    }

    @Override
    protected double ringRadius(Instance inst) {
        return 6.5;
    }

    /** At half health he leaves for the sky; until he is back down nothing hurts him. */
    @Override
    protected void everyTick(Instance inst) {
        if (inst.phase == 0 && !inst.busy && healthLeft(inst) <= .5) {
            inst.busy = true;
            begin(inst, RadahnModel.FORM.move("cataclysm"));
        }
    }

    // =====================================================================================
    // What his moves do
    // =====================================================================================

    @Override
    protected void cue(Instance inst, Rig.Move move, String name) {
        Location ground = ground(inst);
        World world = ground.getWorld();
        Color colour = form(inst).trail();
        switch (name) {
            case "pull" -> horizon(inst, ground);
            case "nock" -> world.playSound(ground, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 2.5f, .6f);
            // One great arrow at whoever he is fighting: half their health, whatever they wear.
            case "shot" -> {
                Player mark = target(inst);
                if (mark == null) return;
                Location from = bow(inst);
                world.playSound(from, Sound.ITEM_CROSSBOW_SHOOT, 3f, .5f);
                add(inst, new Arrow(this, from, aim(from, mark).multiply(2.4), null, 0, 0, 30, .7, 2.2f, colour,
                        p -> pierce(inst, p, shotShare)));
            }
            // A fan of arrows at one player of those in range, not always the nearest.
            case "volley" -> {
                List<Player> marks = playersNear(ground, bowRange);
                marks.removeIf(p -> !BossArena.isTargetable(p));
                if (marks.isEmpty()) return;
                Location from = bow(inst);
                Vector way = aim(from, marks.get(random.nextInt(marks.size())));
                world.playSound(from, Sound.ENTITY_ARROW_SHOOT, 2.5f, .7f);
                for (int k = 0; k < volleyArrows; k++) {
                    double fan = volleyArrows < 2 ? 0 : Math.toRadians(-14 + 28.0 * k / (volleyArrows - 1));
                    add(inst, new Arrow(this, from, way.clone().rotateAroundY(fan).multiply(1.5), null, 0, 0, 40, .5, 1.2f, colour,
                            p -> hurt(inst, p, volleyDamage)));
                }
            }
            // Arrows loosed at the sky for every player in range: each climbs, then turns after its mark.
            case "rain" -> {
                Location from = bow(inst);
                world.playSound(from, Sound.ENTITY_ARROW_SHOOT, 2.5f, .5f);
                int loosed = 0;
                for (Player mark : playersNear(ground, bowRange)) {
                    for (int k = 0; k < rainArrows; k++) {
                        Vector up = new Vector(random.nextDouble() - .5, 2.2, random.nextDouble() - .5).normalize().multiply(.95);
                        add(inst, new Arrow(this, from, up, mark, 10 + 4 * (loosed++ % 5), 9, 110, .5, 1.2f, colour,
                                p -> pierce(inst, p, rainShare)));
                    }
                }
            }
            case "leap" -> world.playSound(ground, Sound.ENTITY_HORSE_ANGRY, 3f, .6f);
            // Gone: the phase is kept on the stand at once, so a restart in mid-fall does not send him up again.
            case "vanish" -> {
                setPhase(inst, 1);
                hide(inst, true);
                world.playSound(ground, Sound.ENTITY_ENDER_DRAGON_FLAP, 3f, .5f);
                world.spawnParticle(Particle.CLOUD, ground.clone().add(0, 2, 0), 60, 1.2, 1.5, 1.2, .1);
                for (Player p : playersNear(ground, aggroRange * 2)) {
                    p.sendMessage(Component.text("Radahn has left the field. Look up.", NamedTextColor.LIGHT_PURPLE));
                }
            }
            case "meteor" -> add(inst, new Meteor(this, ground, craterRadius, until(move, "impact") - inst.moveTick));
            case "impact" -> {
                hide(inst, false);
                crater(inst, ground);
            }
            default -> {
            }
        }
    }

    /** The ticks into {@code move} at which it gives the cue called {@code name}. */
    private static int until(Rig.Move move, String name) {
        for (Rig.Cue cue : move.cues()) if (cue.name().equals(name)) return cue.tick();
        return move.ticks();
    }

    /** Where the bow is while he shoots: ahead of his bow shoulder. */
    private Location bow(Instance inst) {
        return ground(inst).add(0, 2.7, 0).add(facing(inst).multiply(1.2));
    }

    /** The way from {@code from} to the middle of {@code mark}. */
    private static Vector aim(Location from, Player mark) {
        return mark.getLocation().add(0, 1, 0).toVector().subtract(from.toVector()).normalize();
    }

    /** A hit that takes {@code share} of a player's maximum health, through armour and Resistance. */
    private void pierce(Instance inst, Player p, double share) {
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double dealt = TrueDamage.apply(p, inst.stand, (max == null ? 20.0 : max.getValue()) * share, 1.0, 0, false);
        if (dealt <= 0) return;
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 10, .3, .4, .3, 0,
                new Particle.DustOptions(form(inst).trail(), 1.3f));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, .7f);
    }

    /**
     * The hole over his head, between the crossed blades: for a second and a half everyone in its
     * reach is dragged to him along the ground, while a ring of what it swallows closes on him. The
     * move ends with the blades coming down on whoever arrived.
     */
    private void horizon(Instance inst, Location ground) {
        World world = ground.getWorld();
        Location hole = ground.clone().add(0, 5.2, 0);
        Particle.DustOptions dark = new Particle.DustOptions(Color.BLACK, 2.4f), rim = new Particle.DustOptions(form(inst).trail(), 1.3f);
        world.playSound(ground, Sound.BLOCK_BEACON_AMBIENT, 3f, .5f);
        world.playSound(ground, Sound.BLOCK_PORTAL_TRIGGER, 1.2f, .6f);
        int[] age = {0};
        add(inst, () -> {
            if (++age[0] > 34) return false;
            world.spawnParticle(Particle.DUST, hole, 12, .3, .3, .3, 0, dark);
            world.spawnParticle(Particle.PORTAL, hole, 16, .2, .2, .2, 1.2);
            // The ring closes from the edge of its reach to his feet as it turns.
            double radius = HORIZON * (1 - age[0] / 34.0) + 1.5;
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16 + age[0] * .3;
                world.spawnParticle(Particle.DUST, ground.getX() + Math.cos(a) * radius, ground.getY() + .3, ground.getZ() + Math.sin(a) * radius,
                        1, 0, 0, 0, 0, rim);
            }
            for (Player p : playersNear(ground, HORIZON)) {
                Vector to = ground.toVector().subtract(p.getLocation().toVector()).setY(0);
                if (to.lengthSquared() < 4) continue;
                p.setVelocity(p.getVelocity().add(to.normalize().multiply(pullStrength)));
                if (age[0] % 4 == 0) world.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 6, .3, .5, .3, .2);
            }
            return true;
        });
    }

    /** The star lands: everyone still inside the ring dies, whatever they wear or carry. */
    private void crater(Instance inst, Location ground) {
        World world = ground.getWorld();
        world.playSound(ground, Sound.ENTITY_GENERIC_EXPLODE, 8f, .5f);
        world.playSound(ground, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 8f, .6f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, ground.clone().add(0, 1, 0), 14, craterRadius * .4, 1, craterRadius * .4, 0, null, true);
        world.spawnParticle(Particle.FLAME, ground.clone().add(0, 1, 0), 300, craterRadius * .45, .6, craterRadius * .45, .15, null, true);
        world.spawnParticle(Particle.LAVA, ground.clone().add(0, 1, 0), 80, craterRadius * .45, .4, craterRadius * .45, 0, null, true);
        for (Player p : playersNear(ground, craterRadius + 48)) {
            if (Math.hypot(p.getX() - ground.getX(), p.getZ() - ground.getZ()) <= craterRadius) {
                TrueDamage.apply(p, inst.stand, STARFALL, 1.0, 0, false);
            }
        }
    }
}
