package de.joker.randomizer.manager;

import de.cytooxien.realms.api.RealmInformationProvider;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.cache.PlayerCache;
import de.joker.randomizer.data.Database;
import de.joker.randomizer.data.Ranking;
import lombok.Getter;
import org.bukkit.Bukkit;


@Getter
public class ServiceManager {

    private final Database database;
    private final PlayerCache playerCache;
    private final Ranking ranking;
    private final IslandManager islandManager;
    private final SkyRandomizer plugin;
    private RealmInformationProvider informationProvider;

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
        if (informationProvider == null) {
            informationProvider = Bukkit.getServicesManager().load(RealmInformationProvider.class);
        }
        return informationProvider;
    }
}