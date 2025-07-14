package de.joker.randomizer.manager;

import de.cytooxien.realms.api.RealmInformationProvider;
import de.cytooxien.realms.api.RealmPermissionProvider;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.cache.PlayerCache;
import de.joker.randomizer.data.Database;
import de.joker.randomizer.data.Ranking;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;


@Getter
public class ServiceManager {

    private final Database database;
    private final PlayerCache playerCache;
    private final Ranking ranking;
    private final IslandManager islandManager;
    private final SkyRandomizer plugin;
    private RealmInformationProvider informationProvider;
    private RealmPermissionProvider permissionProvider;

    public ServiceManager(Database database, SkyRandomizer plugin) {
        this.database = database;
        this.playerCache = new PlayerCache(database);
        this.ranking = new Ranking(playerCache);
        this.islandManager = new IslandManager(playerCache);
        this.plugin = plugin;
        this.informationProvider = null;
    }

    public void shutdown() {
        playerCache.invalidateAll();
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
        boolean booster = false;
        if (informationProvider != null) {
            var boostsHolder = informationProvider.boostsByPlayer(uuid);
            Integer boosts = null;

            if (boostsHolder != null) {
                boosts = boostsHolder.value();
            }

            int boostsValue = (boosts != null) ? boosts : 0;

            booster = boostsValue > 0;
        }
        return booster;
    }
}