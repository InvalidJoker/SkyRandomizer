package de.joker.randomizer.manager;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import de.cytooxien.realms.api.RealmInformationProvider;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.utils.MessageUtils;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class ItemSpawner {

    private final SkyRandomizer plugin;
    private final IslandManager islandManager;
    private final Random random = new Random();
    private RealmInformationProvider informationProvider;

    private final Map<Player, Integer> playerTimers = new HashMap<>();
    private final Map<Player, BossBar> bossBars = new HashMap<>();
    private static final int MAX_ITEMS_PER_BLOCK = 10;

    public ItemSpawner(SkyRandomizer plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.informationProvider = null;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (informationProvider == null) {
                        informationProvider = Bukkit.getServicesManager().load(RealmInformationProvider.class);
                    }
                    int maxTime = 15;
                    if (informationProvider != null) {
                         int boosts = informationProvider.boostsByPlayer(player.getUniqueId()).value();
                         maxTime = switch (boosts) {
                             case 0 -> 15;
                             case 1 -> 12;
                             default -> 10;
                         };
                    } else {
                        log.warn("RealmInformationProvider not found, using default maxTime.");
                    }

                    playerTimers.putIfAbsent(player, maxTime);
                    int secondsLeft = playerTimers.get(player);

                    secondsLeft--;

                    BossBar bar = bossBars.computeIfAbsent(player, p -> BossBar.bossBar(
                            MessageUtils.parse(""),
                            1.0f,
                            BossBar.Color.GREEN,
                            BossBar.Overlay.PROGRESS
                    ));

                    float progress = Math.max(0f, secondsLeft / (float) maxTime);
                    bar.progress(progress);
                    bar.name(MessageUtils.parse("<gradient:#3AC47D:#8cd1bc>Nächstes Item in " + secondsLeft + "s"));
                    bar.addViewer(player);

                    if (secondsLeft <= 0) {
                        spawnRandomItem(player);
                        secondsLeft = maxTime;
                    }

                    playerTimers.put(player, secondsLeft);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    private void spawnRandomItem(Player player) {
        Location island = islandManager.getOrCreateIsland(player);
        Location spawnLocation = island.clone().add(0.5, 1.25, 0.5);

        int itemCount = (int) spawnLocation.getWorld().getNearbyEntities(spawnLocation, 1.0, 1.0, 1.0)
                .stream()
                .filter(entity -> entity instanceof Item)
                .count();

        if (itemCount >= MAX_ITEMS_PER_BLOCK) {
            return;
        }

        ItemType randomItem = getRandomMaterial();
        Item item = island.getWorld().dropItem(spawnLocation, randomItem.createItemStack(1));
        item.setVelocity(new Vector(0.0, 0.0, 0.0));
    }

    private static final Set<ItemType> BLACKLIST = Set.of(
            ItemType.BARRIER,
            ItemType.DEBUG_STICK,
            ItemType.COMMAND_BLOCK,
            ItemType.STRUCTURE_VOID,
            ItemType.STRUCTURE_BLOCK,
            ItemType.BEDROCK,
            ItemType.AIR,
            ItemType.PLAYER_HEAD,
            ItemType.CHAIN_COMMAND_BLOCK,
            ItemType.REPEATING_COMMAND_BLOCK,
            ItemType.JIGSAW,
            ItemType.COMMAND_BLOCK_MINECART
    );

    private ItemType getRandomMaterial() {
        var reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM);
        var items = reg.stream()
                .filter(item -> !BLACKLIST.contains(item) && item != ItemTypes.AIR);
        return items.skip(random.nextInt(reg.size())).findFirst().orElse(ItemType.STONE);
    }
}
