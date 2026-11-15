package com.Chagui68.entities.boss.herobrine;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.boss.BossArena;
import com.Chagui68.entities.boss.rig.Hazard;
import com.Chagui68.entities.boss.rig.Rig;
import com.Chagui68.entities.boss.rig.RigForm;
import com.Chagui68.entities.boss.rig.RiggedBoss;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Herobrine: a boss that is never where you were looking.
 *
 * <p>Stare at him for two seconds and he is gone, and standing behind whoever stared. He blinks
 * behind his target with his pickaxe already falling, calls lightning down on warned rings, and
 * turns his white eyes on everyone near: the world goes dark and their legs go heavy. Close, he
 * strikes and cleaves with the pick.</p>
 */
public class HerobrineBoss extends RiggedBoss {

    public static final String TAG = "MSC_Herobrine";
    private static final List<RigForm> FORMS = List.of(HerobrineModel.FORM);
    /** How narrow a look counts as a stare, as the cosine of its angle (about 8 degrees). */
    private static final double STARE = .99;

    private final double pickDamage, quakeDamage, boltDamage;
    private final int stareTicks, gloomTicks;
    /** For each boss, how long the player staring at it has been staring. */
    private final Map<UUID, Integer> stared = new HashMap<>();

    public HerobrineBoss(MultiverseCreatures plugin) {
        super(plugin, "herobrine", TAG, new Defaults(1300.0, 12.0, 1.25, 34.0, 0.3, 12));
        pickDamage = number("pick-damage", 11.0);
        quakeDamage = number("quake-damage", 6.0);
        boltDamage = number("bolt-damage", 12.0);
        stareTicks = (int) number("stare-ticks", 40);
        gloomTicks = (int) number("gloom-ticks", 100);
        start();
    }

    @Override
    public List<RigForm> forms() {
        return FORMS;
    }

    @Override
    protected Rig.Move choose(Instance inst, Player target, double dist) {
        if (dist <= 3.2) return HerobrineModel.FORM.move(random.nextBoolean() ? "strike" : "cleave");
        if (dist <= 18) {
            if (inst.ready("blink")) return after(inst, "blink", 120);
            if (inst.ready("gloom")) return after(inst, "gloom", 260);
            if (inst.ready("storm")) return after(inst, "storm", 180);
            return null;
        }
        if (dist <= 30 && inst.ready("storm")) return after(inst, "storm", 180);
        return null;
    }

    private Rig.Move after(Instance inst, String id, int cooldown) {
        inst.delay(id, cooldown);
        return HerobrineModel.FORM.move(id);
    }

    @Override
    protected double weaponDamage(Instance inst) {
        return pickDamage;
    }

    @Override
    protected double ringDamage(Instance inst) {
        return quakeDamage;
    }

    @Override
    protected double ringRadius(Instance inst) {
        return 3.0;
    }

    /** Between attacks, a player who keeps him in the middle of their view finds him behind them. */
    @Override
    protected void everyTick(Instance inst) {
        UUID id = inst.stand.getUniqueId();
        if (inst.move != null || !inst.ready("vanish")) {
            stared.remove(id);
            return;
        }
        Location chest = inst.stand.getLocation().add(0, 1.5, 0);
        for (Player p : playersNear(chest, 24)) {
            Vector to = chest.toVector().subtract(p.getEyeLocation().toVector());
            if (to.lengthSquared() < 9 || p.getEyeLocation().getDirection().dot(to.normalize()) < STARE || !p.hasLineOfSight(inst.stand)) continue;
            if (stared.merge(id, 1, Integer::sum) < stareTicks) return;
            stared.remove(id);
            inst.delay("vanish", 160);
            behind(inst, p);
            return;
        }
        stared.remove(id);
    }

    /** Leaves smoke where he stood and stands a step behind {@code player}, facing their back. */
    private void behind(Instance inst, Player player) {
        Location from = inst.stand.getLocation();
        from.getWorld().spawnParticle(Particle.LARGE_SMOKE, from.clone().add(0, 1.2, 0), 30, .3, .8, .3, .02);
        Location look = player.getLocation();
        look.setPitch(0f);
        Location to = look.clone().subtract(look.getDirection().multiply(1.8));
        if (to.getBlock().getType().isSolid()) to = look.clone();
        to.setYaw(look.getYaw());
        relocate(inst, to);
        to.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, .5f);
        to.getWorld().spawnParticle(Particle.LARGE_SMOKE, to.clone().add(0, 1.2, 0), 30, .3, .8, .3, .02);
    }

    @Override
    protected void cue(Instance inst, Rig.Move move, String name) {
        Player target = target(inst);
        Location ground = ground(inst);
        switch (name) {
            case "blink" -> {
                if (target != null) behind(inst, target);
            }
            // A ring on the ground, and a second later the bolt.
            case "bolt" -> {
                if (target == null) return;
                Location at = target.getLocation();
                BossArena.settle(at);
                add(inst, new Hazard.Blast(this, inst, at, null, 18, 2.4, boltDamage, .4, form(inst).trail(), null, 0));
                int[] age = {0};
                add(inst, () -> {
                    if (++age[0] < 18) return true;
                    at.getWorld().strikeLightningEffect(at);
                    return false;
                });
            }
            case "gloom" -> {
                ground.getWorld().playSound(ground, Sound.AMBIENT_CAVE, 2f, .6f);
                for (Player p : playersNear(ground, 18)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, gloomTicks, 0, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, gloomTicks, 1, false, false));
                }
            }
            default -> {
            }
        }
    }

    @Override
    protected void onGone(Instance inst) {
        stared.remove(inst.stand.getUniqueId());
    }
}
