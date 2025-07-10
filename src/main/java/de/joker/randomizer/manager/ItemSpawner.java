package de.joker.randomizer.manager;

import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.utils.MessageUtils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class ItemSpawner {

    private final SkyRandomizer plugin;
    private final IslandManager islandManager;
    private final Random random = new Random();

    private final BossBar bossBar;
    private int secondsUntilNextItem = 10;

    public ItemSpawner(SkyRandomizer plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.bossBar = BossBar.bossBar(MessageUtils.parse("Nächstes Item"), 1.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                bossBar.progress(Math.max(0, secondsUntilNextItem / 10.0f));

                for (Player player : Bukkit.getOnlinePlayers()) {
                    bossBar.addViewer(player);
                }

                secondsUntilNextItem--;

                if (secondsUntilNextItem <= 0) {
                    spawnRandomItems();
                    secondsUntilNextItem = 10;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void spawnRandomItems() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location island = islandManager.getOrCreateIsland(player);
            Material randomItem = getRandomMaterial();
            island.getWorld().dropItemNaturally(
                    island.clone().add(0.0, 1.5, 0.0),
                    new org.bukkit.inventory.ItemStack(randomItem, 1)
            );
        }
    }

    private Material getRandomMaterial() {
        Material[] values = Material.values();
        Material mat;
        do {
            mat = values[random.nextInt(values.length)];
        } while (!mat.isItem());
        return mat;
    }
}
