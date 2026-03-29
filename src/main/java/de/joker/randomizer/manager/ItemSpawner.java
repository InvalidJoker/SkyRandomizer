package de.joker.randomizer.manager;

import de.cytooxien.realms.api.RealmInformationProvider;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.data.IslandData;
import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

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

        if (itemCount >= MAX_ITEMS_PER_BLOCK) {
            for (Player player : state.players) {
                BossBar bar = getBossBar(player);
                bar.progress(1.0f);
                bar.name(MessageUtils.component(player, "itemspawner.blocked"));
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
            bar.name(MessageUtils.component(player, "itemspawner.next", MessageUtils.placeholder("seconds", secondsLeft)));
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

        Item item = spawnLocation.getWorld().dropItem(spawnLocation, getRandomMaterial());
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

    private static final Set<Material> BLACKLIST = Set.of(
            Material.BARRIER,
            Material.BEDROCK,
            Material.PLAYER_HEAD,
            Material.ENDER_DRAGON_SPAWN_EGG,
            Material.DRAGON_EGG,
            Material.WITHER_SPAWN_EGG,
            Material.WITHER_SKELETON_SKULL,
            Material.GHAST_SPAWN_EGG,
            Material.ENDERMAN_SPAWN_EGG,
            Material.VEX_SPAWN_EGG,
            Material.VINDICATOR_SPAWN_EGG,
            Material.PHANTOM_SPAWN_EGG,
            Material.ELDER_GUARDIAN_SPAWN_EGG,
            Material.BLAZE_SPAWN_EGG,
            Material.WARDEN_SPAWN_EGG,
            Material.LIGHT
    );

    private static final Set<String> ignoredCategories = Set.of(
            "itemGroup.op",
            "itemGroup.search",
            "itemGroup.inventory",
            "itemGroup.hotbar",
            "itemGroup.ingredients"
    );

    private ItemStack getRandomMaterial() {
        List<ItemStack> items = CreativeModeTabs.allTabs().stream()
                .peek(creativeModeTab -> creativeModeTab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, DedicatedServer.getServer().registryAccess())))
                .filter(creativeModeTab -> {

                    if (creativeModeTab.getDisplayName().getContents() instanceof TranslatableContents translatableContents)
                        return !ignoredCategories.contains(translatableContents.getKey());
                    return true;
                })
                .flatMap(creativeModeTab -> creativeModeTab.getDisplayItems().stream())
                .filter(Objects::nonNull)
                .map(CraftItemStack::asBukkitCopy)
                .collect(Collectors.toSet())
                .stream()
                .filter(itemStack -> !itemStack.getType().isAir())
                .filter(itemStack -> !BLACKLIST.contains(itemStack.getType()))
                .toList();

        return items.get(random.nextInt(items.size()));
    }
}
