package de.joker.randomizer.manager;

import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.cache.PlayerCache;
import de.joker.randomizer.data.Database;
import de.joker.randomizer.data.Ranking;
import lombok.Getter;


@Getter
public class ServiceManager {

    private final Database database;
    private final PlayerCache playerCache;
    private final Ranking ranking;
    private final IslandManager islandManager;
    private final SkyRandomizer plugin;
    private final TranslationManager translator;

    public ServiceManager(Database database, SkyRandomizer plugin) {
        this.database = database;
        this.playerCache = new PlayerCache(database);
        this.ranking = new Ranking(playerCache);
        this.islandManager = new IslandManager(playerCache);
        this.plugin = plugin;
        this.translator = new TranslationManager();
    }

    public void shutdown() {
        playerCache.invalidateAll();
    }
}