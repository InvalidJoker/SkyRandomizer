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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.bukkit.scheduler.BukkitRunnable;

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
                    int maxTime = 5;
                    if (informationProvider != null) {
                         int boosts = informationProvider.boostsByPlayer(player.getUniqueId()).value();
                         maxTime = switch (boosts) {
                             case 0 -> 5;
                             case 1 -> 4;
                             default -> 3;
                         };
                    } else {
                        log.warn("RealmInformationProvider not found, using default maxTime.");
                    }

                    playerTimers.putIfAbsent(player, maxTime);
                    int secondsLeft = playerTimers.get(player);

                    BossBar bar = bossBars.computeIfAbsent(player, p -> BossBar.bossBar(
                            MessageUtils.parse(""),
                            1.0f,
                            BossBar.Color.GREEN,
                            BossBar.Overlay.PROGRESS
                    ));

                    float progress = Math.max(0f, secondsLeft / (float) maxTime);
                    bar.progress(progress);
                    bar.name(MessageUtils.parse("<gradient:#3AC47D:#8cd1bc>Nächstes Item " + secondsLeft + "s"));
                    bar.addViewer(player);

                    secondsLeft--;

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
        ItemType randomItem = getRandomMaterial();
        island.getWorld().dropItemNaturally(
                island.clone().add(0.0, 1.5, 0.0),
                randomItem.createItemStack(1)
        );
    }

    private static final Set<ItemType> BLACKLIST = Set.of(
            ItemType.BARRIER,
            ItemType.DEBUG_STICK,
            ItemType.COMMAND_BLOCK,
            ItemType.STRUCTURE_VOID,
            ItemType.BEDROCK,
            ItemType.AIR,
            ItemType.PLAYER_HEAD
    );

    private ItemType getRandomMaterial() {
        var reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM);
        var items = reg.stream()
                .filter(item -> !BLACKLIST.contains(item) && item != ItemTypes.AIR);
        return items.skip(random.nextInt(reg.size())).findFirst().orElse(ItemType.STONE);
    }
}
