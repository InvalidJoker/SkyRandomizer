package de.joker.randomizer.manager;

import de.cytooxien.realms.api.PlayerInformationProvider;
import de.cytooxien.realms.api.RealmInformationProvider;
import de.cytooxien.realms.api.RealmPermissionProvider;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.cache.IslandCache;
import de.joker.randomizer.data.Database;
import de.joker.randomizer.data.Ranking;
import de.joker.randomizer.utils.LocalizationManager;
import de.joker.randomizer.utils.MessageUtils;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class ServiceManager {

    private final Database database;
    private final IslandCache islandCache;
    private final Ranking ranking;
    private final IslandManager islandManager;
    private final CoopManager coopManager;
    private final SkyRandomizer plugin;
    private final LocalizationManager localizationManager;

    @Setter
    private ScoreboardManager scoreboardManager;

    private PlayerInformationProvider playerInformationProvider;
    private RealmInformationProvider informationProvider;
    private RealmPermissionProvider permissionProvider;

    public ServiceManager(Database database, SkyRandomizer plugin) {
        this.database = database;
        this.islandCache = new IslandCache(database);
        this.ranking = new Ranking(islandCache);
        this.islandManager = new IslandManager(islandCache, plugin);
        this.coopManager = new CoopManager(this);
        this.plugin = plugin;
        this.localizationManager = new LocalizationManager(plugin, this::getPlayerInformationProvider);
        MessageUtils.init(localizationManager);
    }

    public void shutdown() {
        islandCache.invalidateAll();
    }

    public PlayerInformationProvider getPlayerInformationProvider() {
        try {
            if (playerInformationProvider == null) {
                playerInformationProvider = Bukkit.getServicesManager().load(PlayerInformationProvider.class);
            }
        } catch (Exception e) {
            playerInformationProvider = null;
        }
        return playerInformationProvider;
    }

    public RealmInformationProvider getInformationProvider() {
        try {
            if (informationProvider == null) {
                informationProvider = Bukkit.getServicesManager().load(RealmInformationProvider.class);
            }
        } catch (Exception e) {
            informationProvider = null;
        }
        return informationProvider;
    }

    public RealmPermissionProvider getPermissionProvider() {
        try {
            if (permissionProvider == null) {
                permissionProvider = Bukkit.getServicesManager().load(RealmPermissionProvider.class);
            }
        } catch (Exception e) {
            permissionProvider = null;
        }
        return permissionProvider;
    }

    public boolean isBooster(Player player) {
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("realms.booster")) {
            return true;
        }

        RealmInformationProvider informationProvider = getInformationProvider();
        if (informationProvider != null) {
            var boosts = informationProvider.boosts().value().stream()
                    .filter(boost -> boost.playerId().equals(uuid))
                    .findFirst()
                    .orElse(null);

            int boostsValue = boosts != null ? boosts.amount() : 0;
            return boostsValue > 0;
        }

        return false;
    }
}
