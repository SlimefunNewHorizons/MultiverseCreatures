package com.Chagui68.entities.boss.malenia;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.boss.rig.Rig;
import com.Chagui68.entities.boss.rig.RigForm;
import com.Chagui68.entities.boss.rig.RiggedBoss;
import com.Chagui68.utils.MscEntityUtils;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Malenia, Blade of Miquella: the {@link MaleniaModel} body fighting with the {@link MaleniaRig} moves
 * on the shared {@link RiggedBoss}.
 *
 * <p>She picks an attack by distance: close, the three-slash combo, the whirlwind or the rising cut;
 * mid range, the lunging thrust or the thrust flurry; far, the leaping slam or the blade dash; below
 * half health, the Waterfowl Dance now and then. Every hit she lands heals her.</p>
 */
public class MaleniaBoss extends RiggedBoss {

    public static final String TAG = "MSC_Malenia";
    private static final List<RigForm> FORMS = List.of(MaleniaRig.FORM);

    private final double bladeDamage, slashDamage, ringDamage, lifesteal;
    private final int waterfowlCooldownTicks;

    public MaleniaBoss(MultiverseCreatures plugin) {
        super(plugin, "malenia", TAG, new Defaults(1500.0, 10.0, 1.3, 30.0, 0.3, 6));
        bladeDamage = number("blade-damage", 12.0);
        slashDamage = number("slash-damage", 4.0);
        ringDamage = number("ring-damage", 10.0);
        lifesteal = number("lifesteal", 0.5);
        waterfowlCooldownTicks = (int) number("waterfowl-cooldown-ticks", 400);
        start();
    }

    @Override
    public List<RigForm> forms() {
        return FORMS;
    }

    @Override
    protected Rig.Move choose(Instance inst, Player target, double dist) {
        if (healthLeft(inst) < .5 && dist <= 6 && inst.ready("waterfowl")) {
            inst.delay("waterfowl", waterfowlCooldownTicks);
            return MaleniaRig.FORM.move("waterfowl");
        }
        if (dist <= 3.5) return pick("combo", "whirlwind", "rising");
        if (dist <= 6.5) return pick("thrust", "flurry");
        if (dist <= 10) return pick("leap", "dash");
        if (dist <= 18) return MaleniaRig.FORM.move("dash");
        return null;
    }

    private Rig.Move pick(String... ids) {
        return MaleniaRig.FORM.move(ids[random.nextInt(ids.length)]);
    }

    @Override
    protected double weaponDamage(Instance inst) {
        return bladeDamage;
    }

    @Override
    protected double slashDamage(Instance inst) {
        return slashDamage;
    }

    @Override
    protected double ringDamage(Instance inst) {
        return ringDamage;
    }

    /** Every hit she lands heals her by {@code lifesteal} of it. */
    @Override
    protected void onHit(Instance inst, Player player, double damage) {
        MscEntityUtils.setVirtualHealth(inst.stand, MscEntityUtils.getVirtualHealth(inst.stand) + damage * lifesteal);
    }
}
