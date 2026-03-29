package de.joker.randomizer.cache;

import de.joker.randomizer.data.Database;
import de.joker.randomizer.data.IslandAssignmentResult;
import de.joker.randomizer.data.IslandData;
import de.joker.randomizer.data.IslandMemberData;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IslandCache {

    private final Database database;
    private final Map<Long, IslandData> islands = new ConcurrentHashMap<>();
    private final Map<UUID, IslandMemberData> membersByUuid = new ConcurrentHashMap<>();

    public IslandCache(Database database) {
        this.database = database;
        loadAllIslandsFromDatabase();
    }

    private void loadAllIslandsFromDatabase() {
        islands.clear();
        membersByUuid.clear();

        try (Connection conn = database.getConnection();
             PreparedStatement islandStatement = conn.prepareStatement("""
                     SELECT id, island_x, max_distance
                     FROM islands
                 """);
             PreparedStatement memberStatement = conn.prepareStatement("""
                     SELECT island_id, uuid, name
                     FROM island_members
                     ORDER BY island_id, name
                 """)) {

            ResultSet islandResult = islandStatement.executeQuery();
            while (islandResult.next()) {
                long id = islandResult.getLong("id");
                int islandX = islandResult.getInt("island_x");
                int maxDistance = islandResult.getInt("max_distance");
                islands.put(id, new IslandData(id, islandX, maxDistance));
            }

            ResultSet memberResult = memberStatement.executeQuery();
            while (memberResult.next()) {
                long islandId = memberResult.getLong("island_id");
                UUID uuid = UUID.fromString(memberResult.getString("uuid"));
                String name = memberResult.getString("name");

                IslandData island = islands.get(islandId);
                if (island == null) {
                    continue;
                }

                IslandMemberData member = new IslandMemberData(uuid, name, islandId);
                island.addMember(member);
                membersByUuid.put(uuid, member);
            }
        } catch (SQLException e) {
            log.error("Failed to load islands from database", e);
        }
    }

    public synchronized IslandData getIslandOfPlayer(UUID uuid) {
        IslandMemberData member = membersByUuid.get(uuid);
        if (member == null) {
            return null;
        }
        return islands.get(member.getIslandId());
    }

    public synchronized IslandMemberData getMember(UUID uuid) {
        return membersByUuid.get(uuid);
    }

    public synchronized IslandData getIsland(long islandId) {
        return islands.get(islandId);
    }

    public synchronized IslandAssignmentResult assignPlayerToNewIsland(UUID uuid, String name) {
        IslandData previousIsland = getIslandOfPlayer(uuid);
        IslandData deletedIsland = shouldDeletePreviousIsland(previousIsland) ? new IslandData(previousIsland) : null;
        int newIslandX = getNextFreeIslandX();

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);

            long islandId;
            try (PreparedStatement insertIsland = conn.prepareStatement("""
                    INSERT INTO islands (island_x, max_distance)
                    VALUES (?, 0)
                """, Statement.RETURN_GENERATED_KEYS)) {
                insertIsland.setInt(1, newIslandX);
                insertIsland.executeUpdate();

                ResultSet generatedKeys = insertIsland.getGeneratedKeys();
                if (!generatedKeys.next()) {
                    throw new SQLException("Could not read generated island id");
                }
                islandId = generatedKeys.getLong(1);
            }

            upsertIslandMember(conn, uuid, name, islandId);

            if (deletedIsland != null) {
                deleteIsland(conn, deletedIsland.getId());
            }

            conn.commit();

            IslandData newIsland = new IslandData(islandId, newIslandX, 0);
            islands.put(islandId, newIsland);
            moveMemberInCache(uuid, name, newIsland, previousIsland, deletedIsland != null);

            if (deletedIsland != null) {
                islands.remove(deletedIsland.getId());
            }

            return new IslandAssignmentResult(newIsland, deletedIsland);
        } catch (SQLException e) {
            log.error("Failed to assign player {} to a new island", uuid, e);
        }

        return previousIsland == null ? null : new IslandAssignmentResult(previousIsland, null);
    }

    public synchronized IslandAssignmentResult assignPlayerToIsland(UUID uuid, String name, long targetIslandId) {
        IslandData targetIsland = islands.get(targetIslandId);
        if (targetIsland == null) {
            return null;
        }

        IslandData previousIsland = getIslandOfPlayer(uuid);
        if (previousIsland != null && previousIsland.getId() == targetIslandId) {
            updateMemberName(uuid, name);
            return new IslandAssignmentResult(targetIsland, null);
        }

        IslandData deletedIsland = shouldDeletePreviousIsland(previousIsland) ? new IslandData(previousIsland) : null;

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);

            upsertIslandMember(conn, uuid, name, targetIslandId);

            if (deletedIsland != null) {
                deleteIsland(conn, deletedIsland.getId());
            }

            conn.commit();
            moveMemberInCache(uuid, name, targetIsland, previousIsland, deletedIsland != null);

            if (deletedIsland != null) {
                islands.remove(deletedIsland.getId());
            }

            return new IslandAssignmentResult(targetIsland, deletedIsland);
        } catch (SQLException e) {
            log.error("Failed to assign player {} to island {}", uuid, targetIslandId, e);
        }

        return null;
    }

    public synchronized void updateMemberName(UUID uuid, String name) {
        IslandMemberData member = membersByUuid.get(uuid);
        if (member == null || Objects.equals(member.getName(), name)) {
            return;
        }

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     UPDATE island_members
                     SET name = ?
                     WHERE uuid = ?
                 """)) {
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
            member.setName(name);
        } catch (SQLException e) {
            log.error("Failed to update member name for {}", uuid, e);
        }
    }

    public synchronized void updateIslandDistance(long islandId, int newDistance) {
        IslandData island = islands.get(islandId);
        if (island == null) {
            return;
        }

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     UPDATE islands
                     SET max_distance = ?
                     WHERE id = ?
                 """)) {
            ps.setInt(1, newDistance);
            ps.setLong(2, islandId);
            ps.executeUpdate();
            island.setDistance(newDistance);
        } catch (SQLException e) {
            log.error("Failed to update island distance for island {}", islandId, e);
        }
    }

    public synchronized List<IslandData> getTopIslands(int limit) {
        return islands.values().stream()
                .sorted((a, b) -> Integer.compare(b.getDistance(), a.getDistance()))
                .limit(limit)
                .toList();
    }

    public synchronized Optional<Integer> getIslandRank(long islandId) {
        List<IslandData> sorted = islands.values().stream()
                .sorted((a, b) -> Integer.compare(b.getDistance(), a.getDistance()))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId() == islandId) {
                return Optional.of(i + 1);
            }
        }

        return Optional.empty();
    }

    public synchronized int getNextFreeIslandX() {
        return islands.values().stream()
                .mapToInt(IslandData::getIslandX)
                .max()
                .orElse(0) + 8;
    }

    public synchronized void invalidateAll() {
        islands.clear();
        membersByUuid.clear();
    }

    private boolean shouldDeletePreviousIsland(IslandData previousIsland) {
        return previousIsland != null && previousIsland.getMemberCount() <= 1;
    }

    private void moveMemberInCache(UUID uuid, String name, IslandData targetIsland, IslandData previousIsland, boolean deletedPreviousIsland) {
        IslandMemberData member = membersByUuid.get(uuid);
        if (member == null) {
            member = new IslandMemberData(uuid, name, targetIsland.getId());
            membersByUuid.put(uuid, member);
        } else {
            member.setName(name);
            member.setIslandId(targetIsland.getId());
        }

        if (previousIsland != null) {
            previousIsland.removeMember(uuid);
        }

        if (!targetIsland.hasMember(uuid)) {
            targetIsland.addMember(member);
        } else {
            targetIsland.getMembers().put(uuid, member);
        }

        if (deletedPreviousIsland && previousIsland != null) {
            previousIsland.getMembers().clear();
        }
    }

    private void upsertIslandMember(Connection conn, UUID uuid, String name, long islandId) throws SQLException {
        if (membersByUuid.containsKey(uuid)) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE island_members
                    SET island_id = ?, name = ?
                    WHERE uuid = ?
                """)) {
                ps.setLong(1, islandId);
                ps.setString(2, name);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            }
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO island_members (uuid, island_id, name)
                VALUES (?, ?, ?)
            """)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, islandId);
            ps.setString(3, name);
            ps.executeUpdate();
        }
    }

    private void deleteIsland(Connection conn, long islandId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                DELETE FROM islands
                WHERE id = ?
            """)) {
            ps.setLong(1, islandId);
            ps.executeUpdate();
        }
    }
}
