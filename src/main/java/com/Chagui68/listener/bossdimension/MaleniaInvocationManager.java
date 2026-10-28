package com.Chagui68.listener.bossdimension;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.LiveStage;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.ritual.BossDimensionManager;
import com.Chagui68.ritual.MaleniaInvocationStructure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Summons Malenia at the Scarlet Bloom, only inside the Boss Dimension: light the four magenta
 * candles and the scarlet rot gathers over the heart; drop a torchflower on it and the rot blooms
 * into a Scarlet Aeonia, out of which she rises.
 */
public class MaleniaInvocationManager implements Listener {

    private static final Color SCARLET = Color.fromRGB(0xC8102E);
    private static final Color ROT = Color.fromRGB(0xE8743B);
    private static final Color GOLD = Color.fromRGB(0xE8C15A);
    /** Ticks the flower blooms before Malenia rises. */
    private static final int ARRIVAL_DELAY = 50;

    private final MultiverseCreatures plugin;
    private final Map<UUID, InvocationData> activeInvocations = new HashMap<>();

    public MaleniaInvocationManager(MultiverseCreatures plugin) {
        this.plugin = plugin;
    }

    private boolean inBossWorld(World world) {
        BossDimensionManager dim = plugin.getBossDimensionManager();
        return dim != null && dim.getBossWorld() != null && world.equals(dim.getBossWorld());
    }

    @EventHandler
    public void onCandleLight(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != MaleniaInvocationStructure.CANDLE) return;
        World world = block.getWorld();
        if (!inBossWorld(world)) return;
        if (plugin.getMalenia() != null && plugin.getMalenia().isBossActiveIn(world)) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location candle = block.getLocation();
            for (int ox = -4; ox <= 0; ox++) {
                for (int oz = -4; oz <= 0; oz++) {
                    Location origin = candle.clone().add(ox, 0, oz);
                    if (!MaleniaInvocationStructure.isStructureComplete(origin)) continue;
                    if (!MaleniaInvocationStructure.containsCandle(origin, candle)) continue;
                    if (activeInvocations.containsKey(world.getUID())) continue;
                    if (!MaleniaInvocationStructure.areAllCandlesLit(origin)) continue;
                    startInvocation(origin);
                    return;
                }
            }
        }, 5L);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() != MaleniaInvocationStructure.OFFERING) return;
        World world = event.getPlayer().getWorld();
        if (!inBossWorld(world)) return;
        InvocationData data = activeInvocations.get(world.getUID());
        if (data == null) return;

        Location heart = MaleniaInvocationStructure.getHeartLocation(data.origin);
        Location dropped = event.getItemDrop().getLocation();
        if (dropped.distance(heart) > MaleniaInvocationStructure.getRadius()) return;

        event.getItemDrop().remove();
        stopInvocation(world);
        MaleniaInvocationStructure.extinguishAllCandles(data.origin);
        arrive(world, heart);
    }

    /** The offering: the rot blooms into a scarlet flower, then bursts, and Malenia is standing in it. */
    private void arrive(World world, Location heart) {
        Fx fx = LiveStage.fxIn(world);
        Vector center = heart.toVector().add(new Vector(0, 0.2, 0));
        fx.sound(center, Sfx.BELL_RESONATE, 3f, 0.5f);
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(heart) > 60 * 60) continue;
            p.showTitle(Title.title(Component.text("Malenia", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text("Blade of Miquella", NamedTextColor.GOLD),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
            p.sendMessage(Component.text("\"I am Malenia, Blade of Miquella.\"", NamedTextColor.RED,
                    TextDecoration.ITALIC));
        }
        new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                tick += 2;
                // Petals unfolding: rings that open wider as the flower blooms.
                double open = 0.4 + 2.8 * tick / (double) ARRIVAL_DELAY;
                for (int petal = 0; petal < 6; petal++) {
                    double angle = petal * Math.PI / 3 + tick * 0.05;
                    Vector tip = Shapes.onCircle(center, open, angle, new Vector(1, 0, 0), new Vector(0, 0, 1))
                            .add(new Vector(0, 0.4 * tick / (double) ARRIVAL_DELAY, 0));
                    fx.line(center, tip, 0.25, fx.dust(petal % 2 == 0 ? SCARLET : ROT, 1.4f));
                }
                if (tick % 6 == 0) fx.cloud(Particle.SPORE_BLOSSOM_AIR, center, 6, 1.5, 0.01);
                if (tick < ARRIVAL_DELAY) return;
                cancel();
                fx.sound(center, Sfx.WARDEN_SONIC_BOOM, 2f, 0.7f);
                fx.flash(center, SCARLET);
                fx.draw(Shapes.sphere(center, 5, 160), fx.dust(ROT, 2.0f));
                fx.burst(center, Particle.CRIMSON_SPORE, 120, 0.3);
                if (plugin.getMalenia() != null) plugin.getMalenia().trySpawn(heart.clone());
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void startInvocation(Location origin) {
        World world = origin.getWorld();
        if (world == null) return;
        Fx fx = LiveStage.fxIn(world);
        Location heart = MaleniaInvocationStructure.getHeartLocation(origin);
        fx.sound(heart.toVector(), Sfx.BELL_RESONATE, 2f, 0.8f);
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(heart) > 40 * 40) continue;
            p.sendMessage(Component.text("✿ The scarlet rot stirs. Offer a torchflower...", NamedTextColor.RED));
        }

        BukkitRunnable task = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!MaleniaInvocationStructure.areAllCandlesLit(origin) || !MaleniaInvocationStructure.isStructureComplete(origin)) {
                    stopInvocation(world);
                    return;
                }
                tick += 2;
                Vector top = heart.toVector();
                // Threads of rot run from each pillar into the heart.
                for (Location pillar : MaleniaInvocationStructure.getPillarTops(origin)) {
                    fx.line(pillar.toVector(), top.clone().add(new Vector(0, 0.2, 0)), 0.4,
                            fx.dust(tick % 8 < 4 ? SCARLET : ROT, 0.9f).sometimes(0.6));
                }
                // A closed bud over the heart, turning slowly, gold at its core.
                Vector bud = top.clone().add(new Vector(0, 1.6, 0));
                Vector u = new Vector(1, 0, 0);
                Vector v = new Vector(0, 0, 1);
                fx.draw(Shapes.circle(bud, 0.9, 24, u, v, tick * 0.05), fx.dust(SCARLET, 1.1f));
                fx.draw(Shapes.circle(bud.clone().add(new Vector(0, 0.35, 0)), 0.55, 16, u, v, -tick * 0.05),
                        fx.dust(ROT, 1.0f));
                fx.draw(Shapes.circle(bud, 0.25, 8, u, v, 0), fx.dust(GOLD, 1.2f));
                if (tick % 10 == 0) fx.cloud(Particle.CRIMSON_SPORE, top.clone().add(new Vector(0, 0.5, 0)), 4, 0.6, 0.01);
                if (tick % 40 == 0) fx.sound(bud, Sfx.BELL_RESONATE, 0.6f, 1.4f);
            }
        };
        task.runTaskTimer(plugin, 0L, 2L);
        activeInvocations.put(world.getUID(), new InvocationData(origin, task));
    }

    private void stopInvocation(World world) {
        InvocationData data = activeInvocations.remove(world.getUID());
        if (data != null && data.task != null) data.task.cancel();
    }

    private record InvocationData(Location origin, BukkitRunnable task) {
    }
}
