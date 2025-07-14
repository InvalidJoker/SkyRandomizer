package de.joker.randomizer.listener;

import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import de.joker.randomizer.utils.SpectatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

import java.util.Arrays;

@Slf4j
public class ExtraProtectionListener implements Listener {

    private final ServiceManager serviceManager;

    public ExtraProtectionListener(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;

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
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (SpectatorUtils.isSpectatorMode(event.getPlayer())) {
            return;
        }
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
        if (SpectatorUtils.isSpectatorMode(event.getPlayer())) {
            return;
        }

        Location loc = event.getBlockClicked().getLocation().clone().add(0, 0, 1);
        Block checkBlock = loc.getBlock();

        Material[] allowedMaterials = {
                Material.BEDROCK, Material.BARRIER, Material.AIR, Material.LAVA, Material.WATER
        };

        if (
                !Arrays.asList(allowedMaterials).contains(checkBlock.getType()) &&
                        event.getBlockClicked().getType() == Material.POWDER_SNOW
        ) {
            event.setCancelled(true);
            MessageUtils.send(event.getPlayer(), "<red>Du kannst keine Blöcke abbauen, die mit deiner Insel verbunden sind!");
            return;
        }

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
            if (SpectatorUtils.isSpectatorMode(event.getPlayer())) {
                return;
            }
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
        if (SpectatorUtils.isSpectatorMode(event.getPlayer())) {
            return;
        }
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
}