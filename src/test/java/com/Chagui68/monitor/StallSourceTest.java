package com.Chagui68.monitor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StallSourceTest {

    @Test
    @DisplayName("A frame of this plugin is blamed on MultiverseCreatures, with the method that ran")
    void ownPlugin() {
        StallSource.Blame b = StallSource.of(new String[]{
                "net.minecraft.world.level.Level.getEntities", "com.Chagui68.entities.boss.fx.Fx.circle",
                "org.bukkit.craftbukkit.scheduler.CraftTask.run"});
        assertEquals("MultiverseCreatures", b.source());
        assertEquals("Fx.circle", b.where());
    }

    @Test
    @DisplayName("Another plugin is named by the first three segments of its package")
    void otherPlugin() {
        StallSource.Blame b = StallSource.of(new String[]{"net.minecraft.Foo.bar", "com.example.shop.Shop.tick"});
        assertEquals("com.example.shop", b.source());
    }

    @Test
    @DisplayName("Slimefun's two package roots are one plugin")
    void slimefun() {
        assertEquals("Slimefun", StallSource.of(new String[]{"io.github.thebusybiscuit.slimefun4.core.X.run"}).source());
        assertEquals("Slimefun", StallSource.of(new String[]{"me.mrCookieSlime.Slimefun.Y.run"}).source());
    }

    @Test
    @DisplayName("Only server frames: the stall is the server's own")
    void server() {
        StallSource.Blame b = StallSource.of(new String[]{
                "net.minecraft.world.level.chunk.ChunkGenerator.fill", "java.lang.Thread.run"});
        assertEquals(StallSource.SERVER, b.source());
    }

    @Test
    @DisplayName("A thread parked on disk or a lock is waiting, unless a plugin asked for it")
    void waiting() {
        assertEquals(StallSource.WAITING, StallSource.of(new String[]{"java.io.FileInputStream.read",
                "net.minecraft.server.Main.run"}).source());
        assertEquals("MultiverseCreatures (waiting)", StallSource.of(new String[]{"java.io.FileInputStream.read",
                "com.Chagui68.music.MusicManager.load"}).source());
    }

    @Test
    @DisplayName("An empty stack does not throw")
    void empty() {
        assertEquals(StallSource.SERVER, StallSource.of(new String[0]).source());
    }
}
