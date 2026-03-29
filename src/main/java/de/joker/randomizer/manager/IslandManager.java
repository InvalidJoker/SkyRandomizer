package de.joker.randomizer.manager;

import de.cytooxien.realms.api.RealmPermissionProvider;
import de.cytooxien.realms.api.model.Group;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.cache.IslandCache;
import de.joker.randomizer.data.IslandAssignmentResult;
import de.joker.randomizer.data.IslandData;
import de.joker.randomizer.data.IslandMemberData;
import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IslandManager {

    private final IslandCache islandCache;
    private final Map<UUID, UUID> textDisplays;
    private final SkyRandomizer plugin;

    public IslandManager(IslandCache islandCache, SkyRandomizer plugin) {
        this.islandCache = islandCache;
        this.textDisplays = new ConcurrentHashMap<>();
        this.plugin = plugin;
    }

    private World getWorld() {
        World world = Bukkit.getWorld(SkyRandomizer.SEASON_WORLD_NAME);
        if (world == null) {
            throw new IllegalStateException("World '" + SkyRandomizer.SEASON_WORLD_NAME + "' not found!");
        }
        return world;
    }

    public IslandData getIslandData(UUID uuid) {
        return islandCache.getIslandOfPlayer(uuid);
    }

    public IslandData getIslandData(Player player) {
        return getIslandData(player.getUniqueId());
    }

    public Location getOrCreateIsland(Player player) {
        islandCache.updateMemberName(player.getUniqueId(), player.getName());
        IslandData islandData = islandCache.getIslandOfPlayer(player.getUniqueId());

        if (islandData != null) {
            return getIslandLocation(islandData);
        }

        IslandAssignmentResult assignmentResult = islandCache.assignPlayerToNewIsland(player.getUniqueId(), player.getName());
        if (assignmentResult == null) {
            throw new IllegalStateException("Could not create island for player " + player.getName());
        }

        IslandData newIsland = assignmentResult.assignedIsland();
        generateIslandAt(newIsland.getIslandX());

        return getIslandLocation(newIsland);
    }

    private void generateIslandAt(int x) {
        int centerY = 64;

        Location bedrock = new Location(getWorld(), x, centerY, 0);
        bedrock.getBlock().setType(Material.BEDROCK);

        for (int i = -4; i <= 1; i++) {
            for (int y = centerY - 5; y <= centerY + 20; y++) {
                Location barrierLocation = new Location(getWorld(), x + 4, y, i);
                barrierLocation.getBlock().setType(Material.BARRIER);
            }
        }

        for (int i = -4; i <= 4; i++) {
            for (int y = centerY - 5; y <= centerY + 20; y++) {
                Location barrierLocation = new Location(getWorld(), x + i, y, -3);
                barrierLocation.getBlock().setType(Material.BARRIER);
            }
        }
    }

    public IslandAssignmentResult createFreshIslandForPlayer(Player player) {
        IslandAssignmentResult assignmentResult = islandCache.assignPlayerToNewIsland(player.getUniqueId(), player.getName());
        if (assignmentResult == null) {
            throw new IllegalStateException("Could not create a fresh island for player " + player.getName());
        }

        if (assignmentResult.deletedIsland() != null) {
            wipeIsland(assignmentResult.deletedIsland());
        }

        generateIslandAt(assignmentResult.assignedIsland().getIslandX());
        return assignmentResult;
    }

    public IslandAssignmentResult movePlayerToIsland(Player player, IslandData targetIsland) {
        IslandAssignmentResult assignmentResult = islandCache.assignPlayerToIsland(player.getUniqueId(), player.getName(), targetIsland.getId());
        if (assignmentResult == null) {
            throw new IllegalStateException("Could not move player " + player.getName() + " to island " + targetIsland.getId());
        }

        if (assignmentResult.deletedIsland() != null) {
            wipeIsland(assignmentResult.deletedIsland());
        }

        return assignmentResult;
    }

    public Location getIslandLocation(Player player) {
        IslandData islandData = islandCache.getIslandOfPlayer(player.getUniqueId());
        return islandData == null ? null : getIslandLocation(islandData);
    }

    public Location getIslandLocation(IslandData islandData) {
        return new Location(getWorld(), islandData.getIslandX(), 64, 0);
    }

    public boolean hasIsland(Player player) {
        return islandCache.getIslandOfPlayer(player.getUniqueId()) != null;
    }

    public Collection<Player> getOnlineMembers(IslandData island) {
        List<Player> players = new ArrayList<>();
        for (IslandMemberData member : island.getMembersView()) {
            Player onlinePlayer = Bukkit.getPlayer(member.getUuid());
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                players.add(onlinePlayer);
            }
        }
        return players;
    }

    public void removeDisplays(IslandData island) {
        for (Player onlineMember : getOnlineMembers(island)) {
            removeDisplay(onlineMember);
        }
    }

    public void wipeIsland(IslandData island) {
        World world = getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        int maxZ = Math.max(1, island.getDistance() + 1);

        for (int x = island.getIslandX() - 3; x <= island.getIslandX() + 4; x++) {
            for (int z = -4; z <= maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    Material replacement = Material.AIR;
                    world.getBlockAt(x, y, z).setType(replacement, false);
                }
            }
        }
    }

    public void createBuildDisplay(Player player, int islandX) {
        World world = player.getWorld();

        double y = player.getEyeLocation().getY();
        double z = 6.0;

        Location location = new Location(world, islandX + 0.5, y, z);

        TextDisplay display = world.spawn(location, TextDisplay.class, td -> {
            td.text(MessageUtils.parse("<gradient:#3AC47D:#8cd1bc>Baue in dieser Richtung um Punkte zu sammeln!"));
            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowed(true);
            td.setSeeThrough(false);
            td.setPersistent(false);
            td.setVisibleByDefault(false);
        });

        textDisplays.put(player.getUniqueId(), display.getUniqueId());
        player.showEntity(plugin, display);
    }

    public void removeDisplay(Player player) {
        UUID entityId = textDisplays.remove(player.getUniqueId());
        if (entityId == null) return;

        for (World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.remove();
                break;
            }
        }
    }

    public void modifyPlayerGroup(Player player, RealmPermissionProvider permissionProvider, int distance) {
        List<Group> groups = permissionProvider.groups();
        var realmGroups = permissionProvider.groupsOfPlayer(player.getUniqueId());
        List<UUID> groupsOfPlayer = List.of();

        if (realmGroups != null) {
            var val = realmGroups.value();
            if (val != null) {
                groupsOfPlayer = val.stream()
                        .map(Group::uniqueId)
                        .toList();
            }
        }

        if (distance >= 100) {
            Group newGroup = groups.stream().filter(group -> group.name().equals("build100")).findFirst().orElse(null);

            if (newGroup != null && !groupsOfPlayer.contains(newGroup.uniqueId())) {
                permissionProvider.addPlayerToGroup(player.getUniqueId(), newGroup.uniqueId());
                MessageUtils.send(player, "<green>Du hast die 100 Blöcke-Marke erreicht und einen neuen Rang erhalten!");
            }
        }

        if (distance >= 1000) {
            Group newGroup = groups.stream().filter(group -> group.name().equals("build1000")).findFirst().orElse(null);

            if (newGroup != null && !groupsOfPlayer.contains(newGroup.uniqueId())) {
                permissionProvider.addPlayerToGroup(player.getUniqueId(), newGroup.uniqueId());
                MessageUtils.send(player, "<green>Du hast die 1000 Blöcke-Marke erreicht und einen neuen Rang erhalten!");
            }
        }

        if (distance >= 10000) {
            Group newGroup = groups.stream().filter(group -> group.name().equals("build10000")).findFirst().orElse(null);

            if (newGroup != null && !groupsOfPlayer.contains(newGroup.uniqueId())) {
                permissionProvider.addPlayerToGroup(player.getUniqueId(), newGroup.uniqueId());
                MessageUtils.send(player, "<green>Du hast die 10000 Blöcke-Marke erreicht und einen neuen Rang erhalten!");
            }
        }
    }
}
