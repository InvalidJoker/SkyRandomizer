package de.joker.randomizer.listener;

import de.joker.randomizer.data.PlayerData;
import de.joker.randomizer.manager.ScoreboardManager;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
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
        serviceManager.getRanking().addPlayerIfNotExists(player.getUniqueId(), player.getName());

        Bukkit.getScheduler().runTaskLater(serviceManager.getPlugin(), () -> {
            player.teleport(islandCenter.clone().add(0.5, 1, 0.5).setDirection(islandCenter.getDirection().setY(0)));
            lastTeleportTimes.put(player.getUniqueId(), Instant.now());

            PlayerData playerData = serviceManager.getPlayerCache().getPlayer(player.getUniqueId());
            if (playerData != null && playerData.getDistance() < 4) {
                serviceManager.getIslandManager().createBuildDisplay(player, islandCenter.getBlockX());
            }

            scoreboardManager.showScoreboard(player);
        }, 1L);

        event.joinMessage(MessageUtils.parse("<green>" + player.getName() + " <gray>ist dem Spiel beigetreten!"));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        serviceManager.getIslandManager().removeDisplay(player);
        scoreboardManager.removeScoreboard(player);
        lastTeleportTimes.remove(player.getUniqueId());

        event.quitMessage(MessageUtils.parse("<red>" + player.getName() + " <gray>hat das Spiel verlassen!"));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }

        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        if (to.getY() < 15) {
            teleportToIsland(player, islandCenter);
            MessageUtils.send(player, "<red>Du bist in die Leere gefallen! Du wirst zurück auf deine Insel teleportiert.");
            return;
        }

        int deltaX = to.getBlockX() - islandCenter.getBlockX();
        int deltaZ = to.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) > 3 || deltaZ < -3) {
            teleportToIsland(player, islandCenter);
            MessageUtils.send(player, "<red>Du kannst dich nicht weiter als 3 Blöcke von deiner Insel nach links, rechts oder hinten entfernen!");
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
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }
        Player player = event.getPlayer();
        Location blockLoc = event.getBlockPlaced().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        if (blockLoc.getBlockX() == islandCenter.getBlockX() && blockLoc.getBlockZ() == islandCenter.getBlockZ()) {
            event.setCancelled(true);
            MessageUtils.send(player, "<red>Du kannst nicht direkt auf dem Spawn-Block bauen!");
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
        MessageUtils.send(player, "<red>Du kannst dich nicht weiter als 3 Blöcke von deiner Insel nach links, rechts oder hinten entfernen!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
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
            MessageUtils.send(player, "<red>Du kannst keine Blöcke abbauen, die mit deiner Insel verbunden sind!");
            return;
        }

        if (Math.abs(deltaX) <= 3 && deltaZ >= -3) {
            return;
        }

        event.setCancelled(true);
        teleportToIsland(player, islandCenter);
        MessageUtils.send(player, "<red>Du kannst keine Blöcke außerhalb deiner Insel abbauen!");
    }

    private void updateDistance(Player player, int currentDistance, int islandX, int blockZ) {
        PlayerData playerData = serviceManager.getPlayerCache().getPlayer(player.getUniqueId());
        if (playerData == null) {
            log.warn("PlayerData not found for player {}", player.getName());
            return;
        }

        int prevMax = playerData.getDistance();
        if (currentDistance > prevMax) {
            if (currentDistance >= 4 && prevMax < 4) {
                serviceManager.getIslandManager().removeDisplay(player);
            }
            serviceManager.getRanking().updatePlayer(player.getUniqueId(), player.getName(), currentDistance);

            for (int y = 59; y <= 69; y++) {
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
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
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
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        lastTeleportTimes.put(player.getUniqueId(), Instant.now());
    }
}