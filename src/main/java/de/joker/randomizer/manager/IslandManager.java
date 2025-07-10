package de.joker.randomizer.manager;

import de.joker.randomizer.cache.PlayerCache;
import de.joker.randomizer.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class IslandManager {

    private final PlayerCache playerCache;

    public IslandManager(PlayerCache playerCache) {
        this.playerCache = playerCache;
    }

    private World getWorld() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            throw new IllegalStateException("World 'world' not found!");
        }
        return world;
    }

    public Location getOrCreateIsland(org.bukkit.entity.Player player) {
        PlayerData playerData = playerCache.getPlayer(player.getUniqueId());

        if (playerData != null && playerData.getIslandX() != 0) {
            return new Location(getWorld(), playerData.getIslandX(), 64, 0);
        }

        int newIslandX = playerCache.getNextFreeIslandX();

        generateIslandAt(newIslandX);

        playerCache.updatePlayerIsland(player.getUniqueId(), player.getName(), newIslandX);

        return new Location(getWorld(), newIslandX, 64, 0);
    }

    private void generateIslandAt(int x) {
        int centerY = 64;

        Location bedrock = new Location(getWorld(), x, centerY, 0);
        bedrock.getBlock().setType(Material.BEDROCK);

        for (int y = centerY - 5; y <= centerY + 5; y++) {
            Location barrierLocation = new Location(getWorld(), x + 4, y, 0);
            barrierLocation.getBlock().setType(Material.BARRIER);
        }
    }

    public Location getIslandLocation(org.bukkit.entity.Player player) {
        PlayerData playerData = playerCache.getPlayer(player.getUniqueId());
        if (playerData != null && playerData.getIslandX() != 0) {
            return new Location(getWorld(), playerData.getIslandX(), 64, 0);
        }
        return null;
    }

    public boolean hasIsland(org.bukkit.entity.Player player) {
        PlayerData playerData = playerCache.getPlayer(player.getUniqueId());
        return playerData != null && playerData.getIslandX() != 0;
    }
}