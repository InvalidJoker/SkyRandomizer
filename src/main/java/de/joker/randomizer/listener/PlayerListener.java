package de.joker.randomizer.listener;

import de.joker.randomizer.data.PlayerData;
import de.joker.randomizer.manager.ScoreboardManager;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@Slf4j
public class PlayerListener implements Listener {

    private final ServiceManager serviceManager;
    private final ScoreboardManager scoreboardManager;

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
            player.teleport(
                    islandCenter.clone().add(0.5, 1, 0.5)
                            .setDirection(islandCenter.getDirection().setY(0))
            );
            scoreboardManager.showScoreboard(player);
        }, 1L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }

        if (to.getY() < 0.5) {
            Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);
            player.teleport(islandCenter.clone().add(0.5, 1, 0.5)
                    .setDirection(islandCenter.getDirection().setY(0)));

            MessageUtils.send(player, "<red>Du bist in die Leere gefallen! Du wirst zurück auf deine Insel teleportiert.");
        }
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);

        event.setRespawnLocation(islandCenter.clone().add(0.5, 1, 0.5)
                .setDirection(islandCenter.getDirection().setY(0)));
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
            updateDistance(player, distance);
            return;
        }

        event.setCancelled(true);
        player.teleport(islandCenter.clone().add(0.5, 1, 0.5)
                .setDirection(islandCenter.getDirection().setY(0)));
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
        player.teleport(islandCenter.clone().add(0.5, 1, 0.5)
                .setDirection(islandCenter.getDirection().setY(0)));
        MessageUtils.send(player, "<red>Du kannst keine Blöcke außerhalb deiner Insel abbauen!");
    }


    private void updateDistance(Player player, int currentX) {
        PlayerData playerData = serviceManager.getPlayerCache().getPlayer(player.getUniqueId());

        if (playerData == null) {
            log.warn("PlayerData not found for player {}", player.getName());
            return;
        }

        int prevMax = playerData.getDistance();

        if (currentX > prevMax) {
            serviceManager.getRanking().updatePlayer(player.getUniqueId(), player.getName(), currentX);

            scoreboardManager.updateForAllPlayers();

            player.sendMessage("§aNeue Rekordweite erreicht: §e" + currentX + " §aBlöcke!"); // TODO: remove this line in production
        }
    }
}