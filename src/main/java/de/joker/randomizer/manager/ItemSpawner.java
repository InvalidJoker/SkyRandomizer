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

    private final Map<Player, Integer> playerTimers = new HashMap<>();
    private final Map<Player, BossBar> bossBars = new HashMap<>();
    private static final int MAX_ITEMS_PER_BLOCK = 10;

    public ItemSpawner(SkyRandomizer plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int maxTime = 20;
                    RealmInformationProvider informationProvider = plugin.getServiceManager().getInformationProvider();
                    try {
                        if (informationProvider != null) {
                            var boostsHolder = informationProvider.boostsByPlayer(player.getUniqueId());
                            Integer boosts = null;

                            if (boostsHolder != null) {
                                boosts = boostsHolder.value();
                            }

                            int boostsValue = (boosts != null) ? boosts : 0;

                            maxTime = switch (boostsValue) {
                                case 0 -> 20;
                                case 1 -> 17;
                                default -> 15;
                            };
                        }
                    } catch (Exception e) {
                        log.error("Error while fetching boosts for player {}: {}", player.getName(), e.getMessage());
                    }

                    playerTimers.putIfAbsent(player, maxTime);
                    int secondsLeft = playerTimers.get(player);

                    Location island = islandManager.getOrCreateIsland(player);
                    Location spawnLocation = island.clone().add(0.5, 1.25, 0.5);

                    int itemCount = (int) spawnLocation.getWorld().getNearbyEntities(spawnLocation, 1.0, 1.0, 1.0)
                            .stream()
                            .filter(entity -> entity instanceof Item)
                            .count();

                    boolean isBlocked = itemCount >= MAX_ITEMS_PER_BLOCK;

                    BossBar bar = bossBars.computeIfAbsent(player, p -> BossBar.bossBar(
                            MessageUtils.parse(""),
                            1.0f,
                            BossBar.Color.GREEN,
                            BossBar.Overlay.PROGRESS
                    ));

                    if (isBlocked) {
                        bar.progress(1.0f);
                        bar.name(MessageUtils.parse("<gradient:#FF6B6B:#FF8E8E>Spawner blockiert! Sammle deine Items!"));
                        bar.color(BossBar.Color.RED);
                    } else {
                        secondsLeft--;

                        float progress = Math.max(0f, secondsLeft / (float) maxTime);
                        bar.progress(progress);
                        bar.name(MessageUtils.parse("<gradient:#3AC47D:#8cd1bc>Nächstes Item in " + secondsLeft + "s"));
                        bar.color(BossBar.Color.GREEN);

                        if (secondsLeft <= 0) {
                            spawnRandomItem(player);
                            secondsLeft = maxTime;
                        }

                        playerTimers.put(player, secondsLeft);
                    }

                    bar.addViewer(player);
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
            ItemType.COMMAND_BLOCK_MINECART,
            ItemType.ENDER_DRAGON_SPAWN_EGG,
            ItemType.DRAGON_EGG,
            ItemType.WITHER_SPAWN_EGG,
            ItemType.WITHER_SKELETON_SKULL,
            ItemType.GHAST_SPAWN_EGG,
            ItemType.ENDERMAN_SPAWN_EGG,
            ItemType.VEX_SPAWN_EGG,
            ItemType.VINDICATOR_SPAWN_EGG,
            ItemType.PHANTOM_SPAWN_EGG,
            ItemType.ELDER_GUARDIAN_SPAWN_EGG,
            ItemType.BLAZE_SPAWN_EGG,
            ItemType.WARDEN_SPAWN_EGG
    );

    private ItemType getRandomMaterial() {
        var reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM);
        var items = reg.stream()
                .filter(item -> !BLACKLIST.contains(item) && item != ItemTypes.AIR);
        return items.skip(random.nextInt(reg.size())).findFirst().orElse(ItemType.STONE);
    }
}
