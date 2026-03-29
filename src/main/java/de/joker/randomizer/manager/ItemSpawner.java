package de.joker.randomizer.manager;

import de.cytooxien.realms.api.RealmInformationProvider;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.data.IslandData;
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

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class ItemSpawner {

    private static final int MAX_ITEMS_PER_BLOCK = 10;

    private final SkyRandomizer plugin;
    private final IslandManager islandManager;
    private final Random random = new Random();

    private final Map<Long, Integer> islandTimers = new HashMap<>();
    private final Map<Player, BossBar> bossBars = new HashMap<>();

    public ItemSpawner(SkyRandomizer plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Map<Long, IslandTickState> islandStates = new LinkedHashMap<>();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    IslandData island = islandManager.getIslandData(player);
                    if (island == null) {
                        islandManager.getOrCreateIsland(player);
                        island = islandManager.getIslandData(player);
                    }

                    if (island == null) {
                        continue;
                    }

                    IslandData finalIsland = island;
                    IslandTickState state = islandStates.computeIfAbsent(finalIsland.getId(), ignored -> new IslandTickState(finalIsland));
                    state.players.add(player);
                    state.maxTime = Math.min(state.maxTime, getMaxTimeForPlayer(player));
                }

                for (IslandTickState state : islandStates.values()) {
                    tickIsland(state);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void tickIsland(IslandTickState state) {
        IslandData island = state.island;
        int maxTime = state.maxTime;
        int secondsLeft = Math.min(islandTimers.getOrDefault(island.getId(), maxTime), maxTime);

        Location spawnLocation = islandManager.getIslandLocation(island).clone().add(0.5, 1.25, 0.5);
        int itemCount = spawnLocation.getWorld()
                .getNearbyEntities(spawnLocation, 1.0, 1.0, 1.0, entity -> entity instanceof Item)
                .size();

        boolean isBlocked = itemCount >= MAX_ITEMS_PER_BLOCK;

        if (isBlocked) {
            for (Player player : state.players) {
                BossBar bar = getBossBar(player);
                bar.progress(1.0f);
                bar.name(MessageUtils.parse("<gradient:#FF6B6B:#FF8E8E>Spawner blockiert! Sammle deine Items!"));
                bar.color(BossBar.Color.RED);
                bar.addViewer(player);
            }
            return;
        }

        secondsLeft--;
        if (secondsLeft <= 0) {
            spawnRandomItem(island);
            secondsLeft = maxTime;
        }
        islandTimers.put(island.getId(), secondsLeft);

        float progress = Math.max(0f, secondsLeft / (float) maxTime);
        for (Player player : state.players) {
            BossBar bar = getBossBar(player);
            bar.progress(progress);
            bar.name(MessageUtils.parse("<gradient:#3AC47D:#8cd1bc>Nächstes Item in " + secondsLeft + "s"));
            bar.color(BossBar.Color.GREEN);
            bar.addViewer(player);
        }
    }

    private BossBar getBossBar(Player player) {
        return bossBars.computeIfAbsent(player, ignored -> BossBar.bossBar(
                MessageUtils.parse(""),
                1.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        ));
    }

    private void spawnRandomItem(IslandData island) {
        Location spawnLocation = islandManager.getIslandLocation(island).clone().add(0.5, 1.25, 0.5);

        int itemCount = (int) spawnLocation.getWorld().getNearbyEntities(spawnLocation, 1.0, 1.0, 1.0)
                .stream()
                .filter(entity -> entity instanceof Item)
                .count();

        if (itemCount >= MAX_ITEMS_PER_BLOCK) {
            return;
        }

        ItemType randomItem = getRandomMaterial();
        Item item = spawnLocation.getWorld().dropItem(spawnLocation, randomItem.createItemStack(1));
        item.setVelocity(new Vector(0.0, 0.0, 0.0));
    }

    private int getMaxTimeForPlayer(Player player) {
        int maxTime = 20;
        RealmInformationProvider informationProvider = plugin.getServiceManager().getInformationProvider();
        try {
            if (informationProvider != null) {
                var boosts = informationProvider.boosts().value().stream()
                        .filter(boost -> boost.playerId().equals(player.getUniqueId()))
                        .findFirst()
                        .orElse(null);

                int boostsValue = boosts != null ? boosts.amount() : 0;
                maxTime = switch (boostsValue) {
                    case 0 -> 20;
                    case 1 -> 17;
                    default -> 15;
                };
            }
        } catch (Exception e) {
            log.error("Error while fetching boosts for player {}: {}", player.getName(), e.getMessage());
        }

        return maxTime;
    }

    private static final class IslandTickState {
        private final IslandData island;
        private final List<Player> players = new ArrayList<>();
        private int maxTime = 20;

        private IslandTickState(IslandData island) {
            this.island = island;
        }
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
            ItemType.WARDEN_SPAWN_EGG,
            ItemType.LIGHT
    );

    private ItemType getRandomMaterial() {
        var reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM);
        var items = reg.stream()
                .filter(item -> !BLACKLIST.contains(item));
        return items.skip(random.nextInt(reg.size())).findFirst().orElse(ItemType.STONE);
    }
}
