package de.joker.randomizer.manager;

import de.cytooxien.realms.api.RealmPermissionProvider;
import de.cytooxien.realms.api.model.Group;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.cache.PlayerCache;
import de.joker.randomizer.data.PlayerData;
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

    private final PlayerCache playerCache;
    private final Map<UUID, UUID> textDisplays;
    private final SkyRandomizer plugin;

    public IslandManager(PlayerCache playerCache, SkyRandomizer plugin) {
        this.playerCache = playerCache;
        this.textDisplays = new ConcurrentHashMap<>();
        this.plugin = plugin;
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

        for (int i = -4; i <= 1; i++) {
            for (int y = centerY - 5; y <= centerY + 20; y++) {
                Location barrierLocation = new Location(getWorld(), x + 4, y, i);
                barrierLocation.getBlock().setType(Material.BARRIER);
            }
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
        });

        textDisplays.put(player.getUniqueId(), display.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hideEntity(plugin, display);
        }

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