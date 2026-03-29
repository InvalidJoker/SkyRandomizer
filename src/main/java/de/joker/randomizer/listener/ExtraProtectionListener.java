package de.joker.randomizer.listener;

import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import de.joker.randomizer.utils.SpectatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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
                MessageUtils.send(shooter, "protection.attack_outside");
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
            MessageUtils.send(shooter, "protection.shoot_outside");
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
            MessageUtils.send(player, "protection.bucket_empty_outside");
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

        if (!Arrays.asList(allowedMaterials).contains(checkBlock.getType())
                && event.getBlockClicked().getType() == Material.POWDER_SNOW) {
            event.setCancelled(true);
            MessageUtils.send(event.getPlayer(), "protection.break_connected");
            return;
        }

        Player player = event.getPlayer();
        Location blockLoc = event.getBlockClicked().getLocation();
        Location islandCenter = serviceManager.getIslandManager().getOrCreateIsland(player);
        int deltaX = blockLoc.getBlockX() - islandCenter.getBlockX();
        int deltaZ = blockLoc.getBlockZ() - islandCenter.getBlockZ();

        if (Math.abs(deltaX) > 3 || deltaZ < -3) {
            event.setCancelled(true);
            MessageUtils.send(player, "protection.bucket_fill_outside");
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
                MessageUtils.send(player, "protection.interact_outside");
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
            MessageUtils.send(player, "protection.drop_outside");
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
            MessageUtils.send(player, "protection.pickup_outside");
        }
    }
}
