package de.joker.randomizer.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.joker.randomizer.data.Database;
import de.joker.randomizer.data.PlayerData;

import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCache {

    private final Database database;
    private final Cache<UUID, PlayerData> cache;
    private final Map<UUID, PlayerData> allPlayers = new ConcurrentHashMap<>();

    public PlayerCache(Database database) {
        this.database = database;
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .expireAfterAccess(Duration.ofMinutes(15))
                .build();

        loadAllPlayersFromDatabase();
    }

    private void loadAllPlayersFromDatabase() {
        allPlayers.clear();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                    SELECT uuid, name, max_distance, island_x
                    FROM players
                """)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int maxDistance = rs.getInt("max_distance");
                int islandX = rs.getInt("island_x");

                PlayerData playerData = new PlayerData(uuid, name, maxDistance, islandX);
                allPlayers.put(uuid, playerData);
                cache.put(uuid, playerData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized PlayerData getPlayer(UUID uuid) {
        PlayerData cached = cache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }

        PlayerData player = allPlayers.get(uuid);
        if (player != null) {
            cache.put(uuid, player);
            return player;
        }

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                    SELECT uuid, name, max_distance, island_x
                    FROM players WHERE uuid = ?
                """)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                int maxDistance = rs.getInt("max_distance");
                int islandX = rs.getInt("island_x");

                player = new PlayerData(uuid, name, maxDistance, islandX);
                allPlayers.put(uuid, player);
                cache.put(uuid, player);
                return player;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public synchronized void updatePlayer(UUID uuid, String name, int newDistance) {
        PlayerData player = getPlayer(uuid);
        if (player == null) {
            player = new PlayerData(uuid, name, newDistance, 0);
        } else {
            player.setDistance(newDistance);
            player.setName(name);
        }

        allPlayers.put(uuid, player);
        cache.put(uuid, player);

        try (Connection conn = database.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO players (uuid, name, max_distance, island_x)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                max_distance = excluded.max_distance
            """);
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, newDistance);
            ps.setInt(4, player.getIslandX());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updatePlayerIsland(UUID uuid, String name, int islandX) {
        PlayerData player = getPlayer(uuid);
        if (player == null) {
            player = new PlayerData(uuid, name, 0, islandX);
        } else {
            player.setIslandX(islandX);
            player.setName(name);
        }

        allPlayers.put(uuid, player);
        cache.put(uuid, player);

        try (Connection conn = database.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO players (uuid, name, max_distance, island_x)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                island_x = excluded.island_x
            """);
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, player.getDistance());
            ps.setInt(4, islandX);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addPlayerIfNotExists(UUID uuid, String name) {
        if (!allPlayers.containsKey(uuid)) {
            PlayerData newPlayer = new PlayerData(uuid, name, 0, 0);
            allPlayers.put(uuid, newPlayer);
            cache.put(uuid, newPlayer);

            try (Connection conn = database.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO players (uuid, name, max_distance, island_x)
                    VALUES (?, ?, ?, ?)
                """);
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Collection<PlayerData> getAllPlayers() {
        return Collections.unmodifiableCollection(allPlayers.values());
    }

    public List<PlayerData> getTopPlayers(int limit) {
        return allPlayers.values().stream()
                .sorted((a, b) -> Integer.compare(b.getDistance(), a.getDistance()))
                .limit(limit)
                .toList();
    }

    public Optional<Integer> getPlayerRank(UUID uuid) {
        List<PlayerData> sorted = allPlayers.values().stream()
                .sorted((a, b) -> Integer.compare(b.getDistance(), a.getDistance()))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getUuid().equals(uuid)) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }

    public int getNextFreeIslandX() {
        return allPlayers.values().stream()
                .mapToInt(PlayerData::getIslandX)
                .max()
                .orElse(0) + 7;
    }

    public void invalidatePlayer(UUID uuid) {
        cache.invalidate(uuid);
    }

    public void invalidateAll() {
        cache.invalidateAll();
        loadAllPlayersFromDatabase();
    }

    public long getCacheSize() {
        return cache.estimatedSize();
    }
}