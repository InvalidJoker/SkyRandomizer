package de.joker.randomizer.data;

import de.joker.randomizer.cache.PlayerCache;

import java.util.*;

public class Ranking {

    private final PlayerCache playerCache;

    public Ranking(PlayerCache playerCache) {
        this.playerCache = playerCache;
    }

    public synchronized void updatePlayer(UUID uuid, String name, int newDistance) {
        playerCache.updatePlayer(uuid, name, newDistance);
    }

    public void addPlayerIfNotExists(UUID uuid, String name) {
        playerCache.addPlayerIfNotExists(uuid, name);
    }

    public List<PlayerData> getTop3() {
        return playerCache.getTopPlayers(3);
    }

    public PlayerRank getRankOfPlayer(UUID uuid) {
        PlayerData player = playerCache.getPlayer(uuid);
        if (player == null) {
            return null;
        }

        Optional<Integer> rank = playerCache.getPlayerRank(uuid);
        return rank.map(r -> new PlayerRank(r, player.getDistance())).orElse(null);
    }

    public List<PlayerData> getTopPlayers(int limit) {
        return playerCache.getTopPlayers(limit);
    }

    public PlayerData getPlayer(UUID uuid) {
        return playerCache.getPlayer(uuid);
    }
}