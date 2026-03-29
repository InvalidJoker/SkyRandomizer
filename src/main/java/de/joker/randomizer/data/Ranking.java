package de.joker.randomizer.data;

import de.joker.randomizer.cache.IslandCache;

import java.util.*;

public class Ranking {

    private final IslandCache islandCache;

    public Ranking(IslandCache islandCache) {
        this.islandCache = islandCache;
    }

    public synchronized void updatePlayer(UUID uuid, String name, int newDistance) {
        IslandData island = islandCache.getIslandOfPlayer(uuid);
        if (island == null) {
            return;
        }
        islandCache.updateMemberName(uuid, name);
        islandCache.updateIslandDistance(island.getId(), newDistance);
    }

    public List<IslandData> getTop3() {
        return islandCache.getTopIslands(3);
    }

    public List<IslandData> getTopIslands(int limit) {
        return islandCache.getTopIslands(limit);
    }

    public IslandData getIslandOfPlayer(UUID uuid) {
        return islandCache.getIslandOfPlayer(uuid);
    }
}
