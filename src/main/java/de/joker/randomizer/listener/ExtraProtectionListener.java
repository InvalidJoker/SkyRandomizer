package de.joker.randomizer.listener;

import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

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
}