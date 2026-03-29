package de.joker.randomizer.listener;

import de.cytooxien.realms.api.RealmPermissionProvider;
import de.joker.randomizer.data.IslandData;
import de.joker.randomizer.manager.ScoreboardManager;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import de.joker.randomizer.utils.SpectatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class PlayerListener implements Listener {

    private final ServiceManager serviceManager;
    private final ScoreboardManager scoreboardManager;
    private final Map<UUID, Instant> lastTeleportTimes = new HashMap<>();

    public PlayerListener(ServiceManager serviceManager, ScoreboardManager scoreboardManager) {
        this.serviceManager = serviceManager;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);
        IslandData islandData = serviceManager.getIslandManager().getIslandData(player);

        Bukkit.getScheduler().runTaskLater(serviceManager.getPlugin(), () -> {
            player.teleport(islandCenter.clone().add(0.5, 1, 0.5).setDirection(islandCenter.getDirection().setY(0)));
            lastTeleportTimes.put(player.getUniqueId(), Instant.now());

            serviceManager.getIslandManager().removeDisplay(player);
            if (islandData != null && islandData.getDistance() < 4) {
                serviceManager.getIslandManager().createBuildDisplay(player, islandCenter.getBlockX());
            }

            applyIslandProgress(player, islandData == null ? 0 : islandData.getDistance());
            scoreboardManager.showScoreboard(player);
        }, 1L);

        event.joinMessage(null);
        broadcastRaw("player.join", player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        serviceManager.getIslandManager().removeDisplay(player);
        scoreboardManager.removeScoreboard(player);
        lastTeleportTimes.remove(player.getUniqueId());

        event.quitMessage(null);
        broadcastRaw("player.quit", player.getName());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (SpectatorUtils.isSpectatorMode(event.getPlayer())) {
            return;
        }

        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }

        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        if (to.getY() < 15) {
            teleportToIsland(player, islandCenter);
            MessageUtils.send(player, "player.void_fall");
            return;
        }

        int deltaX = to.getBlockX() - islandCenter.getBlockX();
        int deltaZ = to.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) > 3 || deltaZ < -3) {
            teleportToIsland(player, islandCenter);
            MessageUtils.send(player, "player.move_limit");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);
        event.setRespawnLocation(islandCenter.clone().add(0.5, 1, 0.5).setDirection(islandCenter.getDirection().setY(0)));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (SpectatorUtils.isSpectatorMode(event.getPlayer())) {
            return;
        }

        Player player = event.getPlayer();
        Location blockLoc = event.getBlockPlaced().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        if (blockLoc.getBlockX() == islandCenter.getBlockX() && blockLoc.getBlockZ() == islandCenter.getBlockZ()) {
            event.setCancelled(true);
            MessageUtils.send(player, "player.place_spawn_block");
            return;
        }

        int deltaX = blockLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = blockLoc.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) <= 3 && deltaZ >= -3) {
            int distance = Math.max(Math.abs(deltaX), Math.max(deltaZ, 0));
            updateDistance(player, distance, islandCenter.getBlockX(), blockLoc.getBlockZ());
            return;
        }

        event.setCancelled(true);
        teleportToIsland(player, islandCenter);
        MessageUtils.send(player, "player.move_limit");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (SpectatorUtils.isSpectatorMode(event.getPlayer())) {
            return;
        }

        Player player = event.getPlayer();
        Location blockLoc = event.getBlock().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);
        int deltaX = blockLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = blockLoc.getBlockZ() - islandCenter.getBlockZ();

        Location checkLocation = blockLoc.clone().add(0, 0, 1);
        Block checkBlock = checkLocation.getBlock();

        Material[] allowedMaterials = {
                Material.BEDROCK, Material.BARRIER, Material.AIR, Material.LAVA, Material.WATER
        };

        if (!Arrays.asList(allowedMaterials).contains(checkBlock.getType())) {
            event.setCancelled(true);
            MessageUtils.send(player, "player.break_connected");
            return;
        }

        if (Math.abs(deltaX) <= 3 && deltaZ >= -3) {
            return;
        }

        event.setCancelled(true);
        teleportToIsland(player, islandCenter);
        MessageUtils.send(player, "player.break_outside");
    }

    private void applyIslandProgress(Player player, int distance) {
        RealmPermissionProvider permissionProvider = serviceManager.getPermissionProvider();
        if (permissionProvider != null) {
            serviceManager.getIslandManager().modifyPlayerGroup(player, permissionProvider, distance);
        }
    }

    private void updateDistance(Player player, int currentDistance, int islandX, int blockZ) {
        IslandData islandData = serviceManager.getIslandManager().getIslandData(player);
        if (islandData == null) {
            log.warn("IslandData not found for player {}", player.getName());
            return;
        }

        int prevMax = islandData.getDistance();
        if (currentDistance > prevMax) {
            serviceManager.getRanking().updatePlayer(player.getUniqueId(), player.getName(), currentDistance);

            if (currentDistance >= 4 && prevMax < 4) {
                serviceManager.getIslandManager().removeDisplays(islandData);
            }

            for (Player onlineMember : serviceManager.getIslandManager().getOnlineMembers(islandData)) {
                applyIslandProgress(onlineMember, currentDistance);
            }

            for (int y = 59; y <= 80; y++) {
                Location barrierLocation = new Location(player.getWorld(), islandX + 4, y, blockZ);
                barrierLocation.getBlock().setType(Material.BARRIER);
            }

            scoreboardManager.updateForAllPlayers();
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && lastTeleportTimes.containsKey(player.getUniqueId())) {
            Instant lastTeleport = lastTeleportTimes.get(player.getUniqueId());
            if (lastTeleport != null && Instant.now().minusSeconds(3).isBefore(lastTeleport)) {
                event.setCancelled(true);
                if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                    player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                }
                player.setFallDistance(0f);
                player.setVelocity(new Vector(0, 0, 0));
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    }

    private void teleportToIsland(Player player, Location islandCenter) {
        player.teleport(islandCenter.clone().add(0.5, 1, 0.5).setDirection(islandCenter.getDirection().setY(0)));
        player.setFallDistance(0f);
        player.setVelocity(new Vector(0, 0, 0));
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
        lastTeleportTimes.put(player.getUniqueId(), Instant.now());
    }

    private void broadcastRaw(String key, String playerName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendRaw(onlinePlayer, key, MessageUtils.placeholder("player", playerName));
        }
    }
}
