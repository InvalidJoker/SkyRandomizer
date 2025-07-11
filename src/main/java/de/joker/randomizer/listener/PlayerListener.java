package de.joker.randomizer.listener;

import de.joker.randomizer.data.PlayerData;
import de.joker.randomizer.manager.ScoreboardManager;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;

import java.time.Instant;
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
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        serviceManager.getIslandManager().removeDisplay(player);
        scoreboardManager.removeScoreboard(player);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
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
        Player player = event.getPlayer();
        Location blockLoc = event.getBlockPlaced().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

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
        Player player = event.getPlayer();
        Location blockLoc = event.getBlock().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        int deltaX = blockLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = blockLoc.getBlockZ() - islandCenter.getBlockZ();

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
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            Location hitLocation = event.getEntity().getLocation();
            Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(shooter);
            int deltaX = hitLocation.getBlockX() - islandCenter.getBlockX();
            int deltaZ = hitLocation.getBlockZ() - islandCenter.getBlockZ();

            if (Math.abs(deltaX) > 3 || deltaZ < -3) {
                event.setCancelled(true);
                MessageUtils.send(shooter, "<red>Du kannst nicht außerhalb deiner Insel angreifen!");
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }

        Location hitLoc = event.getEntity().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(shooter);

        int deltaX = hitLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = hitLoc.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) > 3 || deltaZ < -3) {
            event.setCancelled(true);
            MessageUtils.send(shooter, "<red>Du kannst nicht außerhalb deiner Insel schießen!");
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

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Location blockLoc = event.getBlockClicked().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        int deltaX = blockLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = blockLoc.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) > 3 || deltaZ < -3) {
            event.setCancelled(true);
            MessageUtils.send(player, "<red>Du kannst keine Eimer außerhalb deiner Insel leeren!");
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Location blockLoc = event.getBlockClicked().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        int deltaX = blockLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = blockLoc.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) > 3 || deltaZ < -3) {
            event.setCancelled(true);
            MessageUtils.send(player, "<red>Du kannst keine Eimer außerhalb deiner Insel füllen!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Player player = event.getPlayer();
            Location blockLoc = event.getClickedBlock().getLocation();
            Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

            int deltaX = blockLoc.getBlockX() - islandCenter.getBlockX();
            int deltaZ = blockLoc.getBlockZ() - islandCenter.getBlockZ();

            if (Math.abs(deltaX) > 3 || deltaZ < -3) {
                event.setCancelled(true);
                MessageUtils.send(player, "<red>Du kannst nicht außerhalb deiner Insel interagieren!");
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location dropLoc = event.getItemDrop().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        int deltaX = dropLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = dropLoc.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) > 3 || deltaZ < -3) {
            event.setCancelled(true);
            MessageUtils.send(player, "<red>Du kannst keine Items außerhalb deiner Insel droppen!");
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Location itemLoc = event.getItem().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        int deltaX = itemLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = itemLoc.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) > 3 || deltaZ < -3) {
            event.setCancelled(true);
            MessageUtils.send(player, "<red>Du kannst keine Items außerhalb deiner Insel aufsammeln!");
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