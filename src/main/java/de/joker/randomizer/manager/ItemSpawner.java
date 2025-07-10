package de.joker.randomizer.manager;

import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.utils.MessageUtils;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class ItemSpawner {

    private final SkyRandomizer plugin;
    private final IslandManager islandManager;
    private final Random random = new Random();

    private final BossBar bossBar;
    private int secondsUntilNextItem = 10;
    private final int maxSecondsUntilNextItem = 10;


    public ItemSpawner(SkyRandomizer plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.bossBar = BossBar.bossBar(MessageUtils.parse("Nächstes Item"), 1.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                bossBar.progress(Math.max(0, secondsUntilNextItem / (float) maxSecondsUntilNextItem));

                for (Player player : Bukkit.getOnlinePlayers()) {
                    bossBar.addViewer(player);
                }

                secondsUntilNextItem--;

                if (secondsUntilNextItem <= 0) {
                    spawnRandomItems();
                    secondsUntilNextItem = maxSecondsUntilNextItem;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void spawnRandomItems() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location island = islandManager.getOrCreateIsland(player);
            ItemType randomItem = getRandomMaterial();
            island.getWorld().dropItemNaturally(
                    island.clone().add(0.0, 1.5, 0.0),
                    randomItem.createItemStack(1)
            );
        }
    }

    private ItemType getRandomMaterial() {
        var reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM);

        var items = reg.stream();

        var item = items.skip(random.nextInt((int) reg.size())).findFirst().orElse(null);

        if (item == null) {
            return ItemType.STONE;
        }

        return item;
    }
}
