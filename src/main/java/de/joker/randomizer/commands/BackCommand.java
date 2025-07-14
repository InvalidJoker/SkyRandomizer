package de.joker.randomizer.commands;

import de.joker.randomizer.data.PlayerRank;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import dev.jorel.commandapi.CommandTree;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackCommand {
    private final ServiceManager serviceManager;
    private final Map<UUID, Instant> cooldownMap = new HashMap<>();

    public BackCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("back")
                .withRequirement(sender -> {
                    if (!(sender instanceof Player player)) {
                        return false;
                    }

                    return serviceManager.isBooster(player);
                })
                .withAliases("front", "return", "zurück")
                .executesPlayer((player, args) -> {
                    if (!serviceManager.getIslandManager().hasIsland(player)) {
                        MessageUtils.send(player, "<red>Du hast keine Insel, zu der du zurückkehren kannst!");
                        return;
                    }

                    Instant lastTeleport = cooldownMap.get(player.getUniqueId());
                    Instant now = Instant.now();
                    if (lastTeleport != null && now.isBefore(lastTeleport.plusSeconds(30)) && !player.hasPermission("realms.bypass")) {
                        MessageUtils.send(player, "<red>Du kannst erst in 30 Sekunden wieder zurück teleportieren!");
                        return;
                    }

                    Location location = serviceManager.getIslandManager().getOrCreateIsland(player);

                    World world = location.getWorld();
                    int startX = location.getBlockX();
                    int startZ = location.getBlockZ();
                    PlayerRank rank = serviceManager.getRanking().getRankOfPlayer(player.getUniqueId());
                    if (rank == null) {
                        MessageUtils.send(player, "<red>Dein Rang konnte nicht ermittelt werden!");
                        return;
                    }
                    int maxDistance = rank.getDistance();
                    int targetZ = startZ + maxDistance;

                    int lastGoodX = Integer.MIN_VALUE;
                    int lastGoodY = -1;

                    for (int x = startX - 3; x <= startX + 3; x++) {
                        for (int y = 140; y >= 40; y--) {
                            Block block = world.getBlockAt(x, y, targetZ);

                            if (!block.isEmpty()
                                    && !block.getType().isAir()
                                    && block.getType().isCollidable()
                                    && !block.getType().name().contains("LAVA")
                                    && !block.getType().name().contains("WATER")) {
                                if (y > lastGoodY) {
                                    lastGoodY = y;
                                    lastGoodX = x;
                                }
                                break;
                            }
                        }
                    }

                    if (lastGoodY == -1) {
                        MessageUtils.send(player, "<red>Es wurde kein solider Block gefunden, zu dem du teleportiert werden kannst!");
                        return;
                    }

                    Location teleportLocation = new Location(
                            world,
                            lastGoodX + 0.5,
                            lastGoodY + 1,
                            targetZ + 0.5,
                            player.getLocation().getYaw(),
                            player.getLocation().getPitch()
                    );

                    player.teleport(teleportLocation);
                    player.setFallDistance(0f);
                    cooldownMap.put(player.getUniqueId(), now);
                    player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    MessageUtils.send(player, "<green>Du wurdest zu deinem letzten Standort auf deiner Insel teleportiert!");
                });
    }
}
