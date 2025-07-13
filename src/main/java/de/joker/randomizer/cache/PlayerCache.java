package de.joker.randomizer.cache;

import de.joker.randomizer.data.Database;
import de.joker.randomizer.data.PlayerData;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCache {

    private final Database database;
    private final Map<UUID, PlayerData> allPlayers = new ConcurrentHashMap<>();

    public PlayerCache(Database database) {
        this.database = database;

        loadAllPlayersFromDatabase();
    }

    private void loadAllPlayersFromDatabase() {
        allPlayers.clear();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                    SELECT uuid, name, max_distance, island_x, coins
                    FROM players
                """)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                int maxDistance = rs.getInt("max_distance");
                int islandX = rs.getInt("island_x");
                int coins = rs.getInt("coins");

                PlayerData playerData = new PlayerData(uuid, name, maxDistance, islandX, coins);
                allPlayers.put(uuid, playerData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized PlayerData getPlayer(UUID uuid) {
        PlayerData player = allPlayers.get(uuid);
        if (player != null) {
            return player;
        }

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                    SELECT uuid, name, max_distance, island_x, coins
                    FROM players WHERE uuid = ?
                """)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                int maxDistance = rs.getInt("max_distance");
                int islandX = rs.getInt("island_x");
                int coins = rs.getInt("coins");

                player = new PlayerData(uuid, name, maxDistance, islandX, coins);
                allPlayers.put(uuid, player);
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
            player = new PlayerData(uuid, name, newDistance, 0, 0);
        } else {
            player.setDistance(newDistance);
            player.setName(name);
        }

        allPlayers.put(uuid, player);

        try (Connection conn = database.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO players (uuid, name, max_distance, island_x, coins)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                max_distance = excluded.max_distance
            """);
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, newDistance);
            ps.setInt(4, player.getIslandX());
            ps.setInt(5, player.getCoins());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updatePlayerCoins(UUID uuid, String name, int newCoins) {
        PlayerData player = getPlayer(uuid);
        if (player == null) {
            player = new PlayerData(uuid, name, 0, 0, newCoins);
        } else {
            player.setCoins(newCoins);
            player.setName(name);
        }

        allPlayers.put(uuid, player);

        try (Connection conn = database.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO players (uuid, name, max_distance, island_x, coins)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                coins = excluded.coins
            """);
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, player.getDistance());
            ps.setInt(4, player.getIslandX());
            ps.setInt(5, newCoins);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updatePlayerIsland(UUID uuid, String name, int islandX) {
        PlayerData player = getPlayer(uuid);
        if (player == null) {
            player = new PlayerData(uuid, name, 0, islandX, 0);
        } else {
            player.setIslandX(islandX);
            player.setName(name);
        }

        allPlayers.put(uuid, player);

        try (Connection conn = database.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO players (uuid, name, max_distance, island_x, coins)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                island_x = excluded.island_x
            """);
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, player.getDistance());
            ps.setInt(4, islandX);
            ps.setInt(5, player.getCoins());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addPlayerIfNotExists(UUID uuid, String name) {
        if (!allPlayers.containsKey(uuid)) {
            PlayerData newPlayer = new PlayerData(uuid, name, 0, 0, 0);
            allPlayers.put(uuid, newPlayer);

            try (Connection conn = database.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO players (uuid, name, max_distance, island_x, coins)
                    VALUES (?, ?, ?, ?, ?)
                """);
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
                ps.setInt(5, 0);
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
                .orElse(0) + 8;
    }

    public void invalidateAll() {
        allPlayers.clear();
    }
}